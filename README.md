# cloud-itonami-machi-hub

Open Business Blueprint for **ISIC Rev.5 6810** (real-estate activities,
machinery-and-equipment-free): the **machi-hub** vertical — assessing
vacant houses / vacant rooms for conversion into **urban micro
logistics hubs** (last-mile pickup points + parcel staging rooms) and
publishing the conversion *proposal* — as an OSS business any qualified,
licensed operator can fork, deploy, run, improve and sell.

Built on this workspace's
[`langgraph-clj`](https://github.com/com-junkawasaki/langgraph-clj)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, in-mem checkpoints) — the same actor pattern as
`cloud-itonami-isic-6810` (Realtor-LLM ⊣ RealtorGovernor),
`cloud-itonami-isic-5210` (TerminalAdvisor ⊣ Terminal Storage Governor)
and `cloud-itonami-isic-5320` (courier). Here it is
**HubAdvisor ⊣ HubGovernor** (`:machi-hub-governor`, grep-unique
fleet-wide).

> **Why an actor layer at all?** An LLM is good at summarizing a vacant
> site and drafting a conversion pitch — but it has **no notion of which
> jurisdiction's land-use rules are official, no authority to sign a
> lease or file with a municipality, and no business starting a
> renovation**. Sealing it behind an independent HubGovernor with
> fail-closed jurisdiction facts and a human approval workflow keeps the
> proposal honest and the liability where it belongs: with the operator.

## Scope: what this actor does and does not do

This actor drafts and governs a **conversion-proposal workflow**: site
intake (shape-only — no personal data), per-jurisdiction land-use
checklist references, feasibility + score computation, and a conversion
proposal draft. It does **not** sign leases, file applications, start
renovations, or operate deliveries, and it does not claim to. Real
logistics execution belongs to ISIC 5320 (courier) / `kotoba-lang/soko`
(fulfillment); warehouse-floor robotics belongs to `kuramori`.

## Delegation boundary (do not re-implement here)

| Concern | Owner |
|---|---|
| Real last-mile delivery | `cloud-itonami-isic-5320` |
| Stock / shipment / fulfillment | `kotoba-lang/soko` |
| Warehouse intralogistics robotics | `cloud-itonami/kuramori` (R0 sim only) |
| Terminal/depot scale storage | `cloud-itonami-isic-5210` |
| **This repo** | vacant-property → micro-hub **conversion judgment** |

## Honesty rules

- A jurisdiction with no entry in `machi.hub.registry/jurisdiction-catalog`
  has NO spec basis — `feasible?` returns `:feasible? nil` with
  `:unverified [:zoning-use]`, and the governor HARD-holds. Coverage is
  reported, never padded.
- Every conversion draft carries `:committed false` and `:kind "draft"`.
  Signing, filing and renovation are forbidden effects
  (`machi.hub.governor/forbidden-effects`).
- No owner/tenant personal data rides a proposal or the seed data (G3).

## Layout

```
src/machi/hub/
├── registry.cljc    # pure feasibility + scoring + draft records
├── facts.cljc       # spec-basis layer the governor re-checks
├── store.cljc       # MemStore SSoT + append-only ledger
├── hubadvisor.cljc  # contained intelligence node (mock; LLM-swappable)
├── governor.cljc    # independent censor — HARD G1-G4 + soft escalation
├── phase.cljc       # 0→3 rollout; conversions never auto-commit
├── operation.cljc   # langgraph StateGraph (intake→advise→govern→decide)
└── sim.cljc         # demo driver — walks clean flow + every refusal
test/machi/hub/      # 24 tests / 63 assertions
```

## Run

```bash
kbb -M:test        # 24 tests, 63 assertions
kbb -M:dev:run     # the governed actor demo — every refusal, in the ledger
```

Standalone forks: `deps.edn` resolves `langgraph-clj` via `:local/root`
inside the monorepo; override with git coordinates outside it.

## Blueprint

`blueprint.edn` declares `:itonami.blueprint/governor :machi-hub-governor`,
ISIC Rev.5 6810 primary, logistics optional, maturity `:implemented`
(the proposal plane is real and tested; physical hub operation is the
operator's business, not this repo's claim).

## Governance

AGPL-3.0-or-later. See GOVERNANCE.md / SECURITY.md.
