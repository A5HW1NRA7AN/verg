# Telephony Service (AMI ingest)

This is a **standalone microservice** that connects to your FreePBX/Asterisk instance via **AMI** and generates **missed-call lead** records + optional outbound POST to your lead registry.

It is intentionally **decoupled** from the VERG core service:

- VERG does not need to import or compile this code.
- This service does not call VERG APIs (unless you later choose to).

## Run

From `telephony/service/`:

```bash
mvn spring-boot:run
```

Configure AMI + lead registry in `telephony/service/src/main/resources/application.properties` (or via environment variables).

## Docs

- See [`../infra/AMI_SETUP.md`](../infra/AMI_SETUP.md) for PBX manager user + network setup.
- See [`../infra/CARRIER_PORTABILITY.md`](../infra/CARRIER_PORTABILITY.md) for Twilio → carrier migration notes.

