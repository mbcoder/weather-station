#!/usr/bin/env python

import time
try:
    from smbus2 import SMBus
except ImportError:
    from smbus import SMBus
from bme280 import BME280

println('alive!!!')
println('Temp:21.14')
println('Pressure:682.86')
println('Humidity:22.0')

# Initialise the BME280
bus = SMBus(1)
bme280 = BME280(i2c_dev=bus)

while True:
    temperature = bme280.get_temperature()
    pressure = bme280.get_pressure()
    humidity = bme280.get_humidity()
    println('Temp:{:05.2f}'.format(temperature))
    println('Pressure:{:05.2f}'.format(pressure))
    println('Humidity:{:05.2f}'.format(humidity))
    time.sleep(2)
