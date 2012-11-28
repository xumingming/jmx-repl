(ns jmx.client)

(loop [input (read-line)]
    (if (= "help" input)
      (println "haha")
      (print "invalid command"))
    (println "=>")
    (recur (read-line)))

