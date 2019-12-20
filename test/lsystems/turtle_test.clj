(ns lsystems.turtle-test
  (:require [clojure.test :refer :all]
            [lsystems.turtle :refer :all]
            [lsystems.utils :refer :all]))

(deftest testing-turtle-movement
  (let [ps (new-pen-state 0 0)
        moved-once (forward ps 10)]
    (testing "Forward adds new line segment"
      (is (= (moved-once :lines) '({:from {:x 0 :y 0} :to {:x 0.0 :y -10.0}}))))
    (testing "Forward twice extends last line segment"
      (is (= ((forward moved-once 20) :lines) '({:from {:x 0 :y 0} :to {:x 0.0 :y -30.0}}))))
    (testing "Rotating then moving doesn't extend last line segment, but instead adds a new one"
      (let [moved-rotated-moved (forward (rotate moved-once 25) 10)]
        (is (= (count (moved-rotated-moved :lines)) 2))))
    (testing "Pushing state, moving, then popping resets to last position and angle but keeps the created lines"
      (let [pushed-moved-rotated-moved (forward (rotate (push-pos-and-angle moved-once) 25) 10)
            popped (pop-pos-and-angle pushed-moved-rotated-moved)]
        (is (= (count (pushed-moved-rotated-moved :stack)) 1))
        (is (= (count (popped :lines)) 2))
        (is (and (== (popped :x) 0) (== (popped :y) -10) (= (popped :facing) 0)))))
    (testing "If pen is up position should change but line segment should not be added"
      (let [moved (forward (pen-up ps) 10)]
        (is (empty? (moved :lines)))
        (is (and (== (moved :x) 0) (== (moved :y) -10)))
        (is (== ((forward moved 10) :y) -20))
        (let [pen-down-moved (forward (pen-down moved) 10)]
          (testing "If pen put back down it will draw a line"
            (is (== (pen-down-moved :y) -20))
            (is (= (pen-down-moved :lines) '({:from {:x 0.0 :y -10.0} :to {:x 0.0 :y -20.0}})))))))))

(deftest testing-execute-state-with-rules
  (let [state (seq "F+F") rules (standard-rule-set 10 90) ps (new-pen-state 0 0)]
    (testing "State is executed properly using the standard rule set"
      (let [executed (execute-state-with-rules state rules ps)]
        ;; TODO: check directions, I feel like this should be +10 not -10
        (is (and (approx-eq (executed :x) -10) (approx-eq (executed :y) -10)))
        (is (= (count (executed :lines)) 2))))
    (testing "Rules are called in order"
      (= (execute-state-with-rules
           (seq "123")
           {\1 (fn [s] (assoc s :lines (conj (s :lines) 1)))
            \2 (fn [s] (assoc s :lines (conj (s :lines) 2)))
            \3 (fn [s] (assoc s :lines (conj (s :lines) 3)))}
           ps)
         '(3 2 1))) ;; lists push to front, not to the back
    (testing "Missing rule uses identity instead"
      (= (execute-state-with-rules \I {} ps) ps))))
