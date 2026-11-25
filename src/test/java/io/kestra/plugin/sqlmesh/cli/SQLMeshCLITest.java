package io.kestra.plugin.sqlmesh.cli;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class SQLMeshCLITest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @SuppressWarnings("unchecked")
    void run() throws Exception {
        String environmentKey = "MY_KEY";
        String environmentValue = "MY_VALUE";

        SQLMeshCLI.SQLMeshCLIBuilder<?, ?> terraformBuilder = SQLMeshCLI.builder()
            .id(IdUtils.create())
            .type(SQLMeshCLI.class.getName())
            .docker(DockerOptions.builder().image("ghcr.io/kestra-io/sqlmesh").entryPoint(Collections.emptyList()).build())
            .commands(Property.ofValue(List.of("sqlmesh --version")));

        SQLMeshCLI runner = terraformBuilder.build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, runner, Map.of("environmentKey", environmentKey, "environmentValue", environmentValue));

        ScriptOutput scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));

        runner = terraformBuilder
            .env(Property.ofValue(Map.of("{{ inputs.environmentKey }}", "{{ inputs.environmentValue }}")))
            .beforeCommands(Property.ofValue(List.of("sqlmesh init duckdb")))
            .commands(
                Property.ofValue(
                    List.of(
                        "echo \"::{\\\"outputs\\\":{" +
                            "\\\"customEnv\\\":\\\"$" + environmentKey + "\\\"" +
                            "}}::\"",
                        "sqlmesh info | tr -d ' \n' | xargs -0 -I {} echo '::{\"outputs\":{}}::'"
                    )
                )
            )
            .build();

        scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));
        assertThat(scriptOutput.getVars().get("customEnv"), is(environmentValue));
    }

    @Test
    void failFastStopsOnAFailingBeforeCommand() throws Exception {
        var task = SQLMeshCLI.builder()
            .id(IdUtils.create())
            .type(SQLMeshCLI.class.getName())
            .failFast(Property.ofValue(true))
            .beforeCommands(Property.ofValue(List.of("false")))
            .commands(Property.ofValue(List.of("sqlmesh --version")))
            .build();

        var runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());

        assertThrows(Exception.class, () -> task.run(runContext));
    }

    @Test
    void failingBeforeCommandDoesNotStopTheRunByDefault() throws Exception {
        var task = SQLMeshCLI.builder()
            .id(IdUtils.create())
            .type(SQLMeshCLI.class.getName())
            .beforeCommands(Property.ofValue(List.of("false", "cd {{ workingDir }}")))
            .commands(Property.ofValue(List.of("sqlmesh --version")))
            .build();

        var runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());

        assertThat(task.run(runContext).getExitCode(), is(0));
    }

    @Test
    void toStringDoesNotExposeEnvSecrets() throws Exception {
        var task = SQLMeshCLI.builder()
            .id(IdUtils.create())
            .type(SQLMeshCLI.class.getName())
            .commands(Property.ofValue(List.of("echo")))
            .env(Property.ofValue(Map.of("SECRET_KEY", "super-secret-value")))
            .build();
        // render caches the value on the property, as run() does
        TestsUtils.mockRunContext(runContextFactory, task, Map.of()).render(task.getEnv()).asMap(String.class, String.class);

        assertThat(task.toString(), not(containsString("super-secret-value")));
    }
}
