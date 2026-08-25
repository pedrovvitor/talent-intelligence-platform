FROM node:26-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
USER nginx
EXPOSE 8080
