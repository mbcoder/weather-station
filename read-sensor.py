#!/usr/bin/env python

import time
import sys
try:
    from smbus2 import SMBus
except ImportError:
    from smbus import SMBus
from bme280 import BME280



# Initialise the BME280
bus = SMBus(1)
bme280 = BME280(i2c_dev=bus)

while True:
    temperature = bme280.get_temperature()
    pressure = bme280.get_pressure()
    humidity = bme280.get_humidity()
    sys.stdout.write('Temp:{:05.2f}\n'.format(temperature))
    sys.stdout.write('Pressure:{:05.2f}\n'.format(pressure))
    sys.stdout.write('Humidity:{:05.2f}\n'.format(humidity))
    time.sleep(2)

def main():
    print("Hello World!")
    sys.stdout.write('Temp:99.14\n')
    sys.stdout.write('Pressure:111.86\n')
    sys.stdout.write('Humidity:99.0\n')

if __name__ == "__main__":
    main()
