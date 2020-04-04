(ns dactyl-keyboard.dactyl
  (:refer-clojure :exclude [use import])
  (:require [clojure.core.matrix :refer [array matrix mmul]]
            [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [unicode-math.core :refer :all]))

(defn deg2rad [degrees]
  (* (/ degrees 180) pi))

(def is-preview true)

(defn rcube [sx sy sz rr] (if is-preview
                            (cube sx sy sz)
                            (->>
                              (hull (for [x [-1 1] y [-1 1] z [-1 1]]
                                      (translate [(* x (- (/ sx 2) (/ rr 2)))
                                                  (* y (- (/ sy 2) (/ rr 2)))
                                                  (* z (- (/ sz 2) (/ rr 2)))
                                                  ]
                                                 (sphere (/ rr 2)))
                                      ))
                              (with-fn 20)
                              )
                            )
  )

(defn rcylinder [radius height] 
  (if is-preview
    (cylinder radius height)
    (->>
      (hull
        (translate [0 0 (- (/ height 2) (/ radius 2))] (sphere (/ radius 2)))
        (translate [0 0 (+ (/ height -2) (/ radius 2))] (sphere (/ radius 2)))
        )
      (with-fn 20)
      )
    )
  )

;;;;;;;;;;;;;;;;;;;;;;
;; Shape parameters ;;
;;;;;;;;;;;;;;;;;;;;;;

(def nrows 4)
(def ncols 6)

(def α (/ π 12))                        ; curvature of the columns
(def β (/ π 36))                        ; curvature of the rows
(def centerrow (- nrows 2.5))             ; controls front-back tilt
(def centercol 2)                       ; controls left-right tilt / tenting (higher number is more tenting)
(def tenting-angle (/ π 12))            ; or, change this for more precise tenting control
(def column-style
  (if (> nrows 5) :orthographic :standard))  ; options include :standard, :orthographic, and :fixed
; (def column-style :fixed)
(def pinky-15u false)

(defn column-offset [column] (cond
                               (= column 2) [0 2.82 -4.0]
                               (>= column 4) [0 -14 4.64]            ; original [0 -5.8 5.64]
                               :else [0 0 0]))

(def thumb-offsets [4 -3 7])

(def keyboard-z-offset 9)               ; controls overall height; original=9 with centercol=3; use 16 for centercol=2

(def extra-width 2.5)                   ; extra space between the base of keys; original= 2
(def extra-height 2.5)                  ; original= 0.5

(def wall-z-offset -5)                 ; original=-15 length of the first downward-sloping part of the wall (negative)
(def wall-xy-offset 5)                  ; offset in the x and/or y direction for the first downward-sloping part of the wall (negative)
(def wall-thickness 3)                  ; wall thickness parameter; originally 5

;; Settings for column-style == :fixed
;; The defaults roughly match Maltron settings
;;   http://patentimages.storage.googleapis.com/EP0219944A2/imgf0002.png
;; Fixed-z overrides the z portion of the column ofsets above.
;; NOTE: THIS DOESN'T WORK QUITE LIKE I'D HOPED.
(def fixed-angles [(deg2rad 10) (deg2rad 10) 0 0 0 (deg2rad -15) (deg2rad -15)])
(def fixed-x [-41.5 -22.5 0 20.3 41.4 65.5 89.6])  ; relative to the middle finger
(def fixed-z [12.1    8.3 0  5   10.7 14.5 17.5])
(def fixed-tenting (deg2rad 0))

;@@@@@@@@@@@@@@@@@@@@@@@@@@@
;;;;;;;;;Wrist rest;;;;;;;;;;
;@@@@@@@@@@@@@@@@@@@@@@@@@@
(def wrist-rest-on 1) 						;;0 for no rest 1 for a rest connection cut out in bottom case
(def wrist-rest-back-height 18)				;;height of the back of the wrist rest--Default 34
(def wrist-rest-angle -1) 			        ;;angle of the wrist rest--Default 20
(def wrist-rest-rotation-angle 100)			;;0 default The angle in counter clockwise the wrist rest is at
(def wrist-rest-ledge 3.5)					;;The height of ledge the silicone wrist rest fits inside
(def wrist-rest-y-angle 0)					;;0 Default.  Controls the wrist rest y axis tilt (left to right)


;;Wrist rest to case connections
(def right_wrist_connecter_x   (if (== ncols 5) 13 25))
(def middle_wrist_connecter_x   (if (== ncols 5) -5 0))
(def left_wrist_connecter_x   (if (== ncols 5) -25 -25))
(def wrist_brse_position [15 -40 0])

;;;;;;;;;;;;;;;;;;;;;;;
;; General variables ;;
;;;;;;;;;;;;;;;;;;;;;;;

(def lastrow (dec nrows))
(def cornerrow (dec lastrow))
(def lastcol (dec ncols))

(def rounding-radius 1)

;;;;;;;;;;;;;;;;;
;; Switch Hole ;;
;;;;;;;;;;;;;;;;;

; If you use Cherry MX or Gateron switches, this can be turned on.
; If you use other switches such as Kailh, you should set this as false

; kailh/aliaz: no nubs, 13.9
; outemu: nubs, 14.05x14.1
(def create-side-nubs? true)
(def keyswitch-height 14.2) ;; Was 14.1, then 14.25, then 13.9 (for snug fit with with aliaz/outemy sky switches)
(def keyswitch-width 14.15)

(def sa-profile-key-height 7.39)

(def plate-thickness 3)
(def side-nub-thickness 4)
(def retention-tab-thickness 1.5)
(def retention-tab-hole-thickness (- plate-thickness retention-tab-thickness))
(def mount-width (+ keyswitch-width 3))
(def mount-height (+ keyswitch-height 3))

(def single-plate
  (let [
        outer-cube (rcube mount-width mount-height plate-thickness rounding-radius)
        inner-cut (cube keyswitch-width keyswitch-height (+ plate-thickness 0.01))
        outer-frame (difference outer-cube inner-cut)
        side-nub (->> (with-fn 30 (cylinder 1 2.75))
                      (rotate (/ π 2) [1 0 0])
                      (translate [(+ (/ keyswitch-width 2)) 0 1])
                      (hull (->> (rcube 1.5 2.75 side-nub-thickness rounding-radius)
                                 (translate [(- (/ mount-width 2) (/ 1.5 2))
                                             0
                                             (/ side-nub-thickness 2)])))
                      (translate [0 0 (- plate-thickness side-nub-thickness)]))
        ]
    (->> (union
           (->> outer-frame (translate [0 0 (/ plate-thickness 2)]))
           side-nub
           (mirror [1 0 0] side-nub)
           )
         )
    )
  )

;;;;;;;;;;;;;;;;;;;;;;;;
;; OLED screen holder ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(def oled-pcb-size [27.35 28.3 plate-thickness])
(def oled-screen-offset [0 -1 0])
(def oled-screen-size [24.65 16.65 (+ 0.1 plate-thickness)])
(def oled-mount-size [23.1 23.75 0.5])
(def oled-holder-width (+ 3 (nth oled-pcb-size 0)))
(def oled-holder-height (+ 3 (nth oled-pcb-size 1)))
(def oled-holder-thickness plate-thickness)
(def oled-holder-size [oled-holder-width oled-holder-height oled-holder-thickness])
(def oled-mount-rotation-x (deg2rad 15))
(def oled-mount-rotation-z (deg2rad -3))

;;;;;;;;;;;;;;;;
;; SA Keycaps ;;
;;;;;;;;;;;;;;;;

(def cap-1u 18)
(def cap-1u-top 12)
(def cap-2u (* cap-1u 2))
(def bl2 (/ cap-1u 2))
(def cap-pressed 1) ; percentage, 1 is fully pressed
(def cap-travel 3) ; how much the key switches depress
(def cap-pos (+ 2 (* (- 1 cap-pressed) cap-travel)))
(def sa-cap
  {1 (let [key-cap (hull (->> (polygon [[bl2 bl2] [bl2 (- bl2)] [(- bl2) (- bl2)] [(- bl2) bl2]])
                              (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                              (translate [0 0 0.05]))
                         (->> (polygon [[6 6] [6 -6] [-6 -6] [-6 6]])
                              (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                              (translate [0 0 sa-profile-key-height])))]
       (->> key-cap
            (translate [0 0 (+ cap-pos plate-thickness)])
            (color [220/255 163/255 163/255 1])))
   1.25 (let [
             bw2 (/ (* cap-1u 1.25) 2)
             tw2 (- bw2 4)
             key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                (translate [0 0 0.05]))
                           (->> (polygon [[tw2 tw2] [(- tw2) tw2] [(- tw2) (- tw2)] [tw2 (- tw2)]])
                                (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                (translate [0 0 sa-profile-key-height])))]
         (->> key-cap
              (translate [0 0 (+ cap-pos plate-thickness)])
              (color [240/255 223/255 175/255 1])))
   1.5 (let [bw2 (/ (* cap-1u 1.5) 2)
             tw2 (- bw2 6)
             key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                (translate [0 0 0.05]))
                           (->> (polygon [[11 6] [-11 6] [-11 -6] [11 -6]])
                                (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                (translate [0 0 sa-profile-key-height])))]
         (->> key-cap
              (translate [0 0 (+ cap-pos plate-thickness)])
              (color [240/255 223/255 175/255 1])))
   2 (let [bw2 (/ (* cap-1u 2) 2)
           tw2 (- bw2 6)
           key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                              (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                              (translate [0 0 0.05]))
                         (->> (polygon [[6 16] [6 -16] [-6 -16] [-6 16]])
                              (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                              (translate [0 0 sa-profile-key-height])))]
       (->> key-cap
            (translate [0 0 (+ cap-pos plate-thickness)])
            (color [127/255 159/255 127/255 1])))
   })

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Placement Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def columns (range 0 ncols))
(def rows (range 0 nrows))

(def cap-top-height (+ plate-thickness sa-profile-key-height))
(def row-radius (+ (/ (/ (+ mount-height extra-height) 2)
                      (Math/sin (/ α 2)))
                   cap-top-height))
(def column-radius (+ (/ (/ (+ mount-width extra-width) 2)
                         (Math/sin (/ β 2)))
                      cap-top-height))
(def column-x-delta (+ -1 (- (* column-radius (Math/sin β)))))

(defn offset-for-column [col]
  (if (and (true? pinky-15u) (= col lastcol)) 5.5 0))

(defn extra-rot-x-for-key [row col]
  (cond
    ;(and (= row 3) (= col 2)) (/ π 8)
    ;(and (= row 3) (= col 3)) (/ π 8)
    :else 0
    ))

(defn extra-rot-y-for-key [row col]
  (cond
    ;(and (= row 3) (= col 2)) (/ π -12)
    ;(and (= row 3) (= col 3)) (/ π -36)
    :else 0
    ))

(defn extra-translate-for-key [row col]
  (cond
    ;(and (= row 3) (= col 2)) [-9 -7 13]
    ;(and (= row 3) (= col 3)) [-2 0 5]
    :else [0 0 0]
    ))

(defn apply-key-geometry [translate-fn rotate-x-fn rotate-y-fn column row shape]
  (let [column-angle (* β (- centercol column))
        placed-shape (->> shape
                          (rotate-x-fn  (extra-rot-x-for-key row column))
                          (rotate-y-fn  (extra-rot-y-for-key row column))
                          (translate-fn (extra-translate-for-key row column))
                          (translate-fn [(offset-for-column column) 0 (- row-radius)])
                          (rotate-x-fn  (* α (- centerrow row)))
                          (translate-fn [0 0 row-radius])
                          (translate-fn [0 0 (- column-radius)])
                          (rotate-y-fn  column-angle)
                          (translate-fn [0 0 column-radius])
                          (translate-fn (column-offset column)))
        column-z-delta (* column-radius (- 1 (Math/cos column-angle)))
        placed-shape-ortho (->> shape
                                (translate-fn [0 0 (- row-radius)])
                                (rotate-x-fn  (* α (- centerrow row)))
                                (translate-fn [0 0 row-radius])
                                (rotate-y-fn  column-angle)
                                (translate-fn [(- (* (- column centercol) column-x-delta)) 0 column-z-delta])
                                (translate-fn (column-offset column)))
        placed-shape-fixed (->> shape
                                (rotate-y-fn  (nth fixed-angles column))
                                (translate-fn [(nth fixed-x column) 0 (nth fixed-z column)])
                                (translate-fn [0 0 (- (+ row-radius (nth fixed-z column)))])
                                (rotate-x-fn  (* α (- centerrow row)))
                                (translate-fn [0 0 (+ row-radius (nth fixed-z column))])
                                (rotate-y-fn  fixed-tenting)
                                (translate-fn [0 (second (column-offset column)) 0]))]
    (->> (case column-style
           :orthographic placed-shape-ortho
           :fixed        placed-shape-fixed
           placed-shape)
         (rotate-y-fn  tenting-angle)
         (translate-fn [0 0 keyboard-z-offset]))))

(defn key-place [column row shape]
  (apply-key-geometry translate
                      (fn [angle obj] (rotate angle [1 0 0] obj))
                      (fn [angle obj] (rotate angle [0 1 0] obj))
                      column row shape))

(defn rotate-around-x [angle position]
  (mmul
   [[1 0 0]
    [0 (Math/cos angle) (- (Math/sin angle))]
    [0 (Math/sin angle)    (Math/cos angle)]]
   position))

(defn rotate-around-y [angle position]
  (mmul
   [[(Math/cos angle)     0 (Math/sin angle)]
    [0                    1 0]
    [(- (Math/sin angle)) 0 (Math/cos angle)]]
   position))

(defn rotate-around-z [angle position]
  (mmul
    [[(Math/cos angle) (- (Math/sin angle)) 0]
     [(Math/sin angle) (Math/cos angle) 0]
     [0                    0 1]]
   position))

(defn key-position [column row position]
  (apply-key-geometry (partial map +) rotate-around-x rotate-around-y column row position))

(def key-holes
  (apply union
         (for [column columns
               row rows
               :when (or (.contains [3] column)
                         (not= row lastrow))]
           (->> single-plate
                (key-place column row)))))

(def caps
  (apply union
         (for [column columns
               row rows
               :when (or (.contains [3] column)
                         (not= row lastrow))]
           (->> (sa-cap (if (and (true? pinky-15u) (= column lastcol)) 1.5 1))
                (key-place column row)))))

;;;;;;;;;;;;;;;;;;;;
;; Web Connectors ;;
;;;;;;;;;;;;;;;;;;;;

(def web-thickness 3)
(def post-size rounding-radius)

;(defn web-post-shape [height] (cube post-size post-size height))
;(defn web-post-shape [height] (with-fn 30 (cylinder (/ post-size 2) height)))
(defn web-post-shape [height] (rcylinder post-size height))

(def web-post (->>
                (web-post-shape web-thickness)
                (translate [0 0 (+ (/ web-thickness -2) plate-thickness)])
                ))

(def oled-post (->> (web-post-shape oled-holder-thickness)
                    (translate [0 0 (+ (/ oled-holder-thickness -2) plate-thickness)])
                    ))

