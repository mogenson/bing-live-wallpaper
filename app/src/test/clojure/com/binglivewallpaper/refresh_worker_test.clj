(ns com.binglivewallpaper.refresh-worker-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.binglivewallpaper.image-fetcher :as fetcher]
            [com.binglivewallpaper.refresh-worker :as worker]))

(deftest test-initial-delay-to-next-utc
  (testing "Initial delay must be positive and <= 24 hours"
    (let [delay (worker/initial-delay-to-next-utc 11)]
      (is (pos? delay) "Initial delay must be positive")
      (is (<= delay 86400000) "Initial delay must be <= 24 hours"))))

(deftest test-get-today-utc-date-string-format
  (testing "Date string format must be 8 numeric digits (YYYYMMDD)"
    (let [date-str (fetcher/get-today-utc-date-string)]
      (is (= 8 (count date-str)) "Date string must be 8 characters")
      (is (every? #(Character/isDigit ^char %) date-str) "Date string must contain only digits"))))
