# Security Policy

## Supported versions

Codion is in its final API refinement phase before its `1.0` promotion. Security
fixes are applied to the latest released version. Until promotion, there is no
long-term support branch — please track the most recent release.

## Reporting a vulnerability

Please report security vulnerabilities **privately**, not via public issues.

- Preferred: [GitHub private vulnerability reporting](https://github.com/codion-is/codion/security/advisories/new)
  (Security → Report a vulnerability).
- Alternatively, email **bjorndarri@gmail.com** with `[codion-security]` in the
  subject.

Please include the affected version, the deployment configuration (transport,
filter configuration, authentication), and a reproduction or proof of concept if
available. You will receive an acknowledgement, and a fix or mitigation will be
coordinated before any public disclosure.

Codion is open-source but **not open-contribution** — please report the issue
rather than opening a pull request.

## Security model

Codion is a framework for building internal business and scientific applications,
typically serving 1–10 users (and capable of thousands) on a trusted network. Its
security model reflects that deployment context.

- **Authentication & authorization are delegated to the database** by default.
  Codion does not implement its own user store; access control is enforced by
  database roles (e.g. a read-only role and a read-write role). An application may
  add an `Authenticator` to perform additional checks at connection time.
- **The server is intended to run on a trusted network** (behind a VPN for remote
  access). It is not designed to be exposed directly to the public internet.
- **Transport is encrypted by default.** Both the RMI transport
  (`codion.server.connection.sslEnabled`, default `true`) and the HTTP transport
  (`codion.server.http.secure`, default `true`) use SSL/TLS out of the box.

## Java deserialization

Codion supports remote connections over RMI and over HTTP. The RMI transport and
the *optional* serialization-based HTTP transport exchange Java-serialized objects,
which means they perform Java deserialization of data received from clients. This
is the relevant attack surface for deserialization vulnerabilities, and Codion is
designed to be **safe by default** against it.

### Defenses, and what they mean for a default deployment

1. **JSON is the default HTTP transport; Java serialization over HTTP is opt-in.**
   `codion.server.http.json` defaults to `true` and
   `codion.server.http.serialization` defaults to `false`. The serialization
   endpoints — and therefore every server-side `readObject()` on an HTTP request
   body — are only registered when an operator explicitly enables them. The JSON
   transport performs no Java deserialization of request bodies.

2. **A deserialization filter is mandatory by default.** The server configures a
   JVM-wide [`ObjectInputFilter`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/ObjectInputFilter.html)
   (JEP 290) from a configured `ObjectInputFilterFactory`, and
   `codion.server.objectInputFilterFactoryRequired` defaults to `true`. **If no
   filter factory is configured, the server refuses to start.** This filter applies
   to *all* deserialization in the process, covering both the RMI transport and the
   serialization-based HTTP transport. Disabling the requirement is an explicit,
   documented, non-default action (`...objectInputFilterFactoryRequired=false`,
   "not recommended for production").

3. **Resource-exhaustion limits.** The built-in `SerializationFilterFactory`
   prepends JEP 290 resource limits (`maxbytes`, `maxarray`, `maxdepth`, `maxrefs`)
   enforced *during* deserialization, defending against deserialization-bomb /
   resource-exhaustion attacks regardless of class-level filtering.

As a result, reaching an unfiltered server-side deserialization requires an
operator to take **deliberate, non-default actions** — for example, enabling the
serialization HTTP transport *and* either disabling the mandatory filter or
configuring a deliberately permissive filter. A stock deployment does not expose
an unfiltered deserialization path.

### Why there is no hard-coded allowlist

Codion deserializes application domain objects — the values carried by `Entity`
attributes can be *any* `Serializable` type a given domain defines (custom value
objects, enums, temporal types, numeric types, records, and so on). The framework
cannot know these types at compile time, so a fixed, framework-supplied class
allowlist would break legitimate applications. Codion therefore delegates the
allowlist to the deployer, who knows their domain, while guaranteeing that *some*
filter must be present before the server will run.

The framework provides tooling to make building that allowlist straightforward:
a [serialization filter dry-run mode](documentation/src/docs/asciidoc/technical/server.adoc)
records every class deserialized during a representative run and writes it to a
file that can be used directly as the filter's pattern file.

### Static-analysis note

Static analysis tools (e.g. CodeQL's "Deserialization of user-controlled data")
flag the `readObject()` call in
`framework/servlet/src/main/java/is/codion/framework/servlet/EntityService.java`.
This is a known finding. The call is on the opt-in serialization HTTP transport
(disabled by default) and is governed by the mandatory JVM-wide `ObjectInputFilter`
described above. The deployment-side mitigation is the configured deserialization
filter; see the source comment at the call site and the configuration guide below.

## Hardening checklist

For production deployments:

- **Prefer the JSON transport** (`codion.server.http.json=true`,
  `codion.server.http.serialization=false`, both default) unless you specifically
  need Java serialization over HTTP on a trusted network.
- **Keep the filter requirement on** (`codion.server.objectInputFilterFactoryRequired=true`,
  default) and configure an `ObjectInputFilterFactory` — typically the built-in
  `is.codion.common.rmi.server.SerializationFilterFactory` with a whitelist built
  via the dry-run workflow.
- **Keep the resource-exhaustion limits** at sensible values for your payloads.
- **Keep SSL/TLS enabled** for the RMI and HTTP transports (both default `true`).
- **Run the server on a trusted network** / behind a VPN; do not expose it directly
  to the public internet.
- **Use least-privilege database roles** for application connections.

See `documentation/src/docs/asciidoc/technical/server.adoc` ("Serialization
filtering") for the complete configuration reference, including pattern files,
resource limits, and the dry-run workflow.
