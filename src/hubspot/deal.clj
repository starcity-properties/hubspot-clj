(ns hubspot.deal
  (:require [clojure.spec.alpha :as s]
            [hubspot.http :as h]
            [hubspot.spec :as hs]
            [toolbelt.core :as tb]))


;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


(s/def ::vid
  integer?)

(s/def ::associatedCompanyIds
  (s/+ ::vid))

(s/def ::associatedVids
  (s/+ ::vid))

(s/def ::associations
  (s/keys :opt-un [::associatedCompanyIds ::associatedVids]))


;; ==============================================================================
;; HTTP API =====================================================================
;; ==============================================================================


(defn- params->properties
  [params]
  (reduce (fn [acc [k v]] (conj acc {:name k :value v})) [] params))


(defn create!
  "Create a new deal."
  ([params]
   (create! params {}))
  ([{:keys [properties associations]} opts]
   (h/post-req "deals/v1/deal"
               (-> opts
                   (assoc-in [:params :associations] associations)
                   (assoc-in [:params :properties] (params->properties properties))))))



(defn fetch
  "Fetch a deal by vid."
  ([id]
   (fetch id {} {}))
  ([id params]
   (fetch id params {}))
  ([id params opts]
   (h/get-req (format "deals/v1/deal/%s" id)
              (assoc opts :params params))))


(defn update!
  ([id params]
   (update! id params {}))
  ([id {:keys [properties associations]} opts]
   (let [params (tb/assoc-when
                 {}
                 :associations associations
                 :properties (when-some [ps properties]
                               (params->properties properties)))]
     (h/put-req (format "deals/v1/deal/%s" id) (assoc opts :params params)))))


(comment

  (h/with-api-key ""
    (update! 293488123 {:properties {:dealname             "Josh Lehman"
                                     :dealstage            "qualifiedtobuy"
                                     :application_activity "In-progress"}}))

  )
