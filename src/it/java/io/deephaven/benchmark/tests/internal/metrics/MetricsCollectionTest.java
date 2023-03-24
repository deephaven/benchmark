package io.deephaven.benchmark.tests.internal.metrics;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;

public class MetricsCollectionTest {
    final Bench api = Bench.create(this);

    @Test
    public void collect1MetricSet() {
        var query = """
        from time import sleep
        
        bench_api_metrics_snapshot()
        sleep(0.5)
        mymetrics = bench_api_metrics_collect()
        """;

        api.query(query).fetchAfter("mymetrics", table -> {
            assertEquals("timestamp, origin, category, type, name, value, note", formatCols(table.getColumnNames()),
                    "Wrong column names");
            assertEquals(21, table.getRowCount(), "Wrong row count");
            assertEquals("ClassLoadingImpl", table.getValue(0, "category"), "Wrong bean name");
            assertEquals("TotalLoadedClassCount", table.getValue(0, "name"), "Wrong ");
            assertTrue(table.getValue(3, "value").toString()
                    .matches("init = .* used = .* committed = .* max = .*"));
        }).execute();
    }
    
    @Test
    public void collect2MetricSets() {
        var query = """
        from time import sleep
        
        bench_api_metrics_snapshot()
        sleep(0.5)
        bench_api_metrics_snapshot()
        mymetrics = bench_api_metrics_collect()
        """;

        api.query(query).fetchAfter("mymetrics", table -> {
            assertEquals("timestamp, origin, category, type, name, value, note", formatCols(table.getColumnNames()),
                    "Wrong column names");
            assertEquals(42, table.getRowCount(), "Wrong row count");
        }).execute();
    }
    
    @Test
    public void collectMetricsToFile() {
        var query = """
        from time import sleep
        
        bench_api_metrics_snapshot()
        sleep(0.5)
        mymetrics = bench_api_metrics_collect()
        """;

        api.query(query).fetchAfter("mymetrics", table -> {
            assertEquals(21, table.getRowCount(), "Wrong row count");
            api.metrics().add(table);
        }).execute();
        
        
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
