package com.mbcoder.iot.weatherstation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BMP280 {

  private double temperature = 0;
  private double pressure = 0;
  private double humidity = 0;

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
          //System.out.println("read temp " + line.substring(5));
          temperature = Double.parseDouble(line.substring(5));
        }

        // is the line pressure?
        if (line.startsWith("Pressure:")) {

          //System.out.println("read pressure " + line.substring(9));
          pressure = Double.parseDouble(line.substring(9));
        }

        // is the line humidity?
        if (line.startsWith("Humidity:")) {
          //System.out.println("read humidity " + line.substring(9));
          humidity = Double.parseDouble(line.substring(9));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

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
