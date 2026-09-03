(ns machi.hub.store-contract-test
  "The MemStore contract: ledger is append-only, drafts accumulate,
  sequences are jurisdiction-scoped."
  (:require [clojure.test :refer [deftest is testing]]
            [machi.hub.store :as store]))

(deftest ledger-append-only
  (testing "appended facts are never lost or reordered"
    (let [s (store/mem-store)]
      (store/append-ledger! s {:t :a :seq 1})
      (store/append-ledger! s {:t :b :seq 2})
      (is (= [:a :b] (mapv :t (store/ledger s))))
      (store/append-ledger! s {:t :c :seq 3})
      (is (= [:a :b :c] (mapv :t (store/ledger s)))))))

(deftest draft-history-accumulates
  (testing "committed drafts accumulate; next-sequence counts per jurisdiction"
    (let [s (store/mem-store)]
      (store/commit-record! s {:kind "draft" :jurisdiction "JPN" :draft-id "JPN-MH-00000001"})
      (store/commit-record! s {:kind "draft" :jurisdiction "JPN" :draft-id "JPN-MH-00000002"})
      (is (= 2 (count (store/draft-history s))))
      (is (= 2 (store/next-sequence s "JPN")))
      (is (= 0 (store/next-sequence s "USA-CA"))))))

(deftest roundtrip-through-edn
  (testing "dump-db / load-db preserve state (offline persistence)"
    (let [s (store/seed-db)
          snapshot (store/dump-db s)
          s2 (store/load-db snapshot)]
      (is (= (count (store/all-sites s)) (count (store/all-sites s2))))
      (is (= 900 (:delivery-density-per-km2 (store/demand-of s2 "downtown")))))))

(deftest seed-has-no-personal-data
  (testing "G3: seed sites carry shape only -- no owner/tenant names"
    (doseq [site (store/all-sites (store/seed-db))]
      (is (not (contains? site :owner)))
      (is (not (contains? site :tenant)))
      (is (not (contains? site :id-doc))))))
