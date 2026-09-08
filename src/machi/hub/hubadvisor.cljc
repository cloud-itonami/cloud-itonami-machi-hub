(ns machi.hub.hubadvisor
  "HubAdvisor -- the *contained intelligence node* for machi-hub.

  It normalizes site intake, drafts the per-jurisdiction land-use
  checklist reference, and drafts a micro-hub conversion proposal
  (feasibility + score + draft record). CRITICAL: it is a
  smart-but-untrusted advisor. It returns a *proposal* (rationale +
  cited fields), never a committed lease, a filed application, or a
  real renovation. Every output is censored downstream by
  `machi.hub.governor` before anything touches the SSoT.

  Deterministic mock (same pattern as realtorllm / registrarllm /
  hrllm / opsllm): the actor graph runs offline and the governor
  contract is exercised end-to-end. In production this calls a real
  LLM (murakumo-main via kotoba-llm or equivalent) with the same
  proposal shape.

  Proposal shape:
    {:summary    str
     :rationale  str
     :cites      [kw|str ..]
     :effect     kw
     :value      map
     :stake      kw|nil
     :confidence 0..1}"
  (:require [kotoba.lang.text :as str]
            [machi.hub.registry :as registry]
            [machi.hub.store :as store]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent sites, rents, or vacancy histories. High confidence,
  low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "サイトレコード更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :site/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- assess-land-use
  "Per-jurisdiction land-use checklist reference. `:no-spec?` injects
  the failure mode we defend against: proposing a checklist for a
  jurisdiction with NO spec basis in the facts catalog -- the governor
  must reject this (never invent a jurisdiction's land-use law)."
  [db {:keys [subject no-spec?]}]
  (let [site (store/site db subject)
        jurisdiction (:jurisdiction site)
        facts (registry/jurisdiction-facts jurisdiction)]
    (if (or no-spec? (nil? facts))
      {:summary (str jurisdiction " の土地利用 spec-basis は catalog に無い")
       :rationale "未知の法域に対する要件を作らない（fail closed）"
       :cites []
       :effect :site/assess
       :value {:spec-basis nil :unverified [:zoning-use]}
       :stake nil
       :confidence 0.2}
      {:summary (str jurisdiction " の土地利用確認チェックリスト（"
                     (:zoning-authority facts) "）")
       :rationale "machi.hub.registry の管轄カタログに基づく確認事項の提示"
       :cites [:spec-basis :zoning-use]
       :effect :site/assess
       :value {:spec-basis {:zoning-authority (:zoning-authority facts)
                            :legal-basis (:legal-basis facts)
                            :provenance (:provenance facts)}
               :checks (:zoning-checks facts)}
       :stake nil
       :confidence 0.9})))

(defn- propose-conversion
  "The main deliverable: a micro-hub conversion PROPOSAL DRAFT --
  feasibility assessment + score + draft record. `:stake :actuation`
  is deliberately NOT set: drafting is not actuation. The governor's
  hard gates still re-verify feasibility independently (the advisor
  does not get the last word on its own numbers)."
  [db {:keys [subject area no-spec?]}]
  (let [site (store/site db subject)
        demand (or (store/demand-of db area)
                   (store/demand-of db (:area-id site)))
        jurisdiction (:jurisdiction site)
        facts (registry/jurisdiction-facts jurisdiction)
        seq (store/next-sequence db jurisdiction)
        draft (registry/conversion-draft site demand jurisdiction seq)]
    (cond
      no-spec?
      ;; failure injection: proposal claims a jurisdiction that has no facts
      {:summary "転用ドラフト（spec-basis 無しの法域）"
       :rationale "cites 空で提出 -- governor が拒否すべき"
       :cites []
       :effect :conversion/propose
       :value {:draft draft}
       :stake nil
       :confidence 0.95}

      (nil? facts)
      ;; no spec basis: honest unverified proposal
      {:summary (str jurisdiction " は土地利用 spec-basis 未整備のため検証不能")
       :rationale "fail closed: 未整備法域では :unverified を返す"
       :cites []
       :effect :conversion/propose
       :value {:draft (assoc draft :feasible? nil :unverified [:zoning-use])}
       :stake nil
       :confidence 0.3}

      :else
      {:summary (str "転用ドラフト " (:draft-id draft) ": feasible="
                     (pr-str (:feasible? draft)) " score=" (:total (:score draft)))
       :rationale "registry の閾値・管轄カタログに基づく提案。契約・申請・改修は含まない"
       :cites [:spec-basis :zoning-use :feasibility]
       :effect :conversion/propose
       :value {:draft draft}
       :stake nil
       :confidence 0.9})))

(defn mock-advisor
  "The deterministic advisor: dispatches on `:op`. Returns a function
  (db request) -> proposal."
  []
  (fn [db request]
    (case (:op request)
      :site/intake        (normalize-intake db request)
      :site/assess        (assess-land-use db request)
      :conversion/propose (propose-conversion db request)
      (throw (ex-info (str "advisor: unknown op " (:op request)) {})))))

(defn -advise
  "Operation-facing entry: run the advisor for one request."
  [advisor db request]
  (advisor db request))

(defn trace
  "The audit fact for one advisor invocation."
  [request proposal]
  {:t :advisor-proposal
   :op (:op request)
   :subject (:subject request)
   :effect (:effect proposal)
   :confidence (:confidence proposal)})
