#while :
#do
#  echo Temp:21.14
#  echo Pressure:682.86
#  echo Humidity:63.41
#  sleep 2
#done


import time
import random

while True:
    humidity = random.randint(0,9)
    print('Temp:21.14')
    print('Pressure:682.86')
    print('Humidity:{:05.2f}'.format(humidity))
    time.sleep(2)

