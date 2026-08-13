# Compare GC configurations across cycle times and profiles
# TSV columns: run_label, docker_image, test_package, test_class_list,
#              test_iterations, scale_row_count, distribution, config_options
EXPECTED_COMBOS=32

IMG=ghcr.io/stanbrub/server:jvm25
PKG=io.deephaven.benchmark.tests.train
CLS='*Train'
ITERS=1
ROWSM=1
DIST=random

BASE="-Xms48g -Xmx48g -XX:+UseStringDeduplication -XX:+UseCompactObjectHeaders -DServerStateTracker.reportIntervalMillis=1000"
HUGE="-XX:+AlwaysPreTouch -XX:+UseTransparentHugePages"

G1="-XX:+UseG1GC"
ZGC="-XX:-UseG1GC -XX:+UseZGC"
SHEN="-XX:-UseG1GC -XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational"
PARA="-XX:-UseG1GC -XX:+UseParallelGC"

R="$IMG\t$PKG\t$CLS\t$ITERS\t$ROWSM\t$DIST"

# emit <label> <gc> <cycle_ms> <inc_load_target> — outputs regular and _huge variants
run() {
  local opts="$2 $BASE -DPeriodicUpdateGraph.targetCycleDurationMillis=$3 -Dbench.incLoadTarget=$4"
  echo -e "$1\t$R\t$opts"
  echo -e "${1}_huge\t$R\t$opts $HUGE"
}

# 1000ms cycle
run gc_g1gc_1000_p100_j25  "$G1"   1000  1.00
run gc_zgc_1000_p100_j25   "$ZGC"  1000  1.00
run gc_shen_1000_p100_j25  "$SHEN" 1000  1.00
run gc_para_1000_p100_j25  "$PARA" 1000  1.00

# 100ms cycle, 100% load target
run gc_g1gc_100_p100_j25   "$G1"   100   1.00
run gc_zgc_100_p100_j25    "$ZGC"  100   1.00
run gc_shen_100_p100_j25   "$SHEN" 100   1.00
run gc_para_100_p100_j25   "$PARA" 100   1.00

# 100ms cycle, 90% load target
run gc_g1gc_100_p90_j25    "$G1"   100   0.90
run gc_zgc_100_p90_j25     "$ZGC"  100   0.90
run gc_shen_100_p90_j25    "$SHEN" 100   0.90
run gc_para_100_p90_j25    "$PARA" 100   0.90

# 100ms cycle, 80% load target
run gc_g1gc_100_p80_j25    "$G1"   100   0.80
run gc_zgc_100_p80_j25     "$ZGC"  100   0.80
run gc_shen_100_p80_j25    "$SHEN" 100   0.80
run gc_para_100_p80_j25    "$PARA" 100   0.80

