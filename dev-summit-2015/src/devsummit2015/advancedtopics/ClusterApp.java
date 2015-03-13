package devsummit2015.advancedtopics;
/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;

/**
 * This sample shows how to add different kinds of graphics to a graphics layer.
 */
public class ClusterApp {

  private JMap map;

  // Constructor
  public ClusterApp() { }
  
  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------
  private JMap createMap() {

    MapOptions mapOptions = new MapOptions(MapType.OCEANS, 34.0515888762, -117.190346717, 11);
    final JMap jMap = new JMap(mapOptions);
    
    // create the graphics layer
    final GraphicsLayer graphicsLayer = new GraphicsLayer();
    jMap.getLayers().add(graphicsLayer);
    
    // create the cluster layer
    final ClusterLayer clusterLayer = new ClusterLayer(graphicsLayer);
    jMap.getLayers().add(clusterLayer);
    
    jMap.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            addSimpleMarkerGraphics(graphicsLayer, jMap.getExtent());
          }
        });
      }
      
      @Override
      public void mapExtentChanged(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            clusterLayer.updateClusters();
          }
        });
      }
    });

    return jMap;
  }

  /**
   * Adds graphics symbolized with SimpleMarkerSymbols.
   * @param graphicsLayer
   */
  private void addSimpleMarkerGraphics(GraphicsLayer graphicsLayer, Envelope bounds) {
    SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(Color.RED, 16, Style.CIRCLE);
    double xmin = bounds.getXMin();
    double xmax = bounds.getXMax();
    double xrand;
    double ymin = bounds.getYMin();
    double ymax = bounds.getYMax();
    double yrand;
    for (int i = 0; i < 1000; i++) {
      xrand = xmin + (int) (Math.random() * ((xmax - xmin) + 1));
      yrand = ymin + (int) (Math.random() * ((ymax - ymin) + 1));
      Point point = new Point(xrand, yrand);
      graphicsLayer.addGraphic(new Graphic(point, symbol));
    }
    
  }

  // ------------------------------------------------------------------------
  // Static methods
  // ------------------------------------------------------------------------
  /**
   * Starting point of this application.
   * 
   * @param args
   *            arguments to this application.
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          // instance of this application
          ClusterApp addGraphicsApp = new ClusterApp();

          // create the UI, including the map, for the application.
          JFrame appWindow = addGraphicsApp.createWindow();
          appWindow.add(addGraphicsApp.createUI());
          appWindow.setVisible(true);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
  }

  // ------------------------------------------------------------------------
  // Public methods
  // ------------------------------------------------------------------------
  /**
   * Creates and displays the UI, including the map, for this application.
   * 
   * @return the UI component.
   */
  public JComponent createUI() {
    // application content
    JComponent contentPane = createContentPane();

    // map
    map = createMap();

    contentPane.add(map);

    return contentPane;
  }

  // ------------------------------------------------------------------------
  // Private methods
  // ------------------------------------------------------------------------
  /**
   * Creates a content pane.
   * 
   * @return a content pane.
   */
  private static JLayeredPane createContentPane() {
    JLayeredPane contentPane = new JLayeredPane();
    contentPane.setLayout(new BorderLayout(0, 0));
    contentPane.setVisible(true);
    return contentPane;
  }

  /**
   * Creates a window.
   * 
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("Cluster Layer Application");
    window.setBounds(100, 100, 1000, 700);
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.getContentPane().setLayout(new BorderLayout(0, 0));
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowEvent) {
        super.windowClosing(windowEvent);
        map.dispose();
      }
    });
    return window;
  }
}
