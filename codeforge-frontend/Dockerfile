# syntax=docker/dockerfile:1

# ── Build ─────────────────────────────────────────────────────────────────
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .

# Empty by default: the app then calls the API on its own origin, which is how
# the Compose setup serves it — nginx routes /api and /ws to the backend. Pass
# an origin to build the app for a backend that lives somewhere else.
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# ── Serve ─────────────────────────────────────────────────────────────────
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
