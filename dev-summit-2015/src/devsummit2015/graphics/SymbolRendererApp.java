/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/
package devsummit2015.graphics;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.esri.client.local.ArcGISLocalTiledLayer;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.SimpleRenderer;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.map.GraphicsLayer;
import com.esri.map.GraphicsLayer.RenderingMode;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.runtime.ArcGISRuntime;

public class SymbolRendererApp {

  private static final int MS_DELAY = 40; // how often to move points, in milliseconds
  private static Integer[] graphicNumOptions = 
    {new Integer(1000), new Integer(10000), new Integer(100000)};

  // Default symbols
  private static PictureMarkerSymbol symbol;
  private Random random = new Random();
  
  private JMap map;
  private Envelope mapExtent;
  private GraphicsLayer graphicsLayer;
  private JComboBox graphicsNumCombo;
  private HashMap<Integer, Point> latestDynamicPoints = new HashMap<Integer, Point>();
  
  private boolean addWithSymbol = true;
  
  private static JFrame appWindow;

  private static final String FSP = System.getProperty("file.separator");

  // Constructor
  public SymbolRendererApp() {
	  initSymbol();
  }
  
  private void initSymbol() {
	  URL url = getClass().getResource("taxi.png");
	    try {
	      BufferedImage image = ImageIO.read(url);
	      symbol = new PictureMarkerSymbol(image);
	      symbol.setSize(20, 20);
	    }
	    catch (Exception e) {
	      System.err.println("unable to create picture marker symbol");
	      //return new SimpleMarkerSymbol(Color.YELLOW, 12, Style.CIRCLE);
	    }
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
    graphicsLayer = new GraphicsLayer(RenderingMode.DYNAMIC);
    // set a renderer for the graphics layer
    SimpleRenderer particleRenderer = new SimpleRenderer(symbol);
    graphicsLayer.setRenderer(particleRenderer);
    jMap.getLayers().add(graphicsLayer);

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

  private void addGraphics(int numberOfGraphicsToAdd) {

    double minx = mapExtent.getXMin();
    double maxx = mapExtent.getXMax();
    double miny= mapExtent.getYMin();
    double maxy = mapExtent.getYMax();

    int i = 0;
    long startTime = System.currentTimeMillis();
    while (i < numberOfGraphicsToAdd) {
      Point point = new Point((random.nextFloat()*(maxx-minx)) + minx, (random.nextFloat()*(maxy-miny)) + miny);
      int id;
      if (addWithSymbol) {
    	  id = graphicsLayer.addGraphic(new Graphic(point, symbol));
      } else {
    	  id = graphicsLayer.addGraphic(new Graphic(point, null));  
      }
      latestDynamicPoints.put(Integer.valueOf(id), point);
      i++;
    }
    JOptionPane.showMessageDialog(appWindow, "Total time: " + (System.currentTimeMillis() - startTime)/1000.0 + " seconds.");
  }
  
  private void addGraphicsBulk(int numberOfGraphicsToAdd) {

	    double minx = mapExtent.getXMin();
	    double maxx = mapExtent.getXMax();
	    double miny= mapExtent.getYMin();
	    double maxy = mapExtent.getYMax();

	    int i = 0;
	    long startTime = System.currentTimeMillis();
	    int BATCH_SIZE = 1000;
	    Graphic[] batchGraphics = new Graphic[BATCH_SIZE];
	    int c = 0;
	    while (i < numberOfGraphicsToAdd) {
	    	
	      Point point = new Point((random.nextFloat()*(maxx-minx)) + minx, (random.nextFloat()*(maxy-miny)) + miny);
	      int id;
	      if (addWithSymbol) {
	    	  batchGraphics[c++] = new Graphic(point, symbol);
	      }
	      if (c == 100) {
	    	  graphicsLayer.addGraphics(batchGraphics);
	    	  c = 0;
	      }
	      
	      //latestDynamicPoints.put(Integer.valueOf(id), point);
	      i++;
	    }
	    JOptionPane.showMessageDialog(appWindow, "Total time: " + (System.currentTimeMillis() - startTime)/1000.0 + " seconds.");
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
          SymbolRendererApp addGraphicsApp = new SymbolRendererApp();

          // create the UI, including the map, for the application.
          appWindow = addGraphicsApp.createWindow();
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

    // static button
    JButton addStaticButton = new JButton("Add using Symbol");
    addStaticButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int numGraphicsToAdd = ((Integer)graphicsNumCombo.getSelectedItem()).intValue();
        addWithSymbol = true;
        addGraphics(numGraphicsToAdd);
      }
    });
    tb.add(addStaticButton);
    
    JButton addBulk = new JButton("Bulk Add using Symbol");
    addBulk.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int numGraphicsToAdd = ((Integer)graphicsNumCombo.getSelectedItem()).intValue();
        addWithSymbol = true;
        addGraphicsBulk(numGraphicsToAdd);
      }
    });
    //tb.add(addBulk);

    // dynamic button
    JButton addButton = new JButton("Add using Renderer");
    addButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int numGraphicsToAdd = ((Integer)graphicsNumCombo.getSelectedItem()).intValue();
        addWithSymbol = false;
        addGraphics(numGraphicsToAdd);
      }
    });
    tb.add(addButton);
    
    graphicsNumCombo = new JComboBox(graphicNumOptions);
    graphicsNumCombo.setMaximumSize(new Dimension(100, 30));
    graphicsNumCombo.setSelectedIndex(0);
    tb.add(graphicsNumCombo);

    JButton clearGraphicsButton = new JButton("Clear graphics");
    clearGraphicsButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        graphicsLayer.removeAll();
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
    JFrame window = new JFrame("Symbol Vs Renderer");
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
