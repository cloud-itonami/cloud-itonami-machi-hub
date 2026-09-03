# ADR-0001: why the actor layer exists (and its honest limits)

Date: 2026-09-03 · Status: accepted

## Context

Converting a vacant house/room into a micro logistics hub mixes three
things an LLM must not own: land-use facts (per jurisdiction, official
sources only), a business decision (the conversion itself), and physical
acts (lease, filing, renovation, delivery).

## Decision

- HubAdvisor is a sealed proposal-only node; HubGovernor is an
  independent censor with HARD gates G1-G4.
- Jurisdiction facts fail closed: no entry = no rule = HOLD, never an
  invented requirement.
- Conversion drafts never auto-commit at any phase (structural, not a
  rollout milestone).
- The proposal plane is the whole scope; execution belongs to other
  actors (5320 courier / soko / kuramori).

## Limits

- This repo does not evaluate real buildings, does not know any real
  vacancy, and cannot value a lease. `score` is a ranking heuristic.
- The jurisdiction catalog (JPN, USA-CA) is a seed, not a survey.
- The advisor is a deterministic mock; a real LLM swap must keep the
  same proposal shape and remain behind the governor.
