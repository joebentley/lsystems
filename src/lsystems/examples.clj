(ns lsystems.examples
  (:require
    [lsystems.core :refer [nth-step]]
    [lsystems.turtle :refer :all]
    [clojure2d.core :as c]
    [clojure2d.color :as col]))

(defn fractal-binary-tree []
  (render-to-canvas-by-executing-state-with-rules
    (nth-step {1 '(1 1) 0 '(1 "[" 0 "]" 0)} 0 9)
    {0 (fn [s] (forward s 0.2))
     1 (fn [s] (forward s 0.4))
     "[" (fn [s] (-> s (push-pos-and-angle) (rotate (- 45))))
     "]" (fn [s] (-> s (pop-pos-and-angle) (rotate (+ 45))))}

    (fn [canvas]
      (show-window canvas "Fractal binary tree")
      (save-canvas-to-file canvas "example-renders/fractal-binary-tree.jpg"))
    :auto-resize-padding 50))

(defn fractal-plant []
  (let [width 600 height 600]
    (render-to-canvas-by-executing-state-with-rules
      (nth-step {\X (seq "F+[[X]-X]-F[-FX]+X") \F '(\F \F)}
                \X
                6)
      (standard-rule-set 10 25)

      (fn [canvas]
        (show-window canvas "Fractal plant")
        (save-canvas-to-file canvas "example-renders/fractal-plant.jpg"))

      :facing -25 :width width :height height :auto-resize-padding 50
      :canvas-function (fn [canvas]
                         (c/gradient-mode canvas 0 0 :white 0 height :light-green)
                         (c/rect canvas 0 0 width height)
                         (c/set-color canvas :forest-green)
                         (c/set-stroke canvas 1.3)))))

(defn dragon-curve []
  (render-to-canvas-by-executing-state-with-rules
    (nth-step {\X (seq "X+YF+") \Y (seq "-FX-Y")}
              '(\F\X)
              10)
    {\F (fn [s] (forward s 10))
     \- (fn [s] (rotate s (- 90)))
     \+ (fn [s] (rotate s (+ 90)))}

    (fn [canvas]
      (show-window canvas "Dragon curve")
      (save-canvas-to-file canvas "example-renders/dragon-curve.jpg"))

    :height 400 :auto-resize-padding 50
    :canvas-function (fn [canvas]
                       (c/set-background canvas :skyblue)
                       (c/set-color canvas :white)
                       (c/set-stroke canvas 2))))

(defn abop-fig1.24 []
  "Example of using render-to-canvas-grid

  Source: http://algorithmicbotany.org/papers/abop/abop-ch1.pdf"
  (let [palette (take-last 6 (col/resample 10 (col/palette-presets :greens-6)))
        width 2000 height 2000]
    (render-to-canvas-grid
      3 2 width height

      (fn [canvas]
        ;(show-window canvas "abop-fig1.24")
        (save-canvas-to-file canvas "example-renders/abop-fig1.24.png"))

      (map-indexed
           ;; set color from palette for each figure
           (fn [i m] (assoc m :canvas-function (fn [canvas] (c/set-color canvas (nth palette i)))))
           (list
             {:state (nth-step {\F (seq "F[+F]F[-F]F")} \F 5)
              :rules (standard-rule-set 10 25.7)}
             {:state (nth-step {\F (seq "F[+F]F[-F][F]")} \F 5)
              :rules (standard-rule-set 10 20)}
             {:state (nth-step {\F (seq "FF-[-F+F+F]+[+F-F-F]")} \F 4)
              :rules (standard-rule-set 10 22.5)}
             {:state (nth-step {\X (seq "F[+X]F[-X]+X") \F '(\F \F)} \X 7)
              :rules (standard-rule-set 10 20)}
             {:state (nth-step {\X (seq "F[+X][-X]FX") \F '(\F \F)} \X 7)
              :rules (standard-rule-set 10 25.7)}
             {:state (nth-step {\X (seq "F-[[X]+X]+F[+FX]-X") \F '(\F \F)} \X 5)
              :rules (standard-rule-set 10 22.5)}))

      :canvas-function (fn [canvas]
                         (c/gradient-mode canvas 0 0 :white width height :light-blue)
                         (c/rect canvas 0 0 width height)
                         (c/set-stroke canvas 3))
      :padding 50)))

(def all-examples [#'fractal-binary-tree #'fractal-plant #'dragon-curve #'abop-fig1.24])

(defn -main
  "Parse arguments and run the given example"
  [& args]
  (let [fn-name (fn [f] (:name (meta f)))]
    (if (or (empty? args) (some #{"--list"} args))          ;; is argument list empty or contains "--list"?
      ;; list all the examples
      (do (println "examples:")
          (doseq [example all-examples]
            (println (fn-name example))))
      (if (some #{"--all"} args)
        ;; do all of the examples
        (doseq [example all-examples] (example))
        ;; get the example
        (let [chosen-example (last args)
              index (.indexOf (map (comp str fn-name) all-examples) chosen-example)]
          (if-not (neg? index)
            (do (println (str "running example: " chosen-example "..."))
                ((all-examples index)))
            (println (str "example " chosen-example " not found"))))))))