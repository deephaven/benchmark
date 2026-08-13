# Sweep heap size to find failure points per GC and test class
# TSV columns: run_label, docker_image, test_package, test_class_list,
#              test_iterations, scale_row_count, distribution, config_options

IMG=ghcr.io/stanbrub/server:jvm25
PKG=io.deephaven.benchmark.tests.train
ITERS=1
ROWSM=10
DIST=random
STEP=1

BASE="-XX:+AlwaysPreTouch -XX:+UseTransparentHugePages -XX:+UseStringDeduplication -XX:+UseCompactObjectHeaders -DServerStateTracker.reportIntervalMillis=1000"
CYCLE="-DPeriodicUpdateGraph.targetCycleDurationMillis=1000 -Dbench.incLoadTarget=1.00"

declare -A GCS=(
  [g1gc]="-XX:+UseG1GC"
  [zgc]="-XX:-UseG1GC -XX:+UseZGC"
  [shen]="-XX:-UseG1GC -XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational"
  [para]="-XX:-UseG1GC -XX:+UseParallelGC"
)

# Per-class, per-GC heap ranges: "class  gc  min  max"
COMBOS=(				# Failure Success
  "FilterTrain       g1gc   22  24"	#     20G     21G
  "FilterTrain       zgc     3   5"	#      1G      2G
  "FilterTrain       shen   18  19"	#     19G     17G
  "FilterTrain       para    7   7"	#      3G      4G
  "FormulaTrain      g1gc    2   4"	#              1G
  "FormulaTrain      zgc     2   4"	#              1G
  "FormulaTrain      shen    5   7"	#      3G      4G
  "FormulaTrain      para    2   4"	#              1G
  "NaturalJoinTrain  g1gc   18  20"	#     16G     17G
  "NaturalJoinTrain  zgc    17  19"	#     15G     16G
  "NaturalJoinTrain  shen   19  21"	#     17G     18G
  "NaturalJoinTrain  para   21  23"	#     19G     20G
  "AggByTrain        g1gc    2   4"	#              1G
  "AggByTrain        zgc     2   4"	#              1G
  "AggByTrain        shen    5   7"	#      3G      4G
  "AggByTrain        para   18  20"	#     16G     17G
  "OrderedTrain      g1gc    4   6"	#      2G      3G
  "OrderedTrain      zgc     4   6"	#      2G      3G
  "OrderedTrain      shen    4   6"	#      2G      3G
  "OrderedTrain      para    4   6"	#      2G      3G
  "UpdateByTrain     g1gc   16  18"	#     14G     15G
  "UpdateByTrain     zgc    17  19"	#     15G     16G
  "UpdateByTrain     shen   18  20"	#     16G     17G
  "UpdateByTrain     para   21  23"	#     19G     20G
)

for combo in "${COMBOS[@]}"; do
  read -r cls gc min_heap max_heap <<< "$combo"
  for h in $(seq ${max_heap} -${STEP} ${min_heap}); do
    opts="${GCS[$gc]} -Xms${h}g -Xmx${h}g $BASE $CYCLE"
    echo -e "gc_${gc}_${cls}_h${h}_j25\t$IMG\t$PKG\t*${cls}\t$ITERS\t$ROWSM\t$DIST\t$opts"
  done
done

