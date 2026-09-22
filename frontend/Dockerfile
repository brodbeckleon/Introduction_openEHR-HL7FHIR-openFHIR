FROM node:22-alpine

WORKDIR /app

COPY package.json package-lock.json ./
# The stamp is written here too, so a volume seeded from this image starts in step and the
# entrypoint has nothing to do on a first run.
RUN npm install && md5sum package-lock.json | cut -d' ' -f1 > node_modules/.lock-stamp

COPY . .

# Outside /app on purpose: the compose file bind-mounts ./frontend over /app, which would shadow
# anything kept there — including the very script that exists to cope with that mount.
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 5173

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["npm", "run", "dev", "--", "--host"]
