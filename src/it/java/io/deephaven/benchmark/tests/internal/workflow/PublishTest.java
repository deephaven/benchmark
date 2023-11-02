/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.internal.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URL;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.util.Filer;

/**
 * Build a directory structure like what can be seen in the deephaven-benchmark GCloud bucket. Then run the publish
 * query to generate a result table and verify the result. The goal is to test some benchmark corner cases like obsolete
 * benchmarks, new benchmarks, benchmarks a few days into the next release, etc.
 */
public class PublishTest {
    final String[] csvFileNames = {"benchmark-metrics.csv", "benchmark-platform.csv", "benchmark-results.csv"};
    final String[] stageRunIds = {"run-1bc89703ab", "run-1bd2e385a7", "run-1bd80a0738", "run-1bdd3080da",
            "run-1bf1cb8f1b", "run-1bf6f1a736", "run-1bfc184e13", "run-1c013f1353", "run-1c06655366", "run-1c0b8bfec6"};
    final Bench api = Bench.create(this);

    @BeforeEach
    public void setup() throws Exception {
        var baseTablesPy = resource("/io/deephaven/benchmark/run/profile/benchmark_tables.dh.py");
        var srcUri = resource("workflow.md").toString().replace("workflow.md", "it-deephaven-benchmark/nightly");
        stageRuns(srcUri, "/data/it-deephaven-benchmark/nightly", stageRunIds);
        stageFile(baseTablesPy.toString(), "/data/it-deephaven-benchmark/benchmark_tables.dh.py");
    }

    @Test
    public void verifyPublishQuery() throws Exception {
        var publishUrl = getClass().getResource("/io/deephaven/benchmark/run/profile/queries/publish.py");
        api.setName("Verify Publish Query");

        var q = getURLText(publishUrl);
        q = updateQuerySnippetSourceDirectories(q);

        api.query(q).fetchAfter("nightly_worst_score_small", table -> {
            assertEquals("""
                Static_Benchmark|Chng5d|Var5d|Rate|ChngRls|Scr|ScrProb
                AsOfJoin- Join On 2 Cols 1 Match|-44.1%|1.0%|1,111,111|-44.1%|-44.9|0.00%
                ReverseAsOfJoin- Join On 2 Cols 1 Match|-3.2%|2.5%|1,933,301|-3.2%|-1.3|20.89%
                WhereNotIn- 1 Filter Col|-0.3%|1.0%|362,236,812|-0.7%|-0.3|76.96%
                VarBy- 2 Group 160K Unique Combos Float|-0.2%|2.2%|12,213,326|-0.7%|-0.1|94.35%
                ParquetWrite- Lz4Raw Multi Col|0.8%|3.7%|2,869,769|1.0%|0.2|82.35%
                SelectDistinct- 1 Group 250 Unique Vals|0.6%|1.3%|57,973,815|0.7%|0.4|65.86%
                Where- 2 Filters|2.7%|6.0%|990,671,179|-1.0%|0.4|65.47%
                Vector- 5 Calcs 1M Groups Dense Data|9.8%|13.2%|46,943,765|2.4%|0.7|45.55%
                WhereOneOf- 2 Filters|6.7%|5.8%|325,609,160|9.0%|1.2|24.50%
                CumCombo- 6 Ops No Groups|3.8%|1.4%|30,721,966|3.9%|2.7|0.78%""",
                    table.toCsv("|"), "Wrong worst score table results");
        }).fetchAfter("nightly_best_score_small", table -> {
            assertEquals("""
                Static_Benchmark|Chng5d|Var5d|Rate|ChngRls|Scr|ScrProb
                CumCombo- 6 Ops No Groups|3.8%|1.4%|30,721,966|3.9%|2.7|0.78%
                WhereOneOf- 2 Filters|6.7%|5.8%|325,609,160|9.0%|1.2|24.50%
                Vector- 5 Calcs 1M Groups Dense Data|9.8%|13.2%|46,943,765|2.4%|0.7|45.55%
                Where- 2 Filters|2.7%|6.0%|990,671,179|-1.0%|0.4|65.47%
                SelectDistinct- 1 Group 250 Unique Vals|0.6%|1.3%|57,973,815|0.7%|0.4|65.86%
                ParquetWrite- Lz4Raw Multi Col|0.8%|3.7%|2,869,769|1.0%|0.2|82.35%
                VarBy- 2 Group 160K Unique Combos Float|-0.2%|2.2%|12,213,326|-0.7%|-0.1|94.35%
                WhereNotIn- 1 Filter Col|-0.3%|1.0%|362,236,812|-0.7%|-0.3|76.96%
                ReverseAsOfJoin- Join On 2 Cols 1 Match|-3.2%|2.5%|1,933,301|-3.2%|-1.3|20.89%
                AsOfJoin- Join On 2 Cols 1 Match|-44.1%|1.0%|1,111,111|-44.1%|-44.9|0.00%""",
                    table.toCsv("|"), "Wrong best score table results");
        }).execute();
        api.awaitCompletion();
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

    // Change the nfs directory required for the BenchmarkDemo to data for local processing of the data
    // that was placed in the default Deephaven docker install location in test setup
    private String updateQuerySnippetSourceDirectories(String q) {
        q = q.replace(".exists('/nfs/deephaven-benchmark')", ".exists('/data/it-deephaven-benchmark')");
        return q.replace("file:///nfs", "file:///data").replace("/deephaven-benchmark", "/it-deephaven-benchmark");
    }

    private URL resource(String name) {
        return getClass().getResource(name);
    }

    private void stageRuns(String srcUri, String dstDir, String... runIds) throws Exception {
        var q = """
        import os
        os.system('rm -rf /data/it-deephaven-benchmark')
        """;
        api.query(q).execute();
        api.awaitCompletion();

        for (String runId : runIds) {
            for (String fileName : csvFileNames) {
                var relPath = '/' + runId + '/' + fileName;
                stageFile(srcUri + relPath, dstDir + relPath);
            }
        }
    }

    private void stageFile(String srcUri, String dstPath) throws Exception {
        var fileContents = getURLText(new URL(srcUri));
        var q = """
        contents = '''${fileContents}'''
        import os, sys
        os.makedirs(os.path.dirname('${dstPath}'), exist_ok=True)
        with open('${dstPath}', 'w') as f:
            f.write(contents)
        """;
        q = q.replace("${dstPath}", dstPath);
        q = q.replace("${fileContents}", fileContents);
        api.query(q).execute();
        api.awaitCompletion();
    }

    private String getURLText(URL publishUrl) {
        var text = Filer.getURLText(publishUrl);
        return text.endsWith("\n") ? text : (text + '\n');
    }

}
