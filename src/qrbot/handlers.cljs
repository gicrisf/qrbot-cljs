(ns qrbot.handlers
  (:require [qrbot.state :as state]
            [qrbot.telegram :as tg]))

;; Handle requests that just transitioned to Processing state
(defn handle-processing-request [bot request]
  (println (str "Processing request " (:id request) " from " (:chat-id request)))

  ;; Send a "generating..." message
  (tg/send-message bot
                   (:chat-id request)
                   (str "Bot is now Processing: " (:text request))
                   {:reply-to-message-id (:id request)}))

;; Handle requests that just transitioned to Completed state
(defn handle-completed-request [bot request]
  (println (str "Completed request " (:id request) " from " (:chat-id request)))

  (let [{:keys [chat-id format response id text]} request]
    (when-not response
      (throw (js/Error. "No local writing path provided")))

    (case format
      :png (tg/send-photo bot
                          chat-id
                          response
                          {:caption (str "QR image for request: " text)
                           :reply-to-message-id id})
      :svg (tg/send-document bot
                             chat-id
                             response
                             {:caption (str "QR image for request: " text)
                              :reply-to-message-id id})
      (println "Unsupported format:" format))))

;; Handle requests that transitioned to Error state
(defn handle-error-request [bot request]
  (let [error (:response request)]
    (println (str "Request " (:id request) " failed with error:"))
    (println (str "  Error message: " (.-message error)))
    (println (str "  Error stack: " (.-stack error)))

    (tg/send-message bot
                     (:chat-id request)
                     "Sorry, there was an error generating your QR code. Please try again."
                     {:reply-to-message-id (:id request)})))

;; Compare two requests and detect state transitions
(defn handle-request-transition [bot old-request new-request]
  (when (not= (:state old-request) (:state new-request))
    ;; State changed!
    (case (:state new-request)
      :new nil  ; Nothing to do for new requests
      :processing (handle-processing-request bot new-request)
      :completed (handle-completed-request bot new-request)
      :error (handle-error-request bot new-request)
      nil)))

;; Watcher function that gets called on every state change
(defn watch-requests [bot]
  (fn [_key _ref old-state new-state]
    (let [old-requests (:requests old-state)
          new-requests (:requests new-state)]

      ;; Check each request for state transitions
      (doseq [[id new-req] new-requests]
        (when-let [old-req (get old-requests id)]
          (handle-request-transition bot old-req new-req))))))

;; Handle chat mode changes (for settings)
(defn handle-chat-mode-change [bot old-chat new-chat]
  (when (not= (:mode old-chat) (:mode new-chat))
    (case (:mode new-chat)
      :normal
      (tg/set-commands bot
                       [{:command "/start" :description "Show start message"}
                        {:command "/help" :description "Show help message"}
                        {:command "/settings" :description "Change settings"}]
                       {:chat-id (:id new-chat)})

      :settings
      (tg/set-commands bot
                       [{:command "/set_png" :description "Set QR code format to PNG"}
                        {:command "/set_svg" :description "Set QR code format to SVG"}]
                       {:chat-id (:id new-chat)})

      nil)))

;; Watcher for chat changes
(defn watch-chats [bot]
  (fn [_key _ref old-state new-state]
    (let [old-chats (:chats old-state)
          new-chats (:chats new-state)]

      ;; Check each chat for mode transitions
      (doseq [[id new-chat] new-chats]
        (when-let [old-chat (get old-chats id)]
          (handle-chat-mode-change bot old-chat new-chat))))))

;; Setup all watchers
(defn setup-watchers! [bot]
  (add-watch state/app-state :request-watcher (watch-requests bot))
  (add-watch state/app-state :chat-watcher (watch-chats bot))
  (println "State watchers installed"))

;; Remove all watchers
(defn remove-watchers! []
  (remove-watch state/app-state :request-watcher)
  (remove-watch state/app-state :chat-watcher)
  (println "State watchers removed"))
