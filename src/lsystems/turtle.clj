(ns lsystems.turtle
  "Everything to do with drawing the L-system to the screen.

  We use the excellent [Clojure2D](https://github.com/Clojure2D/clojure2d) to do the rendering, and we implement
  a simple version of [turtle graphics](https://en.wikipedia.org/wiki/Turtle_graphics)–where a pen is
  controlled by movement commands and lines are drawn out along its movement path.

  The central object is the pen state, produced by `(new-pen-state ...)`, which stores the current pen position
  and orientation, whether or not the pen is down, a stack for pushing and popping the pen position and orientation,
  and a list for storing the generated line segments, which are added by the function `(forward [pen-state by-pixels])`
  if the pen is down.

  The actual rendering is done by the `(draw-lines [canvas line-segments])` function.

  Line segments are of the form of a map `{ :from { :x :y } :to { :x :y } }`."
  {:doc/format :markdown}
  (:require
    [clojure2d.core :as c]
    ;[clojure2d.color :as col]
    [fastmath.core :as m]))

(defn new-pen-state
  "Creates a new pen state object. Keeps track of the pens current position, its orientation, whether or not it is
  down, a stack for pushing and popping the current position and orientation, and a list for storing calculated line
  segments to be rendered later."
  [x y & {:keys [facing pen-is-down?] :or {facing 0 pen-is-down? true}}]
  (if (not (and (integer? x) (integer? y))) (throw (IllegalArgumentException. "x and y must be integers")))
  {:x x :y y                                                ;; x y in screen coordinates (0, 0) is top left, increasing x goes right, increasing y goes down
   :facing facing                                           ;; facing is clockwise direction in degrees relative to north
   :pen-is-down? pen-is-down?                               ;; are we currently drawing?
   :stack '()                                               ;; for pushing and popping position and angle
   :lines '()                                               ;; list of line segments of form { :from { :x :y } :to { :x :y } }
   })

(defn get-pos-and-angle
  "Get the position and facing angle from the pen state."
  [pen-state]
  (select-keys pen-state [:x :y :facing]))

;; Factor to convert from degree to radians
(def ^:private deg-to-rad (/ m/-PI 180.0))

(defn new-line-segment
  "Create a new line segment."
  [from-x from-y to-x to-y]
  { :from { :x from-x :y from-y } :to { :x to-x :y to-y } })

(defn forward
  "Move the pen forward by `by-pixels` in the direction specified by (pen-state :facing), adding a line segment
  to (pen-state :lines) if the pen is down. Returns the updated pen-state."
  [pen-state by-pixels]
  (let [{old-x :x old-y :y facing :facing lines :lines} pen-state
        new-pos {:x (+ old-x (* (m/sin (* facing deg-to-rad)) by-pixels))
                 :y (+ old-y (* (m/cos (+ (* facing deg-to-rad) m/-PI)) by-pixels))} ;; screen-space y is down
        ;; store the new line segment only if pen is down
        new-lines (if (pen-state :pen-is-down?)
                    (conj lines (new-line-segment old-x old-y (new-pos :x) (new-pos :y)))
                    lines)]

    ;; return the updated state
    (merge pen-state new-pos {:lines new-lines})))

(defn rotate
  "Rotate the pen's facing direction and return the updated state."
  [pen-state by-angle]
  (assoc pen-state :facing (+ (pen-state :facing) by-angle)))

(defn pen-up
  "Forward will no longer draw lines, it will just move the cursor."
  [pen-state]
  (assoc pen-state :pen-is-down? false))

(defn pen-down
  "Forward will draw lines, as well as moving the cursor."
  [pen-state]
  (assoc pen-state :pen-is-down? true))

(defn push-pos-and-angle
  "Push the pens position and facing direction onto the stack."
  [pen-state]
  (let [{stack :stack} pen-state]
    (assoc pen-state :stack (conj stack (get-pos-and-angle pen-state)))))

(defn pop-pos-and-angle
  "Pop the pens position and facing direction off the stack and set the current position and facing angle to it."
  [pen-state]
  (let [{stack :stack} pen-state
        pos-and-angle (peek stack)]
    (merge (assoc pen-state :stack (pop stack))
           pos-and-angle)))

(defn execute-state-with-rules
  "Take the L-system state, a rules map from L-system characters to functions that take and return
  the current pen state, and the initial pen state, and executes each character of the L-system
  state in turn, returning the pen state containing the finished line segments in (pen-state :lines)."
  [state rules pen-state]
  (if (or (empty? state) (nil? state))
    pen-state
    (let [current (first state)
          ;; if we don't find the current state in the rules, use identity
          state-transformation-fn (get rules current identity)]
      (recur (rest state) rules (state-transformation-fn pen-state)))))

(defn draw-lines
  "Draw the line segments given by `line-segments` onto the canvas."
  [canvas line-segments]
  (doseq [{from :from to :to} line-segments]
    (c/line canvas (from :x) (from :y) (to :x) (to :y))))

(defn make-canvas-and-window [width height window-name]
  "Make canvas and show window, returning both canvas and window (in a map w/ keywords :canvas, :window)."
  (if (not (and (integer? width) (integer? height)))
    (throw (IllegalArgumentException. "width and height must be integers")))
  (if (not (string? window-name)) (throw (IllegalArgumentException. "window-name must be a string")))

  ;; set everything as fast as possible
  (set! *warn-on-reflection* true)
  (set! *unchecked-math* :warn-on-boxed)
  (m/use-primitive-operators)

  (let [my-canvas (c/canvas width height :high)]
    {:canvas my-canvas
     :window (c/show-window my-canvas window-name)}))

(defn- map-line-segments
  "Map over all line segments, applying f-x to the x coord of both :from and :to, and same for y"
  [f-x f-y line-segments]
  (map (fn [{from :from to :to}] {:from {:x (f-x (from :x)) :y (f-y (from :y))}
                                  :to   {:x (f-x (to :x))   :y (f-y (to :y))}}) line-segments))

(defn- line-segments-to-points
  "Get all points from the given list of line segments."
  [line-segments]
  (flatten (map (fn [segment] (list (segment :from) (segment :to))) line-segments)))

(defn fit-line-segments-to-screen
  "Resize and move all line segments so that they fit on screen. Can optionally pass :padding keyword
  to pad figure on every side of screen."
  [width height line-segments & {:keys [padding] :or {padding 0}}]

  (let [all-points (line-segments-to-points line-segments)
        min-x (apply min (map :x all-points)) min-y (apply min (map :y all-points))
        ;; transform all points so that minimum x-y is at origin
        transformed (map-line-segments (fn [x] (- x min-x))
                                       (fn [y] (- y min-y)) line-segments)
        ;; find maximum x or y extent of the transformed figure
        all-points (line-segments-to-points transformed)
        max-coord (apply max (concat (map :x all-points) (map :y all-points)))
        ;; normalize everything by max coord. Now everything is between [0, 1]
        scaled (map-line-segments (fn [x] (/ x max-coord))
                                  (fn [y] (/ y max-coord)) transformed)
        ;; apply padding
        figure-size (- (max width height) (* 2 padding))
        scaled (if (zero? padding)
                 scaled
                 (map-line-segments (fn [x] (+ padding (* x figure-size)))
                                    (fn [y] (+ padding (* y figure-size))) scaled))]
    ;(prn (apply max (concat (map :x (line-segments-to-points scaled)) (map :y (line-segments-to-points scaled)))))
    scaled))

(defn setup-window-and-execute-state
  "Setup a window and canvas and pen-state with given options and execute a given L-system state.

  The :canvas-function key should be a function that takes a canvas object and can be used to do extra
  drawing or changing the canvas settings.

  Use the :auto-resize? key along with :auto-resize-padding to automatically resize the drawn figure to
  fit on the screen.

  Description of all keys:

  - `:name` string, name of the window, default: \"L-system\"
  - `:width` `:height` integer, width and height in pixels of the window, default 600 600
  - `:initial-x` `:initial-y` integer, initial position of the pen. Does nothing if `auto-resize?` is true. default 300 300
  - `:facing` float, the initial facing direction of the pen. default 0
  - `:canvas-function` described above. default `identity` (do nothing)
  - `:auto-resize?` boolean, whether or not to automatically fit the drawing to the canvas. default `true`
  - `:auto-resize-padding` integer, number of pixels padding when fitting the drawing to the canvas. default 100
  - `:export-file-name` string, file name to save the image at, doesn't save if `nil`. default `nil`"
  {:doc/format :markdown}
  [state rules & {:keys [name width height initial-x initial-y facing canvas-function
                         auto-resize? auto-resize-padding export-file-name]
                  :or   {name "L-system" width 600 height 600 initial-x 300 initial-y 300 facing 0
                         canvas-function identity auto-resize? true auto-resize-padding 100
                         export-file-name nil}}]
    (c/with-canvas
      [canvas ((make-canvas-and-window width height name) :canvas)]
      (c/set-color canvas 0 0 0)                            ;; set stroke to black
      (c/set-background canvas 255 255 255)                 ;; set background to white
      (canvas-function canvas)                              ;; execute the passed functions
      (let [pen-state (new-pen-state (int initial-x) (int initial-y) :facing facing)
            ;; execute the state with the given rules to get the line segments that the pen draws out
            executed-state (execute-state-with-rules state rules pen-state)
            line-segments (executed-state :lines)
            line-segments (if auto-resize? (fit-line-segments-to-screen width height
                                                                        line-segments :padding auto-resize-padding)
                                           line-segments)]

        ;; draw the calculated line segments
        (draw-lines canvas line-segments)
        ;; save image file
        (if (not (nil? export-file-name)) (c/save canvas export-file-name)))))
