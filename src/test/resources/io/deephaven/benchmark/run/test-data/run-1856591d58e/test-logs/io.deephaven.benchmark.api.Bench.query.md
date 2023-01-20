# Test Class - io.deephaven.benchmark.api.Bench

## Test - Write Platform Details

### Query 1
````
import deephaven.perfmon as pm
from deephaven import empty_table

pil = pm.process_info_log()
pil2 = empty_table(0).snapshot(source_table=pil, do_init=True)
````

