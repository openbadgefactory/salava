(ns salava.registry
   (:require [salava.core.plugins :refer [defplugins]]))

;(macroexpand '(defplugins enabled :badge :page :gallery :file :user)))

(defplugins enabled :badge :page :gallery :file :user)

