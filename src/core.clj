(ns core
  (:import (java.awt Color Dimension)
           (java.awt.event ActionListener KeyEvent KeyListener)
           (javax.swing JFrame JOptionPane JPanel Timer)))

(def point-size 10)
(def width 20)
(def height 20)
(def dirs {KeyEvent/VK_RIGHT [1 0]
           KeyEvent/VK_DOWN  [0 1]
           KeyEvent/VK_LEFT  [-1 0]
           KeyEvent/VK_UP    [0 -1]})

(defn create-snake
  []
  {:body  (list [1 1])
   :dir   [1 0]
   :color (Color. 15 160 70)
   :type  :snake})

(defn create-apple
  []
  {:location [(rand-int width) (rand-int height)]
   :color    (Color. 210 50 90)
   :type     :apple})

(defmulti paint (fn [_g object & _] (:type object)))

(defn fill-point
  [g location color]
  (let [[x y] location]
    (.setColor g color)
    (.fillRect g (* x point-size) (* y point-size) point-size point-size)))

(defmethod paint :apple
  ([g {:keys [location color]}]
   (fill-point g location color)))

(defmethod paint :snake
  ([g {:keys [body color]}]
   (doseq [location body]
     (fill-point g location color))))

(defn add-points
  [& pts]
  (apply map + pts))

(defn move
  [{:keys [body dir] :as snake} & grow]
  (assoc snake :body (cons (add-points (first body) dir)
                           (if grow body (butlast body)))))

(defn eats?
  [snake apple]
  (= (first (:body snake)) (:location apple)))

(defn update-position
  [snake apple]
  (dosync
    (if (eats? @snake @apple)
      (do (ref-set apple (create-apple))
          (alter snake move :grow))
      (alter snake move))))

(defn allowed-dir?
  [dir new-dir]
  (let [[cx cy] dir
        [nx ny] new-dir]
    (and (not= cx nx) (not= cy ny))))

(defn turn
  [{:keys [dir] :as snake} new-dir]
  (if (allowed-dir? dir new-dir)
    (assoc snake :dir new-dir)
    snake))

(defn update-directions
  [snake new-dir]
  (when new-dir (dosync (alter snake turn new-dir))))

(defn head-overlaps-screen?
  [{[[x y]] :body}]
  (or (> x width) (> y height) (< x 0) (< y 0)))

(defn head-overlaps-body?
  [{[head & body] :body}]
  (contains? (set body) head))

(defn lose?
  [snake]
  (or (head-overlaps-body? snake) (head-overlaps-screen? snake)))

(defn reset-game
  [snake apple]
  (dosync (ref-set apple (create-apple))
          (ref-set snake (create-snake))))

(defn game-panel [snake apple frame]
  (doto
    (proxy [JPanel ActionListener KeyListener] []
      (paintComponent [g]
        (proxy-super paintComponent g)
        (paint g @snake)
        (paint g @apple))
      (actionPerformed [_e]
        (update-position snake apple)
        (when (lose? @snake)
          (JOptionPane/showMessageDialog frame "Você perdeu")
          (reset-game snake apple))
        (.repaint this))
      (getPreferredSize []
        (Dimension. (* width point-size)
                    (* height point-size)))
      (keyPressed [e]
        (update-directions snake (dirs (.getKeyCode e))))
      (keyReleased [_e]))
    (.setFocusable true)))

(defn -main
  []
  (let [snake (ref (create-snake))
        apple (ref (create-apple))
        frame (JFrame. "Snake")
        panel (game-panel snake apple frame)
        timer (Timer. 500 panel)]
    (doto panel
      (.setFocusable true)
      (.addKeyListener panel))
    (doto frame
      (.add panel)
      (.pack)
      (.setVisible true)
      (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE))
    (.start timer)))