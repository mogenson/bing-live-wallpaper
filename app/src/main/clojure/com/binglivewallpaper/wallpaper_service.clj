(ns com.binglivewallpaper.wallpaper-service
  (:require [com.binglivewallpaper.wallpaper-engine])
  (:import [android.service.wallpaper WallpaperService WallpaperService$Engine])
  (:gen-class
   :name com.binglivewallpaper.BingWallpaperService
   :extends android.service.wallpaper.WallpaperService
   :prefix "service-"))

(defn service-onCreateEngine
  ^WallpaperService$Engine [^WallpaperService this]
  (com.binglivewallpaper.BingEngine. this))
