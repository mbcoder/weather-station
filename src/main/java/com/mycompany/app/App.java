/**
 * Copyright 2019 Esri
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

package com.mycompany.app;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureTableEditResult;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.data.ServiceGeodatabase;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.MapView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    //private MapView mapView;
    private static final String SERVICE_LAYER_URL =
        "https://sampleserver6.arcgisonline.com/arcgis/rest/services/DamageAssessment/FeatureServer";
    private ServiceFeatureTable featureTable;

    public static void main(String[] args) {

        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {

        // set the title and size of the stage and show it
        stage.setTitle("My Map App");
        stage.setWidth(800);
        stage.setHeight(700);
        stage.show();

        // create a JavaFX scene with a stack pane as the root node and add it to the scene
        StackPane stackPane = new StackPane();
        Scene scene = new Scene(stackPane);
        stage.setScene(scene);

        Button button = new Button("press me");
        button.setOnAction(event -> {
            System.out.println("pressed!");


            Random random = new Random();

            // create a map point from a point
            Point point = new Point(random.nextDouble()* 10000, random.nextDouble() * 10000, SpatialReferences.getWebMercator());

            // add a new feature to the service feature table
            addFeature(point, featureTable);

        });



        // create a service geodatabase from the service layer url and load it
        var serviceGeodatabase = new ServiceGeodatabase(SERVICE_LAYER_URL);
        serviceGeodatabase.addDoneLoadingListener(() -> {

            // create service feature table from the service geodatabase's table first layer
            featureTable = serviceGeodatabase.getTable(0);

            featureTable.loadAsync();
            featureTable.addDoneLoadingListener(()-> {
                System.out.println("table loaded");
                stackPane.getChildren().add(button);
            });

            // create a feature layer from table
            //var featureLayer = new FeatureLayer(featureTable);

        });
        serviceGeodatabase.loadAsync();

        serviceGeodatabase.addDoneLoadingListener(()-> {
            System.out.println("loaded db");
        });



        // Note: it is not best practice to store API keys in source code.
        // An API key is required to enable access to services, web maps, and web scenes hosted in ArcGIS Online.
        // If you haven't already, go to your developer dashboard to get your API key.
        // Please refer to https://developers.arcgis.com/java/get-started/ for more information
        //String yourApiKey = "YOUR_API_KEY";
        //ArcGISRuntimeEnvironment.setApiKey(yourApiKey);

        // create a MapView to display the map and add it to the stack pane
        //mapView = new MapView();
        //stackPane.getChildren().add(mapView);

        // create an ArcGISMap with an imagery basemap
        //ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_IMAGERY);

        // display the map by setting the map on the map view
        //mapView.setMap(map);
    }


    /**
     * Adds a new Feature to a ServiceFeatureTable and applies the changes to the
     * server.
     *
     * @param mapPoint location to add feature
     * @param featureTable service feature table to add feature
     */
    private void addFeature(Point mapPoint, ServiceFeatureTable featureTable) {

        // create default attributes for the feature
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("typdamage", "Destroyed");
        attributes.put("primcause", "Earthquake");

        // creates a new feature using default attributes and point
        Feature feature = featureTable.createFeature(attributes, mapPoint);

        // check if feature can be added to feature table
        if (featureTable.canAdd()) {
            // add the new feature to the feature table and to server
            featureTable.addFeatureAsync(feature).addDoneListener(() -> applyEdits(featureTable));
        } else {
            System.out.println("Cannot add a feature to this feature table");
        }
    }

    /**
     * Sends any edits on the ServiceFeatureTable to the server.
     *
     * @param featureTable service feature table
     */
    private void applyEdits(ServiceFeatureTable featureTable) {

        // apply the changes to the server
        ListenableFuture<List<FeatureTableEditResult>> editResult = featureTable.getServiceGeodatabase().applyEditsAsync();
        editResult.addDoneListener(() -> {
            try {
                List<FeatureTableEditResult> edits = editResult.get();
                // check if the server edit was successful
                if (edits != null && edits.size() > 0) {
                    var featureEditResult = edits.get(0).getEditResult().get(0);
                    if (!featureEditResult.hasCompletedWithErrors()) {
                        System.out.println("Feature successfully added");
                    } else {
                        throw featureEditResult.getError();
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Exception applying edits on server" + e.getCause().getMessage());
            }
        });
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
