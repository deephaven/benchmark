# Run every standard benchmark three times (one pass per row) with the rescaled factors on JVM 25, for comparison
# against the first JVM 25 nightly (2026-11-13, run-Vapbns1, Temurin 25.0.5), which ran this same image. Uses the
# nightly's heap so the results are comparable. Each row is a separate job, so each pass stays under the 6 hour job
# limit, and the shared label puts all runs in one set.
# TSV columns: run_type, run_label, docker_image, test_package, test_class_list,
#              test_iterations, scale_row_count, distribution, config_options
EXPECTED_COMBOS=3

RTYP='adhoc'
IMG=ghcr.io/deephaven/server@sha256:0f0559e7fdcd4d6e73084bca8c0e81f58f896c488a386dd99e22fc5bdd1e23fe
PKG=io.deephaven.benchmark.tests.standard
CLS='*'
ITERS=1
ROWSM=10
DIST=random
OPTS="-Xmx24g"

row() { echo -e "$RTYP\t$1\t$IMG\t$PKG\t$CLS\t$ITERS\t$ROWSM\t$DIST\t$OPTS"; }

row scale5
row scale5
row scale5
