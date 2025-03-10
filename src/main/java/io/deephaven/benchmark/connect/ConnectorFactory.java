/* Copyright (c) 2022-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import java.util.Properties;

/**
 * Instantiate the <code>Connector</code> for the given full-qualified class name with the given properties.
 */
public class ConnectorFactory {
    static public Connector create(String connectorClassName, Properties props) {
        try {
            var myClass = Class.forName(connectorClassName);
            var constructor = myClass.getDeclaredConstructor(Properties.class);
            return (Connector) constructor.newInstance(props);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate Connector: " + connectorClassName, ex);
        }
    }

}
