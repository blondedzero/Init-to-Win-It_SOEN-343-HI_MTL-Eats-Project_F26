# Production image for the Vite/React frontend: build once, then serve the files with nginx,
# which also proxies /api to the backend (see nginx.conf).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Browser key baked into the bundle by Vite; empty by default (the map page then shows a message)
ARG VITE_GOOGLE_MAPS_API_KEY=
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
