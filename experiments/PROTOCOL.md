# Experiment Protocol (factual record)

Date of record: 2026-10-23
Commit at start of record: aca6de8de6fdc1d0e48ccda75d97029c8315f0c8

## System under test
- Java 21, Spring Boot 3.2.5, Spring AMQP, RabbitMQ 3.12-management (Docker)
- Producer: service-a. Consumer and verifier: service-b.
- Row store: in-memory (TenantRowStore). No database.
- Consumer executor: one worker thread (default).

## Detectors
- VERIFIER: signed-header verification, header/body check, statement check, row-owner check, stale-leftover check.
- HEADER_ONLY: signed-header verification and header/body check. Operation checks disabled.

## Conditions
- NONE: clean traffic.
- F1_MISSING_CONTEXT, F2_STALE_REUSE, F3_ASYNC_LOSS, F4_MISSING_FILTER: consumer-side faults, one per run, set by TENANTGUARD_FAULT_MODE.
- H1_FORGED_SIG: header signature is the string "forged".
- H2_BODY_MISMATCH: header signed for one tenant, body names another tenant.

## Ground truth (independent of detector output)
- The runner writes a ledger row for each message before analysis: condition, seed, detector, request ID, intended tenant.
- Request ID is taken from the service response, so the ledger holds IDs the system received.
- Expected positive: any message under F1, F3, F4, H1, H2.
- Expected positive for F2: only if the previous message on the same worker had a different tenant. The first message in each condition is expected negative.
- The F2 rule was changed after the main runs. The earlier rule labeled every F2 message as positive, which counted same-tenant transitions as missed detections. The change was made on 2026-10-23 and is recorded here.

## Workload
- Seeds 1, 2, 3. Seed controls tenant order and a 0 to 30 ms inter-message delay.
- Pilot runs: 20 messages per condition. Main runs: 100 messages per condition. Header runs: 100 per condition.

## Run folders (experiments/runs)
- 20261023-053327: INVALID. Request IDs in the ledger were not the IDs the system received. Do not use.
- 20261023-053956: pilot, VERIFIER, after the ID fix.
- pilot-VERIFIER-r2, pilot-VERIFIER-r3, pilot-HEADER_ONLY-r2, pilot-HEADER_ONLY-r3: pilot repeats.
- 20261023-054441: detector not confirmed from folder name. Check its ledger before citing.
- main-VERIFIER-s1..s3, main-HEADER_ONLY-s1..s3: main runs.
- hdr-VERIFIER-s1..s3, hdr-HEADER_ONLY-s1..s3: header-visible fault runs.
- timing-VERIFIER-s1..s3, timing-HEADER_ONLY-s1..s3: timing runs. detectionTimeMs is in microseconds in these runs only.

## Scoring
- score_run.ps1 joins observed to ledger by request ID and computes TP, FN, FP, TN, and unmatched counts.
- analyze.py: recall and false-positive rate with bootstrap intervals; paired McNemar on per-message correctness; workload identity check per seed.
- latency.py: P50, P95, P99 with bootstrap intervals.

## Known limitations
- F1 and F3 both produce CONTEXT_MISMATCH. The verifier detects them but does not tell them apart.
- F2: the first message after each worker restart cannot be detected.
- Ablation of the two operation checks (statement and row) has not been run.
- Latency is consumer-side only. No end-to-end or throughput measurement.
- Single host, Windows with Docker under WSL2.
- Only RabbitMQ and an in-memory store were tested. No database, no Kafka.