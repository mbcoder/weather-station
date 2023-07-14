/**
 * Copyright 2023 Esri
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.mbcoder.iot.weatherstation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BME280 {

  private double temperature = 0;
  private double pressure = 0;
  private double humidity = 0;
  private Process process;

  public void startReadingSensor() {

    Runnable runnable= () -> {
      ProcessBuilder processBuilder = new ProcessBuilder("python ./read-sensor.py");
      //ProcessBuilder processBuilder = new ProcessBuilder("./test.py");
      try {
        process = processBuilder.start();

        BufferedReader reader =
            new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
          System.out.println(".");
          // is the line temperature?
          if (line.startsWith("Temp:")) {
            System.out.println("read temp " + line.substring(5));
            temperature = Double.parseDouble(line.substring(5));
          }

          // is the line pressure?
          if (line.startsWith("Pressure:")) {

            System.out.println("read pressure " + line.substring(9));
            pressure = Double.parseDouble(line.substring(9));
          }

          // is the line humidity?
          if (line.startsWith("Humidity:")) {
            System.out.println("read humidity " + line.substring(9));
            humidity = Double.parseDouble(line.substring(9));
          }
        }
        System.out.println("fallen out of loop <EOL>");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };

    Thread thread = new Thread(runnable);
    thread.start();

    System.out.println("start complete");
  }

  public void stopReadingSensor() {
    if (process!=null) {
      process.destroy();
    }
  }

  /*
  public void readLatestValues() {
    ProcessBuilder processBuilder = new ProcessBuilder("./test.py");
    try {
      Process process = processBuilder.start();

      BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line;
      while ((line = reader.readLine()) != null) {
        // is the line temperature?
        if (line.startsWith("Temp:")) {
          System.out.println("read temp " + line.substring(5));
          temperature = Double.parseDouble(line.substring(5));
        }

        // is the line pressure?
        if (line.startsWith("Pressure:")) {

          System.out.println("read pressure " + line.substring(9));
          pressure = Double.parseDouble(line.substring(9));
        }

        // is the line humidity?
        if (line.startsWith("Humidity:")) {
          System.out.println("read humidity " + line.substring(9));
          humidity = Double.parseDouble(line.substring(9));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

   */

  public double getTemperature() {
    return temperature;
  }

  public double getPressure() {
    return pressure;
  }

  public double getHumidity() {
    return humidity;
  }
}
