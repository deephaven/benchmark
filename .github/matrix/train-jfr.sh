# Check that -Dbench.jfr recordings and the Training runner's own JFR (GC events into
# benchmark-events.csv) work together. Rows differ only in -Dbench.jfr, so with JFR off, profile,
# and default, compare the jdk.GarbageCollection events, the test-logs.tgz recordings, and timings.
# TSV columns: run_type, run_label, docker_image, test_package, test_class_list,
#              test_iterations, scale_row_count, distribution, config_options
EXPECTED_COMBOS=3

RTYP='adhoc'
IMG=edge
PKG=io.deephaven.benchmark.tests.train
CLS='*Train'
ITERS=1
ROWSM=1
DIST=random

BASE="-Xms48g -Xmx48g -XX:+UseG1GC"

row() { echo -e "$RTYP\t$1\t$IMG\t$PKG\t$CLS\t$ITERS\t$ROWSM\t$DIST\t$2"; }

row train_jfr_off      "$BASE"
row train_jfr_profile  "$BASE -Dbench.jfr=profile"
row train_jfr_default  "$BASE -Dbench.jfr=default"
