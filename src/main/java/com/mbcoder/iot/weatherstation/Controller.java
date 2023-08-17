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

import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;

import eu.hansolo.medusa.FGauge;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.GaugeDesign;
import eu.hansolo.medusa.SectionBuilder;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import one.microproject.rpi.hardware.gpio.sensors.BME280;
import one.microproject.rpi.hardware.gpio.sensors.BME280Builder;

public class Controller implements Initializable {

  @FXML VBox vBox;
  private final UUID weatherStationID = UUID.fromString("db12edca-7d28-45e6-84a3-9484c6e50d12"); // GlobalID of weather station for this unit
  @FXML private Gauge humidityGauge;
  @FXML private Gauge digitalTempGauge;
  @FXML private Gauge barometerGauge;
  private Timer recordingTimer; // timer for reading from the sensors
  private Timer loggingTimer; // timer for logging data to the feature service
  private final boolean simulatedMode = true;
  private double currentTemperature = 0;
  private double currentPressureMb = 0;
  private double currentHumidity = 0;
  private BME280 sensor;
  private ServiceFeatureTable reportTable;
  private Context context;

  /**
   * Initializes the application on start up
   * <p>
   * @param url
   * The location used to resolve relative paths for the root object, or
   * {@code null} if the location is not known.
   *
   * @param resourceBundle
   * The resources used to localize the root object, or {@code null} if
   * the root object was not localized.
   */
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    // set up the UI for the barometer gauge
    buildAndDisplayBarometerGauge();

    // read data from the sensor
    startWeatherRecording();

    // log data into feature service
    startWeatherLogging();
  }

  /**
   * Logs data read from the weather sensor into the feature service
   */
  private void startWeatherLogging() {
    // Connect to the service feature table for logging data on public service
    reportTable = new ServiceFeatureTable("https://services1.arcgis.com/6677msI40mnLuuLr/ArcGIS/rest/services/Weather/FeatureServer/1");
    reportTable.loadAsync();
    reportTable.addDoneLoadingListener(()-> {
      if (reportTable.getLoadStatus() == LoadStatus.LOADED) {
        // start timer for logging weather readings to feature service
        loggingTimer = new Timer();
        loggingTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            System.out.println("logging to feature service");

            // create attributes for the weather report
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("WeatherStationID", weatherStationID);
            attributes.put("ReportTime", Calendar.getInstance());
            attributes.put("Temperature", currentTemperature);
            attributes.put("Pressure", currentPressureMb);
            attributes.put("Humidity", currentHumidity);

            // create the feature
            Feature reportFeature = reportTable.createFeature(attributes, null);

            // add the feature to the table and apply edits to the feature service
            var addFuture = reportTable.addFeatureAsync(reportFeature);
            addFuture.addDoneListener(reportTable::applyEditsAsync);
          }
        }, 10000,1800000);  // log to feature service every 30 minutes
      }
    });
  }

  /**
   * Starts recording data coming from either a simulated source, or the Raspberry Pi.
   */
  @FXML
  private void startWeatherRecording() {
    // instance of bme280 sensor (not used in simulation mode)
    if (!simulatedMode) {
      System.out.println("connecting to sensor");
      context = Pi4J.newAutoContext();
      sensor = BME280Builder.get()
          .context(context)
          .build();
    }

    // timer for reading sensor and logging results
    recordingTimer = new Timer();
    recordingTimer.schedule(new TimerTask() {
      public void run() {
        // is it a simulated feed?
        if (simulatedMode) {
          // make up a random temperature and pressure
          var random = new Random();
          currentTemperature = (random.nextDouble() * 10) + 10;
          currentPressureMb = 990 + random.nextDouble() * 15;
          currentHumidity = 60 + random.nextDouble() * 15;
        } else {
          // read from the sensor
          currentTemperature = sensor.getTemperature();
          currentPressureMb = sensor.getPressure() / 100;
          currentHumidity = sensor.getRelativeHumidity();

          // reset the sensor after each read to prevent i2c locking
          sensor.reset();
        }

        // update the display on JavaFX thread
        Platform.runLater(() -> updateDisplay(currentTemperature, currentPressureMb, currentHumidity));
      }
    }, 1000, 120000); // read sensor every 2 minutes
  }

  /**
   * Updates the display with temperature, pressure and humidity readings.
   * <p>
   * @param temperature temperature in ºC
   * @param pressure    pressure in mB
   * @param humidity    humidity in %
   */
  private void updateDisplay(double temperature, double pressure, double humidity) {

    // update the temperature and humidity
    humidityGauge.setValue(humidity);
    digitalTempGauge.setValue(temperature);
    // update the air pressure
    int intPressure = Double.valueOf(pressure).intValue();
    barometerGauge.setValue(intPressure);
  }

  /**
   * Builds a section style gauge for the air pressure sensor and displays it.
   */
  private void buildAndDisplayBarometerGauge() {

    barometerGauge = GaugeBuilder.create()
      .skinType(Gauge.SkinType.SECTION)
      .needleColor(Color.rgb(95,123,210)) // matches needle to other gauge font color
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
    if (recordingTimer != null) recordingTimer.cancel(); // stop recording timer so the app closes cleanly
    if (loggingTimer != null) loggingTimer.cancel(); // also stop the logging timer
    if (sensor != null) {
      try {
        sensor.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}