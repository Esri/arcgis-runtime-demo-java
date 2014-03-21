/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/
package graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.esri.client.local.ArcGISLocalTiledLayer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.SimpleRenderer;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.runtime.ArcGISRuntime;

public class ParticleGraphicsEllipseApp {

  private JMap map;

  // settings
  private int numEllipses = 200;
  private int pointCount = 1000;
  private static final int MS_DELAY = 30; // how often to move points, in milliseconds
  private ArrayList<Ellipse> ellipses = new ArrayList<Ellipse>(numEllipses);

  // Default symbols
  private SimpleMarkerSymbol symbol1 = new SimpleMarkerSymbol(new Color(15, 15, 15), 4, Style.CIRCLE);
  private SimpleMarkerSymbol  symbol2 = new SimpleMarkerSymbol(new Color(212, 20, 20), 5, Style.CIRCLE);
  private SimpleMarkerSymbol symbol3 = new SimpleMarkerSymbol(new Color(255, 128, 0), 5, Style.CIRCLE);
  private SimpleMarkerSymbol  symbol4 = new SimpleMarkerSymbol(new Color(255, 239, 0), 6, Style.CIRCLE);
  private SimpleMarkerSymbol centerSymbol = new SimpleMarkerSymbol(new Color(0, 0, 0, 180), 8, Style.CIRCLE);

  private Timer actionTimer;
  private Random random = new Random();
  private GraphicsLayer ellipseGraphicsLayer;
  private GraphicsLayer centerGraphicsLayer;
  private JLabel graphicsLabel;
  private Point centerPoint;

  private static final String FSP = System.getProperty("file.separator");

  // Constructor
  public ParticleGraphicsEllipseApp() { }

