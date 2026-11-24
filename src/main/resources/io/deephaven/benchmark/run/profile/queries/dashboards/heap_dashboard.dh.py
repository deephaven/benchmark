# Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending
#
# Deephaven Heap dashboard showing the lowest -Xmx heap max setting
# (derived from hN in the set_id name) per GC per benchmark.
#
# Requirements: Deephaven 41.3 or greater
#
# ruff: noqa: F821
from urllib.request import urlopen; import os
from deephaven import agg

prefix = "gc_"

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(f'{root}/deephaven-benchmark/benchmark_functions.dh.py') as r:
    exec(r.read().decode(), globals(), locals())
    storage_uri = f'{root}/deephaven-benchmark'

actor = "stanbrub"

_raw_rs, _raw_br = load_table_or_empty('result_sets', storage_uri, 'adhoc', actor, prefix)
_raw_rs = _raw_rs.where("origin = `deephaven-engine`")
_raw_be = load_table_or_empty('events', storage_uri, 'adhoc', actor, prefix)

# Max RSS (GB) per set_id + benchmark_name (each benchmark runs in its own JVM)
_rss = _raw_be.where("name = `rss_bytes`") \
    .agg_by([agg.max_("max_rss_bytes = value")], by=["set_id", "benchmark_name"]) \
    .update_view("max_rss_gb = max_rss_bytes / (1024.0 * 1024.0 * 1024.0)") \
    .drop_columns("max_rss_bytes")

# Parse GC name and heap_max from set_id, join with op_rate and max RSS
_prefix_str = actor + "/" + prefix

_sets = _raw_rs.select_distinct(["benchmark_name", "set_id", "op_rate"]) \
    .update_view([
        f"label = set_id.replaceFirst(`{_prefix_str}`, ``)",
        "gc = (String)(label.split(`_`)[0])",
        "heap_max_gb = Integer.parseInt(label.replaceAll(`.*_h(\\\\d+).*`, `$1`))",
        "mode = benchmark_name.contains(`Static`) ? `Static` : `Inc`",
        "base_benchmark = benchmark_name.replaceAll(`[- ]*(Static|Inc)$`, ``).trim()",
    ]) \
    .natural_join(_rss, on=["set_id", "benchmark_name"], joins=["max_rss_gb"])

# For each benchmark × GC × mode, keep only the row with the lowest heap_max_gb
_with_min = _sets.agg_by([agg.min_("min_h = heap_max_gb")], by=["base_benchmark", "gc", "mode"]) \
    .natural_join(_sets, on=["base_benchmark", "gc", "mode", "min_h = heap_max_gb"],
                  joins=["set_id", "op_rate", "max_rss_gb"]) \
    .rename_columns(["heap_max_gb = min_h"])

# Split into Static and Inc, then join into one row per base_benchmark × gc
_static = _with_min.where("mode = `Static`") \
    .view(["base_benchmark", "gc", "heap_max_gb", "max_rss_gb", "op_rate", "set_id"])
_inc = _with_min.where("mode = `Inc`") \
    .view(["base_benchmark", "gc",
           "inc_heap_max_gb = heap_max_gb", "inc_max_rss_gb = max_rss_gb",
           "inc_op_rate = op_rate", "inc_set_id = set_id"])

heap_summary = _static \
    .natural_join(_inc, on=["base_benchmark", "gc"],
                  joins=["inc_heap_max_gb", "inc_max_rss_gb", "inc_op_rate", "inc_set_id"]) \
    .update_view([
        "rss_gain = (max_rss_gb - heap_max_gb) / heap_max_gb * 100",
        "inc_rss_diff = (inc_max_rss_gb - max_rss_gb) / max_rss_gb * 100",
        "inc_rate_diff = (double)(inc_op_rate - op_rate) / op_rate * 100",
    ]) \
    .view(["benchmark = base_benchmark.split(`-`)[0].trim()", "gc", "heap_max_gb", "max_rss_gb", "rss_gain", "op_rate",
           "inc_rss_diff", "inc_rate_diff"]) \
    .format_columns(["rss_gain = Decimal(`0.0'%'`)", "inc_rss_diff = Decimal(`0.0'%'`)", "inc_rate_diff = Decimal(`0.0'%'`)"])


