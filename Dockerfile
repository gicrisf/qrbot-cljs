# Build stage with Node and Java
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /usr/src/app

# Install Node.js
RUN apk add --no-cache nodejs npm

# Copy package files and install dependencies
COPY package.json package-lock.json* ./
RUN npm install

# Copy source code and build configuration
COPY . .

# Build production release
RUN npx shadow-cljs release app

# Production stage
FROM node:alpine AS release
WORKDIR /usr/src/app

# Copy only production dependencies and compiled output
COPY package.json package-lock.json* ./
RUN npm install --production

COPY --from=builder /usr/src/app/out ./out

# Create generated directory and set ownership
RUN mkdir -p generated && chown -R node:node /usr/src/app

# Run the app
USER node
ENTRYPOINT ["node", "out/bot.js"]