(def post-adj (/ post-size -2))
(def web-post-tr (translate [(- (/ mount-width 2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(def web-post-br (translate [(- (/ mount-width 2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))

(defn web-post-tr-e [x y] (translate [(- (/ mount-width 2) (+ post-adj x)) (- (/ mount-height 2) (+ post-adj y)) 0] web-post))
(defn web-post-tl-e [x y] (translate [(+ (/ mount-width -2) (+ post-adj x)) (- (/ mount-height 2) (+ post-adj y)) 0] web-post))
(defn web-post-bl-e [x y] (translate [(+ (/ mount-width -2) (+ post-adj x)) (+ (/ mount-height -2) (+ post-adj y)) 0] web-post))
(defn web-post-br-e [x y] (translate [(- (/ mount-width 2) (+ post-adj x)) (+ (/ mount-height -2) (+ post-adj y)) 0] web-post))

; wide posts for 1.5u keys in the main cluster

(if (true? pinky-15u)
  (do (def wide-post-tr (translate [(- (/ mount-width 1.2) post-adj)  (- (/ mount-height  2) post-adj) 0] web-post))
      (def wide-post-tl (translate [(+ (/ mount-width -1.2) post-adj) (- (/ mount-height  2) post-adj) 0] web-post))
      (def wide-post-bl (translate [(+ (/ mount-width -1.2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
      (def wide-post-br (translate [(- (/ mount-width 1.2) post-adj)  (+ (/ mount-height -2) post-adj) 0] web-post)))
  (do (def wide-post-tr web-post-tr)
      (def wide-post-tl web-post-tl)
      (def wide-post-bl web-post-bl)
      (def wide-post-br web-post-br)))

(defn triangle-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))

(def connectors
  (apply union
         (concat
          ;; Row connections
          (for [column (range 0 (dec ncols))
                row (range 0 lastrow)]
            (triangle-hulls
             (key-place (inc column) row web-post-tl)
             (key-place column row web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place column row web-post-br)))

          ;; Column connections
          (for [column columns
                row (range 0 cornerrow)]
            (triangle-hulls
             (key-place column row web-post-bl)
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tl)
             (key-place column (inc row) web-post-tr)))

          ;; Diagonal connections
          (for [column (range 0 (dec ncols))
                row (range 0 cornerrow)]
            (triangle-hulls
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place (inc column) (inc row) web-post-tl))))))

;;;;;;;;;;;;
;; Thumbs ;;
;;;;;;;;;;;;

(def thumborigin
  (map + (key-position 1 cornerrow [(/ mount-width 2) (- (/ mount-height 2)) 0])
       thumb-offsets))

;top top right
(defn thumb-ttr-place [shape]
  (->> shape
       (rotate (deg2rad  3) [1 0 0])
       (rotate (deg2rad -2) [0 1 0])
       (rotate (deg2rad 7) [0 0 1])
       (translate thumborigin)
       (translate [3 -5 6])))
;top right
(defn thumb-tr-place [shape]
  (->> shape
       (rotate (deg2rad   7) [1 0 0])
       (rotate (deg2rad -8) [0 1 0])
       (rotate (deg2rad  13) [0 0 1]) ; original 10
       (translate thumborigin)
       (translate [-16 -8 4]))) ; original 1.5u  (translate [-12 -16 3])
;top middle
(defn thumb-tm-place [shape]
  (->> shape
       (rotate (deg2rad   7) [1 0 0])
       (rotate (deg2rad -23) [0 1 0])
       (rotate (deg2rad  19) [0 0 1]) ; original 10
       (translate thumborigin)
       (translate [-33.5 -13 -0.5]))) ; original 1.5u (translate [-32 -15 -2])))
; top left
(defn thumb-tl-place [shape]
  (->> shape
       (rotate (deg2rad   9) [1 0 0])
       (rotate (deg2rad -32) [0 1 0])
       (rotate (deg2rad  23) [0 0 1])
       (translate thumborigin)
       (translate [-48 -21 -9]))) ;        (translate [-50 -25 -12])))
; bottom right
(defn thumb-br-place [shape]
  (->> shape
       (rotate (deg2rad  10) [1 0 0])
       (rotate (deg2rad -25) [0 1 0])
       (rotate (deg2rad  22) [0 0 1])
       (translate thumborigin)
       (translate [-24 -32 -4])))
; bottom left
(defn thumb-bl-place [shape]
  (->> shape
       (rotate (deg2rad  10) [1 0 0])
       (rotate (deg2rad -34) [0 1 0])
       (rotate (deg2rad  24) [0 0 1])
       (translate thumborigin)
       (translate [-39 -38 -13])))


(defn thumb-1x-layout [shape]
  (union
   (thumb-br-place shape)
   (thumb-bl-place shape)
   ))

(defn thumb-15x-layout [shape]
  (union
   (thumb-ttr-place shape)
   (thumb-tr-place shape)
   (thumb-tm-place shape)
   (thumb-tl-place shape)
   ))

(def larger-plate
  (let [plate-height (- (/ (- cap-2u mount-height) 3) 0.5)
        top-plate (->> (cube mount-width plate-height web-thickness)
                       (translate [0 (/ (+ plate-height mount-height) 2)
                                   (- plate-thickness (/ web-thickness 2))]))]
    (union top-plate (mirror [0 1 0] top-plate))))

(def thumbcaps
  (union
   (thumb-1x-layout (sa-cap 1))
   (thumb-15x-layout (rotate (/ π 2) [0 0 1] (sa-cap 1.25)))))

(def thumb
  (union
   (thumb-1x-layout single-plate)
   (thumb-15x-layout single-plate)
  ; (thumb-15x-layout larger-plate)
))

(def thumb-post-tr (translate [(- (/ mount-width 2) post-adj)  (- (/ mount-height  2) post-adj) 0] web-post))
(def thumb-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height  2) post-adj) 0] web-post))
(def thumb-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(def thumb-post-br (translate [(- (/ mount-width 2) post-adj)  (+ (/ mount-height -2) post-adj) 0] web-post))

(def thumb-connectors
  (union
   (triangle-hulls
    (thumb-tr-place web-post-tr)
    (thumb-tr-place web-post-br)
    (thumb-ttr-place thumb-post-tl)
    (thumb-ttr-place thumb-post-bl)
   )
   (triangle-hulls
    (thumb-tm-place web-post-tr)
    (thumb-tm-place web-post-br)
    (thumb-tr-place thumb-post-tl)
    (thumb-tr-place thumb-post-bl)
   )
   (triangle-hulls    ; bottom two
    (thumb-bl-place web-post-tr)
    (thumb-bl-place web-post-br)
    (thumb-br-place web-post-tl)
    (thumb-br-place web-post-bl))
   (triangle-hulls
    (thumb-br-place web-post-tr)
    (thumb-br-place web-post-br)
    (thumb-tr-place thumb-post-br))
   (triangle-hulls    ; between top row and bottom row
    (thumb-bl-place web-post-tl)
    (thumb-tl-place web-post-bl)
    (thumb-bl-place web-post-tr)
    (thumb-tl-place web-post-br)
    (thumb-br-place web-post-tl)
    (thumb-tm-place web-post-bl)
    (thumb-br-place web-post-tr)
    (thumb-tm-place web-post-br)
    (thumb-tr-place web-post-bl)
    (thumb-br-place web-post-tr)
    (thumb-tr-place web-post-br))
   (triangle-hulls    ; top two to the middle two, starting on the left
    (thumb-tm-place web-post-tl)
    (thumb-tl-place web-post-tr)
    (thumb-tm-place web-post-bl)
    (thumb-tl-place web-post-br)
    (thumb-br-place web-post-tr)
    (thumb-tm-place web-post-bl)
    (thumb-tm-place web-post-br)
    (thumb-br-place web-post-tr))
   (for [column (range 0 3)]
     (triangle-hulls
       (key-place column cornerrow (web-post-bl-e 0 -2))
       (key-place column cornerrow web-post-bl)
       (key-place column cornerrow (web-post-br-e 0 -2))
       (key-place column cornerrow web-post-br)
       )
     )
   (for [column (range 0 3)]
     (triangle-hulls
       (key-place (inc column) cornerrow (web-post-bl-e 0 -2))
       (key-place (inc column) cornerrow web-post-bl)
       (key-place column cornerrow (web-post-br-e 0 -2))
       (key-place column cornerrow web-post-br)
       )
     )
   ;(triangle-hulls
    ;(key-place 0 cornerrow (web-post-bl-e 0 -2))
    ;(key-place 0 cornerrow web-post-bl)
    ;(key-place 0 cornerrow (web-post-br-e 0 -2))
    ;(key-place 0 cornerrow web-post-br)
     ;)
     (triangle-hulls
       (thumb-tm-place web-post-tl)
       (key-place 0 cornerrow web-post-bl)
       (key-place 0 cornerrow (web-post-bl-e 0 -2))
    )
   (triangle-hulls    ; top two to the main keyboard, starting on the left
    (thumb-tm-place web-post-tl)
    (key-place 0 cornerrow (web-post-bl-e 0 -2))
    (thumb-tm-place web-post-tr)
    (key-place 0 cornerrow (web-post-br-e 0 -2))
    (thumb-tr-place thumb-post-tl)
    (key-place 1 cornerrow (web-post-bl-e 0 -2))
    (thumb-tr-place thumb-post-tr)
    (key-place 1 cornerrow (web-post-br-e 0 -2))
    (thumb-ttr-place thumb-post-tl)
    (thumb-ttr-place thumb-post-tr)
    )
   (triangle-hulls
     (key-place 1 cornerrow (web-post-br-e 0 -2))
     (key-place 2 cornerrow (web-post-bl-e 0 -2))
     (thumb-ttr-place thumb-post-tr)
     (key-place 2 cornerrow (web-post-br-e 0 -2))
     (key-place 3 cornerrow (web-post-bl-e 0 -2))
     )
   (triangle-hulls
     (thumb-ttr-place thumb-post-tr)
     (key-place 3 cornerrow (web-post-bl-e 0 -2))
     (key-place 3 lastrow web-post-tl)
     )
   (triangle-hulls
     (thumb-ttr-place thumb-post-tr)
     (key-place 3 lastrow web-post-tl)
     (thumb-ttr-place thumb-post-br)
     (key-place 3 lastrow web-post-bl)
     )
    ;; on right and top side of "extra key" on row 4
   (triangle-hulls
    (key-place 3 lastrow web-post-br)
    (key-place 4 cornerrow web-post-bl)
    (key-place 3 lastrow web-post-tr)
    (key-place 4 cornerrow web-post-bl)
    (key-place 3 lastrow web-post-tr)
    (key-place 3 cornerrow web-post-br)
    (key-place 3 lastrow web-post-tr)
    (key-place 3 lastrow web-post-tl)
    (key-place 3 cornerrow web-post-bl)
    (key-place 3 cornerrow web-post-br)
    )
    )
  )

;;;;;;;;;;
;; OLED ;;
;;;;;;;;;;

(defn add-vec  [& args]
  "Add two or more vectors together"
  (when  (seq args) 
    (apply mapv + args)))

(defn sub-vec  [& args]
  "Subtract two or more vectors together"
  (when  (seq args) 
    (apply mapv - args)))

(defn div-vec  [& args]
  "Divide two or more vectors together"
  (when  (seq args) 
    (apply mapv / args)))


(def oled-holder
  (->>
    (difference
      ; main body
      (apply cube oled-holder-size)
      ; cut for oled pcb
      (translate [0 0 1] (apply cube (add-vec [0.5 0.5 0.1] oled-pcb-size)))
      ; cut for oled screen
      (translate oled-screen-offset (apply cube oled-screen-size))
      ; cutout for connector pins
      (->> (cube 10 3 10)
           (translate [0 (- (/ (nth oled-pcb-size 1) 2) 2) (+ plate-thickness 1.0)])
           )
      ; cutout for oled cable
      (->> (cube 10 2 10)
           (translate oled-screen-offset)
           (translate [0 (- (+ (/ (nth oled-screen-size 1) 2) 1)) (+ plate-thickness 0.8)])
           )
      (for [x [-2 2] y [-2 2]]
        (translate (div-vec oled-mount-size [x y 1]) (cylinder (/ 2.5 2) 10)))
      )
    (rotate (deg2rad 180) [0 1 0])
    (translate [0 0 (/ oled-holder-thickness 2)])
    )
  )

;;;;;;;;;;
;; Case ;;
;;;;;;;;;;

(defn bottom [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))

(defn bottom-hull [& p]
  (hull p (bottom 0.001 p)))

(def left-wall-x-offset 0) ; original 10
(def left-wall-z-offset 0) ; original 3

(defn assoc-at  [data i item]
  (if  (associative? data)
    (assoc data i item)
    (if-not  (neg? i)
      (letfn  [(assoc-lazy  [i data]
                 (cond  (zero? i)  (cons item  (rest data))
                       (empty? data) data
                       :else  (lazy-seq  (cons  (first data)
                                               (assoc-lazy  (dec i)  (rest data))))))]
        (assoc-lazy i data))
      data)))

(defn left-key-position [row direction]
  (map -
       (key-position 0 row [(* mount-width -0.5) (* direction mount-height 0.5) 0]) 
       [left-wall-x-offset 0 left-wall-z-offset])
  )

(defn left-wall-plate-position [xdir ydir]
  (->> 
    (add-vec
      [left-wall-x-offset 0 left-wall-z-offset]
      (key-position 0 0 [0 0 0])
      [(* mount-width -0.5) (* mount-width 0.5) 0]
      [(* oled-holder-width -0.5) (* oled-holder-height -0.5) 0]
      [(* xdir oled-holder-width 0.5) (* ydir oled-holder-height 0.5) 0]
      [-3 6 -7]
      )
    )
  )

(defn left-wall-plate-place [xdir ydir shape]
  (->> shape
       (translate (left-wall-plate-position xdir ydir))
       (rotate oled-mount-rotation-x [1 0 0])
       (rotate oled-mount-rotation-z [0 0 1])
       )
  )

(defn wall-locate1 [dx dy] [(* dx wall-thickness) (* dy wall-thickness) -1])
(defn wall-locate2 [dx dy] [(* dx wall-xy-offset) (* dy wall-xy-offset) wall-z-offset])
(defn wall-locate3 [dx dy] [(* dx (+ wall-xy-offset wall-thickness)) (* dy (+ wall-xy-offset wall-thickness)) wall-z-offset])

(defn wall-brace [place1 dx1 dy1 post1 place2 dx2 dy2 post2]
  (union
   (hull
    (place1 post1)
    (place1 (translate (wall-locate1 dx1 dy1) post1))
    (place1 (translate (wall-locate2 dx1 dy1) post1))
    (place1 (translate (wall-locate3 dx1 dy1) post1))
    (place2 post2)
    (place2 (translate (wall-locate1 dx2 dy2) post2))
    (place2 (translate (wall-locate2 dx2 dy2) post2))
    (place2 (translate (wall-locate3 dx2 dy2) post2)))
   (bottom-hull
    (place1 (translate (wall-locate2 dx1 dy1) post1))
    (place1 (translate (wall-locate3 dx1 dy1) post1))
    (place2 (translate (wall-locate2 dx2 dy2) post2))
    (place2 (translate (wall-locate3 dx2 dy2) post2))))
  )

(defn key-wall-brace [x1 y1 dx1 dy1 post1 x2 y2 dx2 dy2 post2]
  (wall-brace (partial key-place x1 y1) dx1 dy1 post1
              (partial key-place x2 y2) dx2 dy2 post2))

(def right-wall
  (let [tr (if (true? pinky-15u) wide-post-tr web-post-tr)
        br (if (true? pinky-15u) wide-post-br web-post-br)]
    (union (key-wall-brace lastcol 0 0 1 tr lastcol 0 1 0 tr)
           (for [y (range 0 lastrow)] (key-wall-brace lastcol y 1 0 tr lastcol y 1 0 br))
           (for [y (range 1 lastrow)] (key-wall-brace lastcol (dec y) 1 0 br lastcol y 1 0 tr))
           (key-wall-brace lastcol cornerrow 0 -1 br lastcol cornerrow 1 0 br))))

(def left-wall
  (union
    (wall-brace  (partial key-place 0 0) 0 1 web-post-tl  (partial left-wall-plate-place 1 1) 0 1 oled-post)
    (wall-brace  (partial left-wall-plate-place 1 1) 0 1 oled-post  (partial left-wall-plate-place -1 1) 0 1 oled-post)
    (wall-brace  (partial left-wall-plate-place -1 1) 0 1 oled-post  (partial left-wall-plate-place -1 1) -1 0 oled-post)
    (wall-brace  (partial left-wall-plate-place -1 1) -1 0 oled-post  (partial left-wall-plate-place -1 -1) -1 -1 oled-post)
    (left-wall-plate-place 0 0 oled-holder)

    (wall-brace  (partial left-wall-plate-place -1 -1) -1 -1 oled-post  thumb-tl-place -1 0 web-post-tl)
    (wall-brace  thumb-tl-place -1 0 web-post-tl thumb-tl-place -1 0 web-post-bl)

    (triangle-hulls
      (left-wall-plate-place 1 1 oled-post)
      (key-place 0 0 web-post-tl)
      (key-place 0 0 web-post-bl)

      (key-place 0 0 web-post-bl)
      (left-wall-plate-place 1 1 oled-post)
      (left-wall-plate-place 1 -1 oled-post)

      (left-wall-plate-place 1 -1 oled-post)
      (left-wall-plate-place -1 -1 oled-post)
      (thumb-tl-place web-post-tl)

      (thumb-tl-place web-post-tl)
      (left-wall-plate-place -1 -1 oled-post)
      (left-wall-plate-place 1 -1 oled-post)

      (left-wall-plate-place 1 -1 oled-post)
      (key-place 0 1 web-post-tl)
      (key-place 0 1 web-post-bl)

      (key-place 0 1 web-post-bl)
      (left-wall-plate-place 1 -1 oled-post)
      (thumb-tl-place web-post-tl)

      (key-place 0 1 web-post-bl)
      (key-place 0 2 web-post-tl)
      (thumb-tl-place web-post-tl)

      (thumb-tl-place web-post-tl)
      (thumb-tl-place web-post-tr)

      (thumb-tm-place web-post-tl)

      (thumb-tm-place web-post-tl)
      (thumb-tl-place web-post-tl)
      (key-place 0 2 web-post-tl)

      (key-place 0 2 web-post-tl)
      (key-place 0 2 web-post-bl)
      (thumb-tm-place web-post-tl)
      )
    )
  )

(def case-walls
  (union
   right-wall
   ; back wall
   (for [x (range 0 ncols)] (key-wall-brace x 0 0 1 web-post-tl x       0 0 1 web-post-tr))
   (for [x (range 1 ncols)] (key-wall-brace x 0 0 1 web-post-tl (dec x) 0 0 1 web-post-tr))
   left-wall
   ; front wall
   (key-wall-brace 3 lastrow   0 -1 web-post-bl 3 lastrow 0.5 -1 web-post-br)
   (key-wall-brace 3 lastrow 0.5 -1 web-post-br 4 cornerrow 0.5 -1 web-post-bl)
   (for [x (range 4 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl x       cornerrow 0 -1 web-post-br)) ; TODO fix extra wall
   (for [x (range 5 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl (dec x) cornerrow 0 -1 web-post-br))
   ; thumb walls
   (wall-brace thumb-br-place  0 -1 web-post-br thumb-tr-place  0 -1 thumb-post-br)
   (wall-brace thumb-br-place  0 -1 web-post-br thumb-br-place  0 -1 web-post-bl)
   (wall-brace thumb-ttr-place  0 -1 web-post-br thumb-ttr-place  0 -1 web-post-bl)
   (wall-brace thumb-bl-place  0 -1 web-post-br thumb-bl-place  0 -1 web-post-bl)
   ;(wall-brace thumb-tl-place  0  1 web-post-tr thumb-tl-place  0  1 web-post-tl)
   (wall-brace thumb-bl-place -1  0 web-post-tl thumb-bl-place -1  0 web-post-bl)
   (wall-brace thumb-tl-place -1  0 web-post-tl thumb-tl-place -1  0 web-post-bl)
   ; thumb corners
   (wall-brace thumb-bl-place -1  0 web-post-bl thumb-bl-place  0 -1 web-post-bl)
   ; thumb tweeners
   (wall-brace thumb-br-place  0 -1 web-post-bl thumb-bl-place  0 -1 web-post-br)
   (wall-brace thumb-tl-place -1  0 web-post-bl thumb-bl-place -1  0 web-post-tl)
   (wall-brace thumb-tr-place 0  -1 web-post-br thumb-ttr-place 0  -1 web-post-bl)
   (wall-brace thumb-ttr-place  0 -1 thumb-post-br (partial key-place 3 lastrow)  0 -1 web-post-bl)
   )
  )

; Cutout for controller/trrs jack holder
(def controller-ref (key-position 0 0 (map - (wall-locate2  0  -1) [0 (/ mount-height 2) 0])))
(def controller-cutout-pos (map + [-21 19.8 0] [(first controller-ref) (second controller-ref) 2]))

(def controller-holder-stl-pos
   (add-vec controller-cutout-pos [16.4 -23.9 -2.0]))

(def controller-holder-stl
  (->> (import "controller holder.stl")
     ;(scale [0.97 0.97 1])
     (rotate (deg2rad 90) [1 0 0])
     (rotate oled-mount-rotation-z [0 0 1])
     (translate controller-holder-stl-pos)
     )
  )

(defn intersect-bottom [a b]
  (->> (project (intersection a b))
       (extrude-linear {:height 12.6 :twist 0 :convexity 0})
       (translate [0 0 (/ 12.6 2)])
       )
  )

(defn controller-cutout [shape] (intersect-bottom controller-holder-stl shape))

(defn screw-insert-shape [bottom-radius top-radius height]
  (union
   (->> (binding [*fn* 30]
          (cylinder [bottom-radius top-radius] height)))
   (translate [0 0 (/ height 2)] (->> (binding [*fn* 30] (sphere top-radius))))))

(defn screw-insert [column row bottom-radius top-radius height offset]
  (let [shift-right   (= column lastcol)
        shift-left    (= column 0)
        shift-up      (and (not (or shift-right shift-left)) (= row 0))
        shift-down    (and (not (or shift-right shift-left)) (>= row lastrow))
        position      (if shift-up     (key-position column row (map + (wall-locate2  0  1) [0 (/ mount-height 2) 0]))
                          (if shift-down  (key-position column row (map - (wall-locate2  0 -1) [0 (/ mount-height 2) 0]))
                              (if shift-left (map + (left-wall-plate-position -1 1) (wall-locate3 -1 0))
                                  (key-position column row (map + (wall-locate2  1  0) [(/ mount-width 2) 0 0])))))]
    (->> (screw-insert-shape bottom-radius top-radius height)
         (translate (map + offset [(first position) (second position) (/ height 2)])))))

(defn screw-insert-all-shapes [bottom-radius top-radius height]
  (union 
         ; top left, near usb/trrs
         (screw-insert 0 0         bottom-radius top-radius height [5.8 -3 0])
         ; middle top
         (screw-insert 3 0         bottom-radius top-radius height [-9 0 0])
         ; top right
         (screw-insert lastcol 0         bottom-radius top-radius height [-4 9 0])
         ; lower right
         (screw-insert lastcol lastrow  bottom-radius top-radius height [-4 13 0])
         ; middle bottom
         (screw-insert 3 lastrow         bottom-radius top-radius height [-5 2 0])
         ; thumb cluster, closest to user
         (screw-insert 1 lastrow         bottom-radius top-radius height [-2 -13 0])
         ; thumb cluster left
         (screw-insert 0 lastrow   bottom-radius top-radius height [16 -80 0])
))

; Hole Depth Y: 4.4
(def screw-insert-height 3.5)

; Hole Diameter C: 4.1-4.4
(def screw-insert-bottom-radius (/ 4.0 2))
(def screw-insert-top-radius (/ 4.0 2))
(def screw-insert-holes  (screw-insert-all-shapes screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))

; Wall Thickness W:\t1.65
(def screw-insert-outers (screw-insert-all-shapes (+ screw-insert-bottom-radius 1.65) (+ screw-insert-top-radius 1.65) (+ screw-insert-height 1.5)))
(def screw-insert-screw-holes  (screw-insert-all-shapes 1.5 1.5 350))

(def pinky-connectors
  (apply union
         (concat
          ;; Row connections
          (for [row (range 0 lastrow)]
            (triangle-hulls
             (key-place lastcol row web-post-tr)
             (key-place lastcol row wide-post-tr)
             (key-place lastcol row web-post-br)
             (key-place lastcol row wide-post-br)))

          ;; Column connections
          (for [row (range 0 cornerrow)]
            (triangle-hulls
             (key-place lastcol row web-post-br)
             (key-place lastcol row wide-post-br)
             (key-place lastcol (inc row) web-post-tr)
             (key-place lastcol (inc row) wide-post-tr)))
          ;;
)))

(def pinky-walls
  (union
   (key-wall-brace lastcol cornerrow 0 -1 web-post-br lastcol cornerrow 0 -1 wide-post-br)
   (key-wall-brace lastcol 0 0 1 web-post-tr lastcol 0 0 1 wide-post-tr)))

;@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
;;;;;;;;;Wrist rest;;;;;;;;;;;;;;;;;;;;;;;;;;;
;@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

(def wrist-rest-front-cut

  (scale[1.1, 1, 1](->> (cylinder 7 200)(with-fn 300)
                        (translate [0 -13.4 0]))
              ;(->> (cube 18 10 15)(translate [0 -14.4 0]))
              ))

(def cut-bottom
  (->>(cube 300 300 100)(translate [0 0 -50]))
  )

(def h-offset
  (* (Math/tan(/ (* π wrist-rest-angle) 180)) 88)
  )

(def scale-cos
  (Math/cos(/ (* π wrist-rest-angle) 180))
  )

(def scale-amount
  (/ (* 83.7 scale-cos) 19.33)
  )

(def wrist-rest
  (difference
    (scale [4.25  scale-amount  1] (difference (union
                                                 (difference
                                                   ;the main back circle
                                                   (scale[1.3, 1, 1](->> (cylinder 10 150)(with-fn 200)
                                                                         (translate [0 0 0])))
                                                   ;front cut cube and circle
                                                   (scale[1.1, 1, 1](->> (cylinder 7 201)(with-fn 200)
                                                                         (translate [0 -13.4 0]))
                                                               (->> (cube 18 10 201)(translate [0 -12.4 0]))

                                                               ))
                                                 ;;side fillers
                                                 (->> (cylinder 6.8 200)(with-fn 200)
                                                      (translate [-6.15 -0.98 0]))

                                                 (->> (cylinder 6.8 200)(with-fn 200)
                                                      (translate [6.15 -0.98 0]))
                                                 ;;heart shapes at bottom
                                                 (->> (cylinder 5.9 200)(with-fn 200)
                                                      (translate [-6.35 -2 0]))


                                                 (scale[1.01, 1, 1](->> (cylinder 5.9 200)(with-fn 200)
                                                                        (translate [6.35 -2. 0])))
                                                 )

                                               )
           )

    cut-bottom

    )
  )


;(def right_wrist_connecter_x 25)
(def wrist-rest-base
  (->>
    (scale [1 1 1] ;;;;scale the wrist rest to the final size after it has been cut
           (difference
             (scale [1.08 1.08 1] wrist-rest )
             (->> (cube 200 200 200)(translate [0 0 (+ (+ (/ h-offset 2) (- wrist-rest-back-height h-offset) ) 100)]) (rotate  (/ (* π wrist-rest-angle) 180)  [1 0 0])(rotate  (/ (* π wrist-rest-y-angle) 180)  [0 1 0]))
             ;	(->> (cube 200 200 200)(translate [0 0 (+ (+ (- wrist-rest-back-height h-offset) (* 2 h-offset)) 100)]) (rotate  (/ (* π wrist-rest-angle) 180)  [1 0 0]))
             ;	(->> (cube 200 200 200)(translate [0 0 (+ (+ (/ (* 88 (Math/tan(/ (* π wrist-rest-angle) 180))) 4) 100) wrist-rest-back-height)]) (rotate  (/ (* π wrist-rest-angle) 180)  [1 0 0]))
             (->> (difference
                    wrist-rest
                    (->> (cube 200 200 200)(translate [0 0 (- (+ (/ h-offset 2) (- wrist-rest-back-height h-offset) ) (+ 100  wrist-rest-ledge))]) (rotate  (/ (* π wrist-rest-angle) 180)  [1 0 0])(rotate  (/ (* π wrist-rest-y-angle) 180)  [0 1 0]))
                    ;(->> (cube 200 200 200)(translate [0 0 (- (+ (/ (* 17.7 (Math/tan(/ (* π wrist-rest-angle) 180))) 4) wrist-rest-back-height)(+ 100  wrist-rest-ledge))])(rotate  (/ (* π wrist-rest-angle) 180)  [1 0 0])))
                    )
                  )
             (translate [40 -28 0] (screw-insert-shape screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))
             (translate [-40 -28 0] (screw-insert-shape screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))
             (translate [50 10 0] (screw-insert-shape screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))
             (translate [-50 10 0] (screw-insert-shape screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))
             (translate [-50 10 0] (screw-insert-shape screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))
             (translate [0 40 0] (screw-insert-shape screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))
             );(rotate  (/ (* π wrist-rest-rotation-angle) 180)  [0 0 1])
           )
    )
  )



(def rest-case-cuts
	(union
	;;right cut
			(->> (cylinder 1.85 50)(with-fn 30) (rotate  (/  π 2)  [1 0 0])(translate [right_wrist_connecter_x 24 4.5]))
			(->> (cylinder 2.8 5.2)(with-fn 50) (rotate  (/  π 2)  [1 0 0])(translate [right_wrist_connecter_x (+ 33.8 nrows) 4.5]))
			(->> (cube 6 3 12.2)(translate [right_wrist_connecter_x (+ 21 nrows) 1.5]));;39
	;;middle cut
			(->> (cylinder 1.85 50)(with-fn 30) (rotate  (/  π 2)  [1 0 0])(translate [middle_wrist_connecter_x 14 4.5]))
			(->> (cylinder 2.8 5.2)(with-fn 50) (rotate  (/  π 2)  [1 0 0])(translate [middle_wrist_connecter_x 20 4.5]))
			(->> (cube 6 3 12.2)(translate [middle_wrist_connecter_x (+ 17 nrows) 1.5]))
	;;left
			(->> (cylinder 1.85 50)(with-fn 30) (rotate  (/  π 2)  [1 0 0])(translate [left_wrist_connecter_x 11 4.5]))
			(->> (cylinder 2.8 5.2)(with-fn 50) (rotate  (/  π 2)  [1 0 0])(translate [left_wrist_connecter_x (+ 17.25 nrows) 4.5]))
			(->> (cube 6 3 12.2)(translate [left_wrist_connecter_x (+ 20.0 nrows) 1.5]))
	)
)

(def rest-case-connectors
  (translate [0 10 0]
	(difference
		(union
			(scale [1 1 1.6] (->> (cylinder 6 50)(with-fn 200) (rotate  (/  π 2)  [1 0 0])(translate [right_wrist_connecter_x 5 0])));;right
			(scale [1 1 1.6] (->> (cylinder 6 50)(with-fn 200) (rotate  (/  π 2)  [1 0 0])(translate [middle_wrist_connecter_x -2 0])))
			(scale [1 1 1.6] (->> (cylinder 6 60)(with-fn 200) (rotate  (/  π 2)  [1 0 0])(translate [left_wrist_connecter_x -6 0])))
	;rest-case-cuts
		)
	))
)

(def wrist-rest-locate
(key-position 3 8 (map + (wall-locate1 0 (- 4.9 (* 2 nrows))) [0 (/ mount-height 2) 0]))

)

(def wrest-wall-cut
(->> (for [xyz (range 1.00 10 3)];controls the scale last number needs to be lower for thinner walls
						 (union
							(translate[1, xyz,1] case-walls)
						  ;(translate [0 0 -3])
						)
					)
			))


(def wrist-rest-build
	(difference
		(->> (union
          
			(->> (translate wrist_brse_position (rotate  (/ (* π wrist-rest-rotation-angle) 180) [0 0 1] wrist-rest-base) ))
					(->> (difference
				;wrist-rest-sides

							rest-case-connectors
							rest-case-cuts
							cut-bottom
					;	wrest-wall-cut
							)

					)
			)
			 (translate [(+ (first thumborigin ) 33) (- (second thumborigin) 50) 0])
		)
	 (translate [(+ (first thumborigin ) 33) (- (second thumborigin) 50) 0] rest-case-cuts)
	wrest-wall-cut
	)


	;(translate [25 -103 0]))
)

(defn cut-bottom [shape] (difference shape (translate [0 0 -20] (cube 350 350 40))))

(def case-walls-with-screws (union case-walls screw-insert-outers))

(def model-right (cut-bottom
                   (union
                     key-holes
                     pinky-connectors
                     pinky-walls
                     connectors
                     thumb
                     thumb-connectors
                     (difference case-walls-with-screws
                                 (controller-cutout case-walls-with-screws)
                                 (if (== wrist-rest-on 1) (->> rest-case-cuts	(translate [(+ (first thumborigin ) 33) (- (second thumborigin)  (- 56 nrows)) 0])))
                                 screw-insert-holes))))


(spit "things/right.scad"
      (write-scad
        (union model-right
               (-% controller-holder-stl))
        ))

;(spit "things/left.scad"
      ;(write-scad (mirror [-1 0 0] model-right)))

;(spit "things/right-test.scad"
      ;(write-scad
        ;(union
          ;model-right
          ;caps
          ;thumbcaps

			;;(if (== bottom-cover 1) (->> model-plate-right))
			;(if (== wrist-rest-on 1) (->> wrist-rest-build 		)		)
          ;)
        ;)
      ;)


;(spit "things/right-plate.scad"
      ;(write-scad
       ;(cut
        ;(translate [0 0 -0.1]
                   ;(difference (union case-walls
                                      ;pinky-walls
                                      ;screw-insert-outers)
                               ;(translate [0 0 -10] screw-insert-screw-holes))))))


;(spit "things/wrist-rest.scad"
      ;(write-scad wrist-rest-build))

;(spit "things/caps-crash-test.scad"
      ;(write-scad
       ;(intersection model-right caps)))

;(spit "things/test.scad"
      ;(write-scad
       ;(difference trrs-holder trrs-holder-hole)))

(defn -main [dum] 1)  ; dummy to make it easier to batch
