(defproject starcity/hubspot-clj "0.3.2-SNAPSHOT"
  :description "Clojure bindings to the Hubspot API."
  :url "https://github.com/starcity-properties/hubspot-clj"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [starcity/toolbelt-async "0.4.0"]
                 [starcity/toolbelt-core "0.3.0"]
                 [ring/ring-core "1.6.3"]
                 [http-kit "2.2.0"]
                 [cheshire "5.8.0"]]
  :deploy-repositories [["releases" {:url   "https://clojars.org/repo"
                                     :creds :gpg}]])
