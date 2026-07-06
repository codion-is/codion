# Monitoring a Codion EntityServer with Prometheus & Grafana

The server can register its runtime metrics as JMX MBeans on the platform MBean server. Combined with
the [Prometheus JMX Exporter](https://github.com/prometheus/jmx_exporter) they become standard Prometheus
metrics, ready for Grafana dashboards, alerting and long-term retention — alongside, not instead of, the
Swing server monitor.

This directory contains:

| File | Purpose |
|------|---------|
| `jmx_exporter.yaml` | JMX Exporter config mapping the Codion MBeans to Prometheus metrics |
| `grafana-dashboard.json` | A starter Grafana dashboard |

## 1. Enable JMX on the server

The MBeans are opt-in and off by default. Start the server with:

```
-Dcodion.server.jmx=true
```

This registers three kinds of MBean on the platform MBean server:

| ObjectName | Attributes |
|------------|------------|
| `is.codion:type=EntityServer` | `RequestCount`, `ConnectionCount`, `ConnectionLimit` |
| `is.codion:type=ConnectionPool,username=<user>` | `Size`, `Available`, `InUse`, `Requests`, `FailedRequests`, `Created`, `Destroyed`, `AverageCheckOutTime` |
| `is.codion:type=OperationLatency,operation=<op>` | `Count`, `Sum`, `Buckets` (a duration histogram, one MBean per operation type as it is first served) |

The JVM's own thread/GC/CPU/memory numbers are standard platform MXBeans and are exported separately (see below),
so they are not duplicated here.

## 2. Attach the JMX Exporter agent

Download the [`jmx_prometheus_javaagent`](https://github.com/prometheus/jmx_exporter/releases) jar and add it
to the server JVM (the exporter also exports the standard `jvm_*` / `process_*` metrics automatically):

```
-javaagent:/path/to/jmx_prometheus_javaagent-0.20.0.jar=9940:/path/to/monitoring/jmx_exporter.yaml
```

Metrics are then served at `http://<server-host>:9940/metrics`. Verify with:

```
curl -s http://localhost:9940/metrics | grep codion_
```

> **Exporter version:** `jmx_exporter.yaml` is verified against the **0.20.x** javaagent. The 1.x line uses a
> newer OpenMetrics client that renames hand-assembled histograms (strips `_bucket`, appends `_total`); the
> counter/gauge metrics work there unchanged, but the latency histogram rules need adjusting. Stick with
> 0.20.x for the dashboard as shipped, or `curl` the endpoint and reconcile the histogram rules for 1.x.

## 3. Scrape with Prometheus

```yaml
scrape_configs:
  - job_name: codion
    static_configs:
      - targets: ['server-host:9940']
```

## 4. Import the Grafana dashboard

Grafana → Dashboards → New → Import → upload `grafana-dashboard.json`, then pick your Prometheus data source.
The dashboard covers request rate, connections vs limit, per-operation latency (p95) and throughput, per-pool
usage and check-out time, and JVM heap/GC/threads/CPU. `Operation` and `Pool user` template variables filter
by operation type and pool username.

## Metrics reference

| Metric | Type | Labels | Notes |
|--------|------|--------|-------|
| `codion_server_requests_total` | counter | | Total requests since startup |
| `codion_server_connections` | gauge | | Connected clients |
| `codion_server_connection_limit` | gauge | | `-1` if unlimited |
| `codion_connection_pool_requests_total` | counter | `username` | Reset by the monitor's reset action |
| `codion_connection_pool_failed_requests_total` | counter | `username` | |
| `codion_connection_pool_created_total` | counter | `username` | |
| `codion_connection_pool_destroyed_total` | counter | `username` | |
| `codion_connection_pool_size` / `_available` / `_in_use` | gauge | `username` | |
| `codion_connection_pool_checkout_time_seconds` | gauge | `username` | Average; 0 unless check-out time collection is enabled |
| `codion_operation_latency_seconds_bucket` | histogram | `operation`, `le` | `histogram_quantile()` for percentiles |
| `codion_operation_latency_seconds_count` / `_sum` | | `operation` | |

Example — p99 latency per operation:

```promql
histogram_quantile(0.99, sum by (le, operation) (rate(codion_operation_latency_seconds_bucket[5m])))
```

## Notes

- **Counter resets:** the pool counters are reset by the server monitor's *reset pool statistics* action.
  Prometheus's `rate()` handles counter resets, but avoid using that reset while continuously scraping.
- **Security:** the MBeans are local to the JVM and nothing is exposed until the exporter agent is attached.
  Keep the exporter's HTTP endpoint firewalled/ops-scoped — do not expose it publicly. No JMX remote connector
  is involved.
