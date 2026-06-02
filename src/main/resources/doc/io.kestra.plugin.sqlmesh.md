# How to use the SQLMesh plugin

Run SQLMesh CLI commands — plan, apply, test, and audit transformations — from Kestra flows inside a container.

## Common properties

`containerImage` defaults to `ghcr.io/kestra-io/sqlmesh`. `taskRunner` controls where the container runs — defaults to Docker.

## Tasks

`cli.SQLMeshCLI` runs one or more SQLMesh CLI commands set in `commands` (e.g. `sqlmesh plan --auto-apply`, `sqlmesh run`, `sqlmesh test`). Use `beforeCommands` for setup steps. Pass database credentials and other secrets as environment variables via `env` — store sensitive values in [secrets](https://kestra.io/docs/concepts/secret). Pass supporting config files via `inputFiles` or pull them from [namespace files](https://kestra.io/docs/concepts/namespace-files). Apply runner properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).
