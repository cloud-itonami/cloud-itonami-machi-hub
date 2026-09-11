(ns machi.hub.governor
  "HubGovernor -- the independent compliance layer that earns the
  HubAdvisor the right to commit. The LLM has no notion of land-use
  law, no license to bind a lease, and no business deciding that a
  real renovation starts today, so this MUST be a separate system able
  to *reject* a proposal and fall back to HOLD -- the machi-hub analog
  of RealtorGovernor / RegistrarGovernor / robotaxi's Minimal Risk
  Condition / PolicyGovernor.

  HARD gates (a human approver CANNOT override):

    G1 no-spec-basis   -- a jurisdiction assessment or conversion
                          proposal with no official spec-basis citation
                          is refused. Never invent land-use law.
    G2 unverified-zoning -- a conversion whose jurisdiction has no facts
                          entry (or whose draft carries :unverified
                          zoning) is refused -- fail closed.
    G3 personal-data   -- owner/tenant/id-doc data must not ride a
                          proposal.
    G4 actuation       -- real lease signing / municipal filing /
                          renovation start are NOT ops this actor has.
                          Any proposal claiming an actuation effect is
                          refused outright (structural, not a phase
                          toggle).

  SOFT gates (a human looks):
    - confidence below `confidence-floor` -> escalate.
    - a conversion proposal whose draft is feasible -> still escalates
      for human approval (conversion is a business decision)."
  (:require [machi.hub.facts :as facts]
            [machi.hub.registry :as registry]
            [machi.hub.store :as store]))

(def confidence-floor 0.6)

;; Effects this actor may NEVER claim. A lease is a contract; a
;; municipal filing is a legal act; renovation moves physical material.
;; None of these are proposal artifacts.
(def forbidden-effects #{:lease/sign :municipal/file :renovation/start
                         :delivery/start})

(defn- spec-basis-violations
  [{:keys [op]} proposal]
  (when (contains? #{:site/assess :conversion/propose} op)
    (when (empty? (:cites proposal))
      [{:rule :no-spec-basis
        :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}])))

(defn- zoning-violations
  "G2: the draft must not carry unverified zoning, and the jurisdiction
  must exist in the facts catalog."
  [{:keys [op subject]} st proposal]
  (when (= op :conversion/propose)
    (let [site (store/site st subject)
          draft (get-in proposal [:value :draft])]
      (cond
        (nil? site)
        [{:rule :site-unknown
          :detail "対象サイトが SSoT に無い"}]

        (nil? (facts/registry-facts (:jurisdiction site)))
        [{:rule :unverified-zoning
          :detail (str (:jurisdiction site) " の土地利用 spec-basis が未整備 -- fail closed")}]

        (and (map? draft) (seq (:unverified draft)))
        [{:rule :unverified-zoning
          :detail "ドラフトが未検証の法域判断を含む"}]))))

(defn- actuation-violations
  "G4: the advisor must never claim an effect outside its mandate."
  [proposal]
  (let [effect (:effect proposal)]
    (when (contains? forbidden-effects effect)
      [{:rule :actuation-claimed
        :detail (str "本 actor は実行（" effect "）を持たない -- proposal で止まる")}])))

(defn check
  "Censors a HubAdvisor proposal against the governor rules. Returns
   {:ok? bool :violations [..] :confidence c :escalate? bool :hard? bool}.

   - :hard?     -- at least one HARD violation. Forces HOLD; a human
                   cannot override.
   - :escalate? -- soft: low confidence OR a feasible conversion draft
                   (a human approves the business decision).
   - :ok?       -- clean AND not escalating."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (zoning-violations request st proposal)
                           (actuation-violations proposal)
                           (facts/privacy-violations proposal)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        ;; a feasible conversion draft always wants human eyes
        draft (get-in proposal [:value :draft])
        feasible-conversion? (and (= :conversion/propose (:op request))
                                  (true? (:feasible? draft)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not feasible-conversion?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?)
                        (or low? feasible-conversion?))}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t :governor-hold
   :op (:op request)
   :actor (:actor-id context)
   :subject (:subject request)
   :disposition :hold
   :basis (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
