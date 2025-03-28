/* Copyright (c) 2022-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import io.deephaven.benchmark.connect.Connector;

/**
 * Contains session information that is shared between queries but is managed by <code>BenchQuery</code>. Each
 * <code>Bench</code> instance allows only one reusable Session. If multiple queries are executed from the same
 * <code>Bench</code> instance, the queries will be executed against the same session.
 */
class Session {
    private Connector connector = null;

    void setConnector(Connector connector) {
        this.connector = connector;
    }

    Connector getConnector() {
        return connector;
    }

    void close() {
        if (connector != null) {
            connector.close();
            connector = null;
        }
    }

}
