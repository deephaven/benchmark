/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.train;

import java.util.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * A wrapper for the Bench api that allows the running of operational training (think AOT) tests without requiring the
 * boilerplate logic like imports, parquet reads, time measurement logic, etc. Each <code>test</code> runs two
 * benchmarks; one reading from a static parquet, and the other exercising ticking tables through the
 * <code>AutotuningIncrementalReleaseFilter</code>. This is different from the <code>StandardTestRunner</code> in that
 * it runs more than one operation per benchmark and attempts to cover the majority of the query code base with fewer
 * benchmarks. It is meant for training AOT and for "representative" benchmarks used to compare things like JDK/Python
 * versions and GC types.
 */
final public class TrainTestRunner {
    static final int maxRowFactor = 620;
    final Object testInst;
    final List<String> setupQueries = new ArrayList<>();
    final List<String> teardownQueries = new ArrayList<>();
    private double staticRowFactor = 1;
    private double incRowFactor = 1;
    private String[] tableNames = null;

    TrainTestRunner(Object testInst) {
        this.testInst = testInst;
    }

    public void tables(double staticRowFactor, double incRowFactor, String... names) {
        if (Math.max(staticRowFactor, incRowFactor) > maxRowFactor)
            throw new IllegalArgumentException("Row factors cannot be greater than " + maxRowFactor);
        this.staticRowFactor = staticRowFactor;
        this.incRowFactor = incRowFactor;
        tableNames = names;
    }

    public void addSetupQuery(String query) {
        setupQueries.add(query);
    }

    public void test(String name, long maxExpectedRowCount, String operation, String... loadColumns) {
        if (staticRowFactor <= 0 && incRowFactor <= 0)
            throw new IllegalStateException("At least one of staticRowFactor or incRowFactor must be > 0");

        setupQueries.add(startJfrQuery);
        teardownQueries.add(stopJfrQuery);

        if (staticRowFactor > 0)
            test(name, maxExpectedRowCount, operation, staticRowFactor, true, loadColumns);

        if (incRowFactor > 0) {
            setupQueries.add(startUgpQuery);
            teardownQueries.add(stopUgpQuery);
            operation += "\ntrain_ugp_listener = listen(result, train_ugp_update)";
            test(name, maxExpectedRowCount, operation, incRowFactor, false, loadColumns);
        }
    }

    void test(String name, long maxExpectedRowCount, String operation, double rowFactor, boolean isStatic,
            String... loadColumns) {
        var delegate = new StandardTestRunner(testInst);
        var baseRowCount = delegate.getMainTableRowCount();
        delegate.useCachedSource(false);
        delegate.useLocalParquet(true);
        delegate.setMaxRowFactor(maxRowFactor);
        delegate.setScaleFactors(isStatic ? maxRowFactor : 0, isStatic ? 0 : maxRowFactor);
        delegate.tables(tableNames);

        var headQuery = """
        ${mainTable} = ${mainTable}.head(${trainRowCount})
        loaded_tbl_size = ${mainTable}.size
        """.replace("${trainRowCount}", String.valueOf((long) (baseRowCount * rowFactor)));

        delegate.addSetupQuery(headQuery);
        setupQueries.forEach(delegate::addSetupQuery);
        teardownQueries.forEach(delegate::addTeardownQuery);
        delegate.addPreOpQuery(startProcQuery);
        delegate.addTeardownQuery(stopProcQuery);
        delegate.test(name, maxExpectedRowCount, operation, loadColumns);
    }

    static final String startJfrQuery = """
        import jpy
        Recording = jpy.get_type("jdk.jfr.Recording")
        rec = Recording()
        rec.setName("benchmark")
        
        enabled_events=['jdk.GarbageCollection','jdk.GCHeapSummary']
        for n in enabled_events:
            try:
                rec.enable(n)
            except Exception:
                print(f"Event Not Enabled: {n}")

        disabled_events=['jdk.GCPhaseConcurrent','jdk.GCPhaseConcurrentMark','jdk.GCPhaseConcurrentEvacuation',
            'jdk.G1GarbageCollection','jdk.ShenandoahGarbageCollection','jdk.ZGarbageCollection',
            'jdk.GCReferenceStatistics','jdk.GCWorkerData','jdk.GCCPUTime']
        for n in disabled_events:
            try:
                rec.disable(n)
            except Exception:
                print(f"Event Not Disabled: {n}")

        rec.start()
        """;

    static final String stopJfrQuery = """
        Paths = jpy.get_type("java.nio.file.Paths")
        RecordingFile = jpy.get_type("jdk.jfr.consumer.RecordingFile")

        rec.dump(Paths.get("/data/benchmark.jfr"))
        rec.stop()
        rec.close()
        
        events = RecordingFile.readAllEvents(Paths.get("/data/benchmark.jfr"))
        jfr_rows = []

        def getEventValue(ev, field):
            try:
                return ev.getValue(field)
            except Exception:
                return None
                
        def getNanoValue(ev, duration_field):
            val = ev.getValue(duration_field)
            if val is None or str(val) == "null": return 0
            if isinstance(val, int): return val
            if hasattr(val, "size") and hasattr(val, "get"):
                total = 0
                for i in range(val.size()):
                    d = val.get(i)
                    if d is not None and str(d) != "null": total += d.toNanos()
                return total
            if hasattr(val, "toNanos"): return val.toNanos()
            raise TypeError(f"Unsupported JFR value type: {type(val)}")

        heap_after_gc = {}
        for i in range(events.size()):
            e = events.get(i)
            if e.getEventType().getName() == 'jdk.GCHeapSummary':
                if str(getEventValue(e, 'when')) == 'After GC':
                    heap_after_gc[int(getEventValue(e, 'gcId'))] = float(getEventValue(e, 'heapUsed'))

        for i in range(events.size()):
            e = events.get(i)
            if e.getEventType().getName() != 'jdk.GarbageCollection': continue
            start = e.getStartTime().getEpochSecond() * 1000000000 + e.getStartTime().getNano()
            duration = getNanoValue(e, 'duration')
            gc_id = int(getEventValue(e, 'gcId'))
            name = 'heapUsed'
            value = heap_after_gc.get(gc_id, -1.0)
            jfr_rows.append(['jdk.GarbageCollection', start, duration, name, value])

        if len(jfr_rows) > 0:
            jfr_gc = new_table([
                string_col("origin", ["deephaven-engine" for r in jfr_rows]),
                string_col("type", [r[0] for r in jfr_rows]),
                long_col("start_ns", [r[1] for r in jfr_rows]),
                long_col("duration_ns", [r[2] for r in jfr_rows]),
                string_col("name", [r[3] for r in jfr_rows]),
                double_col("value", [r[4] for r in jfr_rows]),
            ])
            standard_events = merge([standard_events, jfr_gc])
        """;

