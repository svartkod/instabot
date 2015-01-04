(ns instabot.views
  (:require [hiccup.page :as page]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]))

(defn common [title & body]
  (page/html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content
            "width=device-width, initial-scale=1, maximum-scale=1"}]
    (page/include-css "/css/styles.css")
    [:title title]]
   [:body
    [:div {:id "header"}
     [:h1 {:class "container"} [:a {:href "/"} "Instabot"]]]
    [:div {:id "content" :class "container"} body]]))


; INDEX: Search for tag.
; Perhaps show available tags here?
(defn index []
  (common "instabot"
          [:div 
           (form/form-to 
            [:post "/tag"]
            (form/label "tagname" "Tagname:")
            (form/text-field "tagname")
            (form/submit-button "Submit"))]))

(defn media-route [m]
  (str "/media/" (:_id m)))

(defn show-tags-for-media [m]
  [:div {:class "tags"}
   [:p "Tags:"]
   (map (fn [t] [:a {:class "tag" :href (str "/tag/" t)} t]) (:tags m))])

(defn format-date [date-string]
  (clj-time.coerce/from-long (read-string (str date-string "000"))))

(defn parsed-date [m]
 (format-date (:created_time m)))

(defn username-link [id username]
  [:a {:class "user" :href (str "/user/" id)} username])

(defn a-single-media [m]
  [:li {:class "media"}
   [:img {:src (get-in m [:images :standard_resolution :url])}]
   [:div {:class "metadata"} 
    (username-link (get-in m [:user :id]) (get-in m [:user :username]))
    [:span {:class "created-date"} (parsed-date m)]
    [:a {:class "see-more" :href (media-route m)} "Se mer"]
    [:p (str "Likes: " (get-in m [:likes :count]))]
    [:p (str "Comments: " (get-in m [:comments :count]))]
    [:span {:class "tags"} (show-tags-for-media m)]]])

(defn tag [tagname media]
  (common (str "Tag: " (str tagname))
          [:div
           [:h1 tagname]
           [:p (str "Total number of media: " (count media))]
           [:ul {:class "medias"} (map
                 (fn [m] (a-single-media m))
                 media)]]))

(defn media [m]
  (common "media"
          [:div
           [:h1 "Media"]
           [:ul {:class "medias"} (a-single-media m)]
           [:ul (map 
                 (fn [c] [:div {:class "comment"} 
                          [:p 
                           (username-link (get-in c [:from :id]) (get-in c [:from :username]))
                           [:span (format-date (:created_time c))]]
                          [:img {:class "profile-picture" :src (get-in c [:from :profile_picture])}]
                          [:p (:text c)]]) 
                 (get-in m [:comments :data]))]]))

(defn user [u]
 (common "User" 
         [:div 
          [:p (:username u)]
          [:p (:bio u)]
          [:p (:website u)]
          [:p (:full_name u)]
          [:p [:img {:src (:profile_picture u)}]]
          [:p (str "Media: " (get-in u [:counts :media]))]
          [:p (str "Followed by: " (get-in u [:counts :followed_by]))]
          [:p (str "Follows: " (get-in u [:counts :follows]))]
          [:p [:a {:href "/"} "The users media"]]]))
 

; GET /user/:id/media
; Show all media related to the user.

