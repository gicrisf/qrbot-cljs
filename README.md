# QR Code Telegram Bot

A Telegram bot that generates QR codes. ClojureScript version of [qrbot](https://github.com/gicrisf/qrbot).

Users can send any text, and the bot will create a QR code in either PNG or SVG format.

## Deployment

Download the latest release tarball from [GitHub Releases](https://github.com/gicrisf/qrbot-cljs/releases), then extract and run:

```bash
tar -xzf qrbot-cljs-release.tar.gz
cd release-package
docker build -t qrbot .
docker run -d --name qrbot -e "TELEGRAM_TOKEN=<TOKEN>" qrbot
```

The release package includes everything needed to run the bot without requiring Node.js, Java, or building from source. The deployment Docker image uses Bun and only runs the pre-compiled JavaScript. For the development Docker that compiles from source, see below.

## Development

### Prerequisites

- Node.js
- Java 21

### Installation

Clone the repository:

```bash
git clone https://github.com/gicrisf/qrbot-cljs
cd qrbot-cljs
```

Install dependencies:

```bash
npm install
```

Set up environment variables:
- Create a `.env` file in the root directory.
- Add your Telegram bot token:

```env
TELEGRAM_TOKEN=your_telegram_bot_token
```

### Running the Bot

Development build with watch mode:

```bash
npx shadow-cljs watch app
```

Production build:

```bash
npx shadow-cljs release app
node out/bot.js
```

### Building with Docker

Build the Docker image from source:

```bash
docker build -t qrbot-cljs .
```

Run the container:

```bash
docker run -d --name qrbot-container -e "TELEGRAM_TOKEN=<TOKEN>" qrbot-cljs
```

## Commands

- `/start` - display the welcome message.
- `/help` - show the help message with instructions.
- `/settings` - configure the QR code format (PNG or SVG).

## License

MIT
