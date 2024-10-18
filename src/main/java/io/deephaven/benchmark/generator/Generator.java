/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import java.util.concurrent.Future;
import io.deephaven.benchmark.metric.Metrics;

/**
 * A Generator is used to create record data that can be used by benchmark tests. This could range widely from
 * generating Avro records for a Kafka topic to generating Deephaven data in-place through a Barrage query. The idea is
 * to make data for a certain duration and/or number of records and stop. It is implementation-dependent, though highly
 * recommended, that multiple runs using the same configuration produce the exact same records.
 */
public interface Generator {
    /**
     * Produce to completion a set of records within the given boundaries and time delay in a thread and return a future
     * containing any <code>Metrics</code> collected.
     * 
     * @param perRecordPauseMillis the millisecond pause to add between each record sent (practically useful for only
     *        for debugging)
     * @param maxRecordCount the maximum record count to produce
     * @param maxDurationSecs the maximum seconds to produce records
     * @return a future contains metrics for the producer
     */
    public Future<Metrics> produce(int perRecordPauseMillis, long maxRecordCount, int maxDurationSecs);

    /**
     * Close any client connections used and reclaim resources.
     */
    public void close();
}
