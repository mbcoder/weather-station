package com.mbcoder.iot.weatherstation;

import com.pi4j.devices.bmp280.BMP280Declares;
import com.pi4j.devices.bmp280.BMP280Device;
import com.pi4j.plugin.linuxfs.provider.i2c.LinuxFsI2CProvider;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.SectionBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;
import com.pi4j.Pi4J;


import java.text.DecimalFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Controller {

  @FXML
  CheckBox chkSimulated;
  @FXML
  Button btnLogWeather;
  @FXML
  Button btnConnectSensor;
  @FXML
  Label labelTemp;
  @FXML
  Label labelPressure;
  @FXML
  HBox hBox;
  @FXML
  VBox vBox;
  @FXML
  LineChart<Number, Number> tempChart;
  @FXML
  LineChart<Number, Number> pressureChart;
  private static final DecimalFormat formatter = new DecimalFormat("0.00");
  private String weatherStationID = ""; // this is the unique weather station id
  private boolean updateGraph = false; // flag set true if its time to update the graph

  private int readingCount = 0;
  private boolean firstReading = true;
  @FXML private Gauge tempGauge;
  @FXML private Gauge pressureGauge;
  @FXML private Gauge tempGaugeDial;
  @FXML private Gauge digitalTempGauge;
  @FXML private Gauge barometerGauge;

  @FXML
  private NumberAxis xAxisTemp;
  @FXML
  private NumberAxis yAxisTemp;
  @FXML
  private NumberAxis xAxisPressure;
  @FXML
  private NumberAxis yAxisPressure;
  @FXML
  private XYChart.Series<Number, Number> tempSeries;
  private Timer loggingTimer;
  private Timer graphTimer;
  @FXML
  private XYChart.Series<Number, Number> pressureSeries;
  @FXML
  private VBox windowsVBox;
  @FXML
  private GridPane gridPane;


  private BMP280Device weatherSensor;

  public void initialize() {
    try {

      tempSeries = new XYChart.Series<>();
      pressureSeries = new XYChart.Series<>();

      tempSeries.setName("Temperature history");
      pressureSeries.setName("Pressure history");

      weatherStationID = "RaspPi";

      tempGaugeDial = GaugeBuilder.create()
        .skinType(Gauge.SkinType.DASHBOARD)
        .animated(true)
        .title("Temperature \u00B0C ")
        .unit("\u00B0C")
        .maxValue(25)
        .markersVisible(false)
        .barColor(Color.CRIMSON)
        .valueColor(Color.BLACK)
        .titleColor(Color.BLACK)
        .unitColor(Color.WHITE)
        .shadowsEnabled(true)
        .gradientBarEnabled(true)
        .gradientBarStops(new Stop(0.00, Color.LIGHTBLUE),
          new Stop(0.3, Color.LIGHTGREEN),
          new Stop(0.6, Color.YELLOW),
          new Stop(1.00, Color.ORANGE))
        .build();

      barometerGauge = GaugeBuilder.create()
        .skinType(Gauge.SkinType.SECTION)
        .needleColor(Color.SILVER)
        .averageColor(Color.SADDLEBROWN)
        .minValue(940)
        .maxValue(1060)
        .animated(true)
        .highlightSections(true)
        .sections(
          SectionBuilder.create()
            .start(1040)
            .stop(1060)
            .text("VERY DRY")
            .color(Color.rgb(223, 223, 223))
            .highlightColor(Color.rgb(197, 223, 0))
            .textColor(Gauge.DARK_COLOR)
            .build(),
          SectionBuilder.create()
            .start(1020)
            .stop(1040)
            .text("FAIR")
            .color(Color.rgb(223, 223, 223))
            .highlightColor(Color.rgb(251, 245, 0))
            .textColor(Gauge.DARK_COLOR)
            .build(),
          SectionBuilder.create()
            .start(1000)
            .stop(1020)
            .text("CHANGE")
            .color(Color.rgb(223, 223, 223))
            .highlightColor(Color.rgb(247, 206, 0))
            .textColor(Gauge.DARK_COLOR)
            .build(),
          SectionBuilder.create()
            .start(970)
            .stop(1000)
            .text("RAIN")
            .color(Color.rgb(223, 223, 223))
            .highlightColor(Color.rgb(227, 124, 1))
            .textColor(Gauge.DARK_COLOR)
            .build(),
          SectionBuilder.create()
            .start(940)
            .stop(970)
            .text("STORMY")
            .color(Color.rgb(223, 223, 223))
            .highlightColor(Color.rgb(223, 49, 23))
            .textColor(Gauge.DARK_COLOR)
            .build())
        .build();

//      windowsVBox.getChildren().add(tempGaugeDial);

      gridPane.add(tempGaugeDial, 0, 1);
      gridPane.add(barometerGauge, 1, 0);

      digitalTempGauge = GaugeBuilder.create()
        .skinType(Gauge.SkinType.LCD)
//        .animated(true)
        .title("Temperature")
        .subTitle(weatherStationID)
        .unit("\u00B0C")
//        .lcdDesign(LcdDesign.BLUE_LIGHTBLUE2)
//        .thresholdVisible(true)
//        .threshold(25)
        .build();

      digitalTempGauge.setOldValueVisible(false);
      digitalTempGauge.setMaxMeasuredValueVisible(false);
      digitalTempGauge.setMinMeasuredValueVisible(false);

//      windowsVBox.getChildren().add(gauge17);

      gridPane.add(digitalTempGauge, 1, 1);




//      tempChart.getData().add(tempSeries);
//      pressureChart.getData().add(pressureSeries);






      // crude labels for showing latest data
//      labelTemp.setFont(new Font("Arial", 48));
//      labelPressure.setFont(new Font("Arial", 48));


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

        System.out.println("logging");

        // is it a simulated feed?
        if (chkSimulated.isSelected()) {
          // make up a random temperature and pressure
          Random random = new Random();
          currentTemperature = (random.nextDouble() * 10) + 10;
          currentPressureMb = 990 + random.nextDouble() * 15;
        } else {
          // read from the sensor
          currentTemperature = weatherSensor.temperatureC();
          currentPressureMb = weatherSensor.pressureMb();
        }

        // update the display on JavaFX thread
        Platform.runLater(() -> updateDisplay(currentTemperature, currentPressureMb));
      }
    }, 1000, sampleFrequency);

    // timer for updating the graph.  this works by setting a flag which is picked up by the logging timer.
    graphTimer = new Timer();
    //900000; // 4 updates per hour
    int graphUpdateFrequency = 4000;
    graphTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        updateGraph = true;
        System.out.println("graph update time!");
      }
    }, 1000, graphUpdateFrequency);
  }

  private void updateDisplay(double temperature, double pressure) {


    // update the temperature and pressure
    tempGaugeDial.setValue(temperature);
    digitalTempGauge.setValue(temperature);
//    tempGauge.setValue(temperature);
    barometerGauge.setValue(pressure);
//    pressureGauge.setValue(pressure);


//    labelTemp.setText("Temperature " + formatter.format(temperature) + "C");
//    labelPressure.setText("Pressure " + formatter.format(pressure) + " Mb");


    // update the graph if its time - happens less frequently
    if (updateGraph) {

      // set the y axes on the first reading
      if (firstReading) {
        firstReading = false;
//        yAxisTemp.setLowerBound(temperature - 1);
//        yAxisTemp.setUpperBound(temperature + 1);
//        yAxisPressure.setLowerBound(pressure - 10);
//        yAxisPressure.setUpperBound(pressure + 10);
      }

      // add data to series for temp and pressure
//      tempSeries.getData().add(new XYChart.Data(readingCount, temperature));
//      pressureSeries.getData().add(new XYChart.Data(readingCount, pressure));

      // number of reading shown in the graph,  With updates every 15 minutes, this allows for just over a day
      int maxReadings = 100;
//      if (readingCount < maxReadings) {
//        // extent the x axes
//        xAxisTemp.setUpperBound(xAxisTemp.getUpperBound() + 1);
//        xAxisPressure.setUpperBound(xAxisPressure.getUpperBound() + 1);
//      } else {
//        // prune the oldest data and move along one step
//        tempSeries.getData().remove(0);
//        pressureSeries.getData().remove(0);
//        xAxisTemp.setLowerBound(xAxisTemp.getLowerBound() + 1);
//        xAxisTemp.setUpperBound(xAxisTemp.getUpperBound() + 1);
//        xAxisPressure.setLowerBound(xAxisPressure.getLowerBound() + 1);
//        xAxisPressure.setUpperBound(xAxisPressure.getUpperBound() + 1);
//      }

      // adjust y axes bounds to match data
//      if (temperature > yAxisTemp.getUpperBound()) yAxisTemp.setUpperBound(temperature + 1);
//      if (temperature < yAxisTemp.getLowerBound()) yAxisTemp.setLowerBound(temperature - 1);
//
//      if (pressure > yAxisPressure.getUpperBound()) yAxisPressure.setUpperBound(pressure + 1);
//      if (pressure < yAxisPressure.getLowerBound()) yAxisPressure.setLowerBound(pressure - 1);

      readingCount++;

      // reset the flag for graph update
      updateGraph = false;
    }
  }


  /**
   * Stops and releases all resources used in application.
   */
  void terminate() {
    if (loggingTimer != null) loggingTimer.cancel(); // stop timer so the app closes cleanly
    if (graphTimer != null) graphTimer.cancel(); // same for graph timer
  }
}