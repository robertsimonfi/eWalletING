# eWalletING — eIDAS Wallet Integration Sandbox

A hands-on sandbox built to prepare for an ING technical interview (IT Integrator / Solution Architect — eIDAS & eWallet Programme). The goal isn't a production system — it's real, runnable experience with six things the interview covers: SOAP→REST migration, API Gateway/routing, OAuth2/OIDC, TLS/certificates, the eIDAS 2.0 / EUDI Wallet domain, and Docker→Kubernetes scaling.

Where something here is simplified relative to the real spec or a real bank's stack, it's logged in [NOTES.md](NOTES.md) along with why the simplification is safe for learning purposes. Architectural and process decisions made while building this are logged in [DECISIONS.md](DECISIONS.md).

## Modules

| Module | Rehearses |
|---|---|
| [`legacy-core-soap/`](legacy-core-soap/) | Legacy SOAP core-banking stub (Spring-WS, WSDL contract) |
| [`core-facade-rest/`](core-facade-rest/) | REST facade over the SOAP service — anti-corruption layer + circuit breaker |
| [`api-gateway/`](api-gateway/) | Spring Cloud Gateway — routing, rate limiting, logging |
| `identity-provider/` | Keycloak config for OIDC |
| `customer-web/` | OAuth2 authorization-code + PKCE client |
| `pki/` | Mini CA + TLS/mTLS scripts |
| `credential-issuer/`, `mock-wallet-cli/`, `credential-verifier/` | Mock EUDI Wallet: issuance, selective disclosure, trust-list verification |
| `outbox-publisher/`, `kafka-consumer-demo/` | Transactional outbox → Kafka event backbone |
| `k8s/` | Deployment/Service/HPA manifests |

**Status:** Module 0 (Docker Compose baseline) and Module 1 (SOAP→REST→Gateway) are built and verified. Everything below `identity-provider/` in the table above hasn't been built yet — see `DECISIONS.md` for build order and `NOTES.md` for what's simplified so far.

## Stack

Java 21 + Spring Boot 3, Keycloak (OIDC), Kafka/Redpanda, Docker Compose (local dev), `kind`/`minikube` (Kubernetes stage).

## Running locally

```bash
docker compose up
```

Each service exposes a health endpoint; `docker compose ps` should show every service `healthy`.

## Non-goal

This is **not** spec-compliant with the real eIDAS Architecture Reference Framework, ISO 18013-5/mdoc, or OID4VP/OID4VCI in full. It's structurally faithful — same flow shapes, same crypto concepts (signed claims, selective disclosure, certificate chains) — so the mental model transfers to a real implementation.
