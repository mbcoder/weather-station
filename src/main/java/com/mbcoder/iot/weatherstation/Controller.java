package com.mbcoder.iot.weatherstation;

import com.pi4j.devices.bmp280.BMP280Declares;
import com.pi4j.devices.bmp280.BMP280Device;
import com.pi4j.plugin.linuxfs.provider.i2c.LinuxFsI2CProvider;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.LcdDesign;
import eu.hansolo.medusa.Section;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
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
  @FXML private Gauge gauge17;

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
        .title("Dashboard")
        .unit("\u00B0C")
        .maxValue(25)
        .markersVisible(false)
        .barColor(Color.CRIMSON)
        .valueColor(Color.BLACK)
        .titleColor(Color.WHITE)
        .unitColor(Color.WHITE)
        .shadowsEnabled(true)
        .gradientBarEnabled(true)
        .gradientBarStops(new Stop(0.00, Color.LIGHTBLUE),
          new Stop(0.3, Color.LIGHTGREEN),
          new Stop(0.6, Color.YELLOW),
          new Stop(1.00, Color.ORANGE))
        .build();

      windowsVBox.getChildren().add(tempGaugeDial);


      gauge17 = GaugeBuilder.create()
        .skinType(Gauge.SkinType.LCD)
//        .animated(true)
        .title("Temperature")
        .subTitle(weatherStationID)
        .unit("\u00B0C")
//        .lcdDesign(LcdDesign.BLUE_LIGHTBLUE2)
//        .thresholdVisible(true)
//        .threshold(25)
        .build();

      gauge17.setOldValueVisible(false);
      gauge17.setMaxMeasuredValueVisible(false);
      gauge17.setMinMeasuredValueVisible(false);

      windowsVBox.getChildren().add(gauge17);





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
          currentPressureMb = 990 + random.nextDouble() * 10;
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
    gauge17.setValue(temperature);
    tempGauge.setValue(temperature);
//    pressureGauge.setValue(pressure);


    labelTemp.setText("Temperature " + formatter.format(temperature) + "C");
    labelPressure.setText("Pressure " + formatter.format(pressure) + " Mb");


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