(ns jmx.repl
  (require [clojure.java.jmx :as jmx]
           [clojure.string :as string]
           [colorize.core :as color]))
(def wd (atom {}))

(declare ls0 cat0 wd->bean-name wd->path)
(defn get-current-depth []
  (cond
   (nil? (:namespace @wd)) :root
   (nil? (:type @wd)) :namespace
   (nil? (:name @wd)) :type
   true :name))

(defn extract-namespaces [names]
  (sort (set (map (fn [name]
                    (let [real-name (string/split name #":")
                          real-name (first real-name)]
                      real-name)) names))))

(defn extract-types [names]
  (sort (set (map (fn [name]
                    (let [real-name (string/split name #":")
                          real-name (second real-name)
                          real-name (string/split real-name #",")
                          real-name (filter (fn [name]
                                              (.startsWith name "type=")) real-name)

                          real-name (first real-name)
                          real-name (.substring real-name 5)]
                      real-name)) names))))

(defn extract-names [names]
  (sort (set (map (fn [name]
                    (let [real-name (string/split name #":")
                          real-name (second real-name)
                          real-name (string/split real-name #",")

                          real-name (if (= 2 (count real-name))
                                      (filter (fn [name]
                                                (.startsWith name "name=")) real-name)
                                      (filter (fn [name]
                                                (.startsWith name "type=")) real-name))

                          real-name (first real-name)
                          real-name (.substring real-name 5)]
                      real-name)) names))))
(defn extract-attributes [bean-prefix]
  (sort (set (map name (jmx/attribute-names bean-prefix)))))

(defn ls []
  (let [depth (get-current-depth)
        bean-prefix (wd->bean-name @wd)
        names     (ls0 bean-prefix)
        names     (condp = depth
                      ;; if current depth is root, show all the namespace
                      :root (extract-namespaces names)
                      ;; if current depth is namespace, show all the type
                      :namespace (extract-types names)
                      ;; if current depth is type, show all the names
                      :type (extract-names names)
                      :name (extract-attributes bean-prefix)
                      )]
    (doseq [name names]
      (println (color/cyan "\t" name)))))

(defn cat [attr-name]
  (let [attr-value (cat0 attr-name)]
    (println (color/blue attr-value))))

(defn pwd []
  (wd->path @wd))
;; java.lang:name=CMS Old Gen,type=MemoryPoll ->
;; {:namespace java.lang
;;  :name "CMS Old Gen"
;;  :type "MemoryPool"}
(defn cd [name]
  (let [cd-type (cond
                 (= ".." name) :parent
                 (nil? (:namespace @wd)) :namespace
                 (nil? (:type @wd)) :type
                 (nil? (:name @wd)) :name
                 true :error)]
    (if (= :parent cd-type)
      (cond
       (not (nil? (:name @wd)))       (swap! wd #(dissoc % :name))
       (not (nil? (:type @wd)))       (swap! wd #(dissoc % :type))
       (not (nil? (:namespace @wd)))       (swap! wd #(dissoc % :namespace)))
      (if (= :error cd-type)
        (println "No such folder!!!")
        (swap! wd #(assoc % cd-type name)) ))))

(defn wd->bean-name [wd]
  (cond
   (nil? (:namespace wd)) "*:*"
   (nil? (:type wd)) (str (:namespace wd) ":*")
   (nil? (:name wd)) (str (:namespace wd) ":type=" (:type wd) ",*")
   (= (:name wd) (:type wd)) (str (:namespace wd) ":type=" (:type wd))
   true (str (:namespace wd) ":name=" (:name wd) ",type=" (:type wd))))

(defn wd->path [wd]
  (cond
   (nil? (:namespace wd)) "/"
   (nil? (:type wd)) (str "/" (:namespace wd))
   (nil? (:name wd)) (str "/" (:namespace wd) "/" (:type wd))
   (= (:name wd) (:type wd)) (str "/" (:namespace wd) "/" (:type wd))
   true (str "/" (:namespace wd) "/" (:type wd) "/" (:name wd) )))




(defn cat0 [attr-name]
  (let [bean-fullname (wd->bean-name @wd)
        attr-value (jmx/read bean-fullname (keyword attr-name))]
    attr-value))

(defn ls0 [bean-prefix]
  (let [mbeans (->> (jmx/mbean-names bean-prefix) (map #(.getCanonicalName %)))]
    mbeans))

(defn help []
  (let [help-text ["\t help -- print this help"
                   "\t ls   -- list the items in current directory"
                   "\t cd   -- enter a folder"
                   "\t cat  -- print the value of an item"
                   "\t pwd  -- show the current path"]]
    (doseq [ht help-text]
      (println (color/cyan ht)))))

(loop [input "help"]
  (try
      (let [argv (string/split input #" ")
        command (first argv)
        argv (rest argv)]
    (condp = command
      "help" (help)
      "ls" (ls)
      "pwd" (pwd)
      "cd" (apply cd argv)
      "cat" (apply cat argv)
      (help)))
      (catch Throwable e
        (println "ERROR: " e)))

  (print (str "[" (color/green (pwd)) "] => "))
  (flush)
  (recur (read-line)))


