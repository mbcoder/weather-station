/**
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.mbcoder.iot.weatherstation;


import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.devices.bmp280.BMP280Declares;
import com.pi4j.devices.bmp280.BMP280Device;
import com.pi4j.io.serial.Serial;
import com.pi4j.plugin.linuxfs.provider.i2c.LinuxFsI2CProvider;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Weather_logger extends Application {


    private BMP280Device weatherSensor;
    public static void main(String[] args) {

        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {

        int busNum = BMP280Declares.DEFAULT_BUS;
        int address = 0x76; //BMP280Declares.DEFAULT_ADDRESS;

        // set the title and size of the stage and show it
        stage.setTitle("Weather station logger");
        stage.setWidth(800);
        stage.setHeight(700);
        stage.show();

        // create a JavaFX scene with a stack pane as the root node and add it to the scene
        StackPane stackPane = new StackPane();
        Scene scene = new Scene(stackPane);
        stage.setScene(scene);

        HBox hBox = new HBox();
        // button to start logging weather information (temperature and pressure)
        Button btnLogWeather = new Button("Log weather");
        btnLogWeather.setOnAction(event -> {
            logWeather();
        });

        stackPane.getChildren().add(hBox);

        // initialise connection to the BMP280 sensor in the i2c bus.  Using the Pimoroni BMP280
        // which has a default address on bus 1 of 0x76
        var pi4j = Pi4J.newContextBuilder().add(
            LinuxFsI2CProvider.newInstance()).build();

        weatherSensor = new BMP280Device(pi4j, busNum, address);


    }

    private void logWeather() {
        System.out.println("pressure " + weatherSensor.pressureMb());
        System.out.println("temperature " + weatherSensor.temperatureC());
    }

    /**
     * Stops and releases all resources used in application.
     */
    @Override
    public void stop() {

        /*
        if (mapView != null) {
            mapView.dispose();
        }

         */
    }
}
