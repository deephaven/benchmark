# Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending
#
# Deephaven Train dashboard for visualizing benchmark data captured with the training benchmarks.
# The dashboard shows the throughput, jitter, and cycle budget metrics for different sets of JVM options.
#
# Requirements: Deephaven 41.3 or greater
#
# ruff: noqa: F821
from urllib.request import urlopen; import os
from deephaven import ui
from deephaven.ui import use_memo, use_state, use_ref
from deephaven.execution_context import get_exec_ctx
from deephaven import pandas as dhpd
import deephaven.plot.express as dx
import numpy as np

prefix = "gc_"

COLORS = [
    "#636EFA", "#EF553B", "#00CC96", "#AB63FA",
    "#FFA15A", "#19D3F3", "#FF6692", "#B6E880",
    "#FF97FF", "#FECB52",
]

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(f'{root}/deephaven-benchmark/benchmark_functions2.dh.py') as r:
    exec(r.read().decode(), globals(), locals())
    storage_uri = f'{root}/deephaven-benchmark'

# These globals are set by the dashboard component on each render
actor = "gc-fun"
static_inc_filter = 'Inc'
benchmark_filter = "*"
cycle_budget_ms = 100
autotune_pct = '100'
jvm_version = '25'
setid_filter = f'*_{cycle_budget_ms}_p{autotune_pct}_j{jvm_version}*'
bench_result_sets = None
bench_results = None
bench_metrics = None
bench_events = None
gc_metrics = None

# Load raw tables once at module level (just table handles; data stays on server)
_raw_rs, _raw_br = load_table_or_empty('result_sets', storage_uri, 'demo', actor, prefix)
_raw_rs = _raw_rs.where("origin = `deephaven-engine`")
_raw_br = _raw_br.where("origin = `deephaven-engine`")
_raw_bm = load_table_or_empty('metrics', storage_uri, 'demo', actor, prefix)
_raw_be = load_table_or_empty('events', storage_uri, 'demo', actor, prefix)

def filter_benchmarks(table):
    if not benchmark_filter: return table
    benchmark_regex = "|".join(p.strip().replace("*", ".*") for p in benchmark_filter.split(","))
    setid_regex = "|".join(p.strip().replace("*", ".*") for p in setid_filter.split(","))
    filters = [
        f"benchmark_name.matches(`{benchmark_regex}`)",
        f"set_id.matches(`{setid_regex}`)",
    ]
    if static_inc_filter:
        type_regex = "|".join(f".*{t.strip()}.*" for t in static_inc_filter.split(","))
        filters.append(f"benchmark_name.matches(`{type_regex}`)")
    return table.where(filters)

def get_metric_table(metric_name, col_type, metric_table=gc_metrics):
    setids = metric_table.select_distinct(['set_id'])
    benchmarks = metric_table.select_distinct(['benchmark_name'])
    names = metric_table.select_distinct(['name'])
    all_combos = benchmarks.join(setids).join(names)
    base = all_combos.natural_join(
        metric_table.view(['benchmark_name','set_id','name','value']),
        ['benchmark_name','set_id','name']
    )
    tbl = base.group_by(['benchmark_name','name'])
    for idx, (setid,) in enumerate(setids.iter_tuple()):
        setid = setid.replace(actor + '/' + prefix,'')
        tbl = tbl.update(f"{setid}=({col_type})value[{idx}]")
    return tbl.where([f"name=`{metric_name}`"]).drop_columns(['name','set_id','value']) \
        .sort(['benchmark_name'])

def get_event_time_table(event_type, metric_name, col_type, agg, value_col='value'):
    gc_events = bench_result_sets.join(bench_events, ['benchmark_name','origin','set_id'],
        ['type','start','duration','name','value']).sort(['set_id'])
    tbl = gc_events.where([f"type=`{event_type}`"]).group_by(['benchmark_name','set_id','name']) \
        .update([f"value={agg}({value_col})/1_000_000_000"])
    return get_metric_table(metric_name, col_type, tbl)

def get_throughput_table():
    setids = bench_result_sets.select_distinct(['set_id'])
    benchmarks = bench_result_sets.select_distinct(['benchmark_name'])
    all_combos = benchmarks.join(setids)
    base = all_combos.natural_join(
        bench_result_sets.view(['benchmark_name','set_id','op_rate','op_duration']),
        ['benchmark_name','set_id']
    )
    tbl = base.group_by(['benchmark_name']) \
        .update(['max=max(op_rate)','min=min(op_rate)']) \
        .ungroup(['set_id','op_rate','op_duration']) \
        .update(['op_factor=op_rate/min']) \
        .group_by(['benchmark_name']) \
        .update(['max=max(op_factor)','min=min(op_factor)'])
    for idx, (setid,) in enumerate(setids.iter_tuple()):
        setid_clean = setid.replace(actor + '/' + prefix,'')
        tbl = tbl.update(f"{setid_clean}=op_factor[{idx}]") \
            .format_columns(f"{setid_clean} = {setid_clean}=max ? DEEP_GREEN : {setid_clean}=min ? DEEP_RED : NO_FORMATTING")
    return tbl.drop_columns(['set_id','op_rate','max','min','op_duration','op_factor']).sort(['benchmark_name'])

