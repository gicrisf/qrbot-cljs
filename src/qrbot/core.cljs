(ns qrbot.core
  (:require ["dotenv" :as dotenv]
            [qrbot.state :as state]
            [qrbot.handlers :as handlers]
            [qrbot.telegram :as tg]))

(.config dotenv)

;; Disable Telegram bot API deprecation warnings
;; See: https://github.com/yagop/node-telegram-bot-api/issues/838
(aset (.-env js/process) "NTBA_FIX_319" "1")
(aset (.-env js/process) "NTBA_FIX_350" "0")

;; Atom to hold the bot instance - nil when stopped
(defonce bot-instance (atom nil))

(defn setup-handlers!
  "Set up all bot command handlers"
  [bot]
  ;; /start command
  (tg/on-text bot #"/start"
    (fn [msg]
      (let [chat-id (tg/get-chat-id msg)
            welcome-message "👋 Welcome to the QR Code Bot!
I can generate QR codes from text.
Send me any text, and I'll create a QR code for you!
Use /help for more info."]
        ;; Ensure chat exists in state
        (swap! state/app-state state/new-chat chat-id)
        (tg/send-message bot chat-id welcome-message))))

  ;; /help command
  (tg/on-text bot #"/help"
    (fn [msg]
      (let [chat-id (tg/get-chat-id msg)
            help-message "📖 **How to Use This Bot**
1. Send me any text, and I'll generate a QR code for it.
2. Use /settings to configure the QR code format (e.g., PNG, SVG).
3. Use /start to see the welcome message again.

**Commands:**
/start - Welcome message
/help - Show this help message
/settings - Configure QR code settings"]
        (tg/send-message bot chat-id help-message {:parse-mode "Markdown"}))))

  ;; /settings command
  (tg/on-text bot #"/settings"
    (fn [msg]
      (let [chat-id (tg/get-chat-id msg)
            settings-message "⚙️ **Settings**
Choose the QR code format:
1. PNG (default)
2. SVG

Use /set_png for PNG or /set_svg for SVG."]
        (tg/send-message bot chat-id settings-message)
        ;; Mark user as being in settings mode
        (swap! state/app-state state/set-chat-mode chat-id :settings))))

  ;; /set_png command
  (tg/on-text bot #"/set_png"
    (fn [msg]
      (let [chat-id (tg/get-chat-id msg)]
        (swap! state/app-state state/set-chat-format chat-id :png)
        (tg/send-message bot chat-id "✅ QR code format set to PNG.")
        ;; Back to normal mode
        (swap! state/app-state state/set-chat-mode chat-id :normal))))

  ;; /set_svg command
  (tg/on-text bot #"/set_svg"
    (fn [msg]
      (let [chat-id (tg/get-chat-id msg)]
        (swap! state/app-state state/set-chat-format chat-id :svg)
        (tg/send-message bot chat-id "✅ QR code format set to SVG.")
        ;; Back to normal mode
        (swap! state/app-state state/set-chat-mode chat-id :normal))))

  ;; Handle all non-command messages (for QR generation)
  (tg/on-message bot
    (fn [msg]
      (let [chat-id (tg/get-chat-id msg)
            msg-id (tg/get-message-id msg)
            text (tg/get-text msg)]

        (when text
          ;; Skip commands
          (when-not (.startsWith text "/")
            ;; Ensure chat exists
            (when-not (state/get-chat @state/app-state chat-id)
              (swap! state/app-state state/new-chat chat-id))

            ;; Get chat format preference
            (let [chat (state/get-chat @state/app-state chat-id)
                  format (or (:format chat) :png)]

              ;; Create new request
              (swap! state/app-state state/new-request
                     {:id msg-id
                      :chat-id chat-id
                      :text text
                      :format format})

              ;; Start processing
              (swap! state/app-state state/process-request msg-id)

              ;; Generate QR code (async)
              (-> (state/gen-qr {:text text :format format})
                  (.then (fn [response]
                           (swap! state/app-state state/complete-request msg-id response)))
                  (.catch (fn [error]
                            (swap! state/app-state state/abort-request msg-id error))))))))))

  bot)

(defn stop!
  "Stop the bot if it's running"
  []
  (when-let [bot @bot-instance]
    (println "Stopping bot...")
    (handlers/remove-watchers!)
    (tg/stop-polling bot)
    (reset! bot-instance nil)
    (println "Bot stopped.")))

(defn start!
  "Start the bot (stops existing instance first if running)"
  []
  (stop!)  ; Always stop first to avoid conflicts

  (println "Bot starting...")
  (when-not (.-TELEGRAM_TOKEN js/process.env)
    (throw (js/Error. "TELEGRAM_TOKEN not defined")))

  (let [bot (tg/create-bot (.-TELEGRAM_TOKEN js/process.env))]
    (setup-handlers! bot)
    (handlers/setup-watchers! bot)  ; Install state watchers
    (reset! bot-instance bot)
    (println "Bot running...")
    bot))

;; Called by shadow-cljs on hot reload
(defn reload []
  (println "Code reloaded!")
  ;; Restart the bot on hot reload to pick up new handler code
  (when @bot-instance
    (println "Restarting bot to apply changes...")
    (start!)))

;; Main entry point for production
(defn main []
  (start!))

;; Auto-start on initial load
(defonce _init (start!))
