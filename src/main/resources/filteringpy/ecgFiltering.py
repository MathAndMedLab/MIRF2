import sys

y = [float(x) for x in sys.argv[1].split()]

sys.path.insert(0, '/home/alexandra/anaconda3/lib/python3.7/site-packages')

import pyyawt
import numpy as np

filtered_y = pyyawt.wden(np.array(y),'sqtwolog','s','one', 2, 'sym8')[0]

wander = pyyawt.wden(filtered_y, 'heursure', 's', 'one', 8, 'sym8')[0]
filtered_wander = filtered_y - wander

result = filtered_wander.tolist()

for i in range (min(8, len(result)//300 + 1)):
  for j in range (i*300, min((i+1)*300, len(result))):
    print(result[j], end=' ')
  print()