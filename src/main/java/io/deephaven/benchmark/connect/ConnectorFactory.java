/* Copyright (c) 2022-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

public class ConnectorFactory {
    static public Connector create(String connectorClassName, String hostPort, String userPass) {
        try {
            var myClass = Class.forName(connectorClassName);
            var constructor = myClass.getDeclaredConstructor(String.class, String.class);
            return (Connector) constructor.newInstance(hostPort, userPass);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate Connector: " + hostPort, ex);
        }
    }

}
