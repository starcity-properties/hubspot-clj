(ns hubspot.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [toolbelt.async :as ta]))


(defn maybe [spec]
  (s/or :spec spec :nothing nil?))


(defn channel
  "Takes a spec and returns a spec for a channel. The inner spec is ignored, and
  used just for documentation purposes."
  ([] (channel any?))
  ([spec] ta/chan?))


(defn async
  "Takes a spec and returns an either spec for the passed-in inner spec OR a
  channel."
  ([] (async any?))
  ([spec]
   (s/or :spec spec :channel (channel spec))))


(s/def ::email
  (s/and string? #(string/includes? % "@")))


(defn email?
  "Is the argument an email?"
  [x]
  (s/valid? ::email x))
