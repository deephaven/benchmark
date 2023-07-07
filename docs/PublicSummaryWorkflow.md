# Public Benchmark Summary Workflow (and Demo)

## Background
Benchmarks for Deephaven are run nightly on a bare metal server. Results for successful runs are collected in a read-only
GCloud bucket that is publically available through the ReST API at <https://storage.googleapis.com/deephaven-benchmark>.

The easiest way to access and use the Benchmark data is to run a [Python snippet](PublishedResults.md) in a 
[Deephaven Community Core](https://deephaven.io/community/) (DHC) installation. This requires access to GCloud for that instance.

## Benchmark Summary
As of this writing there are nearly 600 benchmarks produced nightly for DHC. That's great for developers, but users may want
a more concise way of measuring up DHC query operations. 

A summary can be added to the README.md for the [Benchmark Project](https://github.com/stanbrub/benchmark) to give a
simple overview of common query operations. An example is provided in the following forked
[README](https://github.com/stanbrub/benchmark/tree/embed-benchmark-summary-readme).

### Workflow
The table embedded in the example README can be generated along with the nightly Github run, deposited in the 
GCloud bucket on success, and referenced in the README from there. The table is a SVG file embedded using a Markdown
image reference. Though SVG hyperlinks are not acknowledged in GitHub markdown, clicking on the image pops up the SVG document
where remote links then work.

Pros:
- Two ways to get a Benchmark summary with a link; [README](https://github.com/stanbrub/benchmark/tree/embed-benchmark-summary-readme) 
or [GCloud](https://storage.googleapis.com/deephaven-benchmark/benchmark-summary.svg)
- Eliminates the need to check new tables into the Benchmark project
- Simple to generate from a template at the end of the Benchmark run and upload to GCloud using Github workflow

Cons:
- SVG in Markdown is limited (no js scripts, some css events don't work)
- The battle of caches (GCloud, Github, Browser) can delay visibility of changes

## Digging Deeper Demo
Even though there is a [Python snippet](PublishedResults.md) that creates
some tables from the data in the GCloud bucket, users that want to explore DHC benchmarks may be reluctant to download
and install DHC just to look at benchmarks. Navigating from the Benchmark Summary to a Demo DHC worker provides a way
to explore DHC operation performance without the effort of installation and potential troubleshooting.

Having a Demo that uses real Deephaven data rather than mocked up data shows confidence in the product. It also allows
a level of scrutiny that could make benchmark tests and performance better.

### Existing Demo Server Workflow (Using Python Snippet)
At [Deephaven IO](https://deephaven.io/) there is a "Try Demo" button that points the user to a live DHC installation
that has some pre-defined notebooks and data. A Benchmark folder seems to fit well here. Adding the 
[Python snippet](PublishedResults.md) to a notebook can provide access to any of the Benchmark data in the cloud.

Pros:
- Use an existing and maintained Demo cluster
- Using the Python snippet, changes to data or tables do not require a change to the Demo
- No extra storage required for data, since it's in the cloud

Cons:
- Demo servers do not allow internet access from DHC queries
- Running the Pyhon snippet is slower than running from local data

### Existing Demo Server Workflow (Local Scripts and Data)
Like the previous workflow the existing Demo Servers would be used. However, both data and scripts/notebooks would be
copied onto (or checked into) the Demo Servers nightly. Everything would be run locally with no downloads.

Pros:
- Use an existing and maintained Demo cluster
- No worry of internet abuse from DHC, since access is turned off
- Faster query runs, since data is local

Cons:
- Data and scripts either checked-in nightly or copied nightly to keep up to date
- Data duplicated in GCloud and on the Demo server
- Maintain two versions of the Benchmark tables query; one for the cloud and one for local

### New Demo Server Workflow
Provide a new Demo cluster exclusively for Benchmarks that is configured to work with the GCloud bucket.
Provide login access for users to run Benchmark notebooks.

Pros:
- Customizable for the purpose of Benchmarks
- Access control to avoid internet abuse

Cons:
- Another cluster to maintain
- User access maintenance