    static final String startProcQuery = """
        import time, os, threading

        train_page_size = os.sysconf('SC_PAGE_SIZE')
        train_clk_tck = os.sysconf('SC_CLK_TCK')
        train_proc_wall_epoch_ns = time.time_ns()
        train_proc_stat_f = open('/proc/self/stat')
        train_proc_times = [(time.perf_counter_ns(), 0, 0)]
        train_proc_stop = threading.Event()

        def train_proc_loop():
            while not train_proc_stop.wait(1.0):
                train_proc_stat_f.seek(0)
                parts = train_proc_stat_f.read().split()
                train_proc_times.append((time.perf_counter_ns(),
                    int(parts[23]) * train_page_size,
                    (int(parts[13]) + int(parts[14])) * (1000000000 // train_clk_tck)))

        train_proc_thread = threading.Thread(target=train_proc_loop, daemon=True)
        train_proc_thread.start()
        """;

    static final String stopProcQuery = """
        train_proc_stop.set()
        train_proc_thread.join(timeout=5)
        if len(train_proc_times) > 1:
            mono_start = train_proc_times[0][0]
            proc_rows = []
            for i in range(1, len(train_proc_times)):
                mono_curr = train_proc_times[i][0]
                rss_bytes = train_proc_times[i][1]
                cpu_ns = train_proc_times[i][2]
                wall_clock_ns = train_proc_wall_epoch_ns + (mono_curr - mono_start)
                proc_rows.append([wall_clock_ns, rss_bytes, cpu_ns])
        
            rss_events = new_table([
                string_col("origin", ["deephaven-engine"] * len(proc_rows)),
                string_col("type", ["proc.rss"] * len(proc_rows)),
                long_col("start_ns", [r[0] for r in proc_rows]),
                long_col("duration_ns", [0] * len(proc_rows)),
                string_col("name", ["rss_bytes"] * len(proc_rows)),
                double_col("value", [float(r[1]) for r in proc_rows]),
            ])
        
            cpu_events = new_table([
                string_col("origin", ["deephaven-engine"] * len(proc_rows)),
                string_col("type", ["proc.cpu"] * len(proc_rows)),
                long_col("start_ns", [r[0] for r in proc_rows]),
                long_col("duration_ns", [r[2] for r in proc_rows]),
                string_col("name", ["cpu_ns"] * len(proc_rows)),
                double_col("value", [float(r[2]) for r in proc_rows]),
            ])
        
            standard_events = merge([standard_events, rss_events, cpu_events])
        """;

    static final String startUgpQuery = """
        from deephaven.table_listener import listen
        train_wall_epoch_ns = time.time_ns()
        if 'train_ugp_listener' in globals(): train_ugp_listener.stop()
        train_ugp_times = [(time.perf_counter_ns(), 0, 0)]

        def train_ugp_update(update, is_replay):
            ug = update.table.update_graph.j_update_graph
            ugp_cycle_cost = max(0, System.nanoTime() - ug.cycleStartNanoTime())
            train_ugp_times.append((time.perf_counter_ns(), ${mainTable}.size, ugp_cycle_cost))
        """;

    static final String stopUgpQuery = """
        if 'train_ugp_listener' in globals(): train_ugp_listener.stop()
        if len(train_ugp_times) > 1:
            mono_start = train_ugp_times[0][0]
            ugp_rows = []
            for i in range(1, len(train_ugp_times)):
                mono_curr = train_ugp_times[i][0]
                size_prev = train_ugp_times[i - 1][1]
                size_curr = train_ugp_times[i][1]
                ugp_cycle_cost = train_ugp_times[i][2]
                wall_clock_ns = train_wall_epoch_ns + (mono_curr - mono_start)
                delta_rows = max(0, size_curr - size_prev)
                ugp_rows.append([wall_clock_ns, delta_rows, ugp_cycle_cost])
        
            ugp_cycle_events = new_table([
                string_col("origin", ["deephaven-engine"] * len(ugp_rows)),
                string_col("type", ["ugp.cycle.cost"] * len(ugp_rows)),
                long_col("start_ns", [r[0] for r in ugp_rows]),
                long_col("duration_ns", [r[2] for r in ugp_rows]),
                string_col("name", ["duration_rows"] * len(ugp_rows)),
                double_col("value", [float(r[1]) for r in ugp_rows]),
            ])
        
            standard_events = merge([standard_events, ugp_cycle_events])
        """;
}
