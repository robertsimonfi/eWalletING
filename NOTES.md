# Simplification Notes

Per the project brief: this sandbox is not spec-compliant with the real eIDAS Architecture Reference Framework or the real EU/enterprise protocols it borrows from. It's *structurally faithful* — same flow shapes, same crypto concepts — so the mental model transfers. This file tracks every place something was simplified, and why that simplification is safe for learning purposes. Being able to state this precisely is the actual interview material.

Entries are added as each module is built.

---

## Module 0 — Docker Compose baseline

- **`apt-get install curl` in the runtime stage of every Dockerfile**, purely so `docker compose`'s healthcheck has something to call `/actuator/health` with. A leaner production image would use a `HEALTHCHECK` built on something already in the JRE base image (or Spring Boot's own process-exit-code liveness) instead of pulling in a package manager layer — traded a slightly heavier image for a healthcheck that's trivial to read and debug.
- **`depends_on: condition: service_healthy`** chains the startup order (soap → facade → gateway) so a service never starts routing before what it depends on is actually ready to answer, not just "container started." Verified live: `docker compose up` brought all three up in that order and every one reported `healthy`.

## Module 1 — `legacy-core-soap`

- **Fixed in-memory data, no persistence.** A real core-banking mainframe transaction hits a database/CICS region; here `getAccountBalance`/`getCustomerRecord` read from two hardcoded `Map`s. Safe to simplify because the point of this module is the *contract shape and transport* (WSDL, SOAP envelope, fault handling), not data access.
- **One fault type.** Real core-banking SOAP services distinguish many fault categories (validation, authorization, downstream timeout...). Here there's a single `recordNotFoundFault`/`RecordNotFoundException` mapped to a SOAP Client fault, enough to demonstrate the REST facade has to translate SOAP faults into HTTP status codes.
- **No WS-Security.** The brief calls out WS-Security as a real-world SOAP standard; this stub has no authentication on the SOAP endpoint at all. Left out because Module 2/3 cover auth and TLS properly at the gateway layer instead — layering security at the facade/gateway rather than the legacy service is itself realistic (legacy systems are often the reason a facade+gateway exists).

## Module 1 — `core-facade-rest`

- **Gotcha worth remembering for the interview:** Resilience4j's `ignoreExceptions` config only controls whether an exception counts toward the failure-rate math — it does **not** stop the annotation-driven `fallbackMethod` from being invoked. Every exception out of a `@CircuitBreaker`-guarded method (including a legitimate "not found" SOAP fault) goes through the fallback, so the fallback itself has to inspect the cause and rethrow business exceptions as-is rather than reporting them as "upstream unavailable". Verified live: without this check, a 404-shaped request came back as a 503.
- **Verified live, not just compiled:** ran `legacy-core-soap` + `core-facade-rest` together — confirmed happy-path JSON, a real 404 on a not-found account, then killed the SOAP process and watched the breaker go CLOSED → OPEN (80% failure rate over 5 calls, threshold 50%) → subsequent calls short-circuited instantly (`NOT_PERMITTED`, no network hit) → restarted the SOAP service → after the 15s open-state wait, a probe call transitioned it to HALF_OPEN → two more successful calls closed it. This is the exact "kill the SOAP service and narrate what happens" exercise the brief calls for.
- **Each SOAP consumer generates its own client stubs from the shared XSD contract**, rather than the facade depending on a shared library from `legacy-core-soap`. This is deliberate, not an oversight — it's how independent services actually consume a WSDL contract in practice (no compile-time coupling between service and consumer).

## Module 1 — `api-gateway`

- **Blocking (WebMVC) Gateway, not the classic reactive one.** Spring Cloud Gateway historically runs on WebFlux/Netty. Since every other service in this sandbox is a plain servlet-stack Spring MVC app, `spring-cloud-starter-gateway-server-webmvc` was used instead — one thread-per-request model end to end, no need to introduce a second, reactive runtime just for the gateway. Worth being able to explain the tradeoff: the reactive gateway handles far more concurrent connections per instance under I/O-bound load; the WebMVC one is simpler to reason about and matches the rest of this stack.
- **Rate limiting is in-memory and per-instance, not Redis-backed.** Fine for one gateway replica; explicitly breaks once there's more than one (Module 6) — each replica would enforce its own window, so the real ceiling becomes `limit × replica count` instead of `limit`. A real gateway needs a shared store (Redis `INCR`+TTL, or a managed rate-limit service) for the limit to hold under horizontal scaling. Flagged in code, not just here, because this is exactly the kind of tradeoff worth surfacing unprompted in the interview.
- **Verified live, full chain:** started all three Module 1 services together and drove requests through `:8080` (gateway) only — confirmed routing to `core-facade-rest`, a real 404 on a not-found account, the access log capturing method/path/status/latency for every request, the rate limiter returning 429 after 20 requests in a 10s window, and — the brief's actual exercise — killed `legacy-core-soap` and watched a clean 503 propagate through gateway → facade → client with the circuit breaker still doing the same CLOSED→OPEN→short-circuit behavior recorded in the `core-facade-rest` entry above, now proven through the whole stack rather than just at the facade.
