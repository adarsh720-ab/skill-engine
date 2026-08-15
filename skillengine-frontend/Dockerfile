# ---------- Build stage ----------
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# On Render, this is auto-populated from the backend service's RENDER_EXTERNAL_URL
# (see render.yaml). It's the bare origin, e.g. https://skillengine-api.onrender.com
# Locally: docker build --build-arg API_BASE_URL=https://your-backend-host .
ARG API_BASE_URL=https://YOUR-BACKEND-HOST
RUN sed -i "s#https://YOUR-BACKEND-HOST/api/v1#${API_BASE_URL}/api/v1#g" src/environments/environment.prod.ts

RUN npm run build:prod

# ---------- Runtime stage ----------
FROM nginx:1.27-alpine

# Angular's new build system outputs to dist/<project>/browser
COPY --from=build /app/dist/skillengine-ui/browser /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Render sets $PORT at runtime; nginx:alpine's docker-entrypoint.d scripts
# automatically run envsubst on files in /etc/nginx/templates/*.template
ENV PORT=8080
EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]