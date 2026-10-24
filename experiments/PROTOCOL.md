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
## Host and environment (recorded 2026-10-24)
- Windows 11 Home, build 26200. PowerShell 5.1.26100.9711.
- Host CPU: Intel Core i5-1235U, 10 cores, 12 logical processors. RAM 15.7 GB.
- Docker Desktop engine 29.8.2 (client and server). Docker VM limits: 12 CPUs, about 7.6 GiB memory.
- WSL 2, default distribution Ubuntu.
- Host Java: Temurin 21.0.12.1 (not used by the containers). Python 3.12.10. Git 2.36.1.
- Base image digests (match the build log prefixes):
  - eclipse-temurin@sha256:227118764cccd3c3294617061aaa93a08309c63a395eefa83e6b35032992ea99
  - maven@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320
- Container build tools: Maven 3.9.16, Java 21.0.12.1 (Eclipse Adoptium).
- Service B image ID used for the ablation runs: sha256:44603821a646c9add144892a04ba1854709221470729365890436d93f9bc4f90
- Spring Boot 3.2.5 (both services). Lombok version set by the Spring Boot parent.

## Ablation (added 2026-10-24)
- Detector modes, set by TENANTGUARD_DETECTOR_MODE:
  - VERIFIER: statement check, stale-leftover check, row check.
  - STATEMENT_ONLY: statement check and stale-leftover check.
  - ROW_ONLY: row check only.
  - HEADER_ONLY: no operation checks.
- Header checks (signature, missing context, header-body mismatch) run in all modes.
- Runs: abl-{VERIFIER, STATEMENT_ONLY, ROW_ONLY, HEADER_ONLY}-s{1,2,3}. 100 messages per condition. Conditions: NONE, F1, F2, F3, F4.
- Code commit containing the ablation: 19196b7135243702ae1ecc3780f7c22da53e2966
- Results: abl-VERIFIER matches main-VERIFIER, and abl-HEADER_ONLY matches main-HEADER_ONLY, for all seeds under the CORRECTED rule.
- Scoring: score_rules.ps1 writes scored_NAIVE.csv, scored_CORRECTED.csv, summary_NAIVE.csv, summary_CORRECTED.csv in each run folder. The legacy scored.csv files are unchanged in the abl folders.

## F2 labeling correction (recorded 2026-10-24)
- Original (NAIVE) rule: every F2 message expected positive.
- Corrected rule: F2 expected positive only when the previous message on the same worker had a different tenant. The first message in each condition is expected negative.
- The correction was made after the main runs had been scored. It was defined from the mechanism, not from detector output.
- Files and the rule they reflect:
  - main-* scored.csv: CORRECTED (overwritten on 2026-10-23; the NAIVE tables exist only in the scored_NAIVE.csv files produced by score_rules.ps1).
  - pilot-* scored.csv: NAIVE. Not yet rescored with score_rules.ps1.
  - abl-*, hdr-*, timing-*: both rules produced by score_rules.ps1 where run.
- Run 20261023-053327 is INVALID (request IDs in the ledger did not match the system). Excluded from all comparisons. Data kept unchanged.