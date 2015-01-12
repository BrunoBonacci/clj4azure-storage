(defproject clj4azure/clj4azure-storage "0.1.0-SNAPSHOT"
  :description "Clojure wrapper for Azure Storage SDK"
  :url "https://github.com/BrunoBonacci/clj4azure-storage"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.microsoft.azure/azure-storage "1.2.0"]  ; azure blob sdk
                 [org.clojure/tools.logging "0.3.1"]          ; logging
                 [org.slf4j/slf4j-api "1.6.4"]                ; logging
                 ]

  :profiles {:dev {:dependencies [[org.slf4j/slf4j-simple "1.6.4"]]
                   :jvm-opts ["-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"]}}

  )
