package io.kestra.plugin.sqlmesh.cli;

import java.util.List;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.runners.TaskRunner;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.AbstractExecScript;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.runner.docker.Docker;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Run SQLMesh CLI workflows",
    description = """
        Executes SQLMesh commands through the `interpreter` (`/bin/sh -c` by default) within the configured task runner. Defaults to the Docker runner using image `ghcr.io/kestra-io/sqlmesh`. Add `beforeCommands` for setup and prefer `taskRunner` over the deprecated `docker` property.
        The inherited `warningOnStdErr` and `runner` properties are accepted but ignored: stderr is logged as is and `taskRunner` is the only runner setting honored."""
)
@Plugin(
    examples = {
        @Example(
            title = "Orchestrate a SQLMesh project by automatically applying the plan",
            full = true,
            code = {
                """
                    id: sqlmesh_transform
                    namespace: company.team

                    tasks:
                      - id: transform
                        type: io.kestra.plugin.sqlmesh.cli.SQLMeshCLI
                        beforeCommands:
                          - sqlmesh init duckdb
                        commands:
                          - sqlmesh plan --auto-apply"""
            }
        ),
        @Example(
            title = "Plan, apply, and export artifacts with custom runner settings",
            full = true,
            code = {
                """
                    id: sqlmesh_advanced
                    namespace: company.team

                    tasks:
                      - id: transform
                        type: io.kestra.plugin.sqlmesh.cli.SQLMeshCLI
                        beforeCommands:
                          - python -m pip install -r requirements.txt
                        commands:
                          - sqlmesh plan --restate-models staging* --auto-apply
                          - sqlmesh run prod --start 2024-12-01 --end 2024-12-07
                          - sqlmesh fetchdf prod my_model --output /data/exports/model.parquet
                        env:
                          SQLMESH_DB_USER: "{{ secret('db_user') }}"
                          SQLMESH_DB_PASS: "{{ secret('db_pass') }}"
                        containerImage: ghcr.io/kestra-io/sqlmesh:latest
                        taskRunner:
                          type: io.kestra.plugin.scripts.runner.docker.Docker
                        outputFiles:
                          - /data/exports/*.parquet
                    """
            }
        )
    }
)
public class SQLMeshCLI extends AbstractExecScript implements RunnableTask<ScriptOutput> {
    private static final String DEFAULT_IMAGE = "ghcr.io/kestra-io/sqlmesh";

    @Schema(
        title = "Primary SQLMesh CLI commands",
        description = "Required command list executed in order with `/bin/sh -c` inside the task runner."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<List<String>> commands;

    // Own copy: AbstractExecScript has no docker field from 2.0, NoSuchFieldError otherwise
    @Schema(
        title = "Deprecated Docker options",
        description = "Use taskRunner instead; kept for backward compatibility."
    )
    @PluginProperty(group = "deprecated")
    @Deprecated
    private DockerOptions docker;

    @Schema(
        title = "Task runner implementation",
        description = "Defaults to the Docker runner; configure plugin-specific properties to switch execution backends."
    )
    @PluginProperty(group = "execution")
    @Builder.Default
    @Valid
    private TaskRunner<?> taskRunner = Docker.instance();

    @Schema(
        title = "Container image for runner",
        description = "Used only when taskRunner runs in containers; defaults to `ghcr.io/kestra-io/sqlmesh`."
    )
    @PluginProperty(group = "execution")
    @Builder.Default
    private Property<String> containerImage = Property.ofValue(DEFAULT_IMAGE);

    @Schema(
        title = "Fail fast on the first failing command",
        description = """
            If true, `set -e` is prepended to `beforeCommands`, so the first failing line stops the run.
            Defaults to false, so a failing `beforeCommands` line does not stop the run.
            """
    )
    @Builder.Default
    @PluginProperty(group = "execution")
    protected Property<Boolean> failFast = Property.ofValue(false);

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        return this.commands(runContext)
            .withInterpreter(this.getInterpreter())
            .withBeforeCommands(this.getBeforeCommands())
            .withBeforeCommandsWithOptions(true)
            .withCommands(this.commands)
            .run();
    }

    @Override
    protected DockerOptions injectDefaults(RunContext runContext, DockerOptions original) throws IllegalVariableEvaluationException {
        if (original == null) {
            return null;
        }

        var builder = original.toBuilder();
        if (original.getImage() == null) {
            builder.image(DEFAULT_IMAGE);
        }
        if (original.getEntryPoint() == null || original.getEntryPoint().isEmpty()) {
            builder.entryPoint(List.of(""));
        }

        return builder.build();
    }
}
