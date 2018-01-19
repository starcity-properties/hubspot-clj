(ns hubspot.contact
  (:require [hubspot.http :as h]
            [clojure.spec.alpha :as s]
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


(defn create!
  "Create a new contact."
  ([email]
   (create! email {}))
  ([email opts]
   (letfn [(-parse-params [params]
             (-> (reduce (fn [acc [k v]] (conj acc {:property k :value v})) [] params)
                 (tb/conj-when (when-some [e email] {:property "email" :value e}))))]
     (h/post-req "contacts/v1/contact"
                 (assoc opts :params {:properties (-parse-params (:params opts))})))))

(s/fdef create!
        :args (s/cat :email (hs/maybe ::email)
                     :opts (s/? (h/opts? ::create-params)))
        :ret (hs/async ::contact))


(defn fetch
  "Fetch a contact by email or vid."
  ([id]
   (fetch id {}))
  ([id opts]
   (if-not (s/valid? ::fetch-id id)
     (throw (ex-info "Invalid id!" {:id id}))
     (let [[id-name id-val] (s/conform ::fetch-id id)]
       (h/get-req (format "contacts/v1/contact/%s/%s/profile" (name id-name) id-val)
                  opts)))))

(s/fdef fetch
        :args (s/cat :id ::fetch-id
                     :opts (s/? (h/opts? ::fetch-params)))
        :ret (hs/async ::contact))


(defn search
  "Search contacts given a `query` string. Can optionally provide count, offset
  and property.
  https://developers.hubspot.com/docs/methods/contacts/search_contacts"
  ([query]
   (search query {}))
  ([query opts]
   (h/get-req "/contacts/v1/search/query"
              (update opts :params assoc :q query))))

(s/fdef search
        :args (s/cat :query ::query
                     :opts (s/? (h/opts? ::search-params)))
        :ret (hs/async ::search-res))



(comment
  (def api-key "ce0af81c-b158-4ac7-a770-57b65093b07c")

  (require '[clojure.spec.test.alpha :as stest])

  (stest/instrument)

  (create! "test@clojure.com" {:api-key api-key})

  (h/with-api-key api-key
    (fetch 390001))



  )
