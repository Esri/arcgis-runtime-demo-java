package demo;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.datasource.Feature;
import com.esri.arcgisruntime.datasource.FeatureQueryResult;
import com.esri.arcgisruntime.datasource.QueryParameters;
import com.esri.arcgisruntime.datasource.arcgis.FeatureEditResult;
import com.esri.arcgisruntime.datasource.arcgis.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.*;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;

public class MonitorController {
  @FXML
  private SceneView sceneView;

  @FXML
  private TextField searchBox;

  @FXML
  private Button truckButton;

  @FXML
  private Button helicopterButton;

  @FXML
  private ToggleButton flagButton;

  private static GraphicsOverlay groundGraphicsOverlay;
  private static GraphicsOverlay buildingGraphicsOverlay;
  private static GraphicsOverlay airGraphicsOverlay;
  private static Point fireLocation;
  private static PolygonBuilder zoneBuilder;
  private static Graphic zone;
  private static Graphic helicopter;
  private LocatorTask locatorTask;
  private static ListenableFuture<List<GeocodeResult>> geocodeResult;
  private static ServiceFeatureTable pointsFeatureTable;
  private static ServiceFeatureTable polysFeatureTable;

  public void initialize() {

    // create a scene
    ArcGISScene scene = new ArcGISScene();
    scene.setBasemap(Basemap.createImagery());
    sceneView.setArcGISScene(scene);

    // add base surface for elevation data
    String surfaceURL = "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer";
    Surface surface = new Surface();
    surface.getElevationSources().add(new ArcGISTiledElevationSource(surfaceURL));
    scene.setBaseSurface(surface);

    // add a camera
    Camera camera = new Camera(33.98, -117.177526, 5000, 0, 70, 0.0);
    sceneView.setViewpointCamera(camera);

    // get features from an online feature layer
    String pointsURL = "http://sampleserver6.arcgisonline.com/arcgis/rest/services/Sync/WildfireSync/FeatureServer/0";
    pointsFeatureTable = new ServiceFeatureTable(pointsURL);
    FeatureLayer pointsFeatureLayer = new FeatureLayer(pointsFeatureTable);
    scene.getOperationalLayers().add(pointsFeatureLayer);

    String polysURL = "http://sampleserver6.arcgisonline.com/arcgis/rest/services/Sync/WildfireSync/FeatureServer/2";
    polysFeatureTable = new ServiceFeatureTable(polysURL);
    FeatureLayer polysFeatureLayer = new FeatureLayer(polysFeatureTable);
    scene.getOperationalLayers().add(polysFeatureLayer);

    // add graphics overlays
    groundGraphicsOverlay = new GraphicsOverlay();
    groundGraphicsOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.DRAPED);
    sceneView.getGraphicsOverlays().add(groundGraphicsOverlay);

    buildingGraphicsOverlay = new GraphicsOverlay();
    SimpleRenderer renderer = new SimpleRenderer();
    Renderer.SceneProperties renderProperties = renderer.getSceneProperties();
    renderProperties.setExtrusionMode(Renderer.SceneProperties.ExtrusionMode.BASE_HEIGHT);
    renderProperties.setExtrusionExpression("height");
    buildingGraphicsOverlay.setRenderer(renderer);
    buildingGraphicsOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
    sceneView.getGraphicsOverlays().add(buildingGraphicsOverlay);

