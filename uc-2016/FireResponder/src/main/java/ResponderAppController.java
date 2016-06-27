
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
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.function.Function;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.datasource.Feature;
import com.esri.arcgisruntime.datasource.arcgis.Geodatabase;
import com.esri.arcgisruntime.datasource.arcgis.GeodatabaseFeatureTable;
import com.esri.arcgisruntime.datasource.arcgis.ServiceFeatureTable;
import com.esri.arcgisruntime.datasource.arcgis.SyncModel;
import com.esri.arcgisruntime.datasource.arcgis.TileCache;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.Symbol;
import com.esri.arcgisruntime.tasks.geodatabase.GenerateGeodatabaseJob;
import com.esri.arcgisruntime.tasks.geodatabase.GenerateGeodatabaseParameters;
import com.esri.arcgisruntime.tasks.geodatabase.GenerateLayerOption;
import com.esri.arcgisruntime.tasks.geodatabase.GeodatabaseSyncTask;
import com.esri.arcgisruntime.tasks.geodatabase.SyncGeodatabaseJob;
import com.esri.arcgisruntime.tasks.geodatabase.SyncGeodatabaseParameters;
import com.esri.arcgisruntime.tasks.geodatabase.SyncGeodatabaseParameters.SyncDirection;
import com.esri.arcgisruntime.tasks.geodatabase.SyncLayerOption;
import com.esri.arcgisruntime.tasks.route.Route;
import com.esri.arcgisruntime.tasks.route.RouteParameters;
import com.esri.arcgisruntime.tasks.route.RouteResult;
import com.esri.arcgisruntime.tasks.route.RouteTask;
import com.esri.arcgisruntime.tasks.route.Stop;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX controller bound to the view in the layout_responder_app.fxml.
 * <p>
 * It provides implementation for various actions on the UI. Example: download
 * data when the download button is clicked.
 * 
 * @since 100.0.0
 */
public class ResponderAppController {

  // fields annotated with @FXML are initialized based on the "fx:id" property
  // in the FXML
  @FXML
  private MapView mapView;

  @FXML
  private ProgressBar progressBar;

  // map
  private ArcGISMap map;

  // for temporary graphics
  private GraphicsOverlay geometryResult;
  private GraphicsOverlay routeResult;

  // event handlers for map-clicked, and mouse-moved-on-map
  private Function<MouseEvent, Void> functionOnMapClick;
  private Function<MouseEvent, Void> functionOnMouseMove;

  // online feature layer to fetch data from
  private final String SERVICE_URL = "http://sampleserver6.arcgisonline.com/arcgis/rest/services/Sync/WildfireSync/FeatureServer";
  private final String LAYER_URL   = SERVICE_URL + "/0";
  private FeatureLayer featureLayer;

  // offline tiles
  private static String tilePackageFilePath;

  // offline, edit and sync
  private GeodatabaseSyncTask     geodatabaseSyncTask;
  private GeodatabaseFeatureTable offlineFeatureTable;
  private Geodatabase             geodatabase;
  private FeatureLayer            offlineFeatureLayer;
  private static String           geodatabaseFilePath;