def get_jitter_table():
    gc_events = bench_result_sets.join(bench_events, ['benchmark_name','origin','set_id'],
        ['type','start','duration','name','value']).sort(['set_id'])
    jitter = gc_events.where(["type=`ugp.cycle.cost`"]) \
        .group_by(['benchmark_name','set_id']) \
        .update(['jitter=sqrt(avg(duration * duration - avg(duration) * avg(duration)))/avg(duration)'])
    setids = bench_result_sets.select_distinct(['set_id'])
    benchmarks = jitter.select_distinct(['benchmark_name'])
    all_combos = benchmarks.join(setids)
    base = all_combos.natural_join(
        jitter.view(['benchmark_name','set_id','jitter']),
        ['benchmark_name','set_id']
    )
    tbl = base.group_by(['benchmark_name']) \
        .update(['max=max(jitter)','min=min(jitter)']) .ungroup(['set_id','jitter']) \
        .update(['jitter_factor=jitter/min']).group_by(['benchmark_name']) \
        .update(['max=max(jitter_factor)','min=min(jitter_factor)'])
    for idx, (setid,) in enumerate(setids.iter_tuple()):
        setid = setid.replace(actor + '/' + prefix, '')
        tbl = tbl.update(f"{setid}=(double)jitter_factor[{idx}]") \
            .format_columns(f"{setid} = {setid}=max ? DEEP_RED : {setid}=min ? DEEP_GREEN : NO_FORMATTING")
    return tbl.drop_columns(['set_id','jitter','jitter_factor','max','min']).sort(['benchmark_name'])

def get_cycle_onbudget_table():
    budget_nanos = cycle_budget_ms * 1_000_000
    gc_events = bench_result_sets.join(bench_events, ['benchmark_name','origin','set_id'],
        ['type','start','duration','name','value']).sort(['set_id'])
    cycle_events = gc_events.where(["type=`ugp.cycle.cost`"])
    total = cycle_events.count_by('total', ['benchmark_name','set_id'])
    under = cycle_events.where(f"duration <= {budget_nanos}L").count_by('under', ['benchmark_name','set_id'])
    cycles = total.natural_join(under, ['benchmark_name','set_id'], ['under']) \
        .update(['under = isNull(under) ? 0L : under', 'pct_under_budget = 100.0 * under / total'])
    setids = bench_result_sets.select_distinct(['set_id'])
    benchmarks = cycles.select_distinct(['benchmark_name'])
    all_combos = benchmarks.join(setids)
    base = all_combos.natural_join(
        cycles.view(['benchmark_name','set_id','pct_under_budget']),
        ['benchmark_name','set_id']
    )
    tbl = base.group_by(['benchmark_name']) \
        .update(['max=max(pct_under_budget)','min=min(pct_under_budget)']) \
        .ungroup(['set_id','pct_under_budget']) \
        .group_by(['benchmark_name']) \
        .update(['max=max(pct_under_budget)','min=min(pct_under_budget)'])
    for idx, (setid,) in enumerate(setids.iter_tuple()):
        setid = setid.replace(actor + '/' + prefix, '')
        tbl = tbl.update(f"{setid}=(double)pct_under_budget[{idx}]") \
            .format_columns(f"{setid} = {setid}==max ? DEEP_GREEN : {setid}==min ? DEEP_RED : NO_FORMATTING")
    return tbl.drop_columns(['set_id','pct_under_budget','max','min']).sort(['benchmark_name'])

def get_throughput_hilow_table():
    return bench_results.view(['benchmark_name','set_id','op_rate']).group_by(['benchmark_name','set_id']) \
        .update(['E=1.96*(std(op_rate)/sqrt(count(op_rate)))', 'M=avg(op_rate)', 'L=max(0,M-E)', 'H=M+E',
            'set_id=set_id.replace(actor + "/" + prefix,"")'])

def add_summary_rows(tbl, higher_is_better=True, use_geo_mean=True):
    from deephaven import pandas as dhpd
    import pandas as pd

    visible_cols = [col.name for col in tbl.columns if '__TABLE_STYLE_FORMAT' not in col.name]
    df = dhpd.to_pandas(tbl.view(visible_cols))
    gc_cols = [c for c in visible_cols if c != 'benchmark_name']

    row = {'benchmark_name': 'GEO MEAN' if use_geo_mean else 'MEAN'}
    for gc in gc_cols:
        vals = df[gc].dropna().values.astype(float)
        if use_geo_mean:
            vals = vals[vals > 0]
            row[gc] = float(np.exp(np.log(vals).mean())) if len(vals) > 0 else float('nan')
        else:
            row[gc] = float(vals.mean()) if len(vals) > 0 else float('nan')

    full_df = pd.concat([df, pd.DataFrame([row])], ignore_index=True)
    result = dhpd.to_table(full_df)

    max_expr = 'max(' + ','.join(gc_cols) + ')'
    min_expr = 'min(' + ','.join(gc_cols) + ')'
    result = result.update([f'max_val={max_expr}', f'min_val={min_expr}'])
    for gc in gc_cols:
        if higher_is_better:
            result = result.format_columns(f"{gc} = {gc}==max_val ? DEEP_GREEN : {gc}==min_val ? DEEP_RED : NO_FORMATTING")
        else:
            result = result.format_columns(f"{gc} = {gc}==min_val ? DEEP_GREEN : {gc}==max_val ? DEEP_RED : NO_FORMATTING")
    return result.drop_columns(['max_val', 'min_val'])

def get_summary_table():
    from deephaven.column import string_col, double_col
    from deephaven import new_table, pandas as dhpd

    tput_tbl = get_throughput_table()

    def visible_cols(tbl):
        return [c.name for c in tbl.columns if c.name != 'benchmark_name' and '__TABLE_STYLE_FORMAT' not in c.name]

    def geo_mean(df, col):
        vals = df[col].dropna().values.astype(float)
        vals = vals[vals > 0]
        return float(np.exp(np.log(vals).mean())) if len(vals) > 0 else float('nan')

    tput_cols = visible_cols(tput_tbl)
    tput_df = dhpd.to_pandas(tput_tbl.view(['benchmark_name'] + tput_cols))

    columns = [
        string_col("gc_type", tput_cols),
        double_col("throughput_factor", [geo_mean(tput_df, gc) for gc in tput_cols]),
    ]

    import math
    try:
        jit_tbl = get_jitter_table()
        jit_cols = visible_cols(jit_tbl)
        jit_df = dhpd.to_pandas(jit_tbl.view(['benchmark_name'] + jit_cols))
        jit_vals = [geo_mean(jit_df, gc) for gc in jit_cols]
        if not all(math.isnan(v) for v in jit_vals):
            columns.append(double_col("jitter_factor", jit_vals))
    except Exception:
        pass

    try:
        cc_tbl = get_cycle_onbudget_table()
        cc_cols = visible_cols(cc_tbl)
        cc_df = dhpd.to_pandas(cc_tbl.view(['benchmark_name'] + cc_cols))
        budget_col = f"pct_under_{cycle_budget_ms}ms"
        cc_vals = [float(cc_df[gc].dropna().mean()) for gc in cc_cols]
        if not all(math.isnan(v) for v in cc_vals):
            columns.append(double_col(budget_col, cc_vals))
    except Exception:
        pass

    return new_table(columns)

_ctx = get_exec_ctx()

def _compute_rank(t):
    with _ctx:
        return t.update_view(["Rank = ii + 1"])


