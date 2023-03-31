/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import java.util.concurrent.Future;
import io.deephaven.benchmark.metric.Metrics;

public interface Generator {
    public Future<Metrics> produce(int perRecordPauseMillis, long maxRecordCount, int maxDurationSecs);

    public void close();
}
