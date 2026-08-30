(ns com.binglivewallpaper.image-store
  (:import [android.content Context]
           [android.graphics Bitmap Bitmap$CompressFormat BitmapFactory BitmapFactory$Options]
           [java.io File FileOutputStream]))

(def ^:private image-filename "bing_image.jpg")

(defn- image-file
  ^File [^Context context]
  (File. (.getFilesDir context) image-filename))

(defn save
  "Persists the bitmap to internal storage."
  [^Context context ^Bitmap bitmap]
  (let [file (image-file context)]
    (with-open [out (FileOutputStream. file)]
      (.compress bitmap Bitmap$CompressFormat/JPEG 95 out))))

(defn- calculate-in-sample-size
  [^BitmapFactory$Options options ^long req-width ^long req-height]
  (let [height (long (.-outHeight options))
        width (long (.-outWidth options))]
    (if (or (> height req-height) (> width req-width))
      (let [half-height (quot height 2)
            half-width (quot width 2)]
        (loop [in-sample-size 1]
          (if (and (>= (quot half-height in-sample-size) req-height)
                   (>= (quot half-width in-sample-size) req-width))
            (recur (* in-sample-size 2))
            (max 1 in-sample-size))))
      1)))

(defn load-image
  "Loads the cached image from disk. If req-width and req-height are provided,
   downsamples to approximately req-width x req-height."
  ([^Context context]
   (let [file (image-file context)]
     (when (.exists file)
       (BitmapFactory/decodeFile (.getAbsolutePath file)))))
  ([^Context context ^long req-width ^long req-height]
   (let [file (image-file context)]
     (when (.exists file)
       (let [path (.getAbsolutePath file)
             bounds-opts (BitmapFactory$Options.)]
         (set! (.-inJustDecodeBounds bounds-opts) true)
         (BitmapFactory/decodeFile path bounds-opts)
         (when (and (pos? (.-outWidth bounds-opts)) (pos? (.-outHeight bounds-opts)))
           (let [sample-size (calculate-in-sample-size bounds-opts req-width req-height)
                 decode-opts (BitmapFactory$Options.)]
             (set! (.-inSampleSize decode-opts) (int sample-size))
             (BitmapFactory/decodeFile path decode-opts))))))))
