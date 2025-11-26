(ns qrbot.state
  (:require ["qrcode" :as QRCode]
            ["path" :refer [join]]
            ["crypto" :as crypto]
            [qrbot.fs :as fs]))

;; Like interface State for Zustand
(defonce app-state
  (atom {:chats {}
         :requests {}
         :overload-limit 5
         :bot-state :idle})) ;; can also be :overloaded

;; these would be the actions impls in zustand
;; every action get the previous state and returns the new state
(defn new-chat [state chat-id]
  (assoc-in state [:chats chat-id]
            {:id chat-id
             :format :png
             :mode :normal}))

(defn set-chat-format [state chat-id format]
  (assoc-in state [:chats chat-id :format] format))

(defn set-chat-mode [state chat-id mode]
  (assoc-in state [:chats chat-id :mode] mode))

;; Helper query functions (defined before use)
(defn active-requests [state]
  (filter #(#{:new :processing} (:state %))
          (vals (:requests state))))

(defn new-request [state {:keys [id chat-id text format]}]
  (let [active-count (count (active-requests state))]
    (if (>= active-count (:overload-limit state))
      (assoc state :bot-state :overloaded)
      (-> state ;; <-- threading macro
          (assoc-in [:requests id]
                    {:id id
                     :chat-id chat-id
                     :text text
                     :format format
                     :state :new
                     :response nil})
          (assoc :bot-state :idle)))))

(defn process-request [state id]
  (assoc-in state [:requests id :state] :processing))

(defn complete-request [state id response]
  (-> state
      (assoc-in [:requests id :state] :completed)
      (assoc-in [:requests id :response] response)))

(defn abort-request [state id error]
  (-> state
      (assoc-in [:requests id :state] :error)
      (assoc-in [:requests id :response] error)))

;; Additional helper query functions
(defn get-chat [state chat-id]
  (get-in state [:chats chat-id]))

(defn get-request [state request-id]
  (get-in state [:requests request-id]))

;; Async QR generation using Promise chains
(defn gen-qr [{:keys [text format]}]
  (let [;; Use hash of text for filename to avoid any sanitization issues
        hash (.update (.createHash crypto "md5") text)
        hash-hex (.digest hash "hex")
        ;; Take first 16 chars of hash
        filename (.substring hash-hex 0 16)
        output-dir "generated"
        output-path (join output-dir (str filename "_qr." (name format)))]

    (-> (fs/access output-dir)
        (.catch (fn [_err]
                  ;; Directory doesn't exist, create it
                  (fs/mkdir output-dir)))
        (.then (fn []
                 (.toFile QRCode output-path text
                          #js {:type (name format)
                               :scale 10
                               :errorCorrectionLevel "H"})))
        (.then (fn [] output-path)))))
