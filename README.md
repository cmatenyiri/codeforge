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

### The whole app in Docker: two backend replicas behind nginx

```sh
cd codeforge-orchestration
docker compose --profile app up -d --build
docker compose --profile app down           # to stop it
```

Open http://localhost (set `CODEFORGE_HTTP_PORT` to use another port).

- **nginx** serves the frontend and sends `/api` and `/ws` to the `backend`
  service's replicas. `docker logs codeforge-nginx` shows which replica (by
  address) answered each request.
- **The backend** is one service with `replicas: 2`, so its containers are
  `codeforge-orchestration-backend-1` and `-2`. Use `--scale backend=N` for a
  different count. The replicas run with the `docker` profile
  (`application-docker.yml`): dependencies are reached by their service names,
  and each replica gives Judge0 its own hostname for callbacks.
- **What the replicas share:**
  - The database. Liquibase's lock lets only one replica migrate it while the
    others wait, so all of them can start at once. Quartz clusters through it,
    so each contest alarm fires on only one replica.
  - RabbitMQ. It carries live updates to browsers connected to any replica. If
    a Judge0 callback reaches the wrong replica, RabbitMQ forwards it to the
    one waiting for it.

Both ways can run at the same time. A backend started from IntelliJ joins the
same database, broker and Quartz cluster.

## The database

Liquibase owns the schema: `codeforge-backend/src/main/resources/db/changelog`,
in XML. It creates every table, the Quartz ones included, and one admin
account. There is no other seed data.

| username | password      |
| -------- | ------------- |
| `admin`  | `forge-admin` |

Hibernate only validates (`ddl-auto: validate`), so an entity change needs a
changeset alongside it. Otherwise the backend refuses to start. Add a new
file under `changes/` and include it at the end of `db.changelog-master.xml`.
Never edit a changeset that has already run.

To start over from an empty database (stop every backend first):

```sh
docker exec codeforge-mysql mysql -uroot -proot \
  -e "DROP DATABASE codeforge; CREATE DATABASE codeforge;"
```

The next backend to start rebuilds the schema and the admin account.
