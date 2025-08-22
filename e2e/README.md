# E2E Tests (Playwright)

This project uses Playwright test runner (TypeScript) for black-box integration flows. We do not drive a browser UI; we use Playwright's test harness, fixtures, and parallelism to interact with Kafka/ActiveMQ and assert effects (via consumers and Spanner queries).

## Setup
- Install Node.js >= 18
- From repo root:
  - `npm init -y` (first time)
  - `npm i -D @playwright/test kafkajs @google-cloud/spanner dotenv` 
- Configure `.env` in `e2e/` as needed (Kafka brokers, Spanner emulator host, ActiveMQ broker URL).

## Run
- `npx playwright test`

## What tests do
- Produce a malformed ISO 20022 XML message to the router's ActiveMQ input queue.
- Assert router publishes to `exception-queue` and `pacs002-requests` Kafka topic.
- Consume `payment-events` to ensure event JSON emitted.
- Query Spanner emulator to verify `UnifiedMessages` and `Pacs002Messages` written.
- Consume ActiveMQ outbound `pacs002` queue to assert pacs.002 XML produced.
