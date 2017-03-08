/*
 * Copyright 2017 Esri.
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

package com.esri.arcgisruntime.example;

import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;

import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.core.geometry.*;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;


public class App extends Application {

    private MapView mapView;

    // callout show and hide animation duration
    private static final Duration DURATION = new Duration(50);

    @Override
    public void start(Stage stage) throws Exception {

        try {
            // create stack pane and application scene
            StackPane stackPane = new StackPane();
            Scene scene = new Scene(stackPane);

            // set title, size, and add scene to stage
            stage.setTitle("Show Callout Sample");
            stage.setWidth(800);
            stage.setHeight(700);
            stage.setScene(scene);
            stage.show();

            // create ArcGISMap with vector navigation basemap
            ArcGISMap map = new ArcGISMap(Basemap.Type.NAVIGATION_VECTOR, 47.609201, -122.331597, 14);
            // create a view and set map to it
            mapView = new MapView();
            mapView.setMap(map);

            // create 6 station points from geojson string
            String station1 = "{\"type\":\"Point\",\"coordinates\":[-122.32799470424652,47.59850568873259],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station1Pnt = createPointFromGeoJson(station1);
            String station2 = "{\"type\":\"Point\",\"coordinates\":[-122.33112752437592,47.602473763740484],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station2Pnt = createPointFromGeoJson(station2);
            String station3 = "{\"type\":\"Point\",\"coordinates\":[-122.33570337295531,47.60749401439728],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station3Pnt = createPointFromGeoJson(station3);
            String station4 = "{\"type\":\"Point\",\"coordinates\":[-122.33618617057799,47.61182666756116],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station4Pnt = createPointFromGeoJson(station4);
            String station5 = "{\"type\":\"Point\",\"coordinates\":[-122.32020020484924,47.61901562056099],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station5Pnt = createPointFromGeoJson(station5);
            String station6 = "{\"type\":\"Point\",\"coordinates\":[-122.30383872985841,47.64981422491055],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station6Pnt = createPointFromGeoJson(station6);

            // create color and symbols for drawing graphics
            SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, 0xffff0000, 14);

            // create a graphics overlay to display stations
            GraphicsOverlay overlay = new GraphicsOverlay();
            // create station graphics
            Graphic station1Graphic = new Graphic(station1Pnt.getY(), station1Pnt.getX());
            Graphic station2Graphic = new Graphic(station2Pnt.getY(), station2Pnt.getX());
            Graphic station3Graphic = new Graphic(station3Pnt.getY(), station3Pnt.getX());
            Graphic station4Graphic = new Graphic(station4Pnt.getY(), station4Pnt.getX());
            Graphic station5Graphic = new Graphic(station5Pnt.getY(), station5Pnt.getX());
            Graphic station6Graphic = new Graphic(station6Pnt.getY(), station6Pnt.getX());
            // set the maker symbols
            station1Graphic.setSymbol(markerSymbol);
            station2Graphic.setSymbol(markerSymbol);
            station3Graphic.setSymbol(markerSymbol);
            station4Graphic.setSymbol(markerSymbol);
            station5Graphic.setSymbol(markerSymbol);
            station6Graphic.setSymbol(markerSymbol);
            // add the graphics to the overlay
            mapView.getGraphicsOverlays().add(overlay);
            overlay.getGraphics().add(station1Graphic);
            overlay.getGraphics().add(station2Graphic);
            overlay.getGraphics().add(station3Graphic);
            overlay.getGraphics().add(station4Graphic);
            overlay.getGraphics().add(station5Graphic);
            overlay.getGraphics().add(station6Graphic);

            // click event to display the callout
            mapView.setOnMouseClicked(e -> {
                // check that the primary mouse button was clicked and user is not panning
                if (e.isStillSincePress() && e.getButton() == MouseButton.PRIMARY) {
                    // create a point from where the user clicked
                    Point2D point = new Point2D(e.getX(), e.getY());
                    // create a map point from a point
                    Point mapPoint = mapView.screenToLocation(point);
                    // get the map view's callout
                    Callout callout = mapView.getCallout();
                    // set callout title
                    callout.setTitle("Location");
                    // convert to WGS84 for lat/lon format
                    Point geo = (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());
                    // convert to degrees minutes seconds
                    String convertY = convertCoordinate(geo.getY(), true);
                    String convertX = convertCoordinate(geo.getX(), false);
                    // add converted coordinates to callout
                    callout.setDetail(convertY + " : " + convertX);
                    // show call out where user clicks on map
                    callout.showCalloutAt(mapPoint, DURATION);
                    mapView.setViewpointCenterAsync(mapPoint);
                }
            });

            // add map view and control panel to stack pane
            stackPane.getChildren().addAll(mapView);

        } catch (Exception e) {
            // on any error, print the stack trace
            e.printStackTrace();
        }
    }

    /**
     * Stops and releases all resources used in application.
     */
    @Override
    public void stop() throws Exception {

        // release resources when the application closes
        if (mapView != null) {
            mapView.dispose();
        }
    }

    /**
     * Opens and runs application.
     *
     * @param args arguments passed to this application
     */
    public static void main(String[] args) {

        Application.launch(args);
    }

    /**
     * Converts a geojson string to com.esri.core.geometry.Point.
     *
     * @param jsonPoint geoJson string representation of a Point
     * @return com.esri.core.geometry.Point
     * @throws Exception
     */
    static com.esri.core.geometry.Point createPointFromGeoJson(String jsonPoint) throws Exception {

        MapGeometry mapGeom = OperatorImportFromGeoJson.local().execute(GeoJsonImportFlags.geoJsonImportDefaults,
                Geometry.Type.Point,
                jsonPoint,
                null);

        return (com.esri.core.geometry.Point) mapGeom.getGeometry();
    }

    private com.esri.core.geometry.Point createPoint(){
        com.esri.core.geometry.Point point = new com.esri.core.geometry.Point(47.608629, -122.336769);
        return point;
    }

    /**
     * Converts a degree formatted location coordinate into
     * degrees minutes seconds for display
     *
     * @param coordinate location in degrees format
     * @param isLatitude true if Y, false if X
     * @return String formatted in degress minutes seconds
     */
    private String convertCoordinate(double coordinate, boolean isLatitude){
        String[] pString = isLatitude ? new String[]{"N", "S"} :  new String[]{"E", "W"};
        int degree = (int) coordinate;
        int minute = (int) (Math.abs(coordinate) * 60) % 60;
        int second = (int) (Math.abs(coordinate) * 3600) % 60;

        int index = degree < 0 ? 1 : 0;
        degree = Math.abs(degree);

        return degree + pString[index] + " " + minute + "' " + second + "\"";
    }
}
