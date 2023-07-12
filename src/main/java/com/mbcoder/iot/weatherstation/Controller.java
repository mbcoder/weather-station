package com.mbcoder.iot.weatherstation;

import com.pi4j.devices.bmp280.BMP280Declares;
import com.pi4j.devices.bmp280.BMP280Device;
import com.pi4j.plugin.linuxfs.provider.i2c.LinuxFsI2CProvider;
import eu.hansolo.medusa.FGauge;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.GaugeDesign;
import eu.hansolo.medusa.LcdDesign;
import eu.hansolo.medusa.SectionBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import com.pi4j.Pi4J;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Controller {

  @FXML
  VBox vBox;
  private static final DecimalFormat formatter = new DecimalFormat("0.00");
  private String weatherStationID = ""; // this is the unique weather station id

  @FXML private Gauge humidityGauge;
  @FXML private Gauge digitalTempGauge;
  @FXML private Gauge barometerGauge;

  private Timer loggingTimer;
  @FXML
  private GridPane gridPane;

  private BMP280Device weatherSensor;

  public void initialize() {
    try {

      weatherStationID = "RaspPi";

      // set up the UI
      buildAndDisplayGauges();
      // read data
      startWeatherLogging();

    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();

    }
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
    digitalTempGauge.setValue(temperature);
    int intPressure = Double.valueOf(pressure).intValue();

    barometerGauge.setValue(intPressure);

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

    vBox.getChildren().addAll(barometerFGauge);
    gridPane.add(digitalTempGauge, 1, 0);
    digitalTempGauge.setMaxWidth(500);
    humidityGauge.setMaxWidth(500);

  }


  /**
   * Stops and releases all resources used in application.
   */
  void terminate() {
    if (loggingTimer != null) loggingTimer.cancel(); // stop timer so the app closes cleanly
  }
}