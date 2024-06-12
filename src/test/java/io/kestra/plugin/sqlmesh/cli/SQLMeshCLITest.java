package io.kestra.plugin.sqlmesh.cli;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.sqlmesh.cli.SQLMeshCLI;
import io.kestra.core.junit.annotations.KestraTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
            .commands(List.of("sqlmesh --version"));

        SQLMeshCLI runner = terraformBuilder.build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, runner, Map.of("environmentKey", environmentKey, "environmentValue", environmentValue));

        ScriptOutput scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));

        runner = terraformBuilder
            .env(Map.of("{{ inputs.environmentKey }}", "{{ inputs.environmentValue }}"))
            .beforeCommands(List.of("sqlmesh init duckdb"))
            .commands(List.of(
                "echo \"::{\\\"outputs\\\":{" +
                    "\\\"customEnv\\\":\\\"$" + environmentKey + "\\\"" +
                    "}}::\"",
                "sqlmesh info | tr -d ' \n' | xargs -0 -I {} echo '::{\"outputs\":{}}::'"
                             ))
            .build();

        scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));
        assertThat(scriptOutput.getVars().get("customEnv"), is(environmentValue));
    }
}
