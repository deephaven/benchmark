/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.metric.MetricsFuture;
import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.BarrageSessionFactory;
import io.deephaven.client.impl.BarrageSnapshot;
import io.deephaven.client.impl.BarrageSubscription;
import io.deephaven.client.impl.ConsoleSession;
import io.deephaven.client.impl.DaggerDeephavenBarrageRoot;
import io.deephaven.client.impl.FieldInfo;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.client.impl.TableHandleManager;
import io.deephaven.client.impl.script.Changes;
import io.deephaven.engine.context.ExecutionContext;
import io.deephaven.engine.table.TableUpdate;
import io.deephaven.engine.table.impl.InstrumentedTableUpdateListener;
import io.deephaven.engine.updategraph.impl.PeriodicUpdateGraph;
import io.deephaven.extensions.barrage.BarrageSnapshotOptions;
import io.deephaven.extensions.barrage.BarrageSubscriptionOptions;
import io.deephaven.extensions.barrage.table.BarrageTable;
import io.deephaven.qst.TableCreationLogic;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Client that communicates with the Deephaven Server, allows queries to be executed, and results to be retrieved. At
 * present, this connector only supports python queries.
 * <p/>
 * The typical workflow will be initialize connection, execute query, fetch results, close. Note: This class is meant to
 * be used through the Bench api rather than directly.
 */
public class BarrageConnector implements AutoCloseable {
    static {
        System.setProperty("thread.initialization", ""); // Remove server side initializers (e.g. DebuggingInitializer)
    }
    static final int maxFetchCount = 1000;
    final private BarrageSession session;
    final private ConsoleSession console;
    final private ManagedChannel channel;
    final private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    final private BufferAllocator bufferAllocator = new RootAllocator();
    final private Map<String, Subscription> subscriptions = new LinkedHashMap<>();
    final private Map<String, Snapshot> snapshots = new LinkedHashMap<>();
    final private Set<String> variableNames = new HashSet<>();
    final private AtomicBoolean isClosed = new AtomicBoolean(false);
    private Changes changes = null;

    /**
     * Construct a barrage connection and initialize it
     * 
     * @param hostPort a host and port string for connecting to a Deephaven worker (ex. localhost:10000)
     */
    public BarrageConnector(String hostPort) {
        String[] split = hostPort.split(":");
        this.channel = getManagedChannel(split[0], Integer.parseInt(split[1]));
        this.session = getSession(channel);
        try {
            this.console = session.session().console("python").get();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get console for session on host: " + hostPort);
        }
    }

    /**
     * Execute a Deephaven query which is identical to a query that would be used in the UI
     * 
     * @param query a Deephaven query
     */
    public void executeQuery(String query) {
        checkClosed();
        try {
            changes = console.executeCode(query);
            if (changes.errorMessage().isPresent())
                throw new Exception(changes.errorMessage().get());
            updateVariableNames();
        } catch (Exception ex) {
            close();
            throw new RuntimeException("Failed to executed query: " + query, ex);
        }
    }

    /**
     * Get the table names created during all queries for this session
     * 
     * @return table names
     */
    public Set<String> getUsedVariableNames() {
        return Collections.unmodifiableSet(variableNames);
    }

    /**
     * Fetch the rows of a table create or modified by this session's queries
     * 
     * @param table the name of the table to fetch data from
     * @param tableHandler a consumer used to process the result table
     * @return a future containing metrics collected during the fetch
     */
    public Future<Metrics> fetchSnapshotData(String table, Consumer<ResultTable> tableHandler) {
        checkClosed();
        Metrics metrics = new Metrics("test-runner", table, "session");
        MetricsFuture future = new MetricsFuture(metrics);
        snapshots.computeIfAbsent(table, s -> {
            try {
                BarrageSnapshotOptions options = BarrageSnapshotOptions.builder().build();
                TableHandleManager snapshotManager = session.session().batch();
                TableCreationLogic logic = findTable(table).ticket().ticketId().table().logic();
                TableHandle handle = snapshotManager.executeLogic(logic);
                BarrageSnapshot snapshot = session.snapshot(handle, options);
                BarrageTable snapshotTable = snapshot.entireTable();
                tableHandler.accept(CachedResultTable.create(snapshotTable));
                return new Snapshot(handle, snapshot);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to fetch snapshot table data: " + table, ex);
            } finally {
                future.done();
            }
        });
        return future;
    }

