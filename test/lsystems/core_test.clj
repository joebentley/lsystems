(ns lsystems.core-test
  (:require [clojure.test :refer :all]
            [lsystems.core :refer :all]))


(deftest test-combine-keywords
  (testing "Return nil if called with no args"
    (is (nil? (combine-keywords))))
  (testing "Return given keyword if called with one arg"
    (is (= (combine-keywords :a) :a)))
  (testing "Combine two properly"
    (is (= (combine-keywords :a :b) :ab)))
  (testing "Combine three (with third having two chars) properly"
    (is (= (combine-keywords :a :b :cd) :abcd))))

(deftest test-string-to-keywords
  (testing "Return empty list on empty string"
    (is (= (string-to-keywords "") '())))
  (testing "Returns expected list of strings"
    (is (= (string-to-keywords "abcd") '(:a :b :c :d)))))

(deftest test-state-to-string
  (testing "Return empty string on empty list"
    (is (= (state-to-string '()) "")))
  (testing "Returns expected string from list, combining all characters"
    (is (= (state-to-string '(:a :b :cd)) "abcd")))
  (testing "Should handle strings and numbers okay"
    (is (= (state-to-string '(:a :b "cd" :z 2)) "abcdz2"))))

(deftest test-adding-production
  (testing "Should add new production to map"
    (is (= (add-production new-productions :a '(:a :b)) {:a '(:a :b)}))))

(deftest test-step
  (let [productions (add-production new-productions :a '(:a :b))]
    (testing "Advance the state as expected, and work when given a keyword rather than list"
      (is (= (step productions :a) [:a :b])))
    (testing "Step using threading operator"
      (is (= (->> :a
                  (step productions)
                  (step productions))
             [:a :b :b]))))
  ; https://en.wikipedia.org/wiki/L-system#Examples_of_L-systems
  (testing "7th step of Algae"
    (let [algae-system {:A [:A :B] :B :A}]
      (is (= (state-to-string (nth-step algae-system :A 7))
             "ABAABABAABAABABAABABAABAABABAABAAB"))))
  (testing "3rd step of Fractal (binary) tree"              ;; also tests using numbers and strings as variables
    (let [binary-tree-rules {1 [1 1] 0 [1 "[" 0 "]" 0]}]
      (is (= (state-to-string (nth-step binary-tree-rules 0 3))
             "1111[11[1[0]0]1[0]0]11[1[0]0]1[0]0"))))
  (testing "Using rules from strings"
    (let [binary-tree-rules {\1 (seq "11") \0 (seq "1[0]0")}]
      (is (= (state-to-string (nth-step binary-tree-rules \0 3))
             "1111[11[1[0]0]1[0]0]11[1[0]0]1[0]0")))))

(deftest test-step-with-productions
  (testing "3rd step of Fractal binary tree"
    (let* [binary-tree-rules {1 '(1 1) 0 '(1 "[" 0 "]" 0)}
           step (step-with-productions binary-tree-rules)]
      (is (= (state-to-string (->> 0 (step) (step) (step)))
             "1111[11[1[0]0]1[0]0]11[1[0]0]1[0]0")))))

(deftest test-bind-productions
  (testing "3rd step of Fractal (binary) tree using threading operators"
    (is (= (bind-productions {1 '(1 1) 0 '(1 "[" 0 "]" 0)}
                             (->> 0 (step) (step) (step) (state-to-string)))
           "1111[11[1[0]0]1[0]0]11[1[0]0]1[0]0")))
  (testing "3rd step of Fractal without threading operator")
  (is (= (state-to-string (bind-productions {1 '(1 1) 0 '(1 "[" 0 "]" 0)}
                          (step (step (step 0)))))
          "1111[11[1[0]0]1[0]0]11[1[0]0]1[0]0")))