import numpy as np
import pywt
import pyyawt
import sys

adc_resolution = int(sys.argv[1])
y = [float(sys.argv[i]) for i in range(2, len(sys.argv))]
digital_to_analog_conv = 5 / 2**adc_resolution
y_mv = np.array(y) * digital_to_analog_conv
#Gaussian noise removal using DWT with daubechies wavelets of order 8 
# and soft thresholding function combined with rigorous SURE
filtered_y = pyyawt.wden(np.array(y_mv), 'rigrsure', 's','one', 8, 'db8')[0] 


#baseline wander removal using fir-filter (doesn't work yet)

#from scipy.signal import kaiserord, lfilter, firwin, freqz

#sample_rate = 1000
#nyq_rate = sample_rate / 2.0
#width = 1.0/nyq_rate
#ripple_db = 40
#N, beta = kaiserord(ripple_db, width)
#cutoff_hz = 0.67
#taps = firwin(N, cutoff_hz/nyq_rate, window=('kaiser', beta))
#filtered_by_fir = lfilter(taps, 1.0, y_mv)

print(*filtered_y, sep=' ')