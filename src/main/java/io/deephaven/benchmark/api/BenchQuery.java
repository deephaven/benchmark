/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import io.deephaven.benchmark.connect.BarrageConnector;
import io.deephaven.benchmark.connect.ResultTable;
import io.deephaven.benchmark.util.Metrics;
import io.deephaven.benchmark.util.Timer;

/**
 * Represents a query, its execution, and results. The query logic is executed and the resulting tables subscribed to
 * and data fetched. Table contents can be retrieved with fetchDuring (for ticking tables) and fetchAfter (for
 * snapshots) depending on what type of tables the query initiates.
 */
final public class BenchQuery implements Closeable {
    final Bench bench;
    final String logic;
    final QueryLog queryLog;
    final Map<String, Consumer<ResultTable>> snapshotFetchers = new LinkedHashMap<>();
    final Map<String, Function<ResultTable, Boolean>> tickingFetchers = new LinkedHashMap<>();
    private BarrageConnector session = null;

    BenchQuery(Bench bench, String logic, QueryLog queryLog) {
        this.bench = bench;
        this.logic = logic;
        this.queryLog = queryLog;
    }

    /**
     * Subscribes a handler to receive table data after execution of the query logic is completed. (Note: This will not
     * work on ticking tables)
     * 
     * @param table a table name present in the query logic
     * @param tableHandler a consumer that receives a non-live snapshot of the table
     * @return a query configuration instance
     */
    public BenchQuery fetchAfter(String table, Consumer<ResultTable> tableHandler) {
        snapshotFetchers.put(table, tableHandler);
        return this;
    }

    /**
     * Subscribes a handler to receive table data during execution of the query logic. The update interval is defined by
     * the session. On each interval, the handler must return true (continue) or false (end/unsubscribe)
     * 
     * @param table a table name present in the query logic
     * @param tableHandler a function that receives non-live snapshot of the table
     * @return a query configuration instance
     */
    public BenchQuery fetchDuring(String table, Function<ResultTable, Boolean> tableHandler) {
        tickingFetchers.put(table, tableHandler);
        return this;
    }

    /**
     * Execute the query logic through a session
     */
    public void execute() {
        var timer = Timer.start();
        executeBarrageQuery(logic);
        tickingFetchers.entrySet().forEach(e -> bench.addFuture(session.fetchTickingData(e.getKey(), e.getValue())));

        snapshotFetchers.entrySet().forEach(e -> {
            try {
                Future<Metrics> f = session.fetchSnapshotData(e.getKey(), e.getValue());
                Metrics metrics = f.get();
                metrics.set("duration.secs", timer.duration().toMillis() / 1000.0);
                bench.addFuture(f);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to get snapshot of table: " + e.getKey(), ex);
            }
        });
    }

    /**
     * Unsubscribe any fetchers, free used variables, and close the session
     */
    public void close() {
        if (session == null)
            return;

        if (!session.getUsedVariableNames().isEmpty()) {
            String logic = String.join("=None; ", session.getUsedVariableNames()) + "=None\n";
            logic += "System = jpy.get_type('java.lang.System'); System.gc()\n";
            executeBarrageQuery(logic);
        }

        session.close();
        session = null;
    }

    // Add function defs in separate query so if there are errors in the "logic" part, the line numbers match up
    private void executeBarrageQuery(String logic) {
        if (session == null) {
            String deephavenServer = bench.property("deephaven.addr", "localhost:10000");
            session = new BarrageConnector(deephavenServer);
        }
        String snippetsLogic = Bench.profile.replaceProperties(Snippets.getFunctions(logic));
        if (!snippetsLogic.isBlank()) {
            queryLog.logQuery(snippetsLogic);
            session.executeQuery(snippetsLogic);
        }
        String userLogic = Bench.profile.replaceProperties(logic);
        session.executeQuery(userLogic);
        queryLog.logQuery(userLogic);
    }

}
