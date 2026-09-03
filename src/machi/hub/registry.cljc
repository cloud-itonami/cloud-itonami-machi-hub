(ns machi.hub.registry
  "Pure domain core for the machi-hub vertical: feasibility + scoring for
  converting a vacant house / vacant room into an urban micro-logistics-hub
  (last-mile pickup point + parcel staging).

  SELF-CONTAINED (same discipline as cloud-itonami-isic-5210's terminal
  registry): there is no `kotoba-lang/machi-hub` capability library to
  delegate to, so the feasibility arithmetic lives here as pure functions.
  Physical execution (real deliveries, real renovation) belongs to OTHER
  actors -- ISIC 5320 courier / soko fulfillment -- and never to this
  namespace. Everything here is PROPOSAL-ONLY data: a draft record a
  human operator would keep, not an act.

  Honesty rules (same discipline as realty.facts / formation.facts):
    - a jurisdiction with no entry in `jurisdiction-catalog` has NO
      zoning-use spec basis. `feasible?` returns `:feasible? nil` with
      `:unverified [:zoning-use]` -- never a silent pass, never an
      invented rule.
    - the zoning check for a KNOWN jurisdiction is a document/prerequisite
      checklist keyed to that jurisdiction's official authority, cited by
      URL. It does NOT decide land law."
  (:require [clojure.string :as str]))

;; ----------------------------- thresholds -----------------------------
;; Deliberately conservative seed values for a SMALL urban micro-hub
;; (a pickup point + parcel staging room, not a fulfillment center).
;; They are policy parameters of THIS blueprint, not law.

(def min-floor-area-m2 15)
(def max-floor-area-m2 500)

(def min-entrance-width-m 0.9)

(def min-delivery-density-per-km2 200)

(def min-vacancy-months 6)

;; ----------------------------- jurisdiction facts -----------------------------

(def jurisdiction-catalog
  "iso3 (or region-scoped key) -> micro-hub land-use prerequisite map.
  `:zoning-authority` / `:legal-basis` / `:provenance` are the G2-style
  citation the governor requires. Seed values cite REAL official sources;
  coverage is a STARTING catalog, not a survey of all jurisdictions --
  extending it is additive (add one map, cite a real source)."
  {"JPN" {:name "Japan"
          :zoning-authority "法務局・自治体（用途地域の確認は市区町村建築指導課）"
          :legal-basis "建築基準法（用途地域、法48条）・都市計画法"
          :provenance "https://www.mlit.go.jp/city/index.html"
          ;; 宅地建物取引業法上の問屋業扱いや風営法との境界は個別判断 --
          ;; catalog は「確認すべき窓口と根拠法」までしか言わない
          :zoning-checks [:zoning-confirmation-municipal
                          :parking-noise-ordinance
                          :residence-adjacent-notification]}
   "USA-CA" {:name "United States -- California (exemplar)"
             :zoning-authority "City Planning Department (zoning)"
             :legal-basis "Local zoning ordinance (per-city; no state land registry)"
             :provenance "https://www.hcd.ca.gov/"
             :notes "Land-use is per-city; California is an exemplar, not an authority."
             :zoning-checks [:zoning-confirmation-municipal
                             :conditional-use-permit-if-required]}})

(defn jurisdiction-facts
  "The facts entry for `jurisdiction`, or nil when it has NO spec basis.
  Unknown jurisdiction = no invented rule, ever."
  [jurisdiction]
  (get jurisdiction-catalog jurisdiction))

;; ----------------------------- feasibility -----------------------------

(defn- physical-failures
  [{:keys [floor-area-m2 floor entrance-width-m freight-access freight-lift
           vacancy-months]}]
  (into []
        (concat
         (when (or (not (number? floor-area-m2))
                   (< floor-area-m2 min-floor-area-m2))
           [{:rule :floor-area-below-minimum
             :detail (str "min " min-floor-area-m2 " m2, got " floor-area-m2)}])
         (when (and (number? floor-area-m2) (> floor-area-m2 max-floor-area-m2))
           [{:rule :floor-area-above-micro-hub-scale
             :detail (str "micro-hub ceiling " max-floor-area-m2 " m2 -- larger sites are 5210/warehouse territory")}])
         (when (and (number? entrance-width-m)
                    (< entrance-width-m min-entrance-width-m))
           [{:rule :entrance-too-narrow
             :detail (str "min " min-entrance-width-m " m, got " entrance-width-m)}])
         (when (= :none freight-access)
           [{:rule :no-freight-access
             :detail " parcel staging needs street or loading-dock freight access"}])
         (when (and (number? floor) (> floor 1) (not freight-lift))
           [{:rule :upper-floor-without-freight-lift
             :detail "floors above 1 need a freight lift for parcel carts"}])
         (when (and (number? vacancy-months) (< vacancy-months min-vacancy-months))
           [{:rule :vacancy-too-recent
             :detail (str "conversion only proposed after " min-vacancy-months " months vacant")}]))))

