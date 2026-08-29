(ns com.binglivewallpaper.image-fetcher
  (:require [clojure.string :as str])
  (:import [android.graphics Bitmap Bitmap$CompressFormat BitmapFactory]
           [android.util Log]
           [java.io BufferedReader File FileOutputStream InputStream InputStreamReader]
           [java.net HttpURLConnection URL]
           [java.text SimpleDateFormat]
           [java.util Date Locale TimeZone]
           [org.json JSONObject]))

(def ^:private tag "BingImageFetcher")
(def ^:private json-url "https://www.bing.com/HPImageArchive.aspx?format=js&uhd=1&idx=0&n=1&mkt=en-US")
(def ^:private base-url "https://bing.com")
(def ^:private image-suffix "_UHD.jpg")
(def ^:private connect-timeout-ms 15000)
(def ^:private read-timeout-ms 30000)

(defn get-today-utc-date-string
  "Returns today's date formatted as YYYYMMDD in UTC."
  ^String []
  (let [sdf (doto (SimpleDateFormat. "yyyyMMdd" Locale/US)
              (.setTimeZone (TimeZone/getTimeZone "UTC")))]
    (.format sdf (Date.))))

(defn- configure-connection
  ^HttpURLConnection [^String url-str]
  (let [^HttpURLConnection conn (.openConnection (URL. url-str))]
    (doto conn
      (.setConnectTimeout connect-timeout-ms)
      (.setReadTimeout read-timeout-ms)
      (.setRequestMethod "GET")
      (.setUseCaches false)
      (.setDefaultUseCaches false)
      (.setRequestProperty "Cache-Control" "no-cache, no-store, must-revalidate")
      (.setRequestProperty "Pragma" "no-cache"))))

(defn- http-get
  ^String [^String url-str]
  (let [^HttpURLConnection conn (configure-connection url-str)]
    (try
      (let [code (.getResponseCode conn)]
        (if (= code HttpURLConnection/HTTP_OK)
          (with-open [reader (BufferedReader. (InputStreamReader. (.getInputStream conn)))]
            (let [sb (StringBuilder.)]
              (loop [line (.readLine reader)]
                (when line
                  (.append sb line)
                  (.append sb "\n")
                  (recur (.readLine reader))))
              (.toString sb)))
          (do
            (Log/e tag (str "HTTP " code " for " url-str))
            (throw (IllegalStateException. (str "HTTP " code " for " url-str))))))
      (finally
        (.disconnect conn)))))

(defn- download-bitmap
  ^Bitmap [^String url-str]
  (let [^HttpURLConnection conn (configure-connection url-str)]
    (try
      (let [code (.getResponseCode conn)]
        (if (= code HttpURLConnection/HTTP_OK)
          (with-open [^InputStream stream (.getInputStream conn)]
            (BitmapFactory/decodeStream stream))
          (do
            (Log/e tag (str "Image HTTP " code " for " url-str))
            nil)))
      (finally
        (.disconnect conn)))))

(defn fetch
  "Fetches the Bing image-of-the-day metadata, downloads the UHD image,
   and returns a map with {:bitmap bitmap :url url :date date}."
  []
  (Log/d tag (str "Fetching JSON from " json-url))
  (let [json-text (http-get json-url)]
    (Log/d tag (str "Got JSON (" (count json-text) " bytes)"))
    (let [json-obj (JSONObject. json-text)
          images-arr (.getJSONArray json-obj "images")
          first-img (.getJSONObject images-arr 0)
          urlbase (.getString first-img "urlbase")
          date-val (.optString first-img "startdate" "")
          image-url (str base-url urlbase image-suffix)]
      (Log/d tag (str "Image URL: " image-url ", startdate: " date-val))
      (let [today-utc (get-today-utc-date-string)]
        (when (and (not (str/blank? date-val))
                   (neg? (compare date-val today-utc)))
          (Log/w tag (str "Bing returned startdate " date-val " which is earlier than today's UTC date " today-utc))
          (throw (IllegalStateException. (str "Bing image of the day for " today-utc " is not available yet (got startdate=" date-val ")."))))
        (let [bitmap (download-bitmap image-url)]
          (if-not bitmap
            (throw (IllegalStateException. (str "Failed to decode image from " image-url)))
            (do
              (Log/d tag (str "Decoded bitmap: " (.getWidth bitmap) "x" (.getHeight bitmap)))
              {:bitmap bitmap
               :url image-url
               :date date-val})))))))

(defn fetch-to-file
  "Fetches the image and saves it as a JPEG at dest."
  [^File dest]
  (let [result (fetch)]
    (with-open [out (FileOutputStream. dest)]
      (.compress ^Bitmap (:bitmap result) Bitmap$CompressFormat/JPEG 95 out))
    result))
