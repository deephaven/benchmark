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
    final String[] stageRunIds = {"run-1bc89703ab", "run-1bcdbd28c2", "run-1bd2e385a7", "run-1bd80a0738",
            "run-1bdd3080da", "run-1bf1cb8f1b", "run-1bf6f1a736", "run-1bfc184e13", "run-1c013f1353"};
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
            assertEquals(
                """
                Static_Benchmark|Chng5d|Var5d|Rate|ChngRls|ScrProb
                VarBy- 2 Group 160K Unique Combos Float|-4.5%|1.4%|11,756,253|-5.0%|0.14%
                WhereNotIn- 1 Filter Col|-1.6%|0.8%|358,744,394|-1.0%|5.69%
                CumCombo- 6 Ops No Groups|-1.8%|1.2%|29,126,213|-1.8%|14.56%
                AsOfJoin- Join On 2 Cols 1 Match|-0.9%|2.4%|1,979,218|-0.9%|70.50%
                Where- 2 Filters|-1.3%|6.3%|961,307,378|-3.7%|83.63%
                ParquetWrite- Lz4Raw Multi Col|1.5%|3.6%|2,880,350|0.9%|67.55%
                WeightedSum-AggBy- 3 Sums 2 Groups 160K Unique ...|1.7%|1.6%|6,721,785|0.2%|27.95%
                Vector- 5 Calcs 1M Groups Dense Data|13.5%|12.3%|47,913,755|5.4%|27.30%
                SelectDistinct- 1 Group 250 Unique Vals|1.7%|1.3%|58,195,926|1.2%|21.81%
                WhereOneOf- 2 Filters|8.2%|5.3%|325,044,693|9.0%|12.58%""",
                table.toCsv("|"), "Wrong score table results");
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
        return text.endsWith("\n")?text:(text + '\n');
    }

}
