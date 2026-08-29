(ns com.binglivewallpaper.boot-receiver
  (:require [com.binglivewallpaper.refresh-worker :as refresh-worker])
  (:import [android.content Context Intent]
           [android.util Log])
  (:gen-class
   :name com.binglivewallpaper.BootReceiver
   :extends android.content.BroadcastReceiver
   :prefix "receiver-"))

(def ^:private tag "BootReceiver")

(defn receiver-onReceive
  [_ ^Context context ^Intent intent]
  (when (= (.getAction intent) Intent/ACTION_BOOT_COMPLETED)
    (Log/d tag "BOOT_COMPLETED received; scheduling refresh work")
    (refresh-worker/schedule-work context)))
