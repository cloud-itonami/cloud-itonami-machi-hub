# Contributing

`cloud-itonami-machihub` accepts contributions to the OSS blueprint, capability
bindings, policy tests, documentation and operator model.

## Development
The capability layer lives in `kotoba-lang/property`. This repo holds the
business blueprint, the HubAdvisor ⊣ HubGovernor actor (`src/machi.hub/`)
and operator contracts.

```bash
kbb -M:dev:test
kbb -M:lint
```

## Rules
- Do not commit real tenant, parcel-owner, buyer/seller or financial data.
- Keep listings, leases, disclosures and closings behind the
  HubGovernor -- a closing/escrow disbursement never bypasses it.
- Treat property and closing workflows as high-risk: add tests for parcel
  identity, term-overlap, lease, disclosure, sanctions/KYC and audit
  logging.
- Do not add a jurisdiction to `machi.hub.facts/catalog` without a real,
  citable official land-registry source.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests
PRs should describe: what behavior changed, which policy invariant is
affected, how it was tested, whether operator or certification docs need
updates.
