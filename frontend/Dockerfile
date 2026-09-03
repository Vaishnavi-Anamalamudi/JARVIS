FROM node:22-alpine AS build

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html postcss.config.js tailwind.config.js tsconfig.json tsconfig.node.json vite.config.ts ./
COPY src src
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 CMD test -f /var/run/nginx.pid || exit 1
