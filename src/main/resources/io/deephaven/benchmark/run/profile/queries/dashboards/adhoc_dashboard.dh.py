# Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending 
#
# Deephaven Adhoc dashboard for visualizing benchmark data captured with Github adhoc workflows.
# The dashboard shows benchmark rates, metrics (e.g. GC, Compile, Heap), and platform comparisons 
# (e.g. java version, hardware model, python versions) between data sets.
#
# Requirements: Deephaven 0.36.1 or greater
#
# ruff: noqa: F821
from urllib.request import urlopen; import os
from deephaven import ui, merge
from deephaven.ui import use_memo, use_state
from deephaven.plot.figure import Figure
from deephaven.plot import PlotStyle

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(f'{root}/deephaven-benchmark/benchmark_functions.dh.py') as r:
    exec(r.read().decode(), globals(), locals())
    storage_uri = f'{root}/deephaven-benchmark'

def use_dashboard_input():
    actor, set_actor = use_state('')
    prefix, set_prefix = use_state('')
    user_input, set_user_input = use_state({'actor':'','prefix':''})

    def update_user_input():
        set_user_input({'actor':actor,'prefix':prefix})

    input_panel = ui.flex(
        ui.text_field(label='Actor', label_position='side', value=actor, on_change=set_actor),
        ui.text_field(label='Set Label', label_position='side', value=prefix, on_change=set_prefix),
        ui.button('Apply', on_press=lambda: update_user_input()),
        direction="row"
    )
    return user_input, input_panel

def use_benchmark_chart(result_table, row_selection, user_input):
    actor = user_input['actor']; prefix = user_input['prefix']

    setids = get_setids(result_table)
    setprefix = setprefix = f'{actor}/{prefix}'
    selected_benchmark = row_selection['Benchmark']['value']
    ui_figure = Figure()
    for setid in setids:
        setcol = normalize_column_name(setprefix,setid)
        chart_table = result_table.where([f'benchmark_name=`{selected_benchmark}`',f'set_id=`{setid}`']) \
            .sort(['timestamp']).update('run=i+1')
        ui_figure = ui_figure.plot_xy(series_name=setcol, t=chart_table, x="run", y="op_rate")
    return ui.flex(ui_figure.show())

def use_metrics_combo(prop_table, row_selection):
    option, set_option = ui.use_state(None)
    selected_benchmark = row_selection['Benchmark']['value']
    prop_table = prop_table.where(f'benchmark_name=`{selected_benchmark}`')

    items = [ui.item(r['label'],key=r['key']) for r in get_property_list_keys(prop_table).iter_dict()]
    return option,ui.combo_box(items,label='Name',selected_key=option,on_change=set_option,label_position='side')

def use_metrics_chart(metrics_table,row_selection,metric_selection,user_input):
    actor = user_input['actor']; prefix = user_input['prefix']
    setids = get_setids(metrics_table)
    setprefix = f'{actor}/{prefix}'
    selected_benchmark = row_selection['Benchmark']['value']
    metrics_table = metrics_table.where([f'benchmark_name=`{selected_benchmark}`',f'name=`{metric_selection}`'])
    ui_figure = Figure()
    for i, setid in enumerate(setids):
        if i == 0:
            ui_figure = ui_figure.axes(plot_style=PlotStyle.BAR).chart_title(title=f'{selected_benchmark} {metric_selection}')
        setcol = normalize_column_name(setprefix,setid)
        chart_table = metrics_table.where([f'benchmark_name=`{selected_benchmark}`',f'set_id=`{setid}`']) \
            .sort(['timestamp']).update('run=i+1')
        ui_figure = ui_figure.plot_cat(series_name=setcol, t=chart_table, category="run", y="value")
    return ui.flex(ui_figure.show())

def load_table_memo(table_name, parent_table, user_input):
    table_func = globals()[f'load_{table_name}_tables']
    return use_memo(
        lambda: table_func(parent_table,user_input['actor'], user_input['prefix']), [user_input])

@ui.component
def adhoc_dashboard():
    user_input,input_form = use_dashboard_input()
    row_selection,set_row_selection = use_state({'Benchmark':{'value':''}})
    set_table,result_table = load_table_memo('results', None, user_input)
    otherdiff,jardiff,pydiff = load_table_memo('diffs', result_table, user_input)
    runner_metrics, engine_metrics = load_table_memo('metrics', result_table, user_input)
    benchmark_chart = use_benchmark_chart(result_table,row_selection,user_input)
    selected_runner_metric,runner_metrics_combo = use_metrics_combo(runner_metrics,row_selection)
    selected_engine_metric,engine_metrics_combo = use_metrics_combo(engine_metrics,row_selection)
    runner_metrics_chart = use_metrics_chart(runner_metrics,row_selection,selected_runner_metric,user_input)
    engine_metrics_chart = use_metrics_chart(engine_metrics,row_selection,selected_engine_metric,user_input)

    return ui.column([
        ui.row(ui.panel(input_form, title='Data Set'), height='9'),
        ui.row(
            ui.panel(ui.table(set_table, on_row_press=set_row_selection, density='regular'), title='Benchmark Comparison'),
            ui.stack(
                ui.panel(ui.table(otherdiff, density='regular'), title='Other Changes'),
                ui.panel(ui.table(jardiff, density='regular'), title='Jar Changes'),
                ui.panel(ui.table(pydiff, density='regular'), title='Python Changes')
            ),
            height='55'),
        ui.row(
            ui.stack(
                ui.panel(benchmark_chart, title='Run Rates'),
            ),
            ui.stack(
                ui.panel(ui.flex(engine_metrics_combo,engine_metrics_chart,direction='column'), title='Engine Metrics'),
                ui.panel(ui.flex(runner_metrics_combo,runner_metrics_chart,direction='column'), title='Runner Metrics'),
                activeItemIndex=0
            ),
            height='36')
    ])

Adhoc_Dashboard = ui.dashboard(adhoc_dashboard())

def normalize_column_name(prefix, text):
    text = re.sub('^.*/','',text[len(prefix):])
    text = normalize_name(text)
    return re.sub('^_+','',text)

def get_property_list_keys(table):
    return table.select_distinct(['name']).view(['key=name','label=name']).sort(['label'])

def get_setids(bench_results):
    setids = bench_results.select_distinct(['set_id']).sort_descending(['set_id'])
    return [row.set_id for row in setids.iter_tuple()]

def load_results_tables(parent_table, actor, prefix):
    bench_result_sets,bench_results = load_table_or_empty('result_sets',storage_uri,'adhoc',actor, prefix)

    setids = get_setids(bench_result_sets)
    setprefix = f'{actor}/{prefix}'

    bench = bench_result_sets.select_distinct(['Benchmark=benchmark_name'])
    rate1 = None
    for setid in setids:
        setcol = normalize_column_name(setprefix,setid)
        varcol = 'Var_' + setcol
        ratecol = 'Rate_' + setcol
        changecol = 'Change_' + setcol
        right = bench_result_sets.where(['set_id=`' + setid + '`'])
        bench = bench.natural_join(right,on=['Benchmark=benchmark_name'], \
            joins=[varcol+'=variability', ratecol+'=op_rate'])
        if rate1 is None:
            rate1 = ratecol
        else:
            bench = bench.update([changecol + '=(float)gain(' + rate1 + ',' + ratecol + ')'])
    bench = format_columns(bench, pct_cols=('Var_','Change_'), int_cols=('Rate'))
    return bench, bench_results

def load_diffs_tables(parent_table, actor, prefix):
    bench_platform = load_table_or_empty('platform',storage_uri,'adhoc',actor,prefix)
    setids = get_setids(parent_table)
    setprefix = f'{actor}/{prefix}'

    jointbl = bench_platform.where(['origin=`deephaven-engine`']).first_by(['set_id','name'])
    platdiff = jointbl.select_distinct(['name','value']).group_by(['name']) \
        .where(['value.size() > 1']).view(['Name=name'])

    for setid in setids:
        setcol = normalize_column_name(setprefix,setid,)
        right = jointbl.where(['set_id=`' + setid + '`'])
        platdiff = platdiff.natural_join(right,on=['Name=name'], joins=['Val_'+setcol+'=value'])

    jardiff = merge([
        platdiff.where(['Name=`deephaven.version`']),
        platdiff.where(['Name=`dependency.jar.size`']),
        platdiff.where(['Name.endsWith(`.jar`)'])
    ])

    pydiff = merge([
        platdiff.where(['Name=`python.version`']),
        platdiff.where(['Name=`dependency.python.size`']),
        platdiff.where(['Name.endsWith(`.py`)'])
    ])

    otherdiff = platdiff.where_not_in(merge([jardiff,pydiff]), cols=['Name'])
    jardiff = jardiff.update(['Name=Name.replaceAll(`[.]jar$`,``)'])
    pydiff = pydiff.update(['Name=Name.replaceAll(`[.]py$`,``)'])
    return otherdiff, jardiff, pydiff

def load_metrics_tables(parent_table, actor, prefix):
    bench_metrics = load_table_or_empty('metrics',storage_uri,'adhoc',actor,prefix)

    runnerdiff = bench_metrics.where(['origin=`test-runner`'])
    enginediff = bench_metrics.where(['origin=`deephaven-engine`'])

    return runnerdiff, enginediff

