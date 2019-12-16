(ns lsystems.examples
  (:require
    [lsystems.core :refer [nth-step]]
    [lsystems.turtle :refer :all]
    [clojure2d.core :as c]))

(defn fractal-binary-tree []
  (setup-window-and-execute-state
    (nth-step {1 '(1 1) 0 '(1 "[" 0 "]" 0)} 0 9)
    {0 (fn [s] (forward s 0.2))
     1 (fn [s] (forward s 0.4))
     "[" (fn [s] (-> s (push-pos-and-angle) (rotate (- 45))))
     "]" (fn [s] (-> s (pop-pos-and-angle) (rotate (+ 45))))}
    :name "Fractal Binary Tree"))

(defn fractal-plant []
  (let [width 600 height 600]
    (setup-window-and-execute-state
      (nth-step {\X (seq "F+[[X]-X]-F[-FX]+X") \F '(\F \F)}
                \X
                6)
      {\F (fn [s] (forward s 10))
       \- (fn [s] (rotate s (- 25)))
       \+ (fn [s] (rotate s (+ 25)))
       \[ (fn [s] (push-pos-and-angle s))
       \] (fn [s] (pop-pos-and-angle s))}

      :name "Fractal plant" :facing -25
      :width width :height height :auto-resize-padding 50
      :canvas-function (fn [canvas]
                         (c/gradient-mode canvas 0 0 :white 0 height :light-green)
                         (c/rect canvas 0 0 width height)
                         (c/set-color canvas :forest-green)
                         (c/set-stroke canvas 1.3))
      :export-file-name "fractal-plant.jpg")))

(def all-examples [#'fractal-binary-tree #'fractal-plant])

(defn -main
  "Parse arguments and run the given example"
  [& args]
  (let [fn-name (fn [f] (:name (meta f)))]
    (if (or (empty? args) (some #{"--list"} args))          ;; is argument list empty or contains "--list"?
      ;; list all the examples
      (do (println "examples:")
          (doseq [example all-examples]
            (println (fn-name example))))
      ;; get the example
      (let [chosen-example (last args)
            index (.indexOf (map (comp str fn-name) all-examples) chosen-example)]
        (if-not (neg? index)
          (do (println (str "running example: " chosen-example "..."))
              ((all-examples index)))
          (println (str "example " chosen-example " not found")))))))