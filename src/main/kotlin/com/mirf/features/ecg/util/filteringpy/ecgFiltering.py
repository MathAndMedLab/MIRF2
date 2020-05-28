import numpy as np
import matplotlib.pyplot as plt
import pyyawt


#f = open('100.txt')
y = [float(item) for sublist in list(map(lambda x : x.split(), f)) for item in sublist]

filtered_y = pyyawt.wden(np.array(y),'sqtwolog','s','one', 2, 'sym8')[0]

# plt.plot(y[1:250], color ='black')
# plt.plot(filtered_y[1:250], color = 'red')

wander = pyyawt.wden(filtered_y, 'heursure', 's', 'one', 8, 'sym8')[0]
filtered_wander = filtered_y - wander
# plt.plot(y[1:250], color ='black')
# plt.plot(filtered_wander[1:250], color = 'red')

print(y[1:15])
print()
print(filtered_wander[1:15])
