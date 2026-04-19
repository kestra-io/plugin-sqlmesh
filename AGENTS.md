# Kestra SQLMesh Plugin

## What

- Provides plugin components under `io.kestra.plugin.sqlmesh.cli`.
- Includes classes such as `SQLMeshCLI`.

## Why

- What user problem does this solve? Teams need to run SQLMesh commands from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps SQLMesh steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on SQLMesh.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `sqlmesh`

### Key Plugin Classes

- `io.kestra.plugin.sqlmesh.cli.SQLMeshCLI`

### Project Structure

```
plugin-sqlmesh/
├── src/main/java/io/kestra/plugin/sqlmesh/cli/
├── src/test/java/io/kestra/plugin/sqlmesh/cli/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
