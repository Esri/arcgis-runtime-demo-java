/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/
package devsummit2015.graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.esri.client.local.ArcGISLocalTiledLayer;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.SimpleRenderer;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.map.GraphicsLayer;
import com.esri.map.GraphicsLayer.RenderingMode;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.runtime.ArcGISRuntime;

public class MoveGraphicsApp {

  private static final int MS_DELAY = 40; // how often to move points, in milliseconds
  private static Integer[] graphicNumOptions = 
    {new Integer(1000), new Integer(10000), new Integer(100000)};

  // Default symbols
  private SimpleMarkerSymbol  dynamicSymbol = new SimpleMarkerSymbol(Color.RED, 5, Style.CIRCLE);

  //timer to move things
  private Timer actionTimer;
  private Random random = new Random();
  
  private JMap map;
  private Envelope mapExtent;
  private GraphicsLayer dynamicGraphicsLayer;
  private JComboBox graphicsNumCombo;
  private HashMap<Integer, Point> latestDynamicPoints = new HashMap<Integer, Point>();

  private static final String FSP = System.getProperty("file.separator");
  
  private boolean moveWithReplace = true;

  // Constructor
  public MoveGraphicsApp() {
  }

  private JMap createMap() {

    JMap jMap = new JMap();
    jMap.setWrapAroundEnabled(true);

    jMap.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {
            System.out.println("map is ready");
            mapExtent = map.getFullExtent();
          }
        });
      }
    });

    ArcGISLocalTiledLayer tiledLayer =
        new ArcGISLocalTiledLayer(getPathSampleData() + "tpks" + FSP + "Topographic.tpk");
    jMap.getLayers().add(tiledLayer);

    // create a dynamic graphics layer
    dynamicGraphicsLayer = new GraphicsLayer(RenderingMode.DYNAMIC);
    // set a renderer for the graphics layer
    SimpleRenderer particleRenderer = new SimpleRenderer(dynamicSymbol);
    dynamicGraphicsLayer.setRenderer(particleRenderer);
    jMap.getLayers().add(dynamicGraphicsLayer);

    ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        // move all the points in the graphics layers
        movePoints();
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

  private void movePoints() {
    // loop through them all
    if (dynamicGraphicsLayer.getGraphicIDs()!=null) {
      for (int id : dynamicGraphicsLayer.getGraphicIDs()) {
        Point p1 = latestDynamicPoints.get(Integer.valueOf(id));
        Point p2 = getPointForSmoothMove(p1.getX(), p1.getY(), id);
        if (moveWithReplace) {
        	dynamicGraphicsLayer.removeGraphic(id);
        	int newId = dynamicGraphicsLayer.addGraphic(new Graphic(p2, null));
            latestDynamicPoints.put(Integer.valueOf(newId), p2);
        } else {
        	dynamicGraphicsLayer.updateGraphic(id, p2);
            //dynamicGraphicsLayer.movePointGraphic(id, p2);
            latestDynamicPoints.put(Integer.valueOf(id), p2);
        }
        
        //      dynamicGraphicsLayer.movePointGraphic(id, getRandomPointFrom(p.getX(), p.getY(), spreadOfMove));
      }
    }
  }

  /**
   * Smooth mostly x-based motion
   * @param x
   * @param y
   * @param id
   * @return a new Point location
   */
  private Point getPointForSmoothMove(double x, double y, int id) {
    return new Point(x + (Math.cos(id * 0.1) * 50000), y + (Math.sin(id * .1) * 10000));
  }

  private void addDynamicGraphics(int numberOfGraphicsToAdd) {

    double minx = mapExtent.getXMin();
    double maxx = mapExtent.getXMax();
    double miny= mapExtent.getYMin();
    double maxy = mapExtent.getYMax();

    int i = 0;
    while (i < numberOfGraphicsToAdd) {
      Point point = new Point((random.nextFloat()*(maxx-minx)) + minx, (random.nextFloat()*(maxy-miny)) + miny);
      int id = dynamicGraphicsLayer.addGraphic(new Graphic(point, null));
      latestDynamicPoints.put(Integer.valueOf(id), point);
      i++;
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
          MoveGraphicsApp addGraphicsApp = new MoveGraphicsApp();

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
    JComponent contentPane = createContentPane();

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

    // dynamic button
    JButton addButton = new JButton("Add graphics (dynamic)");
    addButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int numGraphicsToAdd = ((Integer)graphicsNumCombo.getSelectedItem()).intValue();
        addDynamicGraphics(numGraphicsToAdd);
      }
    });
    tb.add(addButton);
    
    graphicsNumCombo = new JComboBox(graphicNumOptions);
    graphicsNumCombo.setMaximumSize(new Dimension(100, 30));
    graphicsNumCombo.setSelectedIndex(0);
    tb.add(graphicsNumCombo);

    JButton moveButton = new JButton("Move with Replace");
    moveButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
    	moveWithReplace = true;
        actionTimer.start();
      }
    });
    tb.add(moveButton);
    
    JButton moveUpdateButton = new JButton("Move with Update");
    moveUpdateButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
      	moveWithReplace = false;
        actionTimer.start();
      }
    });
    tb.add(moveUpdateButton);

    JButton clearGraphicsButton = new JButton("Clear graphics");
    clearGraphicsButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        actionTimer.stop();
        dynamicGraphicsLayer.removeAll();
      }
    });
    tb.add(clearGraphicsButton);

    return tb;
  }

  /**
   * Creates a content pane.
   * 
   * @return a content pane.
   */
  private static JPanel createContentPane() {
    JPanel contentPane = new JPanel();
    contentPane.setLayout(new BorderLayout());
    contentPane.setVisible(true);
    return contentPane;
  }

  /**
   * Creates the application window.
   * 
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("Add and Move Graphics Application");
    window.setSize(1000, 1000);
    window.setLocationRelativeTo(null);
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.getContentPane().setLayout(new BorderLayout(0, 0));
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
}
