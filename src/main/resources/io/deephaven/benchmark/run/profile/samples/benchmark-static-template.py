# Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending 
#
# A template Deephaven query that provides a quicker way to produce self-contained
# benchmarks that are simliar to the real ones run every night
# - Helps with the inevitable "How to I test this" questions after poor nightly or
#   adhoc benchmarks
# - Users can run/test this on their own Deephaven setups without having to run the 
#   Benchmark System
# - It's sometimes easier to test new ideas/fixes this way than doing the full 
#   benchmark roundtrip first

import time
from deephaven import empty_table

row_count = 1_000_000

right = empty_table(1010000).update([
    'r_key1=``+(ii%100+1)','r_key2=``+(ii%101+1)','r_wild=``+(ii%10000+1)',
    'r_key4=(int)(ii%99)','r_key5=``+(ii%1010000+1)'
])

source = empty_table(row_count).update([
    'num1=(double)randomInt(0,5)','num2=(double)randomInt(1,11)','key1=``+randomInt(1,101)',
    'key2=``+randomInt(1,102)','key3=randomInt(0,9)','key4=randomInt(0,99)','key5=``+randomInt(1,1_000_001)'
])

start_time = 1676557157537
timed = empty_table(row_count).update([
    'timestamp=start_time+ii','num1=(double)randomInt(0,5)','num2=(double)randomInt(1,11)',
    'key1=``+randomInt(1,101)','key2=``+randomInt(1,102)','key3=randomInt(0,9)','key4=randomInt(0,99)'
])

loaded_table_size = source.size

begin_time = time.perf_counter_ns()

# Add measured operation here
result = source.natural_join(right, on=['key5 = r_key5'])

from deephaven.execution_context import get_exec_ctx
get_exec_ctx().update_graph.j_update_graph.requestRefresh()

end_time = time.perf_counter_ns()

print("Rate(rows/sec):", loaded_table_size / (end_time - begin_time) * 1_000_000_000)

