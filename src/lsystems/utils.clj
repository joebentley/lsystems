(ns lsystems.utils
  (:require [environ.core :refer [env]] [fastmath.core :as m]))

(defn log [message]
  (if (env :debug?) (println (str (java.util.Date.) ": " message))))

(defn approx-eq [a b]
  (< (m/abs (- a b)) 1e-10))