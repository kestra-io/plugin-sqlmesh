package io.kestra.plugin.sqlmesh.cli;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.*;
import io.kestra.core.models.tasks.runners.ScriptService;
import io.kestra.core.models.tasks.runners.TaskRunner;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.RunnerType;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
import io.kestra.plugin.scripts.runner.docker.Docker;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.runners.RunContext;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Orchestrate a SQLMesh project from the Command Line Interface"
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
        )
    }
)
public class SQLMeshCLI extends Task implements RunnableTask<ScriptOutput>, NamespaceFilesInterface, InputFilesInterface, OutputFilesInterface {
    private static final String DEFAULT_IMAGE = "ghcr.io/kestra-io/sqlmesh";

    @Schema(
        title = "The commands to execute before the main list of commands, e.g. to initialize or prepare the environment"
    )
    @PluginProperty(dynamic = true)
    protected List<String> beforeCommands;

    @Schema(
        title = "The commands to run in the container."
    )
    @PluginProperty(dynamic = true)
    @NotNull
    @NotEmpty
    protected List<String> commands;

    @Schema(
        title = "Additional environment variables for the current process."
    )
    @PluginProperty(
        additionalProperties = String.class,
        dynamic = true
    )
    protected Map<String, String> env;

    @Schema(
        title = "Deprecated, use 'taskRunner' instead"
    )
    @PluginProperty
    @Deprecated
    private DockerOptions docker;

    @Schema(
        title = "The task runner to use.",
        description = "Task runners are provided by plugins, each have their own properties."
    )
    @PluginProperty
    @Builder.Default
    @Valid
    private TaskRunner taskRunner = Docker.instance();

    @Schema(title = "The task runner container image, only used if the task runner is container-based.")
    @PluginProperty(dynamic = true)
    @Builder.Default
    private String containerImage = DEFAULT_IMAGE;

    private NamespaceFiles namespaceFiles;

    private Object inputFiles;

    private List<String> outputFiles;

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        return new CommandsWrapper(runContext)
            .withWarningOnStdErr(false)
            .withDockerOptions(injectDefaults(getDocker()))
            .withTaskRunner(this.taskRunner)
            .withContainerImage(this.containerImage)
            .withEnv(Optional.ofNullable(env).orElse(new HashMap<>()))
            .withNamespaceFiles(namespaceFiles)
            .withInputFiles(inputFiles)
            .withOutputFiles(outputFiles)
            .withCommands(
                ScriptService.scriptCommands(
                    List.of("/bin/sh", "-c"),
                    Optional.ofNullable(this.beforeCommands).map(throwFunction(runContext::render)).orElse(null),
                    runContext.render(this.commands)
                )
            )
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
