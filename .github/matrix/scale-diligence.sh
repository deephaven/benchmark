# Checks for the parts of the rescaled runner that the standard scale runs don't cover:
# - The training benchmarks at production size (620x of a 1M row base), with the training runner's usual heap
# - Standard benchmarks with the ascending and runlength distributions, since the nightly only uses random
# - The compare benchmarks, which share runner code but have not been run since the rescale
# Each row is a separate job, so each stays under the 6 hour job limit.
# TSV columns: run_type, run_label, docker_image, test_package, test_class_list,
#              test_iterations, scale_row_count, distribution, config_options
EXPECTED_COMBOS=4

RTYP='adhoc'
IMG=edge
PKG='io.deephaven.benchmark.tests'
DIST_CLS='Where,SortAscending,SumBy,CumSum,NaturalJoin'
TRAIN_OPTS="-Xms48g -Xmx48g -XX:+UseG1GC"
OPTS="-Xmx24g"

row() { echo -e "$RTYP\t$1\t$IMG\t$PKG.$2\t$3\t1\t$4\t$5\t$6"; }

row dd_train      train    '*Train'   1  random     "$TRAIN_OPTS"
row dd_ascending  standard $DIST_CLS  10 ascending  "$OPTS"
row dd_runlength  standard $DIST_CLS  10 runlength  "$OPTS"
row dd_compare    compare  '*'        10 random     "$OPTS"
