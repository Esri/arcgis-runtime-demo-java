package demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MonitorApp extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/fxml/Monitor.fxml"));
    Scene scene = new Scene(root);
    stage.setTitle("Fire Monitor");
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