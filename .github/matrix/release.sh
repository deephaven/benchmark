# Release benchmarks: one full pass per row, so every benchmark gets the same sample count
#
# Each row is a complete pass over the standard suite and publishes its results before the next row
# starts. That matters for a run this long: iterations inside a single row would upload nothing until
# the end, and a failure part way through would lose the whole run. Rows are independent, so a failed
# row costs one pass instead of all of them.
#
# Row count is the sample count. Release ignores the Iterate tag, so a row at test_iterations 1 is a
# single job over every class, and 7 rows means n=7 for all 666 benchmarks. Nightly keeps the tag
# split for its time budget, which leaves its 562 untagged benchmarks at n=1 and 104 tagged at n=5.
#
# Prefer an odd row count. Queries sort a set's runs by rate and report the one at index n/2, along
# with that run's id, so the reported numbers always come from a run that happened either way. What
# an even count costs is symmetry: index n/2 sits above center, leaving more runs below the reported
# one than above it, which biases the rate slightly high. Measured against a 7-run set, dropping any
# one row moves the reported rate a median of +0.1%, so losing a row degrades a set rather than
# invalidating it. build-matrix.sh still rejects an even count for rows sharing a run_label, to keep
# that asymmetry from being introduced on purpose.
#
# All rows share the run_label, so they land in one set and accumulate. <version> takes the label from
# the version the engine reports, so edge becomes a 99 in the last position and cannot collide with
# the release of the same major.minor.
#
# Budget roughly 4 hours per row of test time, plus setup, engine restarts and data generation.
#
# TSV columns: run_type, run_label, docker_image, test_package, test_class_list,
#              test_iterations, scale_row_count, distribution, config_options
EXPECTED_COMBOS=7

RTYP='release'
LABEL='<version>'
IMG=${IMG:-edge}
PKG=io.deephaven.benchmark.tests.standard
CLS='*'
ITERS=1
ROWSM=10
DIST=random
OPTS='<default>'

PASSES=7

for i in $(seq ${PASSES}); do
  echo -e "$RTYP\t$LABEL\t$IMG\t$PKG\t$CLS\t$ITERS\t$ROWSM\t$DIST\t$OPTS"
done
