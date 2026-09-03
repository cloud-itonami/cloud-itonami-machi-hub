# Security Policy

This project handles vacant-property conversion proposals for urban micro
logistics hubs. The demo data is synthetic and shape-only (no personal
data by design, G3), but treat vulnerabilities as potentially high
impact once a live instance ingests real listings.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- personal data leakage into proposals or ledgers (G3 violation)
- authorization bypass
- HubGovernor bypass
- audit-ledger tampering
- jurisdiction-facts fabrication (an advisor inventing land-use law)

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on buyer/seller/tenant data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real buyer/seller/tenant data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
