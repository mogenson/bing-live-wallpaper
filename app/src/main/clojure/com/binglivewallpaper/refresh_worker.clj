(ns com.binglivewallpaper.refresh-worker
  (:require [com.binglivewallpaper.image-fetcher :as fetcher]
            [com.binglivewallpaper.image-store :as store])
  (:import [android.content Context]
           [android.util Log]
           [androidx.work BackoffPolicy Constraints$Builder ExistingPeriodicWorkPolicy
            ListenableWorker$Result NetworkType OneTimeWorkRequest$Builder
            PeriodicWorkRequest$Builder WorkManager Worker WorkerParameters]
           [java.util Calendar TimeZone]
           [java.util.concurrent TimeUnit])
  (:gen-class
   :name com.binglivewallpaper.BingRefreshWorker
   :extends androidx.work.Worker
   :constructors {[android.content.Context androidx.work.WorkerParameters]
                  [android.content.Context androidx.work.WorkerParameters]}
   :init init
   :state state
   :prefix "worker-"
   :methods [^:static [schedule [android.content.Context] void]
             ^:static [runOnceNow [android.content.Context] void]
             ^:static [initialDelayToNextUtc [int] long]]))

(def ^:private tag "BingRefreshWorker")
(def ^:private work-name "bing_daily_refresh")
(def ^:private target-hour-utc 11)
(def ^:private initial-backoff-minutes 30)

(defn worker-init
  [^Context context ^WorkerParameters params]
  [[context params] nil])

(defn worker-doWork
  [^Worker this]
  (Log/d tag (str "doWork start (attempt " (.getRunAttemptCount this) ")"))
  (try
    (let [result (fetcher/fetch)]
      (store/save (.getApplicationContext this)
                  (:bitmap result)
                  (:url result)
                  (:date result))
      (Log/d tag (str "doWork success: " (:url result)))
      (ListenableWorker$Result/success))
    (catch Throwable t
      (Log/e tag (str "doWork failed: " (.getMessage t)) t)
      (ListenableWorker$Result/retry))))

(defn initial-delay-to-next-utc
  ^long [hour-utc]
  (let [now (Calendar/getInstance (TimeZone/getTimeZone "UTC"))
        next (doto ^Calendar (.clone now)
               (.set Calendar/HOUR_OF_DAY (int hour-utc))
               (.set Calendar/MINUTE 0)
               (.set Calendar/SECOND 0)
               (.set Calendar/MILLISECOND 0))]
    (when (<= (.getTimeInMillis next) (.getTimeInMillis now))
      (.add next Calendar/DAY_OF_YEAR 1))
    (- (.getTimeInMillis next) (.getTimeInMillis now))))

(defn worker-initialDelayToNextUtc
  ^long [hour-utc]
  (initial-delay-to-next-utc hour-utc))

(defn- build-constraints
  ^androidx.work.Constraints []
  (.. (Constraints$Builder.)
      (setRequiredNetworkType NetworkType/UNMETERED)
      (setRequiresBatteryNotLow true)
      build))

(defn schedule-work
  [^Context context]
  (let [constraints (build-constraints)
        initial-delay (initial-delay-to-next-utc target-hour-utc)
        worker-cls (Class/forName "com.binglivewallpaper.BingRefreshWorker")]
    (Log/d tag (str "Scheduling periodic work; initial delay = " (quot initial-delay 1000) "s"))
    (let [request (.. (PeriodicWorkRequest$Builder. worker-cls 1 TimeUnit/DAYS)
                      (setConstraints constraints)
                      (setInitialDelay initial-delay TimeUnit/MILLISECONDS)
                      (setBackoffCriteria BackoffPolicy/EXPONENTIAL (long initial-backoff-minutes) TimeUnit/MINUTES)
                      build)]
      (.enqueueUniquePeriodicWork (WorkManager/getInstance context)
                                  work-name
                                  ExistingPeriodicWorkPolicy/CANCEL_AND_REENQUEUE
                                  request))))

(defn worker-schedule
  [^Context context]
  (schedule-work context))

(defn run-once-now
  [^Context context]
  (let [constraints (build-constraints)
        worker-cls (Class/forName "com.binglivewallpaper.BingRefreshWorker")
        request (.. (OneTimeWorkRequest$Builder. worker-cls)
                    (setConstraints constraints)
                    (setBackoffCriteria BackoffPolicy/EXPONENTIAL (long initial-backoff-minutes) TimeUnit/MINUTES)
                    build)]
    (Log/d tag "Enqueuing one-time fetch now")
    (.enqueue (WorkManager/getInstance context) request)))

(defn worker-runOnceNow
  [^Context context]
  (run-once-now context))
