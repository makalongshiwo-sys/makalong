"""Export actual emulator screenshots for visual review; all market fixtures remain labeled."""
from pathlib import Path
import base64,json
from PIL import Image
import io
root=Path('evidence/ci/instrumentation')
for name in ['instrumented-chart-fixture-light','instrumented-chart-fixture-dark','instrumented-chart-fixture-large-text','instrumented-explanation-rates-light','instrumented-etf-history-fixture-light']:
 path=root/(name+'.png')
 if not path.exists():continue
 with Image.open(path) as img:
  img.thumbnail((540,1200))
  stream=io.BytesIO();img.save(stream,format='PNG')
 print('UI_PREVIEW '+json.dumps({'name':name,'mime':'image/png','base64':base64.b64encode(stream.getvalue()).decode()}))
