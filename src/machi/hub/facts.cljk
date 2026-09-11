(ns machi.hub.facts
  "Governor-facing facts the HubGovernor re-checks against every proposal.

  The feasibility arithmetic lives in `machi.hub.registry` (pure). This
  namespace is the SPEC-BASIS layer: which jurisdiction entries exist,
  what their official citations are, and whether a proposal's claims
  reference them. Same honesty discipline as realty.facts / formation.facts:
  a jurisdiction with no entry has NO spec basis -- the advisor must not
  fabricate one, and the governor holds if it tries."
  (:require [kotoba.lang.text :as str]
            [machi.hub.registry :as reg]))

(defn registry-facts
  "The registry catalog entry (kept as a re-export so the governor
  depends on THIS namespace, not on the scoring arithmetic)."
  [jurisdiction]
  (reg/jurisdiction-facts jurisdiction))

(defn known-jurisdiction?
  [jurisdiction]
  (boolean (registry-facts jurisdiction)))

(defn spec-basis-ok?
  "A jurisdiction proposal cites an official source when it names the
  jurisdiction's zoning-authority/legal-basis provenance (directly or
  through registry facts) AND that jurisdiction exists in the catalog."
  [jurisdiction proposal]
  (let [facts (registry-facts jurisdiction)]
    (boolean
     (and facts
          (seq (:cites proposal))
          (or (some #(= :spec-basis %) (:cites proposal))
              (some #(= :zoning-use %) (:cites proposal))
              (seq (filter #(str/includes? (str %) "zoning") (:cites proposal))))))))

(defn privacy-violations
  "G3: personal data (owner/tenant names, id docs) must NOT ride a
  conversion proposal. Returns violations (empty = clean)."
  [proposal]
  (let [v (:value proposal)]
    (cond-> []
      (contains? v :owner) (conj {:rule :personal-data-in-proposal
                                  :detail "owner data must not ride a proposal"})
      (contains? v :tenant) (conj {:rule :personal-data-in-proposal
                                   :detail "tenant data must not ride a proposal"})
      (contains? v :id-doc) (conj {:rule :personal-data-in-proposal
                                   :detail "id documents never ride a proposal"}))))
