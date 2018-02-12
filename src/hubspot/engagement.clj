(ns hubspot.engagement
  (:require [hubspot.http :as h]
            [clojure.spec.alpha :as s]
            [hubspot.spec :as hs]))

;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


(s/def ::id
  integer?)

(s/def ::type
  #{"EMAIL" "CALL" "MEETING" "TASK" "NOTE"})

;; TODO: https://developers.hubspot.com/docs/methods/engagements/create_engagement
(s/def ::metadata
  map?)

(s/def ::ownerId
  integer?)

(s/def ::createdAt
  integer?)

(s/def ::lastUpdated
  integer?)

(s/def ::timestamp
  integer?)

(s/def ::uid
  string?)

(s/def ::portalId
  integer?)


;; fetch ========================================================================


(s/def :engagement.body/engagement
  (s/keys :req-un [::id ::type ::portalId ::active ::createdAt ::lastUpdated ::timestamp]
          :opt-un [::ownerId]))


;; fetch-all ====================================================================


(s/def ::limit
  (s/and pos-int? #(<= % 250)))

(s/def ::offset
  pos-int?)

(s/def ::results
  (s/* ::engagement))

(s/def ::hasMore
  boolean?)

(s/def ::offset
  integer?)

(s/def ::fetch-all-params
  (s/keys :opt-un [::limit ::offset]))


(def fetch-all-params?
  "Is the argument a valid map of parameters to fetch all params with?"
  (partial s/valid? ::fetch-all-params))


;; create =======================================================================


(s/def :create-params/engagement
  (s/keys :req-un [::type]
          :opt-un [::active ::ownerId ::timestamp ::uid ::portalId]))

(s/def ::contactIds
  (s/* integer?))

(s/def ::companyIds
  (s/* integer?))

(s/def ::dealIds
  (s/* integer?))

(s/def ::ownerIds
  (s/* integer?))

(s/def ::workflowIds
  (s/* integer?))

(s/def ::ticketIds
  (s/* integer?))

(s/def ::associations
  (s/keys :opt-un [::contactIds ::companyIds ::dealIds ::ownerIds
                   ::workflowIds ::ticketIds]))

(s/def ::create-params
  (s/keys :req-un [:create-params/engagement ::metadata]
          :opt-un [::associations]))


(def create-params?
  "Is the argument a valid map of parameters to create an engagment?"
  (partial s/valid? ::create-params))


;; update =======================================================================


(s/def :update-params/engagement
  (s/keys :opt-un [::type ::active ::ownerId ::timestamp ::uid ::portalId]))

(s/def ::update-params
  (s/keys :opt-un [:update-params/engagement ::metadata ::associations]))


(def update-params?
  "Is the argument a valid map of parameters to update an engagment?"
  (partial s/valid? ::update-params))


;; engagement ===========================


(s/def ::engagement
  (s/keys :req-un [:engagement.body/engagement ::associations ::metadata]))


;; ==============================================================================
;; HTTP API =====================================================================
;; ==============================================================================


(defn fetch
  "Fetch the engagement with the specified id."
  ([engagement-id]
   (fetch engagement-id {}))
  ([engagement-id opts]
   (h/get-req (str "engagements/v1/engagements/" engagement-id) opts)))

(s/fdef fetch
        :args (s/cat :engagement-id ::id
                     :opts (s/? h/request-options?))
        :ret (hs/async ::engagement))


(defn fetch-all
  "Fetch all engagements, with optionally supplied `limit` and `offset`."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "engagements/v1/engagements/paged"
              (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (hs/async (s/keys :req-un [::results ::hasMore ::offset])))


(defn create!
  "Create a new engagement given `opts`."
  ([params]
   (create! params {}))
  ([params opts]
   (h/post-req "engagements/v1/engagements" (assoc opts :params params))))

(s/fdef create!
        :args (s/cat :params ::create-params
                     :opts (s/? h/request-options?))
        :ret (hs/async ::engagement))


(defn update!
  "Update the existing engagement at `engagement-id` given `opts`."
  ([engagement-id params]
   (update! engagement-id params {}))
  ([engagement-id params opts]
   (h/patch-req (str "engagements/v1/engagements/" engagement-id)
                (assoc opts :params params))))

(s/fdef update!
        :args (s/cat :engagement-id ::id
                     :params ::update-params
                     :opts (s/? h/request-options?))
        :ret (hs/async ::engagement))
