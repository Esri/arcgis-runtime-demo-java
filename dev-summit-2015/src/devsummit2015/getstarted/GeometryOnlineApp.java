/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/
package devsummit2015.getstarted;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.core.tasks.query.QueryTask;
import com.esri.map.ArcGISFeatureLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;
import com.esri.map.MapOverlay;

/**
 * This sample demonstrates use of map overlay and query task.
 * <p>
 * The overlay is used to detect mouse-click. On mouse-click, the geometry
 * engine is used to calculate a buffer around the clicked point. Then the query
 * task is used to retrieve states that intersect the buffer.
 */
public class GeometryOnlineApp {

  private JFrame window;
  private JMap map;
  private GraphicsLayer graphicsLayer;
  private ArcGISFeatureLayer featureLayer;
  private SimpleFillSymbol stateSymbol = new SimpleFillSymbol(new Color(0, 0, 255, 80));

  public GeometryOnlineApp() {
    window = new JFrame();
    //window.setSize(800, 600);
    window.setExtendedState(Frame.MAXIMIZED_BOTH);
    window.setLocationRelativeTo(null); // center on screen
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.getContentPane().setLayout(new BorderLayout(0, 0));

    // dispose map just before application window is closed.
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowEvent) {
        super.windowClosing(windowEvent);
        map.dispose();
      }
    });

    // center of USA
    MapOptions mapOptions = new MapOptions(MapType.TOPO, 37.77279077295881, -96.44323104731787, 3);

    //create a map using the map options
    map = new JMap(mapOptions);
    window.getContentPane().add(map);

    featureLayer = new ArcGISFeatureLayer("http://sampleserver1.arcgisonline.com/ArcGIS/rest/services/Demographics/ESRI_Census_USA/MapServer/5");
    map.getLayers().add(featureLayer);

    // The code below shows how to add a tiled layer if you don't use MapOptions
    //ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
    // "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
    // map.getLayers().add(tiledLayer);

    graphicsLayer = new GraphicsLayer();
    map.getLayers().add(graphicsLayer);

    map.addMapOverlay(onMouseClickOverlay);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {

      @Override
      public void run() {
        try {
          GeometryOnlineApp application = new GeometryOnlineApp();
          application.window.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  private MapOverlay onMouseClickOverlay = new MapOverlay() {

    private static final long serialVersionUID = 1L;

    @Override
    public void onMouseClicked(MouseEvent event) {
      graphicsLayer.removeAll();

      // add buffer as a graphic
      Point mapPoint = map.toMapPoint(event.getX(), event.getY());
      final Geometry buffer = GeometryEngine.buffer(
          mapPoint, map.getSpatialReference(), 200000, map.getSpatialReference().getUnit());
      graphicsLayer.addGraphic(new Graphic(buffer, new SimpleFillSymbol(new Color(255, 0, 0, 255))));

      // get states at the buffered area
      QueryTask queryTask = new QueryTask(featureLayer.getUrl());
      QueryParameters queryParams = new QueryParameters();
      queryParams.setInSpatialReference(map.getSpatialReference());
      queryParams.setOutSpatialReference(map.getSpatialReference());
      queryParams.setGeometry(buffer);
      queryParams.setReturnGeometry(true);
      queryParams.setOutFields(new String[] {"STATE_NAME"});

      queryTask.execute(queryParams, new CallbackListener<FeatureResult>() {

        @Override
        public void onError(Throwable arg0) {
          // deal with any exception
        }

        @Override
        public void onCallback(FeatureResult result) {
          for (Object objFeature : result) {
            Feature feature = (Feature) objFeature;
            graphicsLayer.addGraphic(new Graphic(feature.getGeometry(), stateSymbol));
            graphicsLayer.addGraphic(new Graphic(buffer, new SimpleFillSymbol(new Color(255, 0, 0, 255))));


          }
        }
      });
    }
  };
}