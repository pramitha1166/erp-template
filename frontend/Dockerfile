FROM node:22-alpine AS deps
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /build
COPY --from=deps /build/node_modules ./node_modules
COPY . .
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
