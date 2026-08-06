# Simplification Notes

Per the project brief: this sandbox is not spec-compliant with the real eIDAS Architecture Reference Framework or the real EU/enterprise protocols it borrows from. It's *structurally faithful* — same flow shapes, same crypto concepts — so the mental model transfers. This file tracks every place something was simplified, and why that simplification is safe for learning purposes. Being able to state this precisely is the actual interview material.

Entries are added as each module is built.

---

## Module 1 — `legacy-core-soap`

- **Fixed in-memory data, no persistence.** A real core-banking mainframe transaction hits a database/CICS region; here `getAccountBalance`/`getCustomerRecord` read from two hardcoded `Map`s. Safe to simplify because the point of this module is the *contract shape and transport* (WSDL, SOAP envelope, fault handling), not data access.
- **One fault type.** Real core-banking SOAP services distinguish many fault categories (validation, authorization, downstream timeout...). Here there's a single `recordNotFoundFault`/`RecordNotFoundException` mapped to a SOAP Client fault, enough to demonstrate the REST facade has to translate SOAP faults into HTTP status codes.
- **No WS-Security.** The brief calls out WS-Security as a real-world SOAP standard; this stub has no authentication on the SOAP endpoint at all. Left out because Module 2/3 cover auth and TLS properly at the gateway layer instead — layering security at the facade/gateway rather than the legacy service is itself realistic (legacy systems are often the reason a facade+gateway exists).
