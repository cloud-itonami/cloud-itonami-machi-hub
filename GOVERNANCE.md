# Governance

`cloud-itonami-machihub` is an OSS open-business blueprint for converting
vacant houses / rooms into urban micro logistics hubs (proposal plane).

## Maintainers
Maintainers may merge changes that preserve these invariants:
- proposals without a jurisdiction facts entry can never commit
  (fail closed on land-use).
- conversion drafts can never auto-commit at any phase.
- lease signing, municipal filing, renovation start and delivery start
  are forbidden effects — the actor stays a proposal plane.
- no owner/tenant personal data rides proposals or ledgers (G3).
- the HubGovernor remains independent of the HubAdvisor advisor.
- hard policy violations (no-spec-basis, unverified-zoning,
  actuation-claimed, personal-data) cannot be overridden by human approval.
- every proposal, assessment, approval and hold path is auditable
  (append-only ledger).
- personal data and credentials stay outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model, storage contract, public business model, operator certification or license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is a separate trust mark and should require security, audit and data-flow review.

Certified operators can lose certification for:
- bypassing listing or lease policy checks
- mishandling tenant data
- misrepresenting certification status
- failing to respond to security incidents