  private JMap createMap() {

    JMap jMap = new JMap();
    jMap.setWrapAroundEnabled(true);

    ArcGISLocalTiledLayer tiledLayer =
        new ArcGISLocalTiledLayer(getPathSampleData() + "tpks" + FSP + "Topographic.tpk");
    jMap.getLayers().add(tiledLayer);

    // graphics layer for center point
    centerGraphicsLayer = new GraphicsLayer();
    centerGraphicsLayer.setRenderer(new SimpleRenderer(centerSymbol));
    jMap.getLayers().add(centerGraphicsLayer);

    // graphics layer for all our moving points
    ellipseGraphicsLayer = new GraphicsLayer();
    jMap.getLayers().add(ellipseGraphicsLayer);

    ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        // move all the points!
        movePointsOnEllipses();
      }
    };

    // set up timer
    actionTimer = new Timer(MS_DELAY , actionListener);
    actionTimer.setRepeats(true);

    return jMap;
  }

  private String getPathSampleData() {
    String dataPath = null;
    String javaPath = ArcGISRuntime.getInstallDirectory();
    if (javaPath != null) {
      if (!(javaPath.endsWith("/") || javaPath.endsWith("\\"))){
        javaPath += FSP;
      }
      dataPath = javaPath + "sdk" + FSP + "samples" + FSP + "data" + FSP;
    }
    File dataFile = new File(dataPath);
    if (!dataFile.exists()) { 
      dataPath = ".." + FSP + "data" + FSP;
    }
    return dataPath;
  }

  private void movePointsOnEllipses() {
    // loop through them all
    for (Ellipse e : ellipses) {
      e.updateAllPoints();
    }
  }

  private void addEllipses(int numberOfEllipses) {
    SimpleMarkerSymbol[] symbols = {symbol4, symbol3, symbol2, symbol1};
    // some values that works well
    int majorAxisLength = 4612483;
    int minorAxisLength = 1843676;

    centerPoint = new Point(500000-5000000, 500000 + 3000000);
    centerGraphicsLayer.addGraphic(new Graphic(centerPoint, null));

    for (int i = 0; i < numberOfEllipses; i++) {
      Point center = new Point(random.nextInt(500000)-5000000, random.nextInt(500000) + 3000000);
      int majorAxisDirection = random.nextInt(60);
      LinearUnit unit = new LinearUnit(LinearUnit.Code.METER);

      Geometry ellipse = GeometryEngine.geodesicEllipse(center, map.getSpatialReference(), majorAxisLength, minorAxisLength,
          majorAxisDirection, pointCount, unit, Geometry.Type.MULTIPOINT);

      if (ellipse instanceof MultiPoint) {
        SimpleMarkerSymbol symbol = symbols[i%4];
        MultiPoint mp = (MultiPoint) ellipse;
        Ellipse currentEllipse = new Ellipse(mp); 
        ellipses.add(currentEllipse);
        int j = 0;
        while (j < mp.getPointCount()) {
          if (j%8==0) {
            Point point = mp.getPoint(j);
            int id = ellipseGraphicsLayer.addGraphic(new Graphic(point, symbol));
            currentEllipse.addPoint(id, j);
          }
          j++;
        }
      }
    }
  }

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
          ParticleGraphicsEllipseApp addGraphicsApp = new ParticleGraphicsEllipseApp();

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

  /**
   * Creates and displays the UI, including the map, for this application.
   * 
   * @return the UI component.
   */
  public JComponent createUI() {
    // application content
    JPanel contentPane = createContentPane();

    // UI panel
    final JToolBar addGraphicsPanel = createToolBar();
    contentPane.add(addGraphicsPanel, BorderLayout.NORTH);

    // map
    map = createMap();
    contentPane.add(map);

    return contentPane;
  }

  private JToolBar createToolBar() {
    JToolBar tb = new JToolBar();

    // ellipse button
    JButton ellipseButton = new JButton("Add ellipses");
    ellipseButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        addEllipses(numEllipses);
        graphicsLabel.setText(new Integer(ellipseGraphicsLayer.getNumberOfGraphics()).toString());
      }
    });
    tb.add(ellipseButton);

    JButton moveButton = new JButton("Move graphics");
    moveButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        actionTimer.start();
      }
    });
    tb.add(moveButton);

    JButton clearGraphicsButton = new JButton("Clear graphics");
    clearGraphicsButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        actionTimer.stop();
        ellipseGraphicsLayer.removeAll();
        graphicsLabel.setText("0");
      }
    });
    tb.add(clearGraphicsButton);


    JLabel label = new JLabel("  Number of graphics: ");
    tb.add(label);

    graphicsLabel = new JLabel();
    Dimension dim = new Dimension(120, 30);
    graphicsLabel.setSize(dim);
    graphicsLabel.setPreferredSize(dim);
    graphicsLabel.setMaximumSize(dim);
    graphicsLabel.setMinimumSize(dim);
    tb.add(graphicsLabel);

    return tb;
  }

  private static JPanel createContentPane() {
    JPanel contentPane = new JPanel();
    contentPane.setLayout(new BorderLayout());
    contentPane.setVisible(true);
    return contentPane;
  }

  private JFrame createWindow() {
    JFrame window = new JFrame("Particle Graphics Application");
    window.setSize(900, 900);
    window.setLocationRelativeTo(null);
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.getContentPane().setLayout(new BorderLayout());
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowEvent) {
        super.windowClosing(windowEvent);
        if (map != null) {
          map.dispose();
        }
      }
    });
    return window;
  }

  // Note: boxing and unboxing warnings left to improve code readability 
  class Ellipse {
    private MultiPoint ellipse;
    private int numberOfPoints;
    private Map<Integer, Integer> idToIndex;

    Ellipse(MultiPoint ellipse) {
      this.ellipse = ellipse;
      numberOfPoints = ellipse.getPointCount();
      idToIndex = new LinkedHashMap<Integer, Integer>(pointCount);
    }

    public void updateAllPoints() {
      for (Entry<Integer, Integer> e : idToIndex.entrySet()) {
        int id = e.getKey();
        ellipseGraphicsLayer.movePointGraphic(id, getNextPoint(e.getValue()));
        updateIndex(id);
      }
    }

    public void addPoint(int id, int index) {
      idToIndex.put(id, index);
    }

    private void updateIndex(int id) {
      int prevIndex = idToIndex.get(id);
      if (prevIndex+1 == numberOfPoints) {
        idToIndex.put(id, 0);
      } else {
        idToIndex.put(id, prevIndex+1);
      }
    }

    /**
     * Returns the Point to move to from the current index passed in.
     * @param index current position in ellipse
     */
    private Point getNextPoint(int index) {
      if (index < 0 || index >= numberOfPoints) {
        return new Point(0, 0); // or thrown an error
      } else if (index+1 == numberOfPoints) {
        return ellipse.getPoint(0);
      } else {
        return ellipse.getPoint(index + 1);
      }
    }
  }
}