  // geometry
  private Symbol geometrySymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, 0xaa0ff0aa, null);
  private Point  geometryCenter;

  // route
  private static String   routeFilePath;
  private RouteParameters routeParameters;
  private Stop            routeFrom;
  private Stop            routeTo;
  private Symbol          routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xff00ffff, 4);

  static {
    try {
      geodatabaseFilePath = "/home/linux/git/devtopia/vija6672/demos/UC2016/FireResponder/src/main/resources/data/offline/offline.geodatabase";
      // Paths.get(ResponderAppController.class.getResource("/data/offline/offline.geodatabase").toURI()).toString();
      File file = new File(geodatabaseFilePath);
      if (file.exists()) {
        file.delete();
      }
      tilePackageFilePath = Paths.get(ResponderAppController.class.getResource("/data/tiled packages/RedlandsBasemap.tpk").toURI()).toString();
      routeFilePath = Paths.get(ResponderAppController.class.getResource("/data/network_analyst/Redlands/northamerica.geodatabase").toURI()).toString();
    } catch (URISyntaxException e) {
      System.out.println("Error in initialization.");
    }
  }

  // this method is invoked by JavaFX framework during initialization
  public void initialize() {
    // create a basemap from local data (a tile package)
    TileCache tileCache = new TileCache(tilePackageFilePath); // "/data/tiled
                                                              // packages/RedlandsBasemap.tpk"
    Basemap offlineBasemap = new Basemap(new ArcGISTiledLayer(tileCache));

    // create a map
    map = new ArcGISMap(offlineBasemap);
    mapView.setMap(map);

    // add feature data
    ServiceFeatureTable featureTable = new ServiceFeatureTable(LAYER_URL);
    featureLayer = new FeatureLayer(featureTable);
    map.getOperationalLayers().add(featureLayer);

    // create overlay for temporary graphics
    geometryResult = new GraphicsOverlay();
    mapView.getGraphicsOverlays().add(geometryResult);
    routeResult = new GraphicsOverlay();
    mapView.getGraphicsOverlays().add(routeResult);

    // set functions to execute when map is clicked and mouse moved over the
    // map
    mapView.setOnMouseClicked(mapClickEvent -> {
      if (functionOnMapClick != null) {
        functionOnMapClick.apply(mapClickEvent);
      }
    });
    mapView.setOnMouseMoved(mapMoveEvent -> {
      if (functionOnMouseMove != null) {
        functionOnMouseMove.apply(mapMoveEvent);
      }
    });
  }

  // methods annotated with @FXML are invoked based on the "onAction" property
  // in the FXML
  @FXML
  private void onDownloadClicked(ActionEvent downloadClickEvent) {
    // create a task
    geodatabaseSyncTask = new GeodatabaseSyncTask(SERVICE_URL);

    // setup parameters
    GenerateGeodatabaseParameters params = new GenerateGeodatabaseParameters();

    // download features in visible area
    params.setExtent(mapView.getVisibleArea().getExtent());

    // download layer 0
    params.setSyncModel(SyncModel.PER_LAYER);
    params.getLayerOptions().add(new GenerateLayerOption(0));

    // create job, attach listener
    GenerateGeodatabaseJob downloadGeodatabaseJob = geodatabaseSyncTask.generateGeodatabaseAsync(params, geodatabaseFilePath);
    downloadGeodatabaseJob.addJobDoneListener(() -> {
      handleError(downloadGeodatabaseJob.getError());
      progressBar.setVisible(false);
    });

    // execute operation
    progressBar.setVisible(true);
    downloadGeodatabaseJob.start();
  }

  @FXML
  private void onOfflineClicked(ActionEvent event) {
    // remove online layer
    if (featureLayer != null) {
      map.getOperationalLayers().remove(featureLayer);
    }

    // create a geodatabase
    geodatabase = new Geodatabase(geodatabaseFilePath);
    geodatabase.loadAsync();

    // create a feature layer using the geodatabase
    geodatabase.addDoneLoadingListener(() -> {
      offlineFeatureTable = geodatabase.getGeodatabaseFeatureTableByServiceLayerId(0);
      offlineFeatureLayer = new FeatureLayer(offlineFeatureTable);
      map.getOperationalLayers().add(offlineFeatureLayer);
    });
  }

  @FXML
  private void onGeometryClicked(ActionEvent geometryClickEvent) {
    functionOnMapClick = ((mapClickEvent) -> {
      // reset on right-click
      if (mapClickEvent.getButton() == MouseButton.SECONDARY) {
        functionOnMapClick = null;
        functionOnMouseMove = null;
        geometryCenter = null;
        mapView.getCallout().setVisible(false);
        return null;
      }

      // clear current graphics
      geometryResult.getGraphics().clear();

      // set the clicked point on map as center
      Point2D point = new Point2D(mapClickEvent.getX(), mapClickEvent.getY());
      geometryCenter = mapView.screenToLocation(point);

      return null;
    });

    functionOnMouseMove = ((eventMapMove) -> {
      // return if center is not assigned
      if (geometryCenter == null) {
        return null;
      }

      // clear current graphics
      geometryResult.getGraphics().clear();

      // get the clicked point in map coordinates
      Point2D point = new Point2D(eventMapMove.getX(), eventMapMove.getY());
      Point gometryTo = mapView.screenToLocation(point);

      // create buffer
      double diameter = GeometryEngine.distanceBetween(geometryCenter, gometryTo);
      Polygon bufferArea = GeometryEngine.buffer(geometryCenter, diameter);
      double area = GeometryEngine.area(bufferArea);

      // add to graphics overlay
      Graphic bufferGraphic = new Graphic(bufferArea, geometrySymbol);
      geometryResult.getGraphics().add(bufferGraphic);

      // display info as a callout
      Platform.runLater(() -> {
        VBox boxInfo = new VBox();
        boxInfo.getChildren().add(new Label("Diameter: " + String.format("%.2f", diameter)));
        boxInfo.getChildren().add(new Label("Area: " + String.format("%.2f", area)));
        mapView.getCallout().setCustomView(boxInfo);
        mapView.getCallout().showCalloutAt(geometryCenter);
      });

      return null;
    });
  }

  @FXML
  private void onRouteClicked(ActionEvent routeClickEvent) {
    // setup the route task
    RouteTask routeTask = new RouteTask(routeFilePath, "Routing_ND"); // "/data/network_analyst/Redlands/northamerica.geodatabase"
    routeTask.loadAsync();

    // use the route task after it is loaded
    routeTask.addDoneLoadingListener(() -> {
      try {
        routeParameters = routeTask.generateDefaultParametersAsync().get();
      } catch (Exception ex) {
        handleError(ex);
      }

      // add the clicked point as source
      functionOnMapClick = ((mapClickEvent) -> {
        // reset functions on right-click
        if (mapClickEvent.getButton() == MouseButton.SECONDARY) {
          functionOnMapClick = null;
          functionOnMouseMove = null;
          routeFrom = null;
          mapView.getCallout().setVisible(false);
          return null;
        }

        // set the clicked point as start of route
        Point2D point = new Point2D(mapClickEvent.getX(), mapClickEvent.getY());
        Point pointOnMap = mapView.screenToLocation(point);
        routeFrom = new Stop(pointOnMap);

        return null;
      });

      // find new route using the mouse pointer as destination
      functionOnMouseMove = ((mapMoveEvent) -> {
        // return if routeFrom is not assigned
        if (routeFrom == null) {
          return null;
        }

        // clear existing graphics
        routeResult.getGraphics().clear();

        // set the current mouse position as end of route
        Point2D mapMovePoint = new Point2D(mapMoveEvent.getX(), mapMoveEvent.getY());
        Point routeToPoint = mapView.screenToLocation(mapMovePoint);
        routeTo = new Stop(routeToPoint);

        // add the stops
        routeParameters.getStops().clear();
        routeParameters.getStops().add(routeFrom);
        routeParameters.getStops().add(routeTo);

        // calculate all possible routes, and select one
        ListenableFuture<RouteResult> routeResultFuture = routeTask.solveAsync(routeParameters);
        routeResultFuture.addDoneListener(() -> {
          try {
            Route bestRoute = routeResultFuture.get().getRoutes().get(0);

            // add new route
            Graphic routeGraphic = new Graphic(bestRoute.getRouteGeometry(), routeSymbol);
            routeResult.getGraphics().add(routeGraphic);

            // display route summary
            Platform.runLater(() -> {
              VBox boxInfo = new VBox();
              boxInfo.getChildren().add(new Label("Distance: " + String.format("%.2f", bestRoute.getTotalLength())));
              boxInfo.getChildren().add(new Label("Time: " + String.format("%.2f", bestRoute.getTotalTime()) + " minutes."));

              mapView.getCallout().setCustomView(boxInfo);
              mapView.getCallout().showCalloutAt(routeToPoint);
            });
          } catch (Exception e1) {
            e1.printStackTrace();
          }
        });

        return null;
      });
    });
  }

  @FXML
  private void onEditClicked(ActionEvent editClickEvent) {
    functionOnMapClick = ((mapClickEvent) -> {

      if (mapClickEvent.getButton() == MouseButton.SECONDARY) {
        mapView.getCallout().setVisible(false);
        return null;
      }

      // clear previous results
      featureLayer.clearSelection();

      // identify the clicked features
      try {
        // create a point from where the user clicked
        Point2D pointOnScreen = new Point2D(mapClickEvent.getX(), mapClickEvent.getY());

        IdentifyLayerResult identifyResult = mapView.identifyLayerAsync(offlineFeatureLayer, pointOnScreen, 10, 1).get();
        Feature hitFeature = (Feature) identifyResult.getIdentifiedElements().get(0);

        // select and display
        featureLayer.selectFeature(hitFeature);
        displayFeature(hitFeature, pointOnScreen);
      } catch (Exception e) {
        e.printStackTrace();
      }

      return null;
    });
  }

  private void displayFeature(Feature feature, Point2D pointOnScreen) {
    // display feature info
    Platform.runLater(() -> {
      VBox selectedFeatureInfo = new VBox();
      Label id = new Label("ID: " + feature.getAttributes().get("objectid").toString());
      Label type = new Label("Type: " + feature.getAttributes().get("eventtype").toString());
      Object description = feature.getAttributes().get("description");
      TextArea txtDescription = new TextArea((description == null ? "" : description.toString()));
      Button btnSave = new Button("Save");

      // update data when save is clicked
      btnSave.setOnAction(saveClickEvent -> {
        feature.getAttributes().put("description", txtDescription.getText());
        offlineFeatureTable.updateFeatureAsync(feature);
        mapView.getCallout().setVisible(false);
      });

      selectedFeatureInfo.getChildren().add(id);
      selectedFeatureInfo.getChildren().add(type);
      selectedFeatureInfo.getChildren().add(new Label("Description"));
      selectedFeatureInfo.getChildren().add(txtDescription);
      selectedFeatureInfo.getChildren().add(btnSave);
      mapView.getCallout().setCustomView(selectedFeatureInfo);
      mapView.getCallout().showCalloutAt(mapView.screenToLocation(pointOnScreen));
    });
  }

  @FXML
  private void onSyncClicked(ActionEvent syncClickEvent) {
    // setup parameters
    SyncGeodatabaseParameters params = new SyncGeodatabaseParameters();
    params.setSyncDirection(SyncDirection.BIDIRECTIONAL);
    params.getLayerOptions().add(new SyncLayerOption(0));

    // attach listener
    SyncGeodatabaseJob geodatabaseSyncJob = geodatabaseSyncTask.syncGeodatabaseAsync(params, geodatabase);
    geodatabaseSyncJob.addJobDoneListener(() -> {
      handleError(geodatabaseSyncJob.getError());
      progressBar.setVisible(false);
    });

    // start sync
    progressBar.setVisible(true);
    geodatabaseSyncJob.start();
  }

  @FXML
  private void onExitClicked(ActionEvent exitClickEvent) {
    mapView.dispose();
    System.exit(0);
  }

  // shows a popup window if there is any error
  private void handleError(Throwable th) {
    if (th == null) {
      return;
    }
    Platform.runLater(() -> {
      Stage stage = new Stage();
      stage.setTitle("Error");
      Label lblError = new Label(th.getMessage());
      stage.setScene(new Scene(lblError, -1, -1));
      stage.show();
    });
  }
}
