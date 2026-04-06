package io.kestra.plugin.sqlmesh.cli;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.*;
import io.kestra.core.models.tasks.runners.TaskRunner;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
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
    description = "Executes SQLMesh commands through `/bin/sh -c` within the configured task runner. Defaults to the Docker runner using image `ghcr.io/kestra-io/sqlmesh`; add `beforeCommands` for setup and prefer `taskRunner` over the deprecated docker property."
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
public class SQLMeshCLI extends Task implements RunnableTask<ScriptOutput>, NamespaceFilesInterface, InputFilesInterface, OutputFilesInterface {
    private static final String DEFAULT_IMAGE = "ghcr.io/kestra-io/sqlmesh";

    @Schema(
        title = "Setup commands before main run",
        description = "Optional steps executed with `/bin/sh -c` before commands, useful for initialization or installing prerequisites."
    )
    @PluginProperty(group = "execution")
    protected Property<List<String>> beforeCommands;

    @Schema(
        title = "Primary SQLMesh CLI commands",
        description = "Required command list executed in order with `/bin/sh -c` inside the task runner."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<List<String>> commands;

    @Schema(
        title = "Extra environment variables",
        description = "Key-value map merged into the task environment; values support templating."
    )
    @PluginProperty(group = "execution", 
        additionalProperties = String.class,
        dynamic = true
    )
    protected Map<String, String> env;

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
    @PluginProperty(dynamic = true, group = "execution")
    @Builder.Default
    private String containerImage = DEFAULT_IMAGE;

    @PluginProperty(group = "source")
    private NamespaceFiles namespaceFiles;

    @PluginProperty(group = "source")
    private Object inputFiles;

    @PluginProperty(group = "destination")
    private Property<List<String>> outputFiles;

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        var renderedOutputFiles = runContext.render(this.outputFiles).asList(String.class);
        return new CommandsWrapper(runContext)
            .withWarningOnStdErr(false)
            .withDockerOptions(injectDefaults(getDocker()))
            .withTaskRunner(this.taskRunner)
            .withContainerImage(this.containerImage)
            .withEnv(Optional.ofNullable(env).orElse(new HashMap<>()))
            .withNamespaceFiles(namespaceFiles)
            .withInputFiles(inputFiles)
            .withOutputFiles(renderedOutputFiles.isEmpty() ? null : renderedOutputFiles)
            .withInterpreter(Property.ofValue(List.of("/bin/sh", "-c")))
            .withBeforeCommands(this.beforeCommands)
            .withCommands(this.commands)
            .run();
    }

    private DockerOptions injectDefaults(DockerOptions original) {
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
