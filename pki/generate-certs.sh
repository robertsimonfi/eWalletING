#!/usr/bin/env bash
# Generates a mini two-tier CA (root -> intermediate -> leaf certs) and
# everything it signs for Module 3. Fully re-runnable — certs here are
# learning artifacts, not secrets to preserve, so this script always starts
# clean rather than trying to detect/reuse what's already there.
#
# Root -> intermediate -> leaf (not root -> leaf directly) is deliberate:
# it's how real PKI hierarchies actually work (the root stays offline/unused
# day-to-day), and it's the only way "truncate the chain" is a meaningful
# exercise — with a single root signing leaves directly, omitting the root
# from what the server sends doesn't break anything, since the client already
# has that exact root cached locally.
set -euo pipefail
cd "$(dirname "$0")"

# Git Bash on Windows auto-converts anything that looks like a POSIX path —
# including openssl "-subj /C=NL/O=.../CN=..." strings — into a Windows path
# behind the scenes. Disable that for this script.
export MSYS_NO_PATHCONV=1

OUT=certs
STOREPASS="changeit"

rm -rf "$OUT"
mkdir -p "$OUT"

ROOT_KEY="$OUT/root-ca.key"
ROOT_CERT="$OUT/root-ca.crt"
INT_KEY="$OUT/intermediate-ca.key"
INT_CERT="$OUT/intermediate-ca.crt"
# What every client actually trusts. Only the root — the intermediate is
# something servers must present as part of their own chain, not something
# clients pre-trust directly.
CA_CERT="$ROOT_CERT"

echo "== Root CA: key + self-signed certificate (offline in a real PKI; here just 'the trust anchor') =="
openssl genrsa -out "$ROOT_KEY" 4096
openssl req -x509 -new -nodes -key "$ROOT_KEY" -sha256 -days 3650 \
  -subj "/C=NL/O=eWalletING Sandbox/CN=eWalletING Root CA" \
  -addext "basicConstraints=critical,CA:true" \
  -addext "keyUsage=critical,keyCertSign,cRLSign" \
  -out "$ROOT_CERT"

echo "== Intermediate CA: key + CSR, signed by the root =="
openssl genrsa -out "$INT_KEY" 4096
openssl req -new -key "$INT_KEY" \
  -subj "/C=NL/O=eWalletING Sandbox/CN=eWalletING Intermediate CA" \
  -out "$OUT/intermediate.csr"
INT_EXT="$OUT/intermediate.ext.cnf"
printf "basicConstraints=critical,CA:true,pathlen:0\nkeyUsage=critical,keyCertSign,cRLSign\n" > "$INT_EXT"
openssl x509 -req -in "$OUT/intermediate.csr" \
  -CA "$ROOT_CERT" -CAkey "$ROOT_KEY" -CAcreateserial \
  -out "$INT_CERT" -days 1825 -sha256 -extfile "$INT_EXT"
rm -f "$INT_EXT" "$OUT/intermediate.csr"

# $1 = identity name (used for filenames), $2 = CN, $3 = subjectAltName value,
# $4 = extendedKeyUsage (serverAuth or clientAuth)
issue_cert() {
  local name=$1 cn=$2 sans=$3 eku=$4
  echo "== Issuing $name (CN=$cn, EKU=$eku), signed by the intermediate =="
  openssl genrsa -out "$OUT/$name.key" 2048
  openssl req -new -key "$OUT/$name.key" \
    -subj "/C=NL/O=eWalletING Sandbox/CN=$cn" \
    -out "$OUT/$name.csr"

  local extfile="$OUT/$name.ext.cnf"
  printf "subjectAltName=%s\nextendedKeyUsage=%s\nbasicConstraints=CA:false\n" "$sans" "$eku" > "$extfile"

  openssl x509 -req -in "$OUT/$name.csr" \
    -CA "$INT_CERT" -CAkey "$INT_KEY" -CAcreateserial \
    -out "$OUT/$name.crt" -days 397 -sha256 -extfile "$extfile"
  rm -f "$extfile" "$OUT/$name.csr"

  # Full chain a server/client actually presents on the wire: leaf + intermediate
  # (NOT the root — peers already trust the root directly, don't need it repeated).
  cat "$OUT/$name.crt" "$INT_CERT" > "$OUT/$name-chain.pem"

  # PKCS12 keystore: this identity's private key + its chain, for Spring Boot's
  # server.ssl.key-store / the mTLS client SSLContext.
  openssl pkcs12 -export -in "$OUT/$name-chain.pem" -inkey "$OUT/$name.key" \
    -name "$name" -out "$OUT/$name.p12" -passout pass:"$STOREPASS"
}

issue_cert "api-gateway-server"  "api-gateway"       "DNS:api-gateway,DNS:localhost,IP:127.0.0.1"  "serverAuth"
issue_cert "core-facade-server"  "core-facade-rest"  "DNS:core-facade-rest,DNS:localhost,IP:127.0.0.1" "serverAuth"
issue_cert "api-gateway-client"  "api-gateway-client" "DNS:api-gateway,DNS:localhost"               "clientAuth"

echo "== Truststore: just the ROOT certificate — anyone verifying a peer's chain trusts this, and only this =="
keytool -importcert -noprompt -trustcacerts -alias ewalleting-root-ca \
  -file "$ROOT_CERT" -keystore "$OUT/truststore.p12" -storetype PKCS12 -storepass "$STOREPASS"

echo "== Deliberately expired server cert, for the 'what breaks and why' exercise =="
openssl genrsa -out "$OUT/api-gateway-server-expired.key" 2048
openssl req -new -key "$OUT/api-gateway-server-expired.key" \
  -subj "/C=NL/O=eWalletING Sandbox/CN=api-gateway" \
  -out "$OUT/expired.csr"
EXT_EXPIRED="$OUT/expired.ext.cnf"
printf "subjectAltName=DNS:api-gateway,DNS:localhost,IP:127.0.0.1\nextendedKeyUsage=serverAuth\nbasicConstraints=CA:false\n" > "$EXT_EXPIRED"
openssl x509 -req -in "$OUT/expired.csr" \
  -CA "$INT_CERT" -CAkey "$INT_KEY" -CAcreateserial \
  -out "$OUT/api-gateway-server-expired.crt" -sha256 -extfile "$EXT_EXPIRED" \
  -not_before 20200101000000Z -not_after 20200201000000Z
rm -f "$EXT_EXPIRED" "$OUT/expired.csr"
cat "$OUT/api-gateway-server-expired.crt" "$INT_CERT" > "$OUT/api-gateway-server-expired-chain.pem"
openssl pkcs12 -export -in "$OUT/api-gateway-server-expired-chain.pem" -inkey "$OUT/api-gateway-server-expired.key" \
  -name "api-gateway-server-expired" -out "$OUT/api-gateway-server-expired.p12" -passout pass:"$STOREPASS"

echo "== Truncated chain, for the other exercise: leaf cert with the intermediate stripped off =="
cp "$OUT/api-gateway-server.crt" "$OUT/api-gateway-server-leaf-only.pem"
openssl pkcs12 -export -in "$OUT/api-gateway-server.crt" -inkey "$OUT/api-gateway-server.key" \
  -name "api-gateway-server-truncated" -out "$OUT/api-gateway-server-truncated.p12" -passout pass:"$STOREPASS"

echo
echo "Done. Everything is under pki/certs/ (gitignored). Keystore/truststore password: $STOREPASS"
