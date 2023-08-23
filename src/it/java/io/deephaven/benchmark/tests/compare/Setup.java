package io.deephaven.benchmark.tests.compare;

public class Setup {
    static public String flink = """
    import time
    import pandas as pd
    from pyflink.common import Row
    from pyflink.table import (EnvironmentSettings, TableEnvironment, TableDescriptor, Schema, DataTypes, FormatDescriptor)
    from pyflink.table.expressions import lit, col
    from pyflink.table.udf import udtf

    t_env = TableEnvironment.create(EnvironmentSettings.in_batch_mode())
    t_env.get_config().set("parallelism.default", "16")

    def count_rows(table_result):
        count = 0
        with table_result.collect() as rows:
            for row in rows:
                count = count + 1
        return count
    """;
}
