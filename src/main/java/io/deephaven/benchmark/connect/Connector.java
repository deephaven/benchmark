/* Copyright (c) 2022-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import io.deephaven.benchmark.metric.Metrics;

public interface Connector extends AutoCloseable {

    public void executeQuery(String query);

    public Set<String> getUsedVariableNames();

    public Future<Metrics> fetchSnapshotData(String table, Consumer<ResultTable> tableHandler);

    public Future<Metrics> fetchTickingData(String table, Function<ResultTable, Boolean> tableHandler);

    public void close();

}
