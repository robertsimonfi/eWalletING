# Simplification Notes

Per the project brief: this sandbox is not spec-compliant with the real eIDAS Architecture Reference Framework or the real EU/enterprise protocols it borrows from. It's *structurally faithful* — same flow shapes, same crypto concepts — so the mental model transfers. This file tracks every place something was simplified, and why that simplification is safe for learning purposes. Being able to state this precisely is the actual interview material.

Entries are added as each module is built.

---

## Module 1 — `legacy-core-soap`

- **Fixed in-memory data, no persistence.** A real core-banking mainframe transaction hits a database/CICS region; here `getAccountBalance`/`getCustomerRecord` read from two hardcoded `Map`s. Safe to simplify because the point of this module is the *contract shape and transport* (WSDL, SOAP envelope, fault handling), not data access.
- **One fault type.** Real core-banking SOAP services distinguish many fault categories (validation, authorization, downstream timeout...). Here there's a single `recordNotFoundFault`/`RecordNotFoundException` mapped to a SOAP Client fault, enough to demonstrate the REST facade has to translate SOAP faults into HTTP status codes.
- **No WS-Security.** The brief calls out WS-Security as a real-world SOAP standard; this stub has no authentication on the SOAP endpoint at all. Left out because Module 2/3 cover auth and TLS properly at the gateway layer instead — layering security at the facade/gateway rather than the legacy service is itself realistic (legacy systems are often the reason a facade+gateway exists).

## Module 1 — `core-facade-rest`

- **Gotcha worth remembering for the interview:** Resilience4j's `ignoreExceptions` config only controls whether an exception counts toward the failure-rate math — it does **not** stop the annotation-driven `fallbackMethod` from being invoked. Every exception out of a `@CircuitBreaker`-guarded method (including a legitimate "not found" SOAP fault) goes through the fallback, so the fallback itself has to inspect the cause and rethrow business exceptions as-is rather than reporting them as "upstream unavailable". Verified live: without this check, a 404-shaped request came back as a 503.
- **Verified live, not just compiled:** ran `legacy-core-soap` + `core-facade-rest` together — confirmed happy-path JSON, a real 404 on a not-found account, then killed the SOAP process and watched the breaker go CLOSED → OPEN (80% failure rate over 5 calls, threshold 50%) → subsequent calls short-circuited instantly (`NOT_PERMITTED`, no network hit) → restarted the SOAP service → after the 15s open-state wait, a probe call transitioned it to HALF_OPEN → two more successful calls closed it. This is the exact "kill the SOAP service and narrate what happens" exercise the brief calls for.
- **Each SOAP consumer generates its own client stubs from the shared XSD contract**, rather than the facade depending on a shared library from `legacy-core-soap`. This is deliberate, not an oversight — it's how independent services actually consume a WSDL contract in practice (no compile-time coupling between service and consumer).