@ui.component
def gc_bench_summary_dashboard():
    # --- Filter state ---
    static_inc_val, set_static_inc = use_state("Inc")
    cycle_budget_str, set_cycle_budget = use_state("100")
    autotune_val, set_autotune = use_state("100")
    jvm_val, set_jvm = use_state("25")

    # Applied state — tables only recompute when Apply is clicked
    applied, set_applied = use_state({
        "static_inc": "Inc",
        "cycle_budget": "100", "autotune": "100", "jvm": "25",
    })

    def handle_apply():
        set_applied({
            "static_inc": static_inc_val,
            "cycle_budget": cycle_budget_str, "autotune": autotune_val, "jvm": jvm_val,
        })

    # Derived from applied (committed) values
    a_cycle = applied["cycle_budget"]
    a_autotune = applied["autotune"]
    a_jvm = applied["jvm"]
    cycle_budget_ms_local = int(a_cycle) if a_cycle.isdigit() else 100
    setid_filter_local = f'*_{a_cycle}_p{a_autotune}_j{a_jvm}*'

    def compute_tables():
        global actor, static_inc_filter, benchmark_filter, cycle_budget_ms, autotune_pct, jvm_version
        global setid_filter, bench_result_sets, bench_results, bench_metrics, bench_events, gc_metrics
        static_inc_filter = applied["static_inc"]
        benchmark_filter = "*"
        cycle_budget_ms = cycle_budget_ms_local
        autotune_pct = a_autotune
        jvm_version = a_jvm
        setid_filter = setid_filter_local

        rs = _raw_rs.sort(['set_id'])
        br = _raw_br.sort(['set_id'])
        bm = _raw_bm
        be = _raw_be

        rs, br, bm, be = [filter_benchmarks(t) for t in [rs, br, bm, be]]
        bench_result_sets = rs
        bench_results = br
        bench_metrics = bm
        bench_events = be
        gc_metrics = rs.join(bm, ['benchmark_name','origin','set_id'], ['name','value']).sort(['set_id'])

        from deephaven import new_table
        from deephaven.column import string_col

        def _empty(label="No data"):
            return new_table([string_col("benchmark_name", [label])])

        # Strip filter substring and Static/Inc prefix from names
        substr = f"_{a_cycle}_p{a_autotune}_j{a_jvm}"
        inc_static = applied["static_inc"]
        def _clean_name(name):
            import re
            name = name.replace(substr, '')
            name = re.sub(rf'[\s_-]*{re.escape(inc_static)}[\s_-]*', '_', name)
            name = name.strip('_')
            return name
        def _rename(tbl):
            format_cols = [c.name for c in tbl.columns if '__TABLE_STYLE_FORMAT' in c.name]
            if format_cols:
                tbl = tbl.drop_columns(format_cols)
            renames = []
            for c in tbl.columns:
                new_name = _clean_name(c.name)
                if new_name != c.name:
                    renames.append(f"{new_name}={c.name}")
            if renames:
                tbl = tbl.rename_columns(renames)
            # Also clean gc_type and benchmark_name cell values
            col_names = [c.name for c in tbl.columns]
            import re
            def _clean_expr(col):
                return f'{col} = {col}.replace(`{substr}`, ``).replaceAll(`[\\\\s_-]*{re.escape(inc_static)}[\\\\s_-]*`, `_`).replaceAll(`^_|_$`, ``)'
            if "gc_type" in col_names:
                tbl = tbl.update_view([_clean_expr('gc_type')])
            if "benchmark_name" in col_names:
                tbl = tbl.update_view([_clean_expr('benchmark_name')])
            return tbl

        try:
            t_summary = _rename(get_summary_table())
        except Exception:
            t_summary = _empty("No data")

        try:
            t_throughput = add_summary_rows(_rename(get_throughput_table()), higher_is_better=True)
        except Exception as e:
            import traceback; traceback.print_exc()
            t_throughput = _empty("No data")

        try:
            t_jitter = add_summary_rows(_rename(get_jitter_table()), higher_is_better=False)
        except Exception as e:
            import traceback; traceback.print_exc()
            t_jitter = _empty("No data")

        try:
            t_on_budget = _rename(get_cycle_onbudget_table())
            t_on_budget = add_summary_rows(t_on_budget, higher_is_better=True, use_geo_mean=False)
            cc_cols = [col.name for col in t_on_budget.columns if col.name != 'benchmark_name' and '__TABLE_STYLE_FORMAT' not in col.name]
            t_on_budget = t_on_budget.format_columns([f"{c} = Decimal(`0.00'%'`)" for c in cc_cols])
        except Exception as e:
            import traceback; traceback.print_exc()
            t_on_budget = _empty("No data")

        return t_summary, t_throughput, t_jitter, t_on_budget

    gc_summary, gc_throughput, gc_jitter, gc_on_budget = use_memo(
        compute_tables, [applied]
    )

    filter_label = f"{applied['static_inc']} | {a_cycle}ms | p{a_autotune} | j{a_jvm}"

    controls = ui.flex(
        ui.picker(
            "Static", "Inc",
            label="Static/Inc",
            selected_key=static_inc_val,
            on_selection_change=set_static_inc,
        ),
        ui.picker(
            "100", "1000",
            label="Cycle Budget (ms)",
            selected_key=cycle_budget_str,
            on_selection_change=set_cycle_budget,
        ),
        ui.picker(
            "80", "90", "100", "110",
            label="Autotune %",
            selected_key=autotune_val,
            on_selection_change=set_autotune,
        ),
        ui.picker(
            "17", "25",
            label="JVM Version",
            selected_key=jvm_val,
            on_selection_change=set_jvm,
        ),
        ui.button("Apply", on_press=handle_apply, variant="accent"),
        direction="row",
        gap="size-200",
        align_items="end",
        flex_grow=0,
        flex_shrink=0,
    )

    is_inc = applied["static_inc"] != "Static"

    top_row = ui.flex(
        ui.flex(
            ui.heading(f"GC Summary — {filter_label}", level=4),
            ui.table(gc_summary),
            direction="column", flex_grow=1,
        ),
        ui.flex(
            ui.heading(f"GC Throughput — {filter_label}", level=4),
            ui.table(gc_throughput),
            direction="column", flex_grow=1,
        ),
        direction="row", flex_grow=1, gap="size-200",
    )

    content = [controls, top_row]

    if is_inc:
        bottom_row = ui.flex(
            ui.flex(
                ui.heading(f"GC Jitter — {filter_label}", level=4),
                ui.table(gc_jitter),
                direction="column", flex_grow=1,
            ),
            ui.flex(
                ui.heading(f"GC On Budget — {filter_label}", level=4),
                ui.table(gc_on_budget),
                direction="column", flex_grow=1,
            ),
            direction="row", flex_grow=1, gap="size-200",
        )
        content.append(bottom_row)

    return ui.flex(
        *content,
        direction="column",
        flex_grow=1,
        gap="size-100",
        UNSAFE_style={"height": "100%", "overflow": "auto"},
    )


@ui.component
def gc_throughput_dashboard():
    # Applied state — only this triggers re-render (on form submit)
    applied, set_applied = use_state({
        "static_inc": "Inc",
        "cycle_budget": "100", "autotune": "100", "jvm": "25",
        "benchmark": None,
    })

    def handle_submit(data):
        set_applied({
            "static_inc": "Inc",
            "cycle_budget": data.get("cycle_budget", "100"),
            "autotune": data.get("autotune", "100"),
            "jvm": data.get("jvm", "25"),
            "benchmark": data.get("benchmark", None),
        })

    a_cycle = applied["cycle_budget"]
    a_autotune = applied["autotune"]
    a_jvm = applied["jvm"]
    a_bench = applied["benchmark"]
    setid_filter_local = f'*_{a_cycle}_p{a_autotune}_j{a_jvm}*'

    # Load available benchmarks based on applied filters
    def compute_benchmarks():
        sid_filter = applied["static_inc"]
        sid_regex = setid_filter_local.replace("*", ".*")
        type_regex = "|".join(f".*{t.strip()}.*" for t in sid_filter.split(",")) if sid_filter else ".*"
        rs = _raw_rs.where([
            f"set_id.matches(`{sid_regex}`)",
            f"benchmark_name.matches(`{type_regex}`)",
        ])
        try:
            return sorted(dhpd.to_pandas(
                rs.select_distinct(["benchmark_name"])
            )["benchmark_name"].tolist())
        except Exception:
            return []

    all_benchmarks = use_memo(compute_benchmarks, [applied])

    # Use the applied benchmark; auto-select first if it's invalid
    effective_bench = a_bench
    if all_benchmarks and (not effective_bench or effective_bench not in all_benchmarks):
        effective_bench = all_benchmarks[0]

    # Compute CDF/CCDF and time series from applied state
    def compute_distribution():
        if not effective_bench:
            return None, None, None, None
        try:
            from deephaven import agg
            setid_regex = setid_filter_local.replace("*", ".*")
            _replace_expr = f'set_id.replace("{actor}" + "/" + "{prefix}","")'

            # --- CDF/CCDF ---
            events = _raw_be \
                .where(['type = `ugp.cycle.cost`']) \
                .view(['benchmark_name', 'start', 'cycleCost=duration',
                       f'gc_type=set_id.replace("{actor}" + "/" + "{prefix}","")'])
            events = events.where([
                f'benchmark_name = `{effective_bench}`',
                f'gc_type.matches(`{setid_regex}`)',
            ])

            n = events.count_by("N", by=["benchmark_name", "gc_type"])

            cdf_t = (
                events
                .sort(["benchmark_name", "gc_type", "cycleCost"])
                .partition_by(["benchmark_name", "gc_type"]).transform(_compute_rank).merge()
                .natural_join(n, on=["benchmark_name", "gc_type"])
                .update_view(["Percentile = 100.0 * Rank / N", "cycleCostSec = cycleCost / 1_000_000_000.0"])
            )
            ccdf_t = (
                events
                .sort(["benchmark_name", "gc_type", "cycleCost"])
                .partition_by(["benchmark_name", "gc_type"]).transform(_compute_rank).merge()
                .natural_join(n, on=["benchmark_name", "gc_type"])
                .update_view(["Percentile = 100.0 * (1.0 - (double)Rank / N)", "cycleCostSec = cycleCost / 1_000_000_000.0"])
            )

            # --- Throughput time series ---
            median_runs = _raw_rs.where([
                f'benchmark_name = `{effective_bench}`',
                f'set_id.matches(`{setid_regex}`)',
            ]).view(['benchmark_name', 'set_id', 'run_id'])

            tput_raw = _raw_be.where(['name = `duration_rows`']) \
                .where_in(median_runs, cols=['benchmark_name', 'set_id', 'run_id'])
            min_starts = tput_raw.agg_by([agg.min_("min_start = start")], by=["benchmark_name", "set_id", "run_id"])
            tput_t = tput_raw \
                .natural_join(min_starts, on=["benchmark_name", "set_id", "run_id"]) \
                .view(['benchmark_name',
                    'duration_offset=(start - min_start) / 1_000_000_000.0',
                    'throughput=value * 1_000_000_000.0 / duration',
                    f'gc_type=({_replace_expr}).isEmpty() ? set_id : {_replace_expr}']) \
                .sort("duration_offset")

            # --- GC activity events ---
            gc_raw = _raw_be.where(['type = `jdk.GarbageCollection`']) \
                .where_in(median_runs, cols=['benchmark_name', 'set_id', 'run_id'])
            min_starts_gc = gc_raw.agg_by([agg.min_("min_start = start")], by=["benchmark_name", "set_id", "run_id"])
            gc_t = gc_raw \
                .natural_join(min_starts_gc, on=["benchmark_name", "set_id", "run_id"]) \
                .view(['benchmark_name',
                    'gc_start=(start - min_start) / 1_000_000_000.0',
                    'gc_duration=duration / 1_000_000_000.0',
                    f'gc_type=({_replace_expr}).isEmpty() ? set_id : {_replace_expr}']) \
                .sort("gc_start")

            return cdf_t, ccdf_t, tput_t, gc_t
        except Exception:
            return None, None, None, None

    cdf_table, ccdf_table, tput_table, gc_table = use_memo(compute_distribution, [applied])

    # --- Controls: wrapped in ui.form so pickers don't make server calls until Apply ---
    controls = ui.form(
        ui.flex(
            ui.picker(
                *all_benchmarks,
                label="Benchmark",
                name="benchmark",
                default_selected_key=all_benchmarks[0] if all_benchmarks else None,
            ) if all_benchmarks else ui.text("No benchmarks"),
            ui.picker(
                "100", "1000",
                label="Cycle Budget (ms)",
                name="cycle_budget",
                default_selected_key="100",
            ),
            ui.picker(
                "80", "90", "100", "110",
                label="Autotune %",
                name="autotune",
                default_selected_key="100",
            ),
            ui.picker(
                "17", "25",
                label="JVM Version",
                name="jvm",
                default_selected_key="25",
            ),
            ui.button("Apply", type="submit", variant="accent"),
            direction="row",
            gap="size-200",
            align_items="end",
        ),
        on_submit=handle_submit,
        flex_grow=0,
        flex_shrink=0,
    )

    # --- Plots ---
    has_data = cdf_table is not None and ccdf_table is not None
    if has_data:
        gc_types = sorted(dhpd.to_pandas(
            cdf_table.select_distinct(["gc_type"])
        )["gc_type"].tolist())
        color_map = {gc: COLORS[i % len(COLORS)] for i, gc in enumerate(gc_types)}

        def make_cdf_updater(title):
            def update(fig):
                fig.update_layout(
                    title=dict(text=title, font=dict(size=14)),
                    xaxis=dict(title="Cycle Cost (sec)"),
                    yaxis=dict(title="Percentile"),
                    margin=dict(t=40, b=40, l=50, r=10),
                    colorway=[color_map[gc] for gc in gc_types],
                    legend=dict(orientation="v", yanchor="bottom", y=0, xanchor="left", x=1.02),
                )
                return fig
            return update

        cdf_plot = dx.line(
            cdf_table, x="cycleCostSec", y="Percentile",
            by=["gc_type"], log_x=True,
            title=f"{effective_bench} CDF",
            unsafe_update_figure=make_cdf_updater(f"{effective_bench} CDF"),
        )
        ccdf_plot = dx.line(
            ccdf_table, x="cycleCostSec", y="Percentile",
            by=["gc_type"], log_x=True, log_y=True,
            title=f"{effective_bench} CCDF",
            unsafe_update_figure=make_cdf_updater(f"{effective_bench} CCDF"),
        )
        left_col = ui.flex(cdf_plot, ccdf_plot, direction="column", flex_grow=1, gap="size-200")

        if tput_table is not None and gc_table is not None:
            def make_throughput_updater(fig):
                fig.update_traces(line=dict(width=1))
                fig.update_layout(
                    title=dict(text=f"{effective_bench} - Throughput", font=dict(size=14)),
                    legend=dict(yanchor="bottom", y=0, xanchor="left", x=1.02),
                    margin=dict(t=40, b=10, l=60, r=10),
                    xaxis=dict(title="", showticklabels=False),
                    colorway=[color_map[gc] for gc in gc_types],
                )
                return fig

            def make_gc_updater(fig):
                fig.update_traces(
                    error_x=dict(type="data", symmetric=False, thickness=1),
                    marker=dict(size=2),
                )
                fig.update_layout(
                    title=dict(text="GC Activity", font=dict(size=14)),
                    legend=dict(yanchor="bottom", y=0, xanchor="left", x=1.02),
                    margin=dict(t=40, b=40, l=60, r=10),
                    xaxis=dict(title="Elapsed Time (s)"),
                    yaxis=dict(title="GC (s)"),
                    colorway=[color_map[gc] for gc in gc_types],
                )
                return fig

            throughput_plot = dx.line(
                tput_table, x="duration_offset", y="throughput",
                color="gc_type",
                unsafe_update_figure=make_throughput_updater,
            )
            gc_plot = dx.scatter(
                gc_table, x="gc_start", y="gc_duration",
                error_x="gc_duration", color="gc_type", log_y=True,
                unsafe_update_figure=make_gc_updater,
            )
            right_col = ui.flex(throughput_plot, gc_plot, direction="column", flex_grow=1, gap="size-200")
        else:
            right_col = ui.text("No throughput/GC data available.")

        plots = ui.flex(left_col, right_col, direction="row", flex_grow=1, gap="size-200")
    else:
        plots = ui.text("Click Apply to load plots.")

    return ui.flex(
        controls,
        plots,
        direction="column",
        flex_grow=1,
        gap="size-100",
        UNSAFE_style={"height": "100%", "overflow": "auto"},
    )


@ui.component
def gc_rankings_dashboard():
    import math
    import pandas as pd
    from deephaven import new_table
    from deephaven.column import string_col

    def compute_rankings():
        global actor, static_inc_filter, benchmark_filter, cycle_budget_ms, autotune_pct, jvm_version
        global setid_filter, bench_result_sets, bench_results, bench_metrics, bench_events, gc_metrics

        si_options = ["Static", "Inc"]
        budget_options = ["100", "1000"]
        autotune_options = ["80", "90", "100", "110"]
        jvm_options = ["17", "25"]

        tput_rows = []
        jitter_rows = []
        budget_rows = []

        for si in si_options:
            for budget in budget_options:
                for auto in autotune_options:
                    for jvm in jvm_options:
                        # Set globals for this combination
                        static_inc_filter = si
                        benchmark_filter = "*"
                        cycle_budget_ms = int(budget)
                        autotune_pct = auto
                        jvm_version = jvm
                        setid_filter = f'*_{budget}_p{auto}_j{jvm}*'

                        settings_label = f"{si}_{budget}_p{auto}_j{jvm}"

                        rs = _raw_rs.sort(['set_id'])
                        br = _raw_br.sort(['set_id'])
                        bm = _raw_bm
                        be = _raw_be
                        rs, br, bm, be = [filter_benchmarks(t) for t in [rs, br, bm, be]]
                        bench_result_sets = rs
                        bench_results = br
                        bench_metrics = bm
                        bench_events = be
                        gc_metrics = rs.join(bm, ['benchmark_name','origin','set_id'], ['name','value']).sort(['set_id'])

                        suffix = f"_{budget}_p{auto}_j{jvm}"

                        import re as _re
                        def _gc_name(col):
                            name = col.replace(suffix, '')
                            name = _re.sub(rf'[\s_-]*{_re.escape(si)}[\s_-]*', '_', name)
                            name = _re.sub(r'_+', '_', name)
                            name = name.strip('_')
                            return name

                        def _visible_cols(tbl):
                            return [c.name for c in tbl.columns if c.name != 'benchmark_name' and '__TABLE_STYLE_FORMAT' not in c.name]

                        def _geo_mean(df, col):
                            vals = df[col].dropna().values.astype(float)
                            vals = vals[vals > 0]
                            return float(np.exp(np.log(vals).mean())) if len(vals) > 0 else float('nan')

                        def _rank_row(gc_means, settings, higher_is_better):
                            # Sort GCs: best first
                            valid = [(gc, v) for gc, v in gc_means.items() if not math.isnan(v)]
                            if not valid:
                                return None
                            valid.sort(key=lambda x: x[1], reverse=higher_is_better)
                            best_val = valid[0][1]
                            row = {"Settings": settings}
                            for i, (gc, val) in enumerate(valid):
                                rank_col = f"{i+1}"
                                if i == 0:
                                    row[rank_col] = gc
                                else:
                                    if best_val != 0:
                                        pct = ((val - best_val) / best_val) * 100.0
                                        row[rank_col] = f"{gc} ({pct:+.0f}%)"
                                    else:
                                        row[rank_col] = gc
                            return row

                        # Throughput
                        try:
                            tbl = get_throughput_table()
                            cols = _visible_cols(tbl)
                            df = dhpd.to_pandas(tbl.view(['benchmark_name'] + cols))
                            gc_means = {_gc_name(c): _geo_mean(df, c) for c in cols}
                            r = _rank_row(gc_means, settings_label, higher_is_better=True)
                            if r:
                                tput_rows.append(r)
                        except Exception:
                            pass

                        # Jitter (lower is better)
                        try:
                            tbl = get_jitter_table()
                            cols = _visible_cols(tbl)
                            df = dhpd.to_pandas(tbl.view(['benchmark_name'] + cols))
                            gc_means = {_gc_name(c): _geo_mean(df, c) for c in cols}
                            r = _rank_row(gc_means, settings_label, higher_is_better=False)
                            if r:
                                jitter_rows.append(r)
                        except Exception:
                            pass

                        # On-budget (higher is better, use mean)
                        try:
                            tbl = get_cycle_onbudget_table()
                            cols = _visible_cols(tbl)
                            df = dhpd.to_pandas(tbl.view(['benchmark_name'] + cols))
                            gc_means = {_gc_name(c): float(df[c].dropna().mean()) for c in cols}
                            r = _rank_row(gc_means, settings_label, higher_is_better=True)
                            if r:
                                budget_rows.append(r)
                        except Exception:
                            pass

        def _build_table(rows, label):
            if not rows:
                return new_table([string_col("Settings", [f"No {label} data"])])
            max_rank = max(len(r) - 1 for r in rows)
            settings_vals = [r.get("Settings", "") for r in rows]
            columns = [string_col("Settings", settings_vals)]
            for i in range(max_rank):
                columns.append(string_col(f"r{i+1}", [r.get(str(i+1), "") for r in rows]))
            return new_table(columns)

        t_tput = _build_table(tput_rows, "throughput")
        t_jitter = _build_table(jitter_rows, "jitter")
        t_budget = _build_table(budget_rows, "on-budget")
        return t_tput, t_jitter, t_budget

    tput_rank, jitter_rank, budget_rank = use_memo(compute_rankings, [])

    return ui.flex(
        ui.flex(
            ui.heading("Throughput Rankings (GEO MEAN, higher is better)", level=4),
            ui.table(tput_rank),
            direction="column", flex_grow=1,
        ),
        ui.flex(
            ui.heading("Jitter Rankings (GEO MEAN, lower is better)", level=4),
            ui.table(jitter_rank),
            direction="column", flex_grow=1,
        ),
        ui.flex(
            ui.heading("On-Budget Rankings (MEAN, higher is better)", level=4),
            ui.table(budget_rank),
            direction="column", flex_grow=1,
        ),
        direction="column",
        flex_grow=1,
        gap="size-200",
        UNSAFE_style={"height": "100%", "overflow": "auto"},
    )

