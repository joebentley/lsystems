(ns lsystems.utils
  (:require [environ.core :refer [env]]))

(defn log [message]
  (if (env :debug?) (println (str (java.util.Date.) ": " message))))