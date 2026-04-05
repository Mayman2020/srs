# syntax=docker/dockerfile:1
# Build from repository root: docker build -f deploy/docker/frontend/Dockerfile .
FROM node:20-bookworm AS build
WORKDIR /app
COPY SRS_System_frontend/package.json SRS_System_frontend/package-lock.json ./
RUN npm ci
COPY SRS_System_frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/docker/frontend/nginx-spa.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/admin-communications/browser /usr/share/nginx/html
EXPOSE 80
