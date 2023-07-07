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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
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
        stage.setHeight(700);
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

        labelTemp = new Label("...");
        labelTemp.setFont(new Font("Arial", 48));
        labelPressure = new Label("...");
        labelPressure.setFont(new Font("Arial", 48));
        vBox.getChildren().addAll(labelTemp, labelPressure);
    }

    private void startWeatherLogging() {
        loggingTimer = new Timer();

        loggingTimer.schedule( new TimerTask() {
            public void run() {
                double currentTemperature;
                double currentPresssureMb;

                System.out.println("logging");

                // is it a simulated feed?
                if (chkSimulated.isSelected()) {
                    // make up a random temperature and pressure
                    Random random = new Random();
                    currentTemperature = random.nextDouble() * 10;
                    currentPresssureMb = 990 + random.nextDouble() * 10;


                } else {
                    // read from the sensor
                    currentTemperature = weatherSensor.temperatureC();
                    currentPresssureMb = weatherSensor.pressureMb();
                }

                // update the display on JavaFX thread
                Platform.runLater(()-> updateDisplay(currentTemperature, currentPresssureMb));



            }
        }, 1000, sampleFrequency);
    }

    private void updateDisplay(double temperature, double pressure) {
        labelTemp.setText("Temperature " + formatter.format(temperature) + "C");
        labelPressure.setText("Pressure " + formatter.format(pressure) + " Mb");
    }

    /**
     * Stops and releases all resources used in application.
     */
    @Override
    public void stop() {
        if (loggingTimer != null) loggingTimer.cancel(); // stop timer so the app closes cleanly
    }
}
