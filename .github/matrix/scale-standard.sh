# Run every standard benchmark once per row, five rows in total, to check benchmark scales (for example after a
# rescale or a JVM or engine upgrade). Compare the results against a nightly that ran the same image. To compare
# against a specific nightly, replace edge with that nightly's image digest (ghcr.io/deephaven/server@sha256:...).
# Uses the nightly's heap so the results are comparable. Each row is a separate job, so each pass stays under the
# 6 hour job limit, and the shared label puts all runs in one set. Change the label for each new check.
# TSV columns: run_type, run_label, docker_image, test_package, test_class_list,
#              test_iterations, scale_row_count, distribution, config_options
EXPECTED_COMBOS=5

RTYP='adhoc'
IMG=edge
LABEL=scale_check
PKG=io.deephaven.benchmark.tests.standard
CLS='*'
ITERS=1
ROWSM=10
DIST=random
OPTS="-Xmx24g"

row() { echo -e "$RTYP\t$LABEL\t$IMG\t$PKG\t$CLS\t$ITERS\t$ROWSM\t$DIST\t$OPTS"; }

row
row
row
row
row