    /**
     * Fetch the rows of a table asynchronously through ticking updates. The table handler will be called once for every
     * ticking update and will stop only when the table handler function returns false
     * 
     * @param table the name of the table to fetch data from
     * @param tableHandler a function used to process the results of the table
     * @return a future containing metrics collected during the fetch
     */
    public Future<Metrics> fetchTickingData(String table, Function<ResultTable, Boolean> tableHandler) {
        checkClosed();
        Metrics metrics = new Metrics("test-runner", table, "session");
        MetricsFuture future = new MetricsFuture(metrics);
        subscriptions.computeIfAbsent(table, s -> {
            try {
                BarrageSubscriptionOptions options = BarrageSubscriptionOptions.builder().build();
                TableHandleManager subscriptionManager = session.session().serial();

                TableCreationLogic logic = findTable(table).ticket().ticketId().table().logic();
                TableHandle handle = subscriptionManager.executeLogic(logic);
                BarrageSubscription subscription = session.subscribe(handle, options);

                BarrageTable subscriptionTable = subscription.entireTable();
                subscriptionTable.addUpdateListener(
                        new TableListener(table, subscriptionTable, tableHandler, future, metrics));
                return new Subscription(handle, subscription);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to fetch ticking table data: " + table, ex);
            }
        });
        return future;
    }

    /**
     * Close the connector session and associated resources. Note: Because of the nature of the Deephaven Community Core
     * worker, closing the connector session does not close the session on the server.
     */
    public void close() {
        try {
            if (isClosed.get())
                return;
            isClosed.set(true);
            subscriptions.values().forEach(s -> {
                s.handle.close();
                s.subscription.close();
            });
            subscriptions.clear();
            variableNames.clear();
            console.close();
            session.close();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to close Session", ex);
        } finally {
            try {
                session.session().closeFuture().get(5, TimeUnit.SECONDS);
            } catch (Exception ex) {
            }
            scheduler.shutdownNow();
            channel.shutdownNow();
        }
    }

    private void checkClosed() {
        if (isClosed.get())
            throw new RuntimeException("Session is closed");
    }

    private FieldInfo findTable(String table) {
        Optional<FieldInfo> found =
                changes.changes().created().stream().filter(f -> f.name().equals(table)).findFirst();
        if (!found.isPresent())
            found = changes.changes().updated().stream().filter(f -> f.name().equals(table)).findFirst();
        return found.get();
    }

    private void updateVariableNames() {
        variableNames.addAll(changes.changes().created().stream().map(c -> c.name()).toList());
        variableNames.addAll(changes.changes().removed().stream().map(c -> c.name()).toList());
    }

    private ManagedChannel getManagedChannel(String host, int port) {
        final ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);
        channelBuilder.usePlaintext();
        // channelBuilder.useTransportSecurity(); If eventually security is needed

        return channelBuilder.build();
    }

    private BarrageSession getSession(ManagedChannel channel) {
        BarrageSessionFactory barrageSessionFactory = DaggerDeephavenBarrageRoot.create().factoryBuilder()
                .managedChannel(channel).scheduler(scheduler).allocator(bufferAllocator).build();

        var barrageSession = barrageSessionFactory.newBarrageSession();
        getExecutionContext().open();
        return barrageSession;
    }

    private ExecutionContext getExecutionContext() {
        final var updateGraph = PeriodicUpdateGraph.newBuilder("DEFAULT").existingOrBuild();
        return ExecutionContext.newBuilder().markSystemic().emptyQueryScope().newQueryLibrary()
                .setUpdateGraph(updateGraph).build();
    }

    record Subscription(TableHandle handle, BarrageSubscription subscription) {
    }

    record Snapshot(TableHandle handle, BarrageSnapshot snapshot) {
    }

    class TableListener extends InstrumentedTableUpdateListener {
        static final long serialVersionUID = 2589173746389448005L;
        final Function<ResultTable, Boolean> refreshHandler;
        final String tableName;
        final BarrageTable table;
        final MetricsFuture future;
        final Metrics metrics;
        final AtomicLong ticks = new AtomicLong(0);
        final long beginTime = System.currentTimeMillis();

        public TableListener(String tableName, BarrageTable table, Function<ResultTable, Boolean> refreshHandler,
                MetricsFuture future, Metrics metrics) {
            super("Table '" + tableName + "' Listener");
            this.refreshHandler = refreshHandler;
            this.tableName = tableName;
            this.table = table;
            this.future = future;
            this.metrics = metrics;
            manage(table);
        }

        @Override
        protected void onFailureInternal(final Throwable originalException, final Entry sourceEntry) {
            finish();
            throw new RuntimeException("Failed listening to table: " + tableName, originalException);
        }

        @Override
        public void onUpdate(final TableUpdate upstream) {
            ticks.incrementAndGet();
            boolean isContinued = refreshHandler.apply(CachedResultTable.create(table));
            if (isContinued)
                finish();
        }

        private void finish() {
            long duration = System.currentTimeMillis() - beginTime;
            metrics.set("duration.secs", duration / 1000.0).set("tick.count", ticks.get())
                    .set("send.rate", ticks.get() / (duration / 1000.0));
            future.done();
        }
    }

}
