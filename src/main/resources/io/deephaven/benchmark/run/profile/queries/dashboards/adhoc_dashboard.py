# ruff: noqa: F821
from urllib.request import urlopen; import os
from deephaven import ui, merge, input_table, dtypes as dht
from deephaven.ui import use_memo, use_state
from deephaven.plot.figure import Figure

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen('file:///data/deephaven-benchmark/benchmark_functions.dh.py') as r:
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

def use_benchmark_chart(full_table, row_selection, actor, prefix):
    setids = full_table.select_distinct(['set_id']).sort_descending(['set_id'])
    setids = [row.set_id for row in setids.iter_tuple()]
    setprefix = setprefix = f'{actor}/{prefix}'

    print("Row Selection:", type(row_selection), row_selection)

    if row_selection is None:
        return 'No Benchmark Selected', ui.flex(Figure().show())

    selected_benchmark = row_selection['Benchmark']['value']
    print("Selected:", type(selected_benchmark))

    ui_figure = Figure()
    for setid in setids:
        setcol = normalize_column_name(setprefix,setid,7)
        chart_table = full_table.view([
            'Benchmark=benchmark_name','Time=epochMillisToInstant(timestamp)',
            'Rate=op_rate','set_id'
        ]).where([f'Benchmark=`{selected_benchmark}`',f'set_id=`{setid}`']).sort(['Time']).update('Run=i+1')

        ui_figure = ui_figure.plot_xy(series_name=setcol, t=chart_table, x="Run", y="Rate")

    new_chart = ui_figure.show()
    return selected_benchmark, ui.flex(new_chart)


@ui.component
def adhoc_dashboard():
    user_input, input_form = use_dashboard_input()
    row_selection, set_row_selection = use_state(None)
    main_table, otherdiff, jardiff, pydiff, full_table = use_memo(
        lambda: load_main_table(user_input['actor'], user_input['prefix']), [user_input])
    selected_benchmark, benchmark_chart = use_benchmark_chart(full_table, row_selection,
        user_input['actor'], user_input['prefix'])
    return ui.column([
        ui.row(ui.panel(input_form, title='Data Set'), height='9'),
        ui.row(
            ui.panel(ui.table(main_table, on_row_press=set_row_selection, density='regular'), title='Benchmark Comparison'),
            ui.stack(
                ui.panel(ui.table(otherdiff, density='regular'), title='Other Changes'),
                ui.panel(ui.table(jardiff, density='regular'), title='Jar Changes'),
                ui.panel(ui.table(pydiff, density='regular'), title='Python Changes')
            ),
            height='55'),
        ui.row(
            ui.stack(
                ui.panel(benchmark_chart, title=f'{selected_benchmark}'),
            ),
            ui.stack(
                ui.panel(ui.text('This space for rent')),
            ),
            height='36')
    ])

Adhoc_Dashboard = ui.dashboard(adhoc_dashboard())

def normalize_column_name(prefix, text, min_len):
    # If prefix contains / do the follow, otherise return normalized text
    text = re.sub('^.*/','',text[len(prefix):])
    text = normalize_name(text)
    return re.sub('^_+','',text)

def test_normalize_column_name():
    setids = ['stanbrub/0.36.0', 'stanbrub/0.35.0', 'stanbrub/edge']
    prefix = 'stanbrub/'
    for setid in setids:
        print(f'Test {setid}',normalize_column_name(prefix,setid,7))

    setids = ['stanbrub/demo-inline-xx1', 'stanbrub/demo-inline-xx2']
    prefix = 'stanbrub/demo-inline-'
    for setid in setids:
        print(f'Test {setid}',normalize_column_name(prefix,setid,7))

test_normalize_column_name()

def format_columns(table,pct_cols=(),int_cols=()):
    column_formats = []
    for col in table.columns:
        n = col.name
        if n.startswith(pct_cols):
            column_formats.append(n + '=Decimal(`0.0%`)')
        if n.startswith(int_cols):
            column_formats.append(n + '=Decimal(`###,##0`)')
    return table.format_columns(column_formats)

def empty_bench_results():
    return input_table({'benchmark_name':dht.string,'origin':dht.string,'timestamp':dht.int64,
        'test_duration':dht.float64,'op_duration':dht.float64,'op_rate':dht.int64,
        'row_count':dht.int64,'set_id':dht.string,'run_id':dht.string})

def empty_bench_results_sets():
    return input_table({'benchmark_name':dht.string,'origin':dht.string,'timestamp':dht.int64,
        'test_duration':dht.float64,'set_op_rates':dht.int64_array,'op_duration':dht.float64,
        'op_rate':dht.int64,'row_count':dht.int64,'variability':dht.float32,'set_id':dht.string,
        'run_id':dht.string,'set_count':dht.int64,'deephaven_version':dht.string})

def empty_bench_platforms():
    return input_table({'origin':dht.string,'name':dht.string,'value':dht.string,
        'set_id':dht.string,'run_id':dht.string})

def load_main_table(actor, prefix):
    actor = actor.strip(); prefix = prefix.strip()
    if actor and prefix:
        bench_results_sets, bench_results = load_bench_results_sets(storage_uri, 'adhoc', actor, prefix)
    else:
        bench_results_sets = empty_bench_results_sets()
        bench_results = empty_bench_results()

    setids = bench_results_sets.select_distinct(['set_id']).sort_descending(['set_id'])
    setids = [row.set_id for row in setids.iter_tuple()]
    setprefix = f'{actor}/{prefix}'

    bench = bench_results_sets.select_distinct(['Benchmark=benchmark_name'])
    rate1 = None
    for setid in setids:
        setcol = normalize_column_name(setprefix,setid,7)
        varcol = 'Var_' + setcol
        ratecol = 'Rate_' + setcol
        changecol = 'Change_' + setcol
        right = bench_results_sets.where(['set_id=`' + setid + '`'])
        bench = bench.natural_join(right,on=['Benchmark=benchmark_name'], \
            joins=[varcol+'=variability', ratecol+'=op_rate'])
        if rate1 is None:
            rate1 = ratecol
        else:
            bench = bench.update([changecol + '=(float)gain(' + rate1 + ',' + ratecol + ')'])
    bench = format_columns(bench, pct_cols=('Var_','Change_'), int_cols=('Rate'))

    if actor and prefix:
        bench_platforms = load_bench_platform(storage_uri, 'adhoc', actor, prefix)
    else:
        bench_platforms = empty_bench_platforms()

    jointbl = bench_platforms.where(['origin=`deephaven-engine`']).first_by(['set_id','name'])
    platdiff = jointbl.select_distinct(['name','value']).group_by(['name']) \
        .where(['value.size() > 1']).view(['Name=name'])

    for setid in setids:
        setcol = normalize_column_name(setprefix,setid,7)
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
    return bench, otherdiff, jardiff, pydiff, bench_results


