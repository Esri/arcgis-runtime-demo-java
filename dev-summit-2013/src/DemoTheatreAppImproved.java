/*
Copyright 2012 Esri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.​
*/
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import com.esri.client.toolkit.overlays.InfoPopupOverlay;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.UniqueValueInfo;
import com.esri.core.renderer.UniqueValueRenderer;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.symbol.Symbol;
import com.esri.core.tasks.ags.geoprocessing.GPFeatureRecordSetLayer;
import com.esri.core.tasks.ags.geoprocessing.GPParameter;
import com.esri.core.tasks.ags.geoprocessing.GPString;
import com.esri.core.tasks.ags.geoprocessing.Geoprocessor;
import com.esri.core.tasks.ags.query.OrderByFields;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.ags.query.QueryTask;
import com.esri.map.ArcGISFeatureLayer;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;

/**
 * Application for Demo Theatre at Esri Dev Summit 2013. 
 * 
 * The first version checked-in is a copy of the DemoTheatreApp. Check-ins will be made
 * to show a step-by-step improvement of this application.
 */
public class DemoTheatreAppImproved {

  // JMap
  private JMap map;

  // progress bar
  private JProgressBar progressBar;

  private GraphicsLayer graphicsLayer;

  private ArcGISFeatureLayer featureLayer;

  private AtomicInteger tasksInProgress = new AtomicInteger(0);

  private JTextField queryText = new JTextField("100000");

  private DefaultTableModel tblModelCities;

  private MouseActionHandler mouseActionHandler;

  private InfoPopupOverlay infoPopupOverlay;

