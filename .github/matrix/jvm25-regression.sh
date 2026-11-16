# Look for the cause of the JVM 25 regression in incremental grouped updateBy (e.g. CumSum 3 Groups inc +28%) by
# changing one JVM 25 default back at a time. Every row runs the same three regressed classes, one per operator
# family, so rows are directly comparable. The jvm25_base and jvm21_base rows are the same-night controls.
# - jvm25_lockmode: legacy stack locking (LockingMode 1), the JVM 21 default, instead of lightweight locking (2)
# - jvm25_safepoint: GuaranteedSafepointInterval 1000, the JVM 21 default, instead of 0
# - *_inlining: C2 inlining decisions for RowSetFactory.union and update graph notification processing, written to
#   the engine log, to diff between JVM 21 and 25
# TSV columns: run_type, run_label, docker_image, test_package, test_class_list,
#              test_iterations, scale_row_count, distribution, config_options
EXPECTED_COMBOS=6

RTYP='adhoc'
IMG25=ghcr.io/deephaven/server@sha256:0f0559e7fdcd4d6e73084bca8c0e81f58f896c488a386dd99e22fc5bdd1e23fe
IMG21=ghcr.io/deephaven/server@sha256:618755645bf6d9503eb224401c4262be9f6260efcaaf91bf35c42660da07470a
PKG=io.deephaven.benchmark.tests.standard
CLS='CumSum,EmMinTick,RollingAvgTick'
ROWSM=10
DIST=random
OPTS="-Xmx24g"
INLINE="-XX:+UnlockDiagnosticVMOptions -XX:CompileCommand=PrintInlining,io.deephaven.engine.rowset.RowSetFactory::union"
NOTIFY="io.deephaven.engine.updategraph.impl.PeriodicUpdateGraph*::processSatisfiedNotifications"
INLINE+=" -XX:CompileCommand=PrintInlining,$NOTIFY"

row() { echo -e "$RTYP\t$1\t$2\t$PKG\t$CLS\t$3\t$ROWSM\t$DIST\t$4"; }

row jvm25_base      $IMG25 3 "$OPTS"
row jvm25_lockmode  $IMG25 3 "$OPTS -XX:+UnlockExperimentalVMOptions -XX:LockingMode=1"
row jvm25_safepoint $IMG25 3 "$OPTS -XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=1000"
row jvm21_base      $IMG21 3 "$OPTS"
row jvm25_inlining  $IMG25 1 "$OPTS $INLINE"
row jvm21_inlining  $IMG21 1 "$OPTS $INLINE"
