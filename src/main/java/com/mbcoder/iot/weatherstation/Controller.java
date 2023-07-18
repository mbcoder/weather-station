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

import com.pi4j.devices.bmp280.BMP280Declares;
import com.pi4j.devices.bmp280.BMP280Device;
import com.pi4j.plugin.linuxfs.provider.i2c.LinuxFsI2CProvider;
import eu.hansolo.medusa.FGauge;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.GaugeDesign;
import eu.hansolo.medusa.SectionBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import com.pi4j.Pi4J;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class Controller implements Initializable {

  @FXML
  VBox vBox;
  private static final DecimalFormat formatter = new DecimalFormat("0.00");
  private String weatherStationID = ""; // this is the unique weather station id

  @FXML
  private Gauge humidityGauge;
  @FXML
  private Gauge digitalTempGauge;
  @FXML
  private Gauge barometerGauge;
  private Timer loggingTimer;

  private BMP280Device weatherSensor;


  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    weatherStationID = "RaspPi";

    // set up the UI
    buildAndDisplayGauges();
    // read data
    startWeatherLogging();

  }

  @FXML
  private void handleSensorConnectButton() {
    // initialise connection to the BMP280 sensor in the i2c bus.  Using the Pimoroni BMP280
    // which has a default address on bus 1 of 0x76
    var pi4j = Pi4J.newContextBuilder().add(
      LinuxFsI2CProvider.newInstance()).build();

    weatherSensor = new BMP280Device(pi4j, BMP280Declares.DEFAULT_BUS, 0x76);
  }

  /**
   * Starts logging data coming from either a simulated source, or the Raspberry Pi.
   */
  @FXML
  private void startWeatherLogging() {
    // timer for reading sensor and logging results

    digitalTempGauge.setSubTitle(weatherStationID);
    loggingTimer = new Timer();

    // 10000; // time between sensor samples in milliseconds
    int sampleFrequency = 1000;
    loggingTimer.schedule(new TimerTask() {
      public void run() {
        double currentTemperature;
        double currentPressureMb;
        double currentHumidity;

        System.out.println("logging");

        // is it a simulated feed?
//        if (chkSimulated.isSelected()) {
          // make up a random temperature and pressure
          Random random = new Random();
          currentTemperature = (random.nextDouble() * 10) + 10;
          currentPressureMb = 990 + random.nextDouble() * 15;
          currentHumidity = 60 + random.nextDouble() * 15;
//        } else {
          // read from the sensor
//          currentTemperature = weatherSensor.temperatureC();
//          currentPressureMb = weatherSensor.pressureMb();
//          currentHumidity = 0;
//          currentHumidity = weatherSensor.humidity();
//        }

        // update the display on JavaFX thread
        Platform.runLater(() -> updateDisplay(currentTemperature, currentPressureMb, currentHumidity));
      }
    }, 1000, sampleFrequency);

  }

  /**
   * Updates the display with temperature, pressure and humidity readings.
   *
   * @param temperature temperature in ºC
   * @param pressure pressure in mB
   * @param humidity humidity in %
   */
  private void updateDisplay(double temperature, double pressure, double humidity) {

    // update the temperature and pressure
    humidityGauge.setValue(humidity);

    System.out.println(humidityGauge.getValue());
    digitalTempGauge.setValue(temperature);
    int intPressure = Double.valueOf(pressure).intValue();

    barometerGauge.setValue(intPressure);

  }

  /**
   * Builds a section style gauge for the air pressure sensor and displays it.
   */
  private void buildAndDisplayGauges() {

    barometerGauge = GaugeBuilder.create()
      .skinType(Gauge.SkinType.SECTION)
      .needleColor(Color.BLACK)
      .title("Atmospheric Pressure")
      .unit(" mbar")
      .unitColor(Color.WHITE)
      .titleColor(Color.WHITE)
      .valueVisible(true)
      .valueColor(Color.WHITE)
      .markersVisible(true)
      .decimals(0)
      .minValue(940)
      .maxValue(1060)
      .animated(true)
      .knobColor(Color.FLORALWHITE)
      .highlightSections(true)
      .sections(
        SectionBuilder.create()
          .start(1040)
          .stop(1060)
          .text("VERY DRY")
          .color(Color.rgb(203, 215, 213))
          .highlightColor(Color.FLORALWHITE)
          .textColor(Gauge.DARK_COLOR)
          .build(),
        SectionBuilder.create()
          .start(1020)
          .stop(1040)
          .text("FAIR")
          .color(Color.rgb(203, 215, 213))
          .highlightColor(Color.FLORALWHITE)
          .textColor(Gauge.DARK_COLOR)
          .build(),
        SectionBuilder.create()
          .start(1000)
          .stop(1020)
          .text("CHANGE")
          .color(Color.rgb(203, 215, 213))
          .highlightColor(Color.FLORALWHITE)
          .textColor(Gauge.DARK_COLOR)
          .build(),
        SectionBuilder.create()
          .start(970)
          .stop(1000)
          .text("RAIN")
          .color(Color.rgb(203, 215, 213))
          .highlightColor(Color.FLORALWHITE)
          .textColor(Gauge.DARK_COLOR)
          .build(),
        SectionBuilder.create()
          .start(940)
          .stop(970)
          .text("STORMY")
          .color(Color.rgb(203, 215, 213))
          .highlightColor(Color.FLORALWHITE)
          .textColor(Gauge.DARK_COLOR)
          .build())
      .build();

    FGauge barometerFGauge = new FGauge(barometerGauge, GaugeDesign.TILTED_BLACK, GaugeDesign.GaugeBackground.WHITE);
    vBox.getChildren().addAll(barometerFGauge);

  }


  /**
   * Stops and releases all resources used in application.
   */
  void terminate() {
    if (loggingTimer != null) loggingTimer.cancel(); // stop timer so the app closes cleanly
  }
}