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

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.devices.bmp280.BMP280Device;
import eu.hansolo.medusa.FGauge;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.GaugeDesign;
import eu.hansolo.medusa.LcdDesign;
import eu.hansolo.medusa.SectionBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import one.microproject.rpi.hardware.gpio.sensors.BME280;
import one.microproject.rpi.hardware.gpio.sensors.BME280Builder;
import one.microproject.rpi.hardware.gpio.sensors.impl.BME280Impl;

public class Controller {

  @FXML
  VBox vBox;
  private static final DecimalFormat formatter = new DecimalFormat("0.00");
  private String weatherStationID = ""; // this is the unique weather station id

  @FXML private Gauge humidityGauge;
  @FXML private Gauge digitalTempGauge;
  @FXML private Gauge barometerGauge;
  private Label pressureReadingLabel;

  private Timer loggingTimer;
  @FXML
  private GridPane gridPane;

  private BMP280Device weatherSensor;
  private boolean simulatedMode = true;
  private double currentTemperature = 0;
  private double currentPressureMb = 0;
  private double currentHumidity = 0;
  //private BME280 sensor;

  public void initialize() {
    try {
      weatherStationID = "RaspPi";

      // set up the UI
      buildAndDisplayGauges();

      // start sensor reading
      //System.out.println("starting sensor");
      //sensor = new BME280();
      //sensor.startReadingSensor();

      // read data
      // startWeatherLogging();

    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
  }

  /**
   * Starts logging data coming from either a simulated source, or the Raspberry Pi.
   */
  @FXML
  private void startWeatherLogging() {
    // instance of bme280 sensor (not used in simulation mode)

    /*
    if (!simulatedMode) {
      sensor = new BME280();
      sensor.startReadingSensor();
    }

     */

    // timer for reading sensor and logging results
    loggingTimer = new Timer();

    // 10000; // time between sensor samples in milliseconds
    int sampleFrequency = 5000;
    loggingTimer.schedule(new TimerTask() {
      public void run() {
        System.out.println("logging");

        // is it a simulated feed?
        if (simulatedMode) {
          // make up a random temperature and pressure
          Random random = new Random();
          currentTemperature = (random.nextDouble() * 10) + 10;
          currentPressureMb = 990 + random.nextDouble() * 15;
          currentHumidity = 60 + random.nextDouble() * 15;
        } else {
          // read from the sensor
          //currentTemperature = sensor.getTemperature();
          //currentPressureMb = sensor.getTemperature();
          //currentHumidity = sensor.getHumidity();
       }

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
    digitalTempGauge.setValue(temperature);
    barometerGauge.setValue(pressure);
    int intPressure = Double.valueOf(pressure).intValue();
    pressureReadingLabel.setText("Atmospheric pressure: " + intPressure + " mB");
  }

  /**
   * Builds a gauge for each sensor and displays it.
   */
  private void buildAndDisplayGauges() {
    humidityGauge = GaugeBuilder.create()
      .skinType(Gauge.SkinType.LCD)
      .title("Humidity")
      .lcdDesign(LcdDesign.GRAY_PURPLE)
      .oldValueVisible(false)
      .maxMeasuredValueVisible(false)
      .minMeasuredValueVisible(false)
      .unit("%")
      .build();

    barometerGauge = GaugeBuilder.create()
      .skinType(Gauge.SkinType.SECTION)
      .needleColor(Color.BLACK)
      .title("Pressure")
      .markersVisible(true)
      .minValue(940)
      .maxValue(1060)
      .animated(true)
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

    digitalTempGauge = GaugeBuilder.create()
      .skinType(Gauge.SkinType.LCD)
      .lcdDesign(LcdDesign.GRAY_PURPLE)
      .title("Temperature")
      .subTitle(weatherStationID)
      .oldValueVisible(false)
      .maxMeasuredValueVisible(false)
      .minMeasuredValueVisible(false)
      .unit("\u00B0C")
      .build();

    gridPane.add(humidityGauge, 0, 0);
    pressureReadingLabel = new Label("Pressure (mB)" );
    pressureReadingLabel.setStyle("-fx-text-fill: royalblue; -fx-font-family: Tahoma;");

    vBox.getChildren().addAll(barometerFGauge, pressureReadingLabel);
    gridPane.add(digitalTempGauge, 1, 0);
    digitalTempGauge.setMaxWidth(500);
    humidityGauge.setMaxWidth(500);

    Button tester = new Button("testing");
    tester.setOnAction(event -> {
      System.out.println("starting sensor");


      Context context = Pi4J.newAutoContext();

      BME280 sensor = BME280Builder.get()
          .context(context)
          .build();

      System.out.println("sensor temp " + sensor.getTemperature());

      //sensor = new BME280();
      //sensor.startReadingSensor();
    });
    vBox.getChildren().add(tester);
  }

  /**
   * Stops and releases all resources used in application.
   */
  void terminate() {
    if (loggingTimer != null) loggingTimer.cancel(); // stop timer so the app closes cleanly
    //if (sensor != null) sensor.stopReadingSensor();
  }
}