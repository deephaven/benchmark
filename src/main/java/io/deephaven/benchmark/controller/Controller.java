/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.controller;

import java.util.Collection;
import java.util.Collections;

/**
 * Represents a mechanism that can manage the external service (e.g. Deephaven Engine) the benchmarks are running
 * against. This includes; start, stop, logging, etc.
 * <p/>
 * Note: For now, this is not part of the Bench API but is used by runners that wrap the Bench API to provide normalized
 * behavior or generated data reuse.
 */
public interface Controller {
    /**
     * Start the services that match the given prefixes according to the following contract
     * <ul>
     * <li>If a service definition (e.g. docker-compose.yml) is not supplied, do nothing</li>
     * <li>If a service definition is supplied, stop the existing service and clear state (e.g. logs)</li>
     * <li>If a service definition is supplied, wait for the service to be in a usable state</li>
     * </ul>
     * 
     * @param services the service names to load or none to load all
     * @return true if the service is running, otherwise false
     */
    public boolean startService(Collection<String> services);

    /**
     * Start all services according to the following contract
     * <ul>
     * <li>If a service definition (e.g. docker-compose.yml) is not supplied, do nothing</li>
     * <li>If a service definition is supplied, stop the existing service and clear state (e.g. logs)</li>
     * <li>If a service definition is supplied, wait for the service to be in a usable state</li>
     * </ul>
     * 
     * @param services the service names to load or none to load all
     * @return true if the service is running, otherwise false
     */
    default public boolean startService() {
        return startService(Collections.emptyList());
    }

    /**
     * Stop the service according to the follow contract:
     * <ul>
     * <li>If a service definition (e.g. docker-compose.yml) is not supplied, do nothing</li>
     * <li>If a service definition is supplied, stop the service and clear state (e.g. logs)</li>
     * </ul>
     * 
     * @return true if the service definition is specified, otherwise false
     */
    public boolean stopService(Collection<String> servicePrefixes);

    /**
     * Stop all services according to the follow contract:
     * <ul>
     * <li>If a service definition (e.g. docker-compose.yml) is not supplied, do nothing</li>
     * <li>If a service definition is supplied, stop all services and clear state (e.g. logs)</li>
     * </ul>
     * 
     * @return true if the service definition is specified, otherwise false
     */
    default public boolean stopService() {
        return stopService(Collections.emptyList());
    }

    /**
     * Stop all services, cleanup state, and start services that match the given prefixes. Implementors can simply call
     * <code>stopService</code> followed by <code>startService</code> if desired.
     * 
     * @param services the service names to load or none to load all
     * @return true if the service restarted, otherwise false
     */
    public boolean restartService(Collection<String> servicePrefixes);

    /**
     * Stop all services, cleanup state, and start all services. Implementors can simply call <code>stopService</code>
     * followed by <code>startService</code> if desired.
     * 
     * @param services the service names to load or none to load all
     * @return true if the service restarted, otherwise false
     */
    default public boolean restartService() {
        return restartService(Collections.emptyList());
    }

    /**
     * Get the available log from the service. Results will vary depending on when the log state was last cleared.
     * 
     * @return the available log from the service
     */
    public String getLog();

}
