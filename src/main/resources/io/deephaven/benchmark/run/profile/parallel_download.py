import asyncio, re, os, traceback
from urllib.request import urlretrieve

def background(f):
    def wrapped(*args, **kwargs):
        return asyncio.get_event_loop().run_in_executor(None, f, *args, **kwargs)
    return wrapped

@background
def download(url):
    try:
        out_path = re.sub('^http.*/deephaven-benchmark/', 'data/deephaven-benchmark/', url)
        os.makedirs(os.path.dirname(out_path), mode=0o777, exist_ok=True)
    except Exception:
        print('Error downloading file:', download, ':', traceback.format_exc())
        return
    try:
        urlretrieve(url + '.gz', out_path + '.gz')
        print('Got', out_path + '.gz')
    except Exception:
        try:
            urlretrieve(url, out_path)
            print('Got', out_path)
        except Exception:
            print('Error downloading file:', out_path, ':', traceback.format_exc())

urls = [
    ${downloadUrls}
]

for url in urls:
    download(url)
