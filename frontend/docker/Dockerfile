FROM node:18.15 as builder
WORKDIR /build
COPY . .
RUN npm install
RUN npm run build
RUN npm run export

FROM nginx:stable-alpine
COPY docker/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/out /usr/share/nginx/html
