(ns hubspot.contact-list
  (:require [hubspot.http :as h]
            [hubspot.spec :as hs]
            [clojure.spec.alpha :as s]))


;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


(s/def ::list-id
  integer?)

(s/def ::vid
  integer?)

(s/def ::vids
  (s/+ ::vid))

(s/def ::email
  hs/email?)

(s/def ::emails
  (s/+ ::email))

(s/def ::add-contact-params
  (s/keys :opt-un [::vids ::emails]))

(s/def ::contact
  (s/keys :req-un [::properties ::vid ::profile-url]
          :opt-un [::identity-profiles ::form-submissions]))

(s/def ::lists
  (s/* map?))

(s/def ::contact-list
  (s/keys :req-un [::lists]))

(s/def ::contact-lists
  (s/+ ::contact-list))

(s/def ::offset
  integer?)

(s/def ::count
  integer?)

(s/def ::fetch-params
  (s/keys :opt-un [::offset ::count]))

;; ==============================================================================
;; HTTP API =====================================================================
;; ==============================================================================


(defn add-contact!
  "Adds a contact (as identified by either a `:vid` or `:email` in `parmas`) to
  the list with the provided `list-id`"
  [list-id params opts]
  (h/post-req
   (format "contacts/v1/lists/%s/add" list-id)
   (assoc opts :params params)))

(s/fdef add-contact!
        :args (s/cat :list-id ::list-id
                     :params ::add-contact-params
                     :opts h/request-options?)
        :ret (hs/async ::contact))


(defn fetch
  "Fetch contact lists."
  ([params]
   (fetch params {}))
  ([params opts]
   (h/get-req "contacts/v1/lists" (assoc opts :params params))))

(s/fdef fetch
        :args (s/alt :unary (s/cat :params ::fetch-params)
                     :binary (s/cat :params ::fetch-params
                                    :opts h/request-options?))
        :ret (hs/async ::contact-lists))
