#!/usr/bin/env python

import time
try:
    from smbus2 import SMBus
except ImportError:
    from smbus import SMBus
from bme280 import BME280

# Initialise the BME280
bus = SMBus(1)
bme280 = BME280(i2c_dev=bus)

print('alive!!!')
print('Temp:21.14')
print('Pressure:682.86')
Print('Humidity:22')

while True:
    temperature = bme280.get_temperature()
    pressure = bme280.get_pressure()
    humidity = bme280.get_humidity()
    print('Temp:{:05.2f}'.format(temperature))
    print('Pressure:{:05.2f}'.format(pressure))
    print('Humidity:{:05.2f}'.format(humidity))
    time.sleep(2)
