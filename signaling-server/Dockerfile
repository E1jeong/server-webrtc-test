FROM node:24-alpine3.23 AS test

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY src ./src
COPY test ./test

RUN npm test

FROM node:24-alpine3.23

ENV HOST=0.0.0.0
ENV PORT=8080

WORKDIR /app

COPY --from=test /app/package*.json ./
RUN npm ci --omit=dev

COPY src ./src

EXPOSE 8080

USER node

CMD ["node", "src/server.js"]
