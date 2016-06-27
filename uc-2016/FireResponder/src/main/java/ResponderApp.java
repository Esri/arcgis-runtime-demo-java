
/*
Copyright 2016 Esri.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.  
*/
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Main class to start the app. The app is divided into two parts:
 * <ol>
 * <li>View: designed in layout_responder_app.fxml
 * <li>Controller: implemented in the {@link ResponderAppController} class.
 * </ol>
 * 
 * @since 100.0.0
 */
public class ResponderApp extends Application {

//  static {
//    ArcGISRuntimeEnvironment.setInstallDirectory("D:\\temp\\arcgis-java-sdk-100.0.0");
//  }

  @Override
  public void start(Stage stage) throws Exception {
    Pane root = (Pane) FXMLLoader.load(getClass().getResource("layout_responder_app.fxml"));
    Scene scene = new Scene(root, -1, -1);
    scene.getStylesheets().add("main.css");
    stage.setTitle("Responder App");
    stage.initStyle(StageStyle.TRANSPARENT);
    stage.setScene(scene);
    stage.setMaximized(true);
    stage.show();
  }

  @Override
  public void stop() throws Exception {
  }

  public static void main(String[] args) {
    Application.launch(args);
  }
}