    airGraphicsOverlay = new GraphicsOverlay();
    airGraphicsOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.ABSOLUTE);
    sceneView.getGraphicsOverlays().add(airGraphicsOverlay);

    // create an online geolocator task
    String locatorURL = "http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer";
    locatorTask = new LocatorTask(locatorURL);
  }

  @FXML
  public void getGeocode() {

    // get the address from the search box
    String address = searchBox.getText();

    // create geocode parameters
    GeocodeParameters geocodeParameters = new GeocodeParameters();
    geocodeParameters.getResultAttributeNames().add("*");
    geocodeParameters.setMaxResults(1);
    geocodeParameters.setOutputSpatialReference(sceneView.getSpatialReference());

    // get the geocode for the address
    geocodeResult = locatorTask.geocodeAsync(address, geocodeParameters);
    geocodeResult.addDoneListener(this::markFire);
  }

  private void markFire() {

    try {
      List<GeocodeResult> results = geocodeResult.get();

      if (results.size() > 0) {
        // get the geocode location
        fireLocation = results.get(0).getDisplayLocation();

        // set attributes for a fire feature
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("eventtype", 7); // fire origin
        attributes.put("description", "fire");

        // create a new feature and add it to the table
        Feature fire = pointsFeatureTable.createFeature(attributes, fireLocation);
        pointsFeatureTable.addFeatureAsync(fire);

        truckButton.setDisable(false);
      }

    } catch (InterruptedException | ExecutionException exception) {
      exception.printStackTrace();
    }

  }

  @FXML
  private void dispatchFireTruck() {

    // apply the changes to the server
    ListenableFuture<List<FeatureEditResult>> editResult = pointsFeatureTable.applyEditsAsync();
    editResult.addDoneListener(() -> {
      try {
        // check if the server edit was successful
        List<FeatureEditResult> edits = editResult.get();
        if (edits != null && edits.size() > 0 && !edits.get(0).hasCompletedWithErrors()) {
          displayMessage("Report sent successfully");
        }
      } catch (InterruptedException | ExecutionException exception) {
        exception.printStackTrace();
      }
    });

    truckButton.setDisable(true);
    helicopterButton.setDisable(false);
  }

  @FXML
  private void spawnHelicopter() {

    try {
      // Find a nearby helipad
      QueryParameters params = new QueryParameters();
      params.setGeometry(GeometryEngine.buffer(fireLocation, 3));
      params.setOutSpatialReference(SpatialReferences.getWgs84());
      params.setWhereClause("eventtype == 10"); //helibase
      FeatureQueryResult queryResult = pointsFeatureTable.queryFeaturesAsync(params).get();
      Feature helipad = queryResult.iterator().next();
      Point helipadLocation = (Point) helipad.getGeometry();

      // create a helicopter distance composite scene symbol graphic
      SimpleMarkerSymbol circleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFF0000FF, 10);
      String modelURI = Paths.get(getClass().getResource("/helicopter/SkyCrane.lwo").toURI()).toString();
      ModelSceneSymbol modelSymbol = new ModelSceneSymbol(modelURI, 0.01);

      DistanceCompositeSceneSymbol compositeSymbol = new DistanceCompositeSceneSymbol();
      compositeSymbol.getRangeCollection().add(new DistanceCompositeSceneSymbol.Range(modelSymbol, 0, 2000));
      compositeSymbol.getRangeCollection().add(new DistanceCompositeSceneSymbol.Range(circleSymbol, 2000, 0));

      // add helicopter graphic
      Point spawnPoint = new Point(helipadLocation.getX(), helipadLocation.getY(), 500, helipadLocation.getSpatialReference());
      helicopter = new Graphic(spawnPoint, compositeSymbol);
      airGraphicsOverlay.getGraphics().add(helicopter);

      dispatchHelicopter();

    } catch (InterruptedException | ExecutionException | URISyntaxException exception) {
      exception.printStackTrace();
    }

  }

  private void dispatchHelicopter() {
    // create an animation timer
    Timeline animator = new Timeline();
    animator.setCycleCount(Animation.INDEFINITE);

    // specify units
    LinearUnit meters = new LinearUnit(LinearUnitId.METERS);
    AngularUnit degrees = new AngularUnit(AngularUnitId.DEGREES);

    animator.getKeyFrames().add(new KeyFrame(Duration.millis(100), actionEvent -> {
      // get the geodesic distance
      GeodesicDistanceResult distance = GeometryEngine.distanceGeodesic((Point) helicopter.getGeometry(), fireLocation,
          meters, degrees, GeodeticCurveType.GEODESIC);

      if (distance.getDistance() > 40) {
        // move helicopter 10 meters towards target
        Point newPosition = GeometryEngine.moveGeodesic((Point) helicopter.getGeometry(), 40, meters, distance
            .getAzimuth1(), degrees, GeodeticCurveType.GEODESIC);
        helicopter.setGeometry(newPosition);
      } else {
        animator.stop();
        helicopterButton.setDisable(true);
        flagButton.setDisable(false);
      }
    }));
    animator.play();
  }

  @FXML
  private void flagFireZone() {
    // create a red point symbol
    SimpleMarkerSymbol clickSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFFF0000, 2);
    SimpleFillSymbol zoneSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.FORWARD_DIAGONAL, 0xFFFF0000, new
        SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFF0000, 2));
    zoneBuilder = new PolygonBuilder(polysFeatureTable.getSpatialReference());

    zone = new Graphic();
    zone.getAttributes().put("height", 30);
    zone.setSymbol(zoneSymbol);
    buildingGraphicsOverlay.getGraphics().add(zone);

    if (flagButton.isSelected()) {

      // add click listener
      sceneView.setOnMouseClicked(event -> {
        if (event.isStillSincePress() && event.getButton() == MouseButton.PRIMARY) {

          Point2D screenPoint = new Point2D(event.getX(), event.getY());

          Point click = sceneView.screenToBaseSurface(screenPoint);
          click = (Point) GeometryEngine.project(click, polysFeatureTable.getSpatialReference());

          groundGraphicsOverlay.getGraphics().add(new Graphic(click, clickSymbol));

          zoneBuilder.addPoint(click);

          if (zoneBuilder.getParts().get(0).getPointCount() > 3) {
            zone.setGeometry(zoneBuilder.toGeometry());
          }
        }
      });

    } else {
      // remove listener
      sceneView.setOnMouseClicked(null);
      flagButton.setDisable(true);
    }
  }

  private static void displayMessage(String message) {
    Platform.runLater(() -> {
      Alert dialog = new Alert(Alert.AlertType.INFORMATION);
      dialog.setHeaderText(null);
      dialog.setContentText(message);
      dialog.showAndWait();
    });
  }
}
