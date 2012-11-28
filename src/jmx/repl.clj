(ns ^{:author "xumingmingv"
      :doc ""}
  jmx.repl
  (require [clojure.java.jmx :as jmx]
           [clojure.string :as string]
           [colorize.core :as color]))

(def ^{:doc
       "This atom contains the current namespace, type, name we are working on
       e.g. {:namespace \"java.lang\"
             :type \"Memory\"
             :name \"xxx\"} translates to JMX bean name:
        java.lang:name=xxx,type=Memory
"} wd (atom {}))

(declare ls0 cat0 wd->bean-name wd->path)
(defn get-current-level
  "Returns which level we are operating at."
  []
  (cond
   (nil? (:namespace @wd)) :root
   (nil? (:type @wd)) :namespace
   (nil? (:name @wd)) :type
   true :name))

(defn extract-namespaces
  "Extracts namespaces from these object names."
  [names]
  (sort (set (map (fn [name]
                    (let [real-name (string/split name #":")
                          real-name (first real-name)]
                      real-name)) names))))

(defn extract-types
  "Extracts types from these object names."
  [names]
  (sort (set (map (fn [name]
                    (let [real-name (string/split name #":")
                          real-name (second real-name)
                          real-name (string/split real-name #",")
                          real-name (filter (fn [name]
                                              (.startsWith name "type=")) real-name)
                          real-name (first real-name)
                          real-name (.substring real-name 5)]
                      real-name)) names))))

(defn extract-names
  "Extracts name part('name=xxx') from these object names."
  [names]
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

(defn extract-attributes
  "Extracts all the attribute name of a mbean."
  [bean-name]
  (sort (set (map name (jmx/attribute-names bean-name)))))

(defn ls
  "Display the items in current direcoty."
  []
  (let [depth (get-current-level)
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

(defn cat
  "Displays the value of an item"
  [attr-name]
  (let [attr-value (cat0 attr-name)]
    (println (color/blue attr-value))))

(defn pwd0 []
  (wd->path @wd))

(defn pwd
  "Displays the current working directory."
  []
  (println (color/cyan (pwd0))))

(defn bean-count [bean-name]
  (count (jmx/mbean-names bean-name)))

(defn bean-exists? [bean-name]
  (> (bean-count bean-name) 0))


;; java.lang:name=CMS Old Gen,type=MemoryPoll ->
;; {:namespace java.lang
;;  :name "CMS Old Gen"
;;  :type "MemoryPool"}
(defn cd
  "Changes the working directory to the specified directory."
  [name]
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
        (let [new-wd (assoc @wd cd-type name)
              bean-name (wd->bean-name new-wd)
              bean-exists? (bean-exists? bean-name)]
          (println "name:" bean-name ", exists:" bean-exists?)
          (if (= bean-exists? false)
            (println "No such directory.")
            (do
              (if (and (= :type cd-type) (= 1 (bean-count bean-name)))
                (swap! wd #(assoc % :name name)))
              (swap! wd #(assoc % cd-type name)))))))))

(defn wd->bean-name
  "Translates the wd data to a JMX bean name."
  [wd]
  (cond
   (nil? (:namespace wd)) "*:*"
   (nil? (:type wd)) (str (:namespace wd) ":*")
   (nil? (:name wd)) (str (:namespace wd) ":type=" (:type wd) ",*")
   (= (:name wd) (:type wd)) (str (:namespace wd) ":type=" (:type wd))
   true (str (:namespace wd) ":name=" (:name wd) ",type=" (:type wd))))

(defn wd->path [wd]
  "Translates the wd data to a file system like path.
    e.g. /java.lang/Memory"
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

  (print (str "[" (color/green (pwd0)) "] => "))
  (flush)
  (recur (read-line)))
