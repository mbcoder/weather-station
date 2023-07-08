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
import com.pi4j.devices.bmp280.BMP280Declares;
import com.pi4j.devices.bmp280.BMP280Device;
import com.pi4j.plugin.linuxfs.provider.i2c.LinuxFsI2CProvider;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Weather_logger extends Application {


    private BMP280Device weatherSensor;
    private String weatherStationID = ""; // this is the unique weather station id
    private CheckBox chkSimulated;
    private int sampleFrequency = 10000; // time between sensor samples in milliseconds
    private Timer loggingTimer;
    private Label labelTemp;
    private Label labelPressure;
    private static final DecimalFormat formatter = new DecimalFormat("0.00");
    private NumberAxis xAxisTemp;
    private NumberAxis yAxisTemp;
    private XYChart.Series tempSeries;
    private NumberAxis xAxisPressure;
    private NumberAxis yAxisPressure;
    private XYChart.Series pressureSeries;
    private int maxReadings = 500;
    private int readingCount = 0;
    private boolean firstReading = true;


    public static void main(String[] args) {

        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {

        int busNum = BMP280Declares.DEFAULT_BUS;
        int address = 0x76; // i2c address if bmp280 sensor

        // set the title and size of the stage and show it
        stage.setTitle("Weather station logger");
        stage.setWidth(800);
        stage.setHeight(500);
        stage.show();

        // create a JavaFX scene with a stack pane as the root node and add it to the scene
        BorderPane borderPane = new BorderPane();
        Scene scene = new Scene(borderPane);
        stage.setScene(scene);

        HBox hBox = new HBox();

        // temp checkbox for simulated feed for running on laptop (default on for now)
        chkSimulated = new CheckBox("Simulated");
        chkSimulated.setSelected(true);
        hBox.getChildren().add(chkSimulated);

        // button to connect to weather sensor
        Button btnConnectSensor = new Button("Connect sensor");
        btnConnectSensor.setOnAction(event -> {
            // initialise connection to the BMP280 sensor in the i2c bus.  Using the Pimoroni BMP280
            // which has a default address on bus 1 of 0x76
            var pi4j = Pi4J.newContextBuilder().add(
                LinuxFsI2CProvider.newInstance()).build();

            weatherSensor = new BMP280Device(pi4j, busNum, address);
        });
        hBox.getChildren().add(btnConnectSensor);

        // button to start logging weather information (temperature and pressure)
        Button btnLogWeather = new Button("Log weather");
        btnLogWeather.setOnAction(event -> {
            startWeatherLogging();
        });
        hBox.getChildren().add(btnLogWeather);

        borderPane.setTop(hBox);

        VBox vBox = new VBox();
        borderPane.setCenter(vBox);

        // crude labels for showing latest data
        labelTemp = new Label("...");
        labelTemp.setFont(new Font("Arial", 48));
        labelPressure = new Label("...");
        labelPressure.setFont(new Font("Arial", 48));

        // defining X axis for temp
        xAxisTemp = new NumberAxis(0, 0, 0);
        xAxisTemp.setLabel("Observation");

        // defining Y axis for temp
        yAxisTemp = new NumberAxis(0, 0, 0);
        yAxisTemp.setLabel("Temp");

        // create line chart and data for temperature
        LineChart chartTemperature = new LineChart(xAxisTemp, yAxisTemp);
        tempSeries = new XYChart.Series();
        tempSeries.setName("Temperature history");
        chartTemperature.getData().add(tempSeries);

        // defining X axes for pressure
        xAxisPressure = new NumberAxis(0, 0, 0);
        xAxisPressure.setLabel("Observation");

        // defining Y aces for pressure
        yAxisPressure = new NumberAxis(0, 0, 0);
        yAxisPressure.setLabel("Pressure");

        // create line chart and data for pressure
        LineChart chartPressure = new LineChart(xAxisPressure, yAxisPressure);
        pressureSeries = new XYChart.Series();
        pressureSeries.setName("Pressure history");
        chartPressure.getData().add(pressureSeries);

        vBox.getChildren().addAll(labelTemp, labelPressure, chartTemperature, chartPressure);
    }

    private void startWeatherLogging() {
        loggingTimer = new Timer();

        loggingTimer.schedule( new TimerTask() {
            public void run() {
                double currentTemperature;
                double currentPressureMb;

                System.out.println("logging");

                // is it a simulated feed?
                if (chkSimulated.isSelected()) {
                    // make up a random temperature and pressure
                    Random random = new Random();
                    currentTemperature = (random.nextDouble() * 2) + 10;
                    currentPressureMb = 990 + random.nextDouble() * 10;
                } else {
                    // read from the sensor
                    currentTemperature = weatherSensor.temperatureC();
                    currentPressureMb = weatherSensor.pressureMb();
                }

                // update the display on JavaFX thread
                Platform.runLater(()-> updateDisplay(currentTemperature, currentPressureMb));
            }
        }, 1000, sampleFrequency);
    }

    private void updateDisplay(double temperature, double pressure) {
        labelTemp.setText("Temperature " + formatter.format(temperature) + "C");
        labelPressure.setText("Pressure " + formatter.format(pressure) + " Mb");

        // set the y axes on the first reading
        if (firstReading) {
            firstReading = false;
            yAxisTemp.setLowerBound(temperature - 1);
            yAxisTemp.setUpperBound(temperature + 1);
            yAxisPressure.setLowerBound(pressure - 10);
            yAxisPressure.setUpperBound(pressure + 10);
        }

        // add data to series for temp and pressure
        tempSeries.getData().add(new XYChart.Data(readingCount, temperature));
        pressureSeries.getData().add(new XYChart.Data(readingCount, pressure));

        if (readingCount < maxReadings) {
            // extent the x axes
            xAxisTemp.setUpperBound(xAxisTemp.getUpperBound() + 1);
            xAxisPressure.setUpperBound(xAxisPressure.getUpperBound() + 1);
        } else {
            // prune the oldest data and move along one step
            tempSeries.getData().remove(0);
            pressureSeries.getData().remove(0);
            xAxisTemp.setLowerBound(xAxisTemp.getLowerBound() + 1);
            xAxisTemp.setUpperBound(xAxisTemp.getUpperBound() + 1);
            xAxisPressure.setLowerBound(xAxisPressure.getLowerBound() + 1);
            xAxisPressure.setUpperBound(xAxisPressure.getUpperBound() + 1);
        }

        // adjust y axes bounds to match data
        if (temperature > yAxisTemp.getUpperBound()) yAxisTemp.setUpperBound(temperature + 1);
        if (temperature < yAxisTemp.getLowerBound()) yAxisTemp.setLowerBound(temperature - 1);

        if (pressure > yAxisPressure.getUpperBound()) yAxisPressure.setUpperBound(pressure + 1);
        if (pressure < yAxisPressure.getLowerBound()) yAxisPressure.setLowerBound(pressure - 1);

        readingCount++;
    }

    /**
     * Stops and releases all resources used in application.
     */
    @Override
    public void stop() {
        if (loggingTimer != null) loggingTimer.cancel(); // stop timer so the app closes cleanly
    }
}
