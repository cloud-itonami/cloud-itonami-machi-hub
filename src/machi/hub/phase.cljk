(ns machi.hub.phase
  "Phase 0->3 staged rollout -- the machi-hub analog of realty.phase /
  formation.phase. Where the HubGovernor answers 'is this allowed?',
  the phase answers 'how much autonomy does the actor have *yet*?'. It
  can only ever make the actor MORE conservative than the governor,
  never the reverse.

    Phase 0  read-only         -- coverage/registry reads only.
    Phase 1  assisted-intake   -- site intake allowed, every write needs
                                  human approval.
    Phase 2  + assess          -- adds jurisdiction land-use assessment
                                  writes (still approval).
    Phase 3  supervised-auto   -- governor-clean, high-confidence INTAKE
                                  writes may auto-commit. Land-use
                                  assessment and conversion proposals
                                  still escalate (a human should see a
                                  jurisdiction determination and the
                                  business case before it becomes the
                                  basis for a lease).

  `:conversion/propose` is deliberately ABSENT from every phase's
  `:auto` set, including phase 3 -- a conversion proposal commits a
  business direction and is always a human call. This is a permanent
  structural fact about this table, not a rollout milestone still to
  come. The HubGovernor's feasible-draft escalation enforces the same
  invariant independently -- two layers, not one, agree on this.")

(def read-ops #{:coverage/report})
(def write-ops #{:site/intake :site/assess :conversion/propose})

;; NOTE the invariant: :conversion/propose is a member of `write-ops`
;; (it is governor-gated like any write) but is NEVER a member of any
;; phase's `:auto` set below. Do not add it there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed
  to auto-commit when governor-clean>}."
  {0 {:label "read-only"       :writes #{}                        :auto #{}}
   1 {:label "assisted-intake" :writes #{:site/intake}            :auto #{}}
   2 {:label "assisted-assess" :writes #{:site/intake :site/assess} :auto #{}}
   3 {:label "supervised-auto" :writes write-ops                  :auto #{:site/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - reads pass through unchanged (phase restricts autonomy, not reads).
  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE
    (:phase-approval), even if the governor was clean.
  - `:conversion/propose` is never auto-eligible at any phase."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a HubGovernor verdict to a base disposition before the phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
