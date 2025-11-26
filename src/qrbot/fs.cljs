(ns qrbot.fs
  "Clean wrapper around node fs and fs/promises.")

;; Sync fs for streams
(def fs (js/require "fs"))

;; Async fs/promises for promise-based operations
(def fs-promises (js/require "fs/promises"))

(defn create-read-stream
  "Create a read stream for reading files.

  Parameters:
  - path: Path to file (string)

  Returns: ReadStream"
  [path]
  (.createReadStream ^js fs path))

(defn mkdir
  "Create a directory (async with promises).

  Parameters:
  - path: Directory path (string)
  - opts: Optional map with:
    - :recursive - Create parent directories if needed (default true)

  Returns: Promise"
  [path & [{:keys [recursive] :or {recursive true}}]]
  (.mkdir ^js fs-promises path (clj->js {:recursive recursive})))

(defn access
  "Check if a file/directory exists (async with promises).

  Parameters:
  - path: Path to check (string)

  Returns: Promise (resolves if exists, rejects if not)"
  [path]
  (.access ^js fs-promises path))
