/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/
package devsummit2015.getstarted;

/**
 * This sample shows the use of a map overlay to detect mouse-click.
 * On a mouse click, a buffer around the clicked point is drawn on top of map.
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.map.ArcGISFeatureLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOptions;
import com.esri.map.MapOverlay;
import com.esri.map.MapOptions.MapType;

public class OverlayApp {

  private JFrame window;
  private JMap map;

  private ArcGISFeatureLayer featureLayer ;
  private GraphicsLayer      graphicsLayer;

  private SimpleFillSymbol bufferSymbol =
      new SimpleFillSymbol(new Color(100, 0, 0, 80));

  public OverlayApp() {
    window = new JFrame();
    window.setSize(800, 600);
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

    //map options allow for a common base map to be chosen
    MapOptions mapOptions = new MapOptions(MapType.TOPO, 37.77279077295881, -96.44323104731787, 4);

    //create a map using the map options
    map = new JMap(mapOptions);
    window.getContentPane().add(map);


    // The code below shows how to add a tiled layer if you don't use MapOptions
    //ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
    // "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer");
    // map.getLayers().add(tiledLayer);

    featureLayer = 
        new ArcGISFeatureLayer("http://sampleserver1.arcgisonline.com/ArcGIS/rest/services/Demographics/ESRI_Census_USA/MapServer/5");
    map.getLayers().add(featureLayer);

    graphicsLayer = new GraphicsLayer();
    map.getLayers().add(graphicsLayer);

    map.addMapOverlay(mapOverlay);
  }

  private MapOverlay mapOverlay = new MapOverlay() {
    
    private static final long serialVersionUID = 1L;

    @Override
    public void onMouseClicked(MouseEvent e) {
      Point pt = map.toMapPoint(e.getX(), e.getY());

      Geometry buffer = GeometryEngine.buffer(pt, map.getSpatialReference(), 200000, map.getSpatialReference().getUnit());

      Graphic g = new Graphic(buffer, bufferSymbol);
      graphicsLayer.addGraphic(g);
    }
  };

  /**
   * @param args
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {

      @Override
      public void run() {
        try {
          OverlayApp application = new OverlayApp();
          application.window.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
