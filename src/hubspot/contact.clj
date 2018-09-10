(ns hubspot.contact
  (:require [clojure.spec.alpha :as s]
            [hubspot.http :as h]
            [hubspot.spec :as hs]
            [toolbelt.core :as tb]))


;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


(s/def ::property
  (s/or :property string? :properties (s/+ string?)))

(s/def ::properties
  map?)

(s/def ::vid
  integer?)

(s/def ::profile-url
  string?)

(s/def ::identity-profiles
  (s/* map?))                           ; TODO

(s/def ::form-submissions
  (s/* map?))                           ; TODO

(s/def ::contact
  (s/keys :req-un [::properties ::vid ::profile-url]
          :opt-un [::identity-profiles ::form-submissions]))


;; create =======================================================================


(s/def ::email
  hs/email?)

(s/def ::create-params
  (s/keys :opt-un [::email]))


;; fetch ========================================================================


(s/def ::propertyMode
  #{"value_only" "value_and_history"})

(s/def ::formSubmissionMode
  #{"all" "none" "newest" "oldest"})

(s/def ::showListMemberships
  boolean?)

(s/def ::fetch-id
  (s/or :vid ::vid :email hs/email?))

(s/def ::fetch-params
  (s/keys :opt-un [::propertyMode ::formSubmissionMode ::showListMemberships]))


;; search =======================================================================


(s/def ::query
  string?)

(s/def ::count
  (s/and pos-int? #(<= % 100)))

(s/def ::offset
  pos-int?)

(s/def ::search-params
  (s/keys :opt-un [::count ::offset ::property]))

(s/def ::has-more
  boolean?)

(s/def ::total
  integer?)

(s/def ::contacts
  (s/+ ::contact))

(s/def ::search-res
  (s/keys :req-un [::query ::offset ::has-more ::total ::contacts]))


;; ==============================================================================
;; HTTP API =====================================================================
;; ==============================================================================


(defn- params->properties
  [params]
  (reduce (fn [acc [k v]] (conj acc {:property k :value v})) [] params))


(defn create!
  "Create a new contact."
  ([email]
   (create! email {} {}))
  ([email params]
   (create! email params {}))
  ([email params opts]
   (letfn [(-parse-params [params]
             (-> (params->properties params)
                 (tb/conj-when (when-some [e email] {:property "email" :value e}))))]
     (h/post-req "contacts/v1/contact"
                 (assoc-in opts [:params :properties] (-parse-params params))))))

(s/fdef create!
        :args (s/alt :unary   (s/cat :email (s/nilable ::email))
                     :binary  (s/cat :email (s/nilable ::email)
                                     :params ::create-params)
                     :ternary (s/cat :email (s/nilable ::email)
                                     :params ::create-params
                                     :opts h/request-options?))
        :ret (hs/async ::contact))


(defn fetch
  "Fetch a contact by email or vid."
  ([id]
   (fetch id {} {}))
  ([id params]
   (fetch id params {}))
  ([id params opts]
   (if-not (s/valid? ::fetch-id id)
     (throw (ex-info "Invalid id!" {:id id}))
     (let [[id-name id-val] (s/conform ::fetch-id id)]
       (h/get-req (format "contacts/v1/contact/%s/%s/profile" (name id-name) id-val)
                  (assoc opts :params params))))))

(s/fdef fetch
        :args (s/alt :unary   (s/cat :id ::fetch-id)
                     :binary  (s/cat :id ::fetch-id
                                     :params ::fetch-params)
                     :ternary (s/cat :id ::fetch-id
                                     :params ::fetch-params
                                     :opts h/request-options?))
        :ret (hs/async ::contact))


(defn search
  "Search contacts given a `query` string. Can optionally provide count, offset
  and property.
  https://developers.hubspot.com/docs/methods/contacts/search_contacts"
  ([query]
   (search query {} {}))
  ([query params]
   (search query params {}))
  ([query params opts]
   (h/get-req "/contacts/v1/search/query"
              (assoc opts :params (assoc params :q query)))))

(s/fdef search
        :args (s/alt :unary   (s/cat :query ::query)
                     :binary  (s/cat :query ::query
                                     :params ::search-params)
                     :ternary (s/cat :query ::query
                                     :params ::search-params
                                     :opts h/request-options?))
        :ret (hs/async ::search-res))


(defn update!
  ([id params]
   (update! id params {}))
  ([id params opts]
   (h/post-req (format "contacts/v1/contact/vid/%s/profile" id)
               (assoc-in opts [:params :properties] (params->properties params)))))

(s/fdef update!
        :args (s/cat :id ::fetch-id
                     :params map?
                     :opts (s/? h/request-options?))
        :ret (hs/async nil?))


(defn update-by-email!
  ([email params]
   (update-by-email! email params {}))
  ([email params opts]
   (h/post-req (format "contacts/v1/contact/email/%s/profile" email)
               (assoc-in opts [:params :properties] (params->properties params)))))


(comment
  (def api-key "")

  (require '[clojure.spec.test.alpha :as stest])

  (stest/instrument)

  (create! "test@clojure.com" {:api-key api-key})

  (h/with-api-key api-key
    (fetch "test2@test.com"))


  (h/with-api-key api-key
    (update! "test@test.com" {:closedate (.getTime (java.util.Date.))}))


  (.getTime (java.util.Date.))


  )
