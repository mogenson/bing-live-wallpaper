(ns com.binglivewallpaper.wallpaper-service
  (:require [com.binglivewallpaper.wallpaper-engine])
  (:import [android.service.wallpaper WallpaperService WallpaperService$Engine])
  (:gen-class
   :name com.binglivewallpaper.BingWallpaperService
   :extends android.service.wallpaper.WallpaperService
   :prefix "service-"))

(defn service-onCreateEngine
  ^WallpaperService$Engine [^WallpaperService this]
  ;; Resolve the engine class by name at runtime. This decouples this
  ;; namespace from the AOT-compilation order of wallpaper-engine, whose
  ;; gen-class'd BingEngine class may not be emitted yet when this namespace
  ;; is compiled.
  (let [ctor (.getConstructor (Class/forName "com.binglivewallpaper.BingEngine")
                              (into-array Class [WallpaperService]))]
    (.newInstance ctor (object-array [this]))))
