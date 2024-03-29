(defproject lsystems "0.2.0-SNAPSHOT"
  :description "Small and simple L-systems implementation in Clojure"
  :url "https://github.com/joebentley/lsystems"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :plugins [[lein-codox "0.10.7"] [lein-environ "1.1.0"]]
  :dependencies [[org.clojure/clojure "1.10.0"] [clojure2d "1.1.0"] [environ "1.1.0"]]
  :repl-options {:init-ns lsystems.core}
  ;:main
  :profiles {:debug {:env {:debug? "true"}}}
  :codox {:source-uri "https://github.com/joebentley/lsystems/blob/master/{filepath}#L{line}"
          ;; /docs is the serve directory used by github pages
          :output-path "docs"
          ;; use MathJax so we can do LaTeX
          :html {:transforms [[:head] [:append
                                       [:script { :src "https://polyfill.io/v3/polyfill.min.js?features=es6"}]
                                       [:script { :id "MathJax-script" :async true :src "https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"}]]]}})
