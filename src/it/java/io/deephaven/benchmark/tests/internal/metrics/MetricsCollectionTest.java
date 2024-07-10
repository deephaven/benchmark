package io.deephaven.benchmark.tests.internal.metrics;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.util.Filer;

/**
 * Test to see what metrics are available in the remote Deephaven Server. Note. These tests should pass when DH is JVM
 * 17+
 */
public class MetricsCollectionTest {
    final Bench api = Bench.create(this);

    @Test
    public void collectMetricSet() {
        var query = """
        bench_api_metrics_init()
        bench_api_metrics_add('c1','n1',2.0,'test')
        bench_api_metrics_add('c2','n2',2.0,'test')
        mymetrics = bench_api_metrics_collect()
        """;

        api.query(query).fetchAfter("mymetrics", table -> {
            assertEquals("timestamp, origin, category, name, value, note", formatCols(table.getColumnNames()),
                    "Wrong column names");
            assertEquals(2, table.getRowCount(), "Wrong row count");
        }).execute();
    }

    @Test
    public void collectNoMetrics() {
        var query = """
        bench_api_metrics_init()
        mymetrics = bench_api_metrics_collect()
        """;

        api.query(query).fetchAfter("mymetrics", table -> {
            assertEquals("timestamp, origin, category, name, value, note", formatCols(table.getColumnNames()),
                    "Wrong column names");
            assertEquals(0, table.getRowCount(), "Wrong row count");
        }).execute();
    }

    @Test
    public void collectMetricsToFile() throws Exception {
        var query = """
        bench_api_metrics_init()
        bench_api_metrics_add('c1','n1',2.0,'test')
        bench_api_metrics_add('c2','n2',2.0,'test')
        mymetrics = bench_api_metrics_collect()
        """;

        api.query(query).fetchAfter("mymetrics", table -> {
            int rowCount = table.getRowCount();
            assertEquals(2, rowCount, "Wrong row count");
            api.metrics().add(table);
        }).execute();
        api.close();

        Path metricsFile = Bench.outputDir.resolve(Bench.metricsFileName);
        assertTrue(Files.exists(metricsFile), "Missing metrics output file");
        var text = Filer.getFileText(metricsFile);
        assertEquals("""
        benchmark_name,origin,timestamp,name,value,note
        MetricsCollectionTest,deephaven-engine,1720569321551,c1.n1,2.0,test
        MetricsCollectionTest,deephaven-engine,1720569321551,c2.n2,2.0,test
        """.trim().replaceAll("[\r\n]+", "\n"), text.replaceAll(",[0-9]+,", ",1720569321551,"), "Wrong csv data");
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

    private String formatCols(Collection<String> columns) {
        var cols = new LinkedHashSet<String>(columns);
        cols.remove("RowPosition");
        cols.remove("RowKey");
        return cols.toString().replace("]", "").replace("[", "");
    }

}
