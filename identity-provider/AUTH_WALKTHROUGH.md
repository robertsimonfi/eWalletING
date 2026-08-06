# OAuth2 flows, one HTTP call at a time

Two independent flows live in this realm (`ewalleting`, served by `identity-provider` on `http://localhost:8180`). Walk both by hand at least once — this is the exercise the brief asks for, and it's the fastest way to actually understand PKCE instead of reciting it.

Prerequisites: `identity-provider` running (`docker compose up -d identity-provider`), `openssl` and `curl` on your PATH.

---

## Flow 1 — Authorization code + PKCE (`customer-web`, a public client)

This is the one a human logs in through. `customer-web` never holds a secret — PKCE (Proof Key for Code Exchange) is what stops an attacker who intercepts the authorization code from exchanging it for a token themselves.

### 1. Generate a PKCE pair yourself

```bash
CODE_VERIFIER=$(openssl rand -base64 96 | tr -d '\n=+/' | cut -c1-64)
CODE_CHALLENGE=$(printf '%s' "$CODE_VERIFIER" | openssl dgst -sha256 -binary | openssl base64 | tr -d '=' | tr '+/' '-_')
echo "verifier:  $CODE_VERIFIER"
echo "challenge: $CODE_CHALLENGE"
```

The **verifier** is a secret only your client ever holds. The **challenge** is `base64url(SHA-256(verifier))` — a one-way fingerprint of it, safe to send over the browser redirect where anyone could see it.

### 2. Build the authorization URL and open it in a real browser

```bash
echo "http://localhost:8180/realms/ewalleting/protocol/openid-connect/auth?response_type=code&client_id=customer-web&redirect_uri=http://localhost:8083/login/oauth2/code/keycloak&scope=openid%20profile%20email&code_challenge=$CODE_CHALLENGE&code_challenge_method=S256&state=manual-test"
```

This step can't be curled — Keycloak's login form has CSRF protection tied to a browser session, same as any real bank login page. Paste the URL into a browser, log in as `robert` / `changeme123`. Keycloak redirects you to `http://localhost:8083/login/oauth2/code/keycloak?code=...&state=manual-test` — `customer-web` won't be running to catch it, so the browser will just show a connection error. That's fine: **copy the `code` value out of the URL bar** before dismissing it.

### 3. Exchange the code for tokens — this call proves PKCE

```bash
AUTH_CODE="<paste the code from step 2>"

curl -s -X POST http://localhost:8180/realms/ewalleting/protocol/openid-connect/token \
  -d "grant_type=authorization_code" \
  -d "client_id=customer-web" \
  -d "redirect_uri=http://localhost:8083/login/oauth2/code/keycloak" \
  -d "code=$AUTH_CODE" \
  -d "code_verifier=$CODE_VERIFIER" | python3 -m json.tool
```

No `client_secret` anywhere — the `code_verifier` is what proves this request comes from the same client that started the flow. **Try it again with the verifier changed by one character** and watch Keycloak reject it (`invalid_grant`). That single failed call is the entire point of PKCE, proven, not described.

### 4. Decode either token by hand

```bash
ACCESS_TOKEN="<paste access_token from step 3>"
echo "$ACCESS_TOKEN" | cut -d. -f2 | tr '_-' '/+' | base64 -d 2>/dev/null | python3 -m json.tool
```

(`cut -d. -f2` takes the middle segment of `header.payload.signature`; the `tr` fixes base64url → base64 alphabet before decoding.)

---

## Flow 2 — Client credentials (`api-gateway`, a confidential client)

No human, no browser, no authorization code — `api-gateway` proves *itself* with a client secret and gets a token representing the service, not a user.

```bash
curl -s -X POST http://localhost:8180/realms/ewalleting/protocol/openid-connect/token \
  -d "grant_type=client_credentials" \
  -d "client_id=api-gateway" \
  -d "client_secret=api-gateway-dev-secret" | python3 -m json.tool
```

Compare the decoded payload of *this* token against the one from Flow 1 — notice `preferred_username` is `service-account-api-gateway`, not `robert`, and there's no `nonce` (that's an OIDC/authentication concept; this is pure OAuth2 authorization, no identity involved).

---

## Exercise: call a protected endpoint with a valid, missing, and garbage token

Once `core-facade-rest` is running as a resource server (`docker compose up -d`):

```bash
# No token — expect 401
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/api/accounts/ACC-1001/balance

# Garbage token — expect 401
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer not-a-real-token" \
  http://localhost:8082/api/accounts/ACC-1001/balance

# Valid token from Flow 1 or Flow 2 — expect 200
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8082/api/accounts/ACC-1001/balance
```

## Exercise: rotate the signing key, watch old tokens die (and learn why "watch" needs a restart)

In the Keycloak admin console (`http://localhost:8180`, login `admin`/`admin`) → realm `ewalleting` → **Realm settings → Keys → Providers → Create provider → rsa-generated**, save it with a higher priority than the existing one (e.g. `200` vs the default `100`). New tokens now carry a new `kid` — confirm by decoding one (step 4, Flow 1) and checking the header.

Grab a token minted just *before* you rotated, then delete the old key provider entirely (**Providers → the old `rsa-generated` row → Delete**). Check Keycloak's own key list — the old key is gone immediately, no grace period:

```bash
curl -s http://localhost:8180/realms/ewalleting/protocol/openid-connect/certs | python3 -m json.tool
```

Now retry the old token against `core-facade-rest` — **it likely still gets a `200`.** That's not a bug: Spring's `NimbusJwtDecoder` caches the JWK set in memory rather than fetching it fresh per request, so `core-facade-rest` is still verifying against a stale local copy of the old key that no longer exists at Keycloak. Restart `core-facade-rest` (`docker compose restart core-facade-rest`) to force a fresh fetch, *then* retry the same old token — now it fails.

That gap between "revoked at the IdP" and "actually stops working everywhere" is the real lesson here: it's not instantaneous, and it's bounded by every downstream service's own cache lifetime, not the IdP's. That's a genuinely good answer if asked how you'd respond to a compromised signing key in production.