(defn- demand-failures
  [{:keys [delivery-density-per-km2 distance-nearest-hub-km]}]
  (into []
        (concat
         (when (or (not (number? delivery-density-per-km2))
                   (< delivery-density-per-km2 min-delivery-density-per-km2))
           [{:rule :demand-below-threshold
             :detail (str "min " min-delivery-density-per-km2 " deliveries/km2/day, got " delivery-density-per-km2)}])
         (when (and (number? distance-nearest-hub-km) (< distance-nearest-hub-km 0.3))
           [{:rule :too-close-to-existing-hub
             :detail "a hub already serves this walkshed (< 0.3 km)"}]))))

(defn feasible?
  "Pure feasibility assessment of converting `site` for `demand` under
  `jurisdiction`.

  Returns
    {:feasible?  true|false|nil   -- nil = zoning UNVERIFIED (unknown jurisdiction)
     :failures   [..]             -- physical/demand rules that fail
     :unverified [..]             -- e.g. [:zoning-use] when the jurisdiction
                                     has no facts entry (fail closed)}

  A nil jurisdiction answer is NEVER a pass: the caller (governor) treats
  :unverified as a hard hold. This function does NOT decide land law --
  for known jurisdictions it returns the checklist the operator must
  confirm with the cited authority."
  [site demand jurisdiction]
  (let [failures (into (physical-failures site) (demand-failures demand))]
    (if-let [facts (jurisdiction-facts jurisdiction)]
      {:feasible? (empty? failures)
       :failures failures
       :unverified []
       :zoning-checks (:zoning-checks facts)
       :zoning-authority (:zoning-authority facts)}
      {:feasible? nil
       :failures failures
       :unverified [:zoning-use]})))

;; ----------------------------- scoring -----------------------------

(defn score
  "Ranking heuristic (0..100-ish composite, no absolute meaning):
    - rent spread vs the operator's micro-hub rent budget
    - demand proximity (distance to the nearest existing hub, capped)
    - freight access quality
  Pure arithmetic -- numbers in, number out. NOT a valuation."
  [{:keys [asking-rent-jpy freight-access floor]} demand]
  (let [;; rent term: 150k/mo budget baseline, softer than linear
        rent-term (let [r (double (or asking-rent-jpy 250000))]
                    (max 0 (- 50 (/ (- r 150000) 5000))))
        ;; demand term: closer to the demand center scores higher; being
        ;; very close to an EXISTING hub is already excluded by feasible?
        demand-term (let [d (double (:delivery-density-per-km2 demand 0))]
                      (min 30 (* 30 (Math/log10 (inc (/ d 200))))))
        proximity-term (let [km (double (:distance-nearest-hub-km demand 10))]
                         (max 0 (- 20 (* 4 km))))
        access-term (case (or freight-access :none)
                      :loading-dock 12
                      :street 8
                      :shared-yard 4
                      :none 0)
        ground-term (if (and (number? floor) (= 1 floor)) 8 0)]
    {:rent (Math/round rent-term)
     :demand (Math/round demand-term)
     :proximity (Math/round proximity-term)
     :access access-term
     :ground-floor ground-term
     :total (+ (Math/round rent-term) (Math/round demand-term)
               (Math/round proximity-term) access-term ground-term)}))

;; ----------------------------- draft record -----------------------------

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn conversion-draft
  "Validate + construct a micro-hub conversion PROPOSAL DRAFT. Pure
  function -- does not sign a lease, file anything with a municipality,
  or move any parcel. The draft id is jurisdiction-scoped (the same
  honest, non-fabricating discipline realty.registry uses: there is no
  international standard for conversion numbers, so we build a
  jurisdiction-scoped sequence, not a fake global id).

  G3 note: the draft carries NO owner/tenant personal data -- only the
  site shape. Whoever runs a live instance adds their own privacy review."
  [site demand jurisdiction sequence]
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "draft: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "draft: sequence must be >= 0" {})))
  (let [draft-id (str (str/upper-case jurisdiction) "-MH-" (zero-pad sequence 8))
        assessment (feasible? site demand jurisdiction)]
    (assoc assessment
           :kind "draft"
           :draft-id draft-id
           :committed false
           :site-shape (select-keys site [:floor-area-m2 :floor :freight-access
                                          :vacancy-months :zoning])
           :demand-shape (select-keys demand [:delivery-density-per-km2
                                              :pickup-demand-index
                                              :distance-nearest-hub-km])
           :jurisdiction jurisdiction
           :score (score site demand))))
