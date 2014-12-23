(ns instabot.core
  (:require [clj-http.client :as client]
            [environ.core :refer [env]]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.walk :as walk])
  (:use
    instagram.oauth
    instagram.callbacks
    instagram.callbacks.handlers
    instagram.api.endpoint)
  (:import
    (instagram.callbacks.protocols SyncSingleCallback)))

(def client-id (:client-id env))
(def client-secret (:client-secret env))
(def redirect-uri (:redirect-uri env))

(def ^:dynamic *creds* (make-oauth-creds client-id
                                         client-secret
                                         redirect-uri))
(defn get-media-blob [tagname]
  (println "get media blob")
  (walk/keywordize-keys (second (first (conj {} ; to get the kind of map we want
                 (get-tagged-medias :oauth *creds* :params {:tag_name tagname}))))))

(defn get-by-pagination-url [media]
  (let [url (get (get media :pagination) :next_url)]
    (walk/keywordize-keys (get (client/get url {:as :json}) :body))))

(defn pagination? [media]
  (not (nil? (get (get media :pagination) :next_url))))

(defn parse-content [media]
  (get media :data))

; First the a get-tagged-media to get the pagination link
; Then continue till there are no pagination links
; Så länge det finns pagination länkar, fortsätt att loopa
(defn get-all-tagged-media [tagname]
  (println "lets do this")
  (loop [result []
         media (get-media-blob tagname)]
    (println "We are in the loop right now")
    (println (count result))
    ;(println (get (first (parse-content media)) "created_time"))
    ;(println (tc/from-long (read-string (get (first (parse-content media)) "created_time"))))
    ; If we have pagination but have extended the date we are parsing, we should stop.
    (println (get media :pagination))
    (if (not (pagination? media))
      (conj result (parse-content media))
      (recur 
       (conj result (parse-content media)) 
       (get-by-pagination-url media)))))

; För att få ut users från datan: (get (second (second (first (get (get tagged :body) "data")))) "from")
; För att få ut id från datan: (get (get (second (second (first (get (get tagged :body) "data")))) "from") "id")

; Problemet med get-tagged-medias är att den returnar x antal media + en pagination. Det kan vara sjukt många poster som har gjorts på hashtagen. Detta löses genom att räkna ut antal sidor och dela upp så det blir mindre än 5.000 requests per timme.

; Dock går det inte att räkna ut detta innan ut vi måste ha en counter som ser till att antal requests inte blir för många per timme.


;; Search funkar bara i 7dagar max.
;; /media/search
;; Search for media in a given area. The default time span is set to 5 days. The time span must not exceed 7 days. Defaults time stamps cover the last 5 days. Can return mix of image and video types.



;; För att göra denna app krävs följande API endpoints hos instagram:


;; Limits:
;; Unauthenticated Calls	5,000 / hour per application
;; 
;; ENDPOINT	UNSIGNED CALLS (PER TOKEN)	SIGNED CALLS (PER TOKEN)
;; POST /media/media-id/likes	30 / hour	100 / hour
;; POST /media/media-id/comments	15 / hour	60 / hour
;; POST /users/user-id/relationships	20 / hour	60 / hour
;;

;; Verkar som denna endpoint är starten: http://instagram.com/developer/endpoints/tags/#get_tags_media_recent


;; Och sen: http://instagram.com/developer/endpoints/users/#get_users
;; Går den att använda med endast ett klient id?