  // ------------------------------------------------------------------------
  // Static methods
  // ------------------------------------------------------------------------
  /**
   * Starting point of this application.
   * 
   * @param args arguments to this application.
   */
  public static void main(String[] args) {

    // Tip: Use HTTP request intercepter such as Fiddler to track requests
    // ProxySetup.setupProxy("localhost", 8888);

    // create the UI, including the map, for the application
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          DemoTheatreAppImproved app = new DemoTheatreAppImproved();
          JFrame appWindow = app.createWindow();
          appWindow.add(app.createUI());
          appWindow.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
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
   * @return the UI for this sample.
   */
  public JComponent createUI() {
    // application content
    JComponent contentPane = createContentPane();

    // progress bar
    progressBar = createProgressBar(contentPane);

    // map
    map = createMap();

    // scrollable-table to display the query result
    tblModelCities = new DefaultTableModel() {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean isCellEditable(int rowIndex, int mColIndex) {
        return false;
      }
    };
    // add the column headers
    tblModelCities.addColumn("City");
    tblModelCities.addColumn("Population");
    tblModelCities.addColumn("Shape");
    tblModelCities.addColumn("Capital?");
    tblModelCities.addColumn("State");
    
    final JTable tblCities = new JTable(tblModelCities);
    JScrollPane tblStateInfoScrollable = new JScrollPane(tblCities);
    tblCities.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        int selectedCityRowId = tblCities.getSelectedRow();
        Point selectedCity = (Point) tblModelCities.getValueAt(selectedCityRowId, 2);
        highlightGeometry(selectedCity);
      }
    });
    // don't show shape column
    TableColumn shapeColumn = tblCities.getColumnModel().getColumn(2);
    tblCities.getColumnModel().removeColumn(shapeColumn);

    // various actions
    JLabel queryLabel = new JLabel("Filter cities with population < ");
    queryLabel.setForeground(Color.WHITE);
    queryText.setMinimumSize(new Dimension(150, 25));
    queryText.setMaximumSize(new Dimension(150, 25));
    queryText.setColumns(10);
    JButton btnFilter = new JButton("Filter");
    btnFilter.setFocusPainted(false);
    btnFilter.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        onQuery();
      }
    });

    JButton btnAddGraphic = new JButton("Add Graphic");
    btnAddGraphic.setFocusPainted(false);
    btnAddGraphic.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        mouseActionHandler.setAction(Action.ADD_GRAPHIC);
        if (infoPopupOverlay != null) {
          infoPopupOverlay.setActive(false);
        }
      }
    });

    JButton btnDriveTime = new JButton("Drive Time");
    btnDriveTime.setFocusPainted(false);
    btnDriveTime.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        mouseActionHandler.setAction(Action.ANALYZE_DRIVE_TIME);
        if (infoPopupOverlay != null) {
          infoPopupOverlay.setActive(false);
        }
      }
    });

    JButton btnViewFeature = new JButton("View Feature");
    btnViewFeature.setFocusPainted(false);
    btnViewFeature.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        mouseActionHandler.setAction(Action.IGNORE);
        if (infoPopupOverlay != null) {
          infoPopupOverlay.setActive(true);
        }
      }
    });

    // top panel for actions
    JPanel topPanel = new JPanel();
    topPanel.setBackground(Color.BLACK);
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
    topPanel.add(Box.createHorizontalGlue());
    topPanel.add(queryLabel);
    topPanel.add(queryText);
    topPanel.add(btnFilter);
    topPanel.add(btnAddGraphic);
    topPanel.add(btnDriveTime);
    topPanel.add(btnViewFeature);
    topPanel.add(Box.createHorizontalGlue());

    // panel to contain query results
    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BorderLayout());
    bottomPanel.add(progressBar, BorderLayout.NORTH);
    bottomPanel.add(tblStateInfoScrollable, BorderLayout.CENTER);
    bottomPanel.setPreferredSize(new Dimension(600, 150));

    contentPane.add(topPanel, BorderLayout.NORTH);
    contentPane.add(map, BorderLayout.CENTER);
    contentPane.add(bottomPanel, BorderLayout.SOUTH);

    return contentPane;
  }

  // ------------------------------------------------------------------------
  // Private methods
  // ------------------------------------------------------------------------
  private void highlightGeometry(Point point) {
    if (point == null) {
      return;
    }

    graphicsLayer.removeAll();
    graphicsLayer.addGraphic(
      new Graphic(point, new SimpleMarkerSymbol(Color.CYAN, 20, SimpleMarkerSymbol.Style.CIRCLE)));
    
    // -----------------------------------------------------------------------------------------
    // Zoom to the highlighted graphic
    // -----------------------------------------------------------------------------------------
    Geometry geometryForZoom = GeometryEngine.buffer(
      point, 
      map.getSpatialReference(), 
      map.getFullExtent().getWidth() * 0.10, 
      map.getSpatialReference().getUnit());
    map.zoomTo(geometryForZoom);
  }

  /**
   * Creates a map.
   * 
   * @return a map.
   */
  private JMap createMap() {

    final JMap jMap = new JMap();

    jMap.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            try {
              Envelope initialExtent = new Envelope(-122.593, 37.642, -122.256, 37.867); 
              // Tip: Use appropriate spatial reference 
              SpatialReference inSR = SpatialReference.create(4326); 
              initialExtent = (Envelope) GeometryEngine.project(initialExtent, inSR, jMap.getSpatialReference()); 
              ((JMap) arg0.getSource()).setExtent(initialExtent);
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
      }

      @Override
      public void mapExtentChanged(final MapEvent arg0) {
        // System.out.println("Extent: " + jMap.getScale());
      }
    });

    // -----------------------------------------------------------------------------------------
    // remote map service layer - base layer with US topology
    // -----------------------------------------------------------------------------------------
    addBaseLayer(jMap);

    // -----------------------------------------------------------------------------------------
    // graphics layer - to add result from the geoprocessing execution
    // -----------------------------------------------------------------------------------------
    /*graphicsLayer = new GraphicsLayer();
    jMap.getLayers().add(graphicsLayer);*/

    featureLayer = new ArcGISFeatureLayer(
       "http://sampleserver6.arcgisonline.com/arcgis/rest/services/USA/MapServer/0");
    // "http://sampleserver6.arcgisonline.com/arcgis/rest/services/WorldTimeZones/MapServer/0");

    // Tip: consider using renderer
    final Symbol SYM_CAPITAL = new SimpleMarkerSymbol(Color.RED, 14, Style.TRIANGLE);
    final SimpleMarkerSymbol SYM_NON_CAPITAL = new SimpleMarkerSymbol(Color.YELLOW, 9, Style.CIRCLE); 
    UniqueValueRenderer uvRenderer = new UniqueValueRenderer(); 
    uvRenderer.setAttributeName1("capital"); 
    uvRenderer.addValue(new UniqueValueInfo(new Object[] {"Y"}, SYM_CAPITAL)); 
    uvRenderer.addValue(new UniqueValueInfo(new Object[] {"N"}, SYM_NON_CAPITAL)); 
    featureLayer.setRenderer(uvRenderer);
    
    // Tip: consider on-demand mode
    // featureLayer.setOperationMode(QueryMode.ON_DEMAND);

    jMap.getLayers().add(featureLayer);

    // Tip: order of layers -> order of graphics
    graphicsLayer = new GraphicsLayer();
    jMap.getLayers().add(graphicsLayer);

    // -----------------------------------------------------------------------------------------
    // geoprocessing service executor
    // -----------------------------------------------------------------------------------------
    mouseActionHandler = new MouseActionHandler(jMap, graphicsLayer);
    mouseActionHandler.setAction(Action.IGNORE);
    jMap.addMouseListener(mouseActionHandler);

    // Tip: get code for infopopup from samples
    addInfopopupOverlay(jMap, featureLayer);

    return jMap;
  }
  
  private void addInfopopupOverlay(JMap jMap, ArcGISFeatureLayer featureLayer) {
    // create the infopopup overlay
    infoPopupOverlay = new InfoPopupOverlay();
    // customize the popup and item titles
    infoPopupOverlay.setPopupTitle("Feature");
    // infoPopupOverlay.setItemTitle("Block: {BLOCK}");
    // add the layer of interest to the overlay
    infoPopupOverlay.addLayer(featureLayer);
    // add the overlay to the map
    jMap.addMapOverlay(infoPopupOverlay);
  }

  /**
   * Creates a window.
   * 
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame("Drive Time Geoprocessing Application");
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

  /**
   * Creates a content pane.
   * 
   * @return a content pane.
   */
  private static JLayeredPane createContentPane() {
    JLayeredPane contentPane = new JLayeredPane();
    contentPane.setBounds(100, 100, 1000, 700);
    contentPane.setLayout(new BorderLayout(0, 0));
    contentPane.setVisible(true);
    return contentPane;
  }


  private void addBaseLayer(JMap jMap) {
    /*final ArcGISDynamicMapServiceLayer baseLayer = new ArcGISDynamicMapServiceLayer(
        "http://sampleserver1.arcgisonline.com/ArcGIS/rest/services/Specialty/ESRI_StateCityHighway_USA/MapServer");
*/
    // Tip: Use tiled layer as a basemap    
    final ArcGISTiledMapServiceLayer baseLayer = new ArcGISTiledMapServiceLayer(
      //"http://services.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer");
      "http://services.arcgisonline.com/ArcGIS/rest/services/ESRI_StreetMap_World_2D/MapServer"); // SR 4326
    
    jMap.getLayers().add(baseLayer);     
  }

  private void onQuery() {
    tblModelCities.setRowCount(0);

    // return if input text is not valid
    if (queryText.getText() == null || queryText.getText().isEmpty()) {
      return;
    }

    // query paremters
    Map<String, OrderByFields> sortOrder = new HashMap<String, OrderByFields>();
    sortOrder.put("pop2000", OrderByFields.DESC);
    Query query = new Query();
    query.setWhere("pop2000 > " + queryText.getText());
    query.setOrderByFields(sortOrder);
    query.setOutFields(new String[] {"*"});

    // Tip: use map's SR
    //query.setOutSpatialReference(SpatialReference.create(3857));
    query.setOutSpatialReference(map.getSpatialReference());

    // Tip: don't fetch geometry if not required
    // query.setReturnGeometry(false);

    FeatureSet queryResult;

    // execute the query
    QueryTask task = new QueryTask(featureLayer.getUrl());
    try {
      queryResult = task.execute(query);

      if (queryResult == null) {
        return;
      }

      for (Graphic result : queryResult.getGraphics()) {
        tblModelCities.addRow(new Object[] { 
          result.getAttributeValue("areaname"),
          result.getAttributeValue("pop2000"), 
          result.getGeometry(),
          result.getAttributeValue("capital"),
          result.getAttributeValue("st")});
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  

  /**
   * Creates a progress bar.
   * 
   * @param parent progress bar's parent. The horizontal axis of the progress bar will be center-aligned to the parent.
   * @return a progress bar.
   */
  private static JProgressBar createProgressBar(final JComponent parent) {
    final JProgressBar progressBar = new JProgressBar();
    progressBar.setSize(260, 20);
    parent.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        progressBar.setLocation(
          parent.getWidth() / 2 - progressBar.getWidth() / 2,
          parent.getHeight() - progressBar.getHeight() - 20);
      }
    });
    progressBar.setStringPainted(true);
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    return progressBar;
  }

  /**
   * Updates progress bar UI from the Swing's Event Dispatch Thread.
   * 
   * @param str string to be set.
   * @param visible flag to indicate visibility of the progress bar.
   */
  private void updateProgresBarUI(final String str, final boolean visible) {
    // Tip: Update UI in the UI thread
    SwingUtilities.invokeLater(new Runnable() {
      @Override 
      public void run() { 
        if (str != null) { 
          progressBar.setString(str); 
        } 
        progressBar.setVisible(visible); 
      }
    });
  }

  public static enum Action {
    ADD_GRAPHIC, ANALYZE_DRIVE_TIME, IGNORE
  }

  /**
   * Class to handle mouse events based on current Action.
   * 
   * - if action is ADD_GRAPHIC, then adds a graphic. 
   * - if action is ANALYZE_DRIVE_TIME, then runs a geoprocessing task for drive time calculations.
   */
  class MouseActionHandler extends MouseInputAdapter {

    private Action currentAction = Action.ANALYZE_DRIVE_TIME;
    
    // URL to remote geoprocessing service
    private final String URL_GEOPROCESSING_SERVICE = 
      "http://sampleserver1.arcgisonline.com/ArcGIS/rest/services/Network/ESRI_DriveTime_US/GPServer/CreateDriveTimePolygons";

    // symbology
    private final SimpleMarkerSymbol SYM_START_POINT = new SimpleMarkerSymbol(Color.RED, 16, Style.DIAMOND);

    private final SimpleLineSymbol SYM_ZONE_BORDER = new SimpleLineSymbol(Color.RED, 1);

    private final SimpleFillSymbol[] zoneFillSymbols = new SimpleFillSymbol[] { 
      new SimpleFillSymbol(new Color(255, 0, 0, 80), SYM_ZONE_BORDER), 
      new SimpleFillSymbol(new Color(255, 165, 0, 80), SYM_ZONE_BORDER), 
      new SimpleFillSymbol(new Color(0, 255, 0, 80), SYM_ZONE_BORDER) };

    JMap jMap;

    GraphicsLayer graphicsLayer;

    /**
     * Creates an object that executes the remote geoprocessing service to calculate zones with different drive times.
     * 
     * @param jMap map to get the start point of drive, and to get the spatial reference.
     * @param graphicsLayer graphics layer to which the result will be added.
     */
    public MouseActionHandler(JMap jMap, GraphicsLayer graphicsLayer) {
      this.jMap = jMap;
      this.graphicsLayer = graphicsLayer;
    }

    public void setAction(Action action) {
      currentAction = action;
    }

    public Action getAction() {
      return currentAction;
    }

    /**
     * Computes the drive time zones on click of the mouse.
     */
    @Override
    public void mouseClicked(MouseEvent mapEvent) {

      super.mouseClicked(mapEvent);
     
      if (mapEvent.getButton() == MouseEvent.BUTTON3) {
        // remove zones from previous computation
        graphicsLayer.removeAll();
        return;
      }
      
      if (currentAction == Action.IGNORE) {
        return;
      } else if (currentAction == Action.ADD_GRAPHIC) {
        handleAddGraphic(mapEvent);        
      } else {
        handleAnalyzeDriveTime(mapEvent);  
      }
    }
    
    private void handleAddGraphic(MouseEvent mapEvent) {
      Point p = jMap.toMapPoint(mapEvent.getPoint().x, mapEvent.getPoint().y);
      // Tip: use the right symbol for geometry
      SimpleMarkerSymbol s = new SimpleMarkerSymbol(Color.RED, 14, Style.CROSS);
      //SimpleLineSymbol s = new SimpleLineSymbol(Color.RED, 10);
      graphicsLayer.addGraphic(new Graphic(p, s));  
    }
    
    private void handleAnalyzeDriveTime(MouseEvent mapEvent) {
      tasksInProgress.incrementAndGet();
      updateProgresBarUI("Computing drive time zones...", true);

      // the click point is the starting point
      Point startPoint = jMap.toMapPoint(mapEvent.getX(), mapEvent.getY());
      Graphic startPointGraphic = new Graphic(startPoint, SYM_START_POINT);
      graphicsLayer.addGraphic(startPointGraphic);

      executeDriveTimes(startPointGraphic);
    }

    private void executeDriveTimes(Graphic startPointGraphic) {

      // create a Geoprocessor that points to the remote geoprocessing service.
      Geoprocessor geoprocessor = new Geoprocessor(URL_GEOPROCESSING_SERVICE);
      // set the output and process spatial reference to the map's spatial reference
      SpatialReference outSR = SpatialReference.create(4326);
      Geometry projectedStartPoint = GeometryEngine.project(
        startPointGraphic.getGeometry(), jMap.getSpatialReference(), outSR);
      Graphic projectedStartPointGraphic = new Graphic(projectedStartPoint, startPointGraphic.getSymbol());
      geoprocessor.setOutSR(outSR);
      geoprocessor.setProcessSR(outSR);

      // initialize the required input parameters: refer to help link in the
      // geoprocessing service URL for a list of required parameters
      List<GPParameter> gpInputParams = new ArrayList<GPParameter>();

      GPFeatureRecordSetLayer gpInputStartpoint = new GPFeatureRecordSetLayer("Input_Location");
      gpInputStartpoint.addGraphic(projectedStartPointGraphic);

      //GPString gpInputDriveTimes = new GPString("Drive_Time");
      // Tip: use GP service info to get the parameter names
      GPString gpInputDriveTimes = new GPString("Drive_Times");
      
      gpInputDriveTimes.setValue("1 2 3");

      gpInputParams.add(gpInputStartpoint);
      gpInputParams.add(gpInputDriveTimes);

      // execute the geoprocessing request
      /*try {
        GPParameter[] result = geoprocessor.execute(gpInputParams);
        updateProgresBarUI(null,  tasksInProgress.decrementAndGet() > 0);
        processResult(result);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(map, ex.getMessage(), "", JOptionPane.ERROR_MESSAGE);
      }*/
      
      // Tip: Do not block UI thread. 
      geoprocessor.executeAsync( 
        gpInputParams, 
        new CallbackListener<GPParameter[]>() {
            @Override 
            public void onError(Throwable th) { 
              th.printStackTrace(); 
            }
            @Override 
            public void onCallback(GPParameter[] result) { 
              updateProgresBarUI(null, tasksInProgress.decrementAndGet() > 0); 
              processResult(result); 
              } 
            } 
        );       
    }

    /**
     * Process result from geoprocessing execution.
     * 
     * @param result output of geoprocessing execution.
     */
    private void processResult(GPParameter[] result) {
      for (GPParameter outputParameter : result) {
        if (outputParameter instanceof GPFeatureRecordSetLayer) {
          GPFeatureRecordSetLayer gpLayer = (GPFeatureRecordSetLayer) outputParameter;
          int zone = 0;
          // get all the graphics and add them to the graphics layer.
          // there will be one graphic per zone.
          for (Graphic graphic : gpLayer.getGraphics()) {
            SpatialReference fromSR = SpatialReference.create(4326);
            Geometry g = graphic.getGeometry();
            Geometry pg = GeometryEngine.project(g, fromSR, jMap.getSpatialReference());
            Graphic theGraphic = new Graphic(pg, zoneFillSymbols[zone++]);
            // add to the graphics layer
            graphicsLayer.addGraphic(theGraphic);
          }
        }
      }
    }
  }
}
