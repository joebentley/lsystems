(ns lsystems.core
  (:require [clojure.walk :as walk]))

(defn keyword-name
  "Get keyword name without the colon"
  [keyword]
  (apply str (rest (str keyword))))

(defn combine-keywords
  "Takes multiple keywords and combines them into one, e.g. (combine-keywords :a :b) -> :ab"
  [& keywords]
  (if (empty? keywords)
    nil
    (keyword (apply str (map keyword-name keywords)))))

(defn string-to-keywords
  "Takes string of characters and turns them into keywords"
  [string]
  (map (comp keyword str) (seq string)))

(defn state-to-string
  "Convert list of keywords, numbers, strings to a single string"
  [keywords]
  (apply str (map (fn [x] (if (keyword? x) (keyword-name x) (str x))) keywords)))

(def new-productions
  "The productions are written as a hash map"
  (hash-map))

(defn add-production
  "Add new production from keyword to list of keywords"
  [productions from to]
  (assoc productions from to))

(defn step
  "Return next iteration of state using the given productions as a vector"
  [productions state]
  (if (not (vector? state))
    ;; wrap in vector if not already a vector
    (step productions (if (seq? state) (vec state)          ;; use vector instead of passed selection for performance
                                       (vector state)))     ;; otherwise just wrap in one
    (vec (flatten (map (fn [v] (get productions v v)) state)))))

(defn step-with-productions
  "Bind step function to given productions"
  [productions]
  (fn [state] (step productions state)))

(defmacro bind-productions
  "Bind productions in all following calls.
  Should probably use `(let [step (step-with-productions productions)] ...)` instead."
  [productions & exprs]
  `(do ~@(walk/prewalk (fn [expr] (if (and (list? expr) (= (first expr) 'step))
                                    `(step ~productions ~@(rest expr))
                                    expr))
                       exprs)))

(defn nth-step
  "Return the nth step using the productions and initial state"
  [productions state n]
  (let [step (step-with-productions productions)]
    (nth (iterate step state) n)))