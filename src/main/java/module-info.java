/*
 * Copyright 2022 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module com.mbcoder.iot.weatherstation {
  // modules required by the app
  requires com.esri.arcgisruntime;
  requires javafx.graphics;
  requires org.slf4j.nop;
  requires javafx.fxml;
  requires java.xml;

  requires eu.hansolo.medusa;

  // make all @FXML annotated objects reflectively accessible to the javafx.fxml module
  opens com.mbcoder.iot.weatherstation to javafx.fxml;

  // Pi4J MODULES
  requires com.pi4j;
  requires com.pi4j.plugin.pigpio;
  requires com.pi4j.plugin.linuxfs;
  requires jdk.unsupported;
  requires org.slf4j;
  requires rpi.drivers;

  exports com.mbcoder.iot.weatherstation;
}
