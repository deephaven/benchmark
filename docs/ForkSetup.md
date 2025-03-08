# Benchmark Fork Setup and Run

A common use case when developing new features for Deephaven Community Core is to write new benchmarks along with the development of a new feature.  While existing benchmarks can be run using [Adhoc Workflows](AdhocWorkflows.md) to check for performance improvements and regressions, new benchmarks require a fork before they are merged. Using a fork branch as a sandbox, without intending to merge the benchmarks, is a valid use case as well.

Fortunately, the same [Adhoc Workflows](AdhocWorkflows.md) can run on a fork as on the main _deephaven-benchmark_ branch. However, the bare metal servers cannot be used without adding the proper Github secrets to the fork. Some of the values of these secrets may be shared within the organization, and some may be unique to the fork owner.

### Cloning the Deephaven Benchmark Repository

In the [Github Main Benchmark Repository](https://github.com/deephaven/benchmark) there is a "Fork" button near the upper right of the page. Clicking it allows you to create a fork of "deephaven-benchmark". Keep in mind that this does not give you any special permissions to use Deephaven Servers and no secrets are inherited. However, the "Adhoc" actions can be run if you have the proper configuration and permissions.

### Required Secrets

Not all secrets are required for each workflow. In the following list, each secret is listed, what it means, and what workflows it is relevant for. To populate these secrets, go to the "Actions secrets and variables" page under "Settings -> Secrets and variables -> Actions". Some of these values will need to be supplied by your administrator.

| Variable                   | Description                                                           | Shared (Ask Admin) | Optional |
| -------------------------- | --------------------------------------------------------------------- | ------------------ | -------- |
| BENCHMARK_GCLOUD           | The GCloud Service Account Key (credentials) in JSON format           | Yes                | No       |
| BENCHMARK_HOST             | The host ip for an existing (non-auto-provisioned) server             | No                 | No       |
| BENCHMARK_USER             | The "run as" user for running benchmarks on the server                | No                 | No       |
| BENCHMARK_KEY              | A private key used by SSH corresponding to a public key on the server | No                 | No       |
| BENCHMARK_METAL_AUTH_TOKEN | The key required to access the bare metal provider API                | Yes                | No       |
| BENCHMARK_METAL_PROJECT_ID | The project id from the bare metal provider for deployments           | Yes                | No       |
| BENCHMARK_SLACK_CHANNEL    | Slack channel to notify on workflow completion (ex. @your-slack-user) | Yes                | Yes      |
| BENCHMARK_SLACK_TOKEN      | Slack token for notification                                          | Yes                | Yes      |
| BENCHMARK_SIGNING_KEY      | Use to GPG-sign published benchmark artifacts to maven central        | Yes                | Yes      |

