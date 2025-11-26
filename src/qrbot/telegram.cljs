(ns qrbot.telegram
  "Clean wrapper around node-telegram-bot-api.

  This namespace provides Clojure-friendly functions for interacting with
  the Telegram Bot API, hiding JavaScript interop details."
  (:require [qrbot.fs :as fs]
            [clojure.set]))

(def TelegramBot (js/require "node-telegram-bot-api"))

;; Bot Creation and Management

(defn create-bot
  "Create a new Telegram bot instance.

  Parameters:
  - token: Telegram bot token (string)
  - opts: Optional map with:
    - :polling - Enable polling (default true)

  Returns: Bot instance"
  [token & [{:keys [polling] :or {polling true}}]]
  (new TelegramBot token (clj->js {:polling polling})))

(defn stop-polling
  "Stop polling for updates.

  Parameters:
  - bot: Bot instance"
  [bot]
  (.stopPolling ^js bot))

;; Message Sending

(defn send-message
  "Send a text message.

  Parameters:
  - bot: Bot instance
  - chat-id: Chat ID (number)
  - text: Message text (string)
  - opts: Optional map with:
    - :reply-to-message-id - Message ID to reply to
    - :parse-mode - 'Markdown' or 'HTML'

  Returns: Promise of sent message"
  [bot chat-id text & [opts]]
  (let [js-opts (clj->js (when opts
                           (-> opts
                               (clojure.set/rename-keys
                                 {:reply-to-message-id :reply_to_message_id
                                  :parse-mode :parse_mode}))))]
    (.sendMessage ^js bot chat-id text js-opts)))

(defn send-photo
  "Send a photo.

  Parameters:
  - bot: Bot instance
  - chat-id: Chat ID (number)
  - photo-path: Path to photo file (string)
  - opts: Optional map with:
    - :caption - Photo caption
    - :reply-to-message-id - Message ID to reply to

  Returns: Promise of sent message"
  [bot chat-id photo-path & [opts]]
  (let [stream (fs/create-read-stream photo-path)
        js-opts (clj->js (when opts
                           (-> opts
                               (clojure.set/rename-keys
                                 {:reply-to-message-id :reply_to_message_id}))))]
    (.sendPhoto ^js bot chat-id stream js-opts)))

(defn send-document
  "Send a document.

  Parameters:
  - bot: Bot instance
  - chat-id: Chat ID (number)
  - doc-path: Path to document file (string)
  - opts: Optional map with:
    - :caption - Document caption
    - :reply-to-message-id - Message ID to reply to

  Returns: Promise of sent message"
  [bot chat-id doc-path & [opts]]
  (let [stream (fs/create-read-stream doc-path)
        js-opts (clj->js (when opts
                           (-> opts
                               (clojure.set/rename-keys
                                 {:reply-to-message-id :reply_to_message_id}))))]
    (.sendDocument ^js bot chat-id stream js-opts)))

;; Event Handlers

(defn on-text
  "Register a handler for messages matching a regex pattern.

  Parameters:
  - bot: Bot instance
  - pattern: Regular expression pattern
  - handler: Function of [msg] to handle matching messages

  Returns: bot instance"
  [bot pattern handler]
  (.onText ^js bot pattern handler)
  bot)

(defn on-message
  "Register a handler for all messages.

  Parameters:
  - bot: Bot instance
  - handler: Function of [msg] to handle messages

  Returns: bot instance"
  [bot handler]
  (.on ^js bot "message" handler)
  bot)

;; Bot Commands Management

(defn set-commands
  "Set bot commands menu for a specific chat.

  Parameters:
  - bot: Bot instance
  - commands: Vector of maps with :command and :description keys
  - opts: Map with:
    - :chat-id - Chat ID to set commands for (required)

  Example:
  (set-commands bot
    [{:command \"/start\" :description \"Start bot\"}
     {:command \"/help\" :description \"Show help\"}]
    {:chat-id 123456})

  Returns: Promise"
  [bot commands {:keys [chat-id]}]
  (let [js-commands (clj->js commands)
        js-scope (clj->js {:scope {:type "chat"
                                   :chat_id chat-id}})]
    (.setMyCommands ^js bot js-commands js-scope)))

;; Message Utilities

(defn get-chat-id
  "Extract chat ID from a message object.

  Parameters:
  - msg: Telegram message object

  Returns: Chat ID (number)"
  [msg]
  (.. ^js msg -chat -id))

(defn get-message-id
  "Extract message ID from a message object.

  Parameters:
  - msg: Telegram message object

  Returns: Message ID (number)"
  [msg]
  (.-message_id ^js msg))

(defn get-text
  "Extract text from a message object.

  Parameters:
  - msg: Telegram message object

  Returns: Message text (string or nil)"
  [msg]
  (.-text ^js msg))

(defn get-user-id
  "Extract user ID from a message object.

  Parameters:
  - msg: Telegram message object

  Returns: User ID (number)"
  [msg]
  (.. ^js msg -from -id))