@ui.component
def gc_extras_dashboard():
    import math
    from deephaven import new_table
    from deephaven.column import string_col, long_col, double_col

    def compute_extras():
        global actor, static_inc_filter, benchmark_filter, cycle_budget_ms, autotune_pct, jvm_version
        global setid_filter, bench_result_sets, bench_results, bench_metrics, bench_events, gc_metrics

        # Configurations to compare (all p100_j25)
        configs = [
            ("Static_1000", "Static", "1000"),
            ("Inc_1000", "Inc", "1000"),
            ("Inc_100", "Inc", "100"),
        ]

        import re as _re

        def _strip_si(name):
            """Remove Static/Inc from benchmark name to create a common key."""
            name = _re.sub(r'(?i)\b(Static|Inc)\b', '', name)
            name = _re.sub(r'_+', '_', name).strip('_')
            name = _re.sub(r'\s*-\s*$', '', name)
            return name

        # Collect per-benchmark GEO MEAN throughput for each config
        config_data = {}  # config_label -> {common_key: geo_mean_throughput}
        for label, si, budget in configs:
            static_inc_filter = si
            benchmark_filter = "*"
            cycle_budget_ms = int(budget)
            autotune_pct = "100"
            jvm_version = "25"
            setid_filter = f'*_{budget}_p100_j25*'

            rs = _raw_rs.sort(['set_id'])
            br = _raw_br.sort(['set_id'])
            bm = _raw_bm
            be = _raw_be
            rs, br, bm, be = [filter_benchmarks(t) for t in [rs, br, bm, be]]
            bench_result_sets = rs
            bench_results = br
            bench_metrics = bm
            bench_events = be
            gc_metrics = rs.join(bm, ['benchmark_name','origin','set_id'], ['name','value']).sort(['set_id'])

            try:
                df = dhpd.to_pandas(
                    bench_result_sets.view(['benchmark_name', 'op_rate'])
                        .where(['!isNull(op_rate)', 'op_rate > 0'])
                )
                bench_means = {}
                for bname, grp in df.groupby('benchmark_name'):
                    key = _strip_si(bname)
                    vals = grp['op_rate'].values.astype(float)
                    if len(vals) > 0:
                        bench_means[key] = float(np.exp(np.log(vals).mean()))
                config_data[label] = bench_means
            except Exception:
                config_data[label] = {}

        # Build table using common benchmark keys
        baseline_label = "Static_1000"
        baseline = config_data.get(baseline_label, {})
        all_keys = sorted(baseline.keys())

        if not all_keys:
            return new_table([string_col("benchmark_name", ["No data"])])

        bench_names = []
        baseline_vals = []
        inc_1000_vals = []
        inc_100_vals = []

        for key in all_keys:
            base_val = baseline.get(key, float('nan'))
            bench_names.append(key)
            baseline_vals.append(base_val)

            # Inc_1000 % diff
            inc1000 = config_data.get("Inc_1000", {}).get(key, float('nan'))
            if not math.isnan(base_val) and base_val > 0 and not math.isnan(inc1000):
                inc_1000_vals.append(((inc1000 - base_val) / base_val) * 100.0)
            else:
                inc_1000_vals.append(float('nan'))

            # Inc_100 % diff
            inc100 = config_data.get("Inc_100", {}).get(key, float('nan'))
            if not math.isnan(base_val) and base_val > 0 and not math.isnan(inc100):
                inc_100_vals.append(((inc100 - base_val) / base_val) * 100.0)
            else:
                inc_100_vals.append(float('nan'))

        t = new_table([
            string_col("benchmark_name", bench_names),
            long_col("Static_1000", [int(round(v)) if not math.isnan(v) else 0 for v in baseline_vals]),
            double_col("Inc_1000", [round(v, 1) if not math.isnan(v) else float('nan') for v in inc_1000_vals]),
            double_col("Inc_100", [round(v, 1) if not math.isnan(v) else float('nan') for v in inc_100_vals]),
        ]).format_columns([
            "Inc_1000 = Decimal(`0.0'%'`)",
            "Inc_100 = Decimal(`0.0'%'`)",
        ])
        return t

    extras_table = use_memo(compute_extras, [])

    return ui.flex(
        ui.flex(
            ui.heading("Throughput Comparison — GEO MEAN across GCs (p100, j25)", level=4),
            ui.table(extras_table),
            direction="column", flex_grow=1,
        ),
        direction="column",
        flex_grow=1,
        gap="size-200",
        UNSAFE_style={"height": "100%", "overflow": "auto"},
    )

gc_rankings = gc_rankings_dashboard()
gc_bench_summary = gc_bench_summary_dashboard()
gc_throughput = gc_throughput_dashboard()
gc_extras = gc_extras_dashboard()
