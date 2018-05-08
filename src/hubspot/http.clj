(ns hubspot.http
  (:require [cheshire.core :as json]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [hubspot.spec :as hs]
            [org.httpkit.client :as http]
            [ring.util.codec :as codec]
            [toolbelt.async :as ta]
            [toolbelt.core :as tb]))


;; ==============================================================================
;; global =======================================================================
;; ==============================================================================


(def ^:private base-url
  "https://api.hubapi.com/")


;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


(s/def ::api-key
  string?)

(s/def ::method
  #{:get :post :delete :patch})

(s/def ::endpoint
  string?)

(s/def ::out-ch
  ta/chan?)

(s/def ::params
  map?)

(s/def ::client-options
  map?)

(s/def ::opts
  (s/keys :opt-un [::client-options ::api-key ::out-ch ::params]))

(s/def ::api-call
  (s/and ::opts (s/keys :req-un [::method ::endpoint])))

;; (s/def ::not-opts
;;   #(empty? (select-keys % [:client-options :api-key :out-ch])))

;; (s/def ::opts*
;;   (s/or :opts (s/keys :req-un [::params]
;;                       :opt-un [::api-key ::out-ch ::client-options])
;;         :params (s/and map? ::not-opts)))


(defn opts?
  "Produces a spec that will validate the options. The zero-arity version does not
  validate the `:params` map--when given a single argument (a spec), it will be
  used to spec the `:params`."
  ([]
   (partial s/valid? ::opts))
  ([params-spec]
   (s/and (comp (partial s/valid? (s/or :empty empty? :params params-spec)) :params)
          ::opts)))


(defn request-options?
  "Is the argument a valid request options map?"
  [x]
  (s/valid? ::opts x))


;; ==============================================================================
;; authorization ================================================================
;; ==============================================================================


(def ^:dynamic *api-key* nil)


(defn api-key []
  *api-key*)

(s/fdef api-key :ret (hs/maybe ::api-key))


(defmacro with-api-key [k & forms]
  `(binding [*api-key* ~k]
     ~@forms))

(s/fdef with-api-key
        :args (s/cat :key (s/or :symbol symbol? :key ::api-key)
                     :forms (s/* list?))
        :ret list?)


(defn use-api-key!
  "Permanently sets a base api key. The key can still be overridden on a
  per-thread basis using with-api-key."
  [s]
  (alter-var-root #'*api-key* (constantly s)))


;; ==============================================================================
;; API ==========================================================================
;; ==============================================================================


(defn- url-endpoint
  "URL for calling a endpoint."
  [endpoint]
  (str base-url endpoint))


(defn get-http-method [k]
  (get {:get    http/get
        :post   http/post
        :delete http/delete
        :patch  http/patch} k))


(defn- encode-params
  [method params]
  (case method
    :get [:query-params params]
    [:body (json/encode (dissoc params :hapikey))]))


(defn prepare-params
  [api-key method params]
  (let [[k params] (->> (assoc params :hapikey api-key) (encode-params method))]
    (merge
     {:throw-exceptions false
      k                 params}
     (when (not= method :get)
       {:query-params {:hapikey api-key}
        :headers      {"Content-Type" "application/json"}}))))


(defn parse-response-body
  [res]
  (when-not (= "0" (get-in res [:headers :content-length]))
    (json/parse-string (:body res) keyword)))


(defn- process [res]
  (try
    (let [body (parse-response-body res)]
      (if (= (:status body) "error")
        {:error body}
        body))
    (catch Throwable t
      {:error {:status     "error"
               :message    (.getMessage t)
               :error-data (ex-data t)}})))


(defn- respond-sync
  [params {:keys [throw-on-error?] :or {throw-on-error? true}}]
  (let [response                @(http/request params)
        {:keys [error] :as res} (process response)]
    (if (and throw-on-error? (some? error))
      (throw (ex-info (get-in res [:error :message]) res))
      res)))


(defn- respond-async
  [params {:keys [out-ch throw-on-error?] :or {throw-on-error? true}}]
  (http/request params
                (fn [res]
                  (let [{:keys [error] :as res} (process res)]
                    (->> (if (and throw-on-error? (some? error))
                           (ex-info (get-in res [:error :message]) res)
                           res)
                         (a/put! out-ch)))
                  (a/close! out-ch))))


(defn api-call
  "Call an API method on Hubspot. If an output channel is supplied, the method
  will place the result in that channel; if not, returns synchronously."
  [{:keys [params client-options api-key method endpoint out-ch throw-on-error?]
    :or   {params         {}
           client-options {}
           api-key        (api-key)}
    :as   opts}]
  (assert (some? api-key) "API Key must not be nil.")
  (let [url    (url-endpoint endpoint)
        params (->> (prepare-params api-key method params)
                    (merge {:method method :url url}))]
    (if-not (some? out-ch)
      (respond-sync params opts)
      (do (respond-async params opts)
          out-ch))))

(s/fdef api-call
        :args (s/cat :opts ::api-call)
        :ret (s/or :result map? :out-ch ::out-ch))


(defmacro defapi
  "Generates a synchronous and async version of the same function."
  [sym method]
  `(defn ~sym
     ([endpoint#]
      (~sym endpoint# {}))
     ([endpoint# opts#]
      (api-call
       (assoc opts#
              :method ~method
              :endpoint endpoint#)))))

(s/fdef defapi
        :args (s/cat :symbol symbol? :method ::method)
        :ret list?)

(defapi post-req :post)
(defapi patch-req :patch)
(defapi get-req :get)
(defapi delete-req :delete)
