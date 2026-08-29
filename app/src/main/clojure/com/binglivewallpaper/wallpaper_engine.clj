(ns com.binglivewallpaper.wallpaper-engine
  (:require [com.binglivewallpaper.image-store :as store]
            [com.binglivewallpaper.refresh-worker :as refresh-worker])
  (:import [android.graphics Bitmap Color Paint Rect]
           [android.service.wallpaper WallpaperService WallpaperService$Engine]
           [android.util Log]
           [android.view SurfaceHolder])
  (:gen-class
   :name com.binglivewallpaper.BingEngine
   :extends android.service.wallpaper.WallpaperService$Engine
   :constructors {[android.service.wallpaper.WallpaperService] [android.service.wallpaper.WallpaperService]}
   :init init
   :state state
   :prefix "engine-"))

(def ^:private tag "BingWallpaperService")

(defn engine-init
  [^WallpaperService service]
  [[service]
   (atom {:service service
          :bitmap nil
          :paint (Paint. (bit-or Paint/ANTI_ALIAS_FLAG Paint/FILTER_BITMAP_FLAG))})])

(defn- scaled-dest-rect
  ^Rect [^Bitmap bmp ^long screen-w ^long screen-h]
  (let [dominant (float (max screen-w screen-h))
        scale-w (/ dominant (float (.getWidth bmp)))
        scale-h (/ dominant (float (.getHeight bmp)))
        scale (max scale-w scale-h)
        scaled-w (int (* (.getWidth bmp) scale))
        scaled-h (int (* (.getHeight bmp) scale))
        left (quot (- screen-w scaled-w) 2)
        top (quot (- screen-h scaled-h) 2)]
    (Rect. left top (+ left scaled-w) (+ top scaled-h))))

(defn- draw-wallpaper
  [^WallpaperService$Engine this]
  (let [holder (.getSurfaceHolder this)]
    (if-not holder
      (Log/w tag "draw: surfaceHolder is null")
      (let [canvas (.lockCanvas holder)]
        (if-not canvas
          (Log/w tag "draw: lockCanvas returned null")
          (try
            (.drawColor canvas Color/BLACK)
            (let [{:keys [bitmap paint]} @(.state this)]
              (if bitmap
                (let [dest (scaled-dest-rect bitmap (.getWidth canvas) (.getHeight canvas))]
                  (Log/d tag (str "draw: canvas=" (.getWidth canvas) "x" (.getHeight canvas) " dest=" dest))
                  (.drawBitmap canvas ^Bitmap bitmap nil dest ^Paint paint))
                (Log/d tag "draw: no bitmap, black screen")))
            (finally
              (.unlockCanvasAndPost holder canvas))))))))

(defn engine-onCreate
  [^WallpaperService$Engine this ^SurfaceHolder _surface-holder]
  (Log/d tag "Engine onCreate")
  (let [{:keys [service]} @(.state this)]
    (refresh-worker/schedule-work service)))

(defn engine-onSurfaceCreated
  [^WallpaperService$Engine this ^SurfaceHolder _holder]
  (Log/d tag "onSurfaceCreated")
  (let [{:keys [service]} @(.state this)
        dm (.. service getResources getDisplayMetrics)
        loaded (store/load-image service (.widthPixels dm) (.heightPixels dm))]
    (Log/d tag (str "Loaded cached bitmap: " (if loaded (str (.getWidth loaded) "x" (.getHeight loaded)) "none")))
    (swap! (.state this) assoc :bitmap loaded)
    (when-not loaded
      (Log/d tag "No cached image — kicking off immediate fetch")
      (refresh-worker/run-once-now service))
    (draw-wallpaper this)))

(defn engine-onSurfaceChanged
  [^WallpaperService$Engine this ^SurfaceHolder _holder _format width height]
  (Log/d tag (str "onSurfaceChanged " width "x" height))
  (draw-wallpaper this))

(defn engine-onVisibilityChanged
  [^WallpaperService$Engine this visible]
  (Log/d tag (str "onVisibilityChanged visible=" visible))
  (when visible
    (let [{:keys [service]} @(.state this)
          dm (.. service getResources getDisplayMetrics)
          loaded (store/load-image service (.widthPixels dm) (.heightPixels dm))]
      (swap! (.state this) assoc :bitmap loaded)
      (draw-wallpaper this))))

(defn engine-onSurfaceDestroyed
  [^WallpaperService$Engine this ^SurfaceHolder _holder]
  (Log/d tag "onSurfaceDestroyed")
  (swap! (.state this) assoc :bitmap nil))
