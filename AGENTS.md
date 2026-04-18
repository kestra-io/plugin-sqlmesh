# Kestra SQLMesh Plugin

## What

- Provides plugin components under `io.kestra.plugin.sqlmesh.cli`.
- Includes classes such as `SQLMeshCLI`.

## Why

- This plugin integrates Kestra with SQLMesh CLI.
- It provides tasks that execute SQLMesh CLI commands.

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
