FROM node:22-alpine AS deps
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /build
COPY --from=deps /build/node_modules ./node_modules
COPY . .
# NEXT_PUBLIC_* values are inlined into the client bundle by `next build`,
# not read at container start — so the API address has to be known here.
# Supplying it at run time in the task definition would silently do nothing
# for browser code. Defaults to localhost for a plain `docker build`.
ARG NEXT_PUBLIC_API_BASE_URL="http://localhost:8080/api"
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
RUN npm run build
# Next serves static assets from public/, but the directory is optional and
# absent from this repo — create it so the runtime COPY below always resolves.
RUN mkdir -p /build/public

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/public ./public
COPY --from=build --chown=app:app /build/.next/standalone ./
COPY --from=build --chown=app:app /build/.next/static ./.next/static
USER app
EXPOSE 3000
CMD ["node", "server.js"]
