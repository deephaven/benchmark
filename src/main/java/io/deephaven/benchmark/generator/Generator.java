/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import java.util.Map;
import java.util.concurrent.Future;
import io.deephaven.benchmark.util.Metrics;

public interface Generator {
    public Future<Metrics> produce(int perRecordPauseMillis, long maxRecordCount, int maxDurationSecs);

    public void produce(Map<String, Object> row);

    public void close();
}
