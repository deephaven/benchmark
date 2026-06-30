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
    static final float incCycleFactor = 0.90f;
    static final double incReleaseFactor = 1.0f;
    final Object testInst;
    final List<String> setupQueries = new ArrayList<>();
    final List<String> teardownQueries = new ArrayList<>();
    private double staticRowFactor = 1;
    private double incRowFactor = 1;
    private long incReleaseRowCount = 0;
    private String[] tableNames = null;

    TrainTestRunner(Object testInst) {
        this.testInst = testInst;
    }

    public void tables(double staticRowFactor, double incRowFactor, String... names) {
        if (Math.max(staticRowFactor, incRowFactor) > maxRowFactor)
            throw new IllegalArgumentException("Row factors cannot be greater than " + maxRowFactor);
        this.staticRowFactor = 0;  // staticRowFactor;
        this.incRowFactor = incRowFactor;
        tableNames = names;
    }

    public void addSetupQuery(String query) {
        setupQueries.add(query);
    }

    public void setIncReleaseRowCount(long incReleaseRowCount) {
        this.incReleaseRowCount = incReleaseRowCount;
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
        var baseRowCount = delegate.getGeneratedRowCount();
        delegate.useCachedSource(false);
        delegate.useLocalParquet(true);
        delegate.setRowFactor(maxRowFactor);
        delegate.tables(tableNames);
        delegate.setScaleFactors(isStatic ? 1 : 0, isStatic ? 0 : 1);
        delegate.setIncReleaseFilter(incCycleFactor, (long) (incReleaseRowCount * incReleaseFactor));

        var headQuery = """
        ${mainTable} = ${mainTable}.head(${trainRowCount})
        loaded_tbl_size = ${mainTable}.size
        """.replace("${trainRowCount}", String.valueOf((long) (baseRowCount * rowFactor * incReleaseFactor)));

        delegate.addSetupQuery(headQuery);
        setupQueries.forEach(delegate::addSetupQuery);
        teardownQueries.forEach(delegate::addTeardownQuery);
        delegate.test(name, maxExpectedRowCount, operation, loadColumns);
    }

    static final String startJfrQuery = """
        import jpy
        Recording = jpy.get_type("jdk.jfr.Recording")
        rec = Recording()
        rec.setName("benchmark")
        
        enabled_events=['jdk.ExecutionSample','jdk.NativeMethodSample','jdk.ThreadCPULoad','jdk.GarbageCollection',
            'jdk.GCPhasePause','jdk.SafepointBegin','jdk.SafepointEnd','jdk.SafepointState',
            'jdk.ObjectAllocationInNewTLAB','jdk.ObjectAllocationOutsideTLAB']
        for n in enabled_events:
            try:
                rec.enable(n)
            except Exception:
                print(f"Event Not Enabled: {n}")

        disabled_events=['jdk.GCPhaseConcurrent','jdk.GCPhaseConcurrentMark','jdk.GCPhaseConcurrentEvacuation',
            'jdk.G1GarbageCollection','jdk.ShenandoahGarbageCollection','jdk.ZGarbageCollection','jdk.GCHeapSummary',
            'jdk.GCReferenceStatistics','jdk.GCWorkerData','jdk.GCCPUTime','jdk.GCPhasePause']
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

        for i in range(events.size()):
            e = events.get(i)
            etype = e.getEventType().getName()
            start = e.getStartTime().getEpochSecond() * 1000000000 + e.getStartTime().getNano()

            if etype == 'jdk.GarbageCollection':
                duration = getNanoValue(e, 'duration')
                name = 'sumOfPauses'
                value = getNanoValue(e, 'sumOfPauses')
            elif etype == 'jdk.GCPhasePause':
                duration = getNanoValue(e, 'duration')
                name = getEventValue(e, 'name')
                value = duration
            else:
                continue

            jfr_rows.append([etype, start, duration, name, value])

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

    static final String startUgpQuery = """
        from deephaven import time_table, perfmon
        from deephaven.table_listener import listen
        import time

        ss_log = perfmon.server_state_log()
        if 'train_ugp_listener' in globals(): train_ugp_listener.stop()
        train_wall_epoch_ns = time.time_ns()
        train_ugp_times = [(time.perf_counter_ns(), 0, 0)]

        def train_ugp_update(update, is_replay):
            ugp_cycle_cost = 0
            if autotune:
                ug = update.table.update_graph.j_update_graph
                ugp_cycle_cost = System.nanoTime() - ug.cycleStartNanoTime()
            train_ugp_times.append((time.perf_counter_ns(), ${mainTable}.size, ugp_cycle_cost))
        """;

    static final String stopUgpQuery = """
        if 'train_ugp_listener' in globals(): train_ugp_listener.stop()
        if len(train_ugp_times) > 1:
            mono_start = train_ugp_times[0][0]
            ugp_rows = []
            for i in range(1, len(train_ugp_times)):
                mono_prev = train_ugp_times[i - 1][0]
                mono_curr = train_ugp_times[i][0]
                size_prev = train_ugp_times[i - 1][1]
                size_curr = train_ugp_times[i][1]
                ugp_cycle_cost = train_ugp_times[i][2]
                delta_ns = mono_curr - mono_prev
                wall_clock_ns = train_wall_epoch_ns + (mono_curr - mono_start)
                delta_rows = max(0, size_curr - size_prev)
                ugp_cycle_cost = max(0, ugp_cycle_cost)
                ugp_rows.append([wall_clock_ns, delta_ns, delta_rows, ugp_cycle_cost])
        
            ugp_events = new_table([
                string_col("origin", ["deephaven-engine"] * len(ugp_rows)),
                string_col("type", ["ugp.delta"] * len(ugp_rows)),
                long_col("start_ns", [r[0] for r in ugp_rows]),
                long_col("duration_ns", [r[1] for r in ugp_rows]),
                string_col("name", ["duration_rows"] * len(ugp_rows)),
                double_col("value", [float(r[2]) for r in ugp_rows]),
            ])
            
            ugp_cycle_events = new_table([
                string_col("origin", ["deephaven-engine"] * len(ugp_rows)),
                string_col("type", ["ugp.cycle.cost"] * len(ugp_rows)),
                long_col("start_ns", [r[0] for r in ugp_rows]),
                long_col("duration_ns", [r[3] for r in ugp_rows]),
                string_col("name", ["duration_rows"] * len(ugp_rows)),
                double_col("value", [float(r[2]) for r in ugp_rows]),
            ])
        
            standard_events = merge([standard_events, ugp_events, ugp_cycle_events])
        
        ss_log = perfmon.server_state_log().ungroup(["IntervalUGPCyclesTimeMicros"]).snapshot()
        if ss_log.size > 0:
            ss_rows = []
            for row in ss_log.iter_dict():
                start = row['IntervalStartTime'].getEpochSecond() * 1000000000 + row['IntervalStartTime'].getNano()
                ss_rows.append((start, row['IntervalCollectionTimeMicros'] * 1000, 
                row['IntervalUGPCyclesTimeMicros'] * 1000))

            ss_events = new_table([
                string_col("origin", ["deephaven-engine"] * len(ss_rows)),
                string_col("type", ["server_state_log"] * len(ss_rows)),
                long_col("start_ns", [r[0] for r in ss_rows]),
                long_col("duration_ns", [r[1] for r in ss_rows]),
                string_col("name", ["ugp.cycle.time"] * len(ss_rows)),
                double_col("value", [r[2] for r in ss_rows]),
            ])
            standard_events = merge([standard_events, ss_events])
        """;
}
