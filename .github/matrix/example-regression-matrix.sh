# Example matrix: a commit bisect with a pinned image as an in-run reference
#
# Kept as the worked example of what a matrix can do. This one attributed a 17% UDF regression to an
# OS library update rather than any Deephaven commit; see the studies under
# 2026-09-03_regression-vs-42/udf4 for the result.
#
# Two row forms can be mixed in one matrix:
#
#   ghcr.io/deephaven/server@sha256:...   pulled as-is. Pin a digest, not a tag, to reproduce a
#                                         nightly: 'edge' has moved every day since.
#   <owner>:<ref>                         built from source on the test server, tagged per owner/ref
#
# The last two rows below request the same ref, which is the point of the per-ref tagging: the first
# builds it, the second reuses that image without a rebuild. Here they differ only in whether JFR is
# on, so the two measurements are of the same binary rather than two builds of the same commit.
#
# Row 1 here is a known-good image, giving every source row a reference measured in the same run on
# the same hardware. Without it, conclusions depend on comparing across runs, which is where several
# false results came from.
#
# JFR is on by default, with a distinct filename per row so recordings can be told apart. It needs
# docker.compose.stop.timeout above 0 in the run type's properties, or the engine is killed before
# the recording flushes and the files come out empty.
#
# config_options is per row, so a row can opt out with PLAIN. Do that when the measurement itself is
# suspect: JFR changed one UDF benchmark by 17%, enough to hide a regression, so confirm an effect
# without JFR before trusting a profiled A/B.
#
# Class lists omit the Test suffix: ConsoleLauncherUtil appends it, so 'UserFormulaTest' matches
# nothing and the run reports zero benchmarks rather than failing.
#
# TSV columns: run_label, docker_image, test_package, test_class_list,
#              test_iterations, scale_row_count, distribution, config_options
EXPECTED_COMBOS=5

PKG=io.deephaven.benchmark.tests.standard
CLASSES='UserFormula'
DIST=random
ROWSM=10
ITERS=3
PLAIN='<default>'

ANCHOR=ghcr.io/deephaven/server@sha256:2672b50205bd7ecef5d9fcc7016bd4345f75ff5333da3d77b9f05ebc50d2638b

jfr_opts() {
    echo "-Xmx24g -XX:StartFlightRecording=name=bench,filename=/data/$1-%t-%p.jfr,settings=profile,maxsize=200m"
}

TOP=deephaven:c50cd7f2fc02b819376a80e8767dff76c0624aa2

# $3 is config_options, defaulting to a JFR recording named after the row
row() { echo -e "$1\t$2\t$PKG\t$CLASSES\t$ITERS\t$ROWSM\t$DIST\t${3:-$(jfr_opts "$1")}"; }

row anchor_img_0911 "$ANCHOR"
row src_base deephaven:9fa9e83c55ccb2e9ddedd61d1ba59d2d8da68c85
row src_dh23630 deephaven:5e417cbd1bb3d8f9e3fbd9233f2034027c5319f3
row src_top "$TOP"
row src_top_nojfr "$TOP" "$PLAIN"
