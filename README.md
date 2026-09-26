# codeforge

## Running

Everything runs from `codeforge-orchestration/`. There are two ways to run the app.

### Developing: backend from IntelliJ, frontend from VS Code

```sh
cd codeforge-orchestration
docker compose up -d        # MySQL, Judge0, RabbitMQ only
```

Then run `CodeforgeApplication` from IntelliJ (or `./gradlew bootRun` in
`codeforge-backend`) and `npm run dev` in `codeforge-frontend`. Open
http://localhost:5173.

The backend uses the default profile (`application.yml`), where every
dependency is on `localhost`. It must listen on port 8080, because Judge0
reports results back to `host.docker.internal:8080`.

### The whole app in Docker: two backends behind nginx

```sh
cd codeforge-orchestration
docker compose --profile app up -d --build
docker compose --profile app down           # to stop it
```

Open http://localhost (set `CODEFORGE_HTTP_PORT` to use another port).

- **nginx** serves the frontend and sends `/api` and `/ws` to `backend-1` and
  `backend-2`. `docker logs codeforge-nginx` shows which backend answered each
  request.
- **The backends** run with the `docker` profile (`application-docker.yml`):
  dependencies are reached by their service names, and each instance gives
  Judge0 its own address for callbacks.
- **What the instances share:**
  - RabbitMQ carries the live updates to browsers on either instance.
  - The database hosts the Quartz cluster, so each contest alarm fires on only
    one instance.

Both ways can run at the same time. A backend started from IntelliJ joins the
same database, broker and Quartz cluster.
