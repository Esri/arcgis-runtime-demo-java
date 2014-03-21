/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/
package getstarted;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.esri.client.local.ArcGISLocalTiledLayer;
import com.esri.core.gdb.Geodatabase;
import com.esri.core.gdb.GdbFeatureTable;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.map.FeatureLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapOverlay;

public class GeometryApp {

  // resources
  private static final String GEODATABASE_PATH = "data/getstarted/usa.geodatabase";
  private static final String TPK_PATH = "data/getstarted/Topographic.tpk";
  private static final int LAYER_ID = 2; // states layer

  private JMap map;
  private GraphicsLayer graphicsLayer;
  private GraphicsLayer graphicsLayerQueryResults;
  private Geodatabase geodatabase;
  private GdbFeatureTable table;
  private JPanel contentPane;
  // buffer distance: will be in the map's units, meters in this case
  private static final double BUFFER_DISTANCE = 200000; // 200 km
  // symbology
  final static SimpleLineSymbol SYM_LINE   = new SimpleLineSymbol(Color.RED, 2.0f);
  final static SimpleMarkerSymbol SYM_POINT =
      new SimpleMarkerSymbol(new Color(200, 0, 0, 200), 8, Style.CIRCLE);
  final static SimpleFillSymbol SYM_BUFFER =
      new SimpleFillSymbol(new Color(0, 0, 255, 80), SYM_LINE);

  // ------------------------------------------------------------------------
  // Constructors
  // ------------------------------------------------------------------------
  public GeometryApp() {
  }

  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------
  /**
   * Map overlay to handle mouse events.
   */
  class MouseOverlay extends MapOverlay {

    private static final long serialVersionUID = 1L;
    // map
    JMap jMap;
    // layer to which graphics will be added to
    GraphicsLayer gLayer;
    GraphicsLayer gLayerResults;

    Polyline polyLine = new Polyline();
    Point    prevPoint;
    boolean  startOver = false;
    Geometry bufferedArea = null;
    AtomicBoolean queryInProgress = new AtomicBoolean(false);

    /**
     * Constructor
     * @param jMap JMap to which this overlay belongs.
     * @param graphicsLayer
     */
    MouseOverlay(JMap jMap, GraphicsLayer graphicsLayer, GraphicsLayer gLayerResults) {
      this.jMap = jMap;
      this.gLayer = graphicsLayer;
      this.gLayerResults = gLayerResults;
    }

    /**
     * Handle mouse-clicks.
     * On left-click - draws either a polyline or a point.
     * On right-click - computes and draws the buffer of the polyline or point.
     */
    @Override
    public void onMouseMoved(MouseEvent event) {
      super.onMouseMoved(event);

      gLayer.removeAll();

      Point currPoint = jMap.toMapPoint(event.getX(), event.getY());
      // point
      bufferedArea = GeometryEngine.buffer(
          currPoint,
          jMap.getSpatialReference(),
          BUFFER_DISTANCE,
          jMap.getSpatialReference().getUnit());
      Graphic currPointGraphic = new Graphic(currPoint, GeometryApp.SYM_POINT);
      gLayer.addGraphic(currPointGraphic);

      // add the buffered area to the graphics layer
      Graphic bufferedGraphic = new Graphic(bufferedArea, GeometryApp.SYM_BUFFER);
      gLayer.addGraphic(bufferedGraphic);

      if (queryInProgress.get() == false) {
        // query
        QueryParameters query = new QueryParameters();
        query.setReturnGeometry(true);
        query.setGeometry(bufferedArea);
        query.setOutFields(new String[] {"STATE_NAME"});

        // execute the query.
        table.queryFeatures(query, new CallbackListener<FeatureResult>() {

          
          @Override
          public void onError(Throwable e) {
            e.printStackTrace();
          }

          @Override
          public void onCallback(FeatureResult result) {
            gLayerResults.removeAll();
            for (Object objFeature : result) {
              Feature feature = (Feature) objFeature;
              gLayerResults.addGraphic(new Graphic(feature.getGeometry(), SYM_BUFFER));
            }
            queryInProgress.set(false);
          }
        });
        queryInProgress.set(true);
      }

      prevPoint = null;
      polyLine.setEmpty();

      return;
    }
  }
  // ------------------------------------------------------------------------
  // Static methods
  // ------------------------------------------------------------------------
  /**
   * Starting point of this application.
   * @param args arguments to this application.
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          // instance of this application
          GeometryApp geometryApp = new GeometryApp();

          // create the UI, including the map, for the application.
          JFrame appWindow = geometryApp.createWindow();
          appWindow.add(geometryApp.createUI());
          appWindow.setVisible(true);
        } catch (Exception e) {
          // on any error, display the stack trace.
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
   */
  public JComponent createUI() throws Exception {
    // application content
    contentPane = new JPanel();
    contentPane.setLayout(new BorderLayout());

    // map
    map = createMap();
    contentPane.add(map, BorderLayout.CENTER);

    return contentPane;
  }

  // ------------------------------------------------------------------------
  // Private methods
  // ------------------------------------------------------------------------
  /**
   * Creates a window.
   * @return a window.
   */
  private JFrame createWindow() {
    JFrame window = new JFrame();
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
   * Creates a map.
   * @return a map.
   */
  private JMap createMap() throws Exception {
    final JMap jMap = new JMap();
    // -----------------------------------------------------------------------------------------
    // Base Layer - set initial map extent to USA
    // -----------------------------------------------------------------------------------------
    final ArcGISLocalTiledLayer tiledLayer = new ArcGISLocalTiledLayer(TPK_PATH);
    jMap.setExtent(new Envelope(-15000000, 2000000, -7000000, 8000000));
    jMap.getLayers().add(tiledLayer);

    // -----------------------------------------------------------------------------------------
    // Graphics Layer - to add lines
    // -----------------------------------------------------------------------------------------
    graphicsLayerQueryResults = new GraphicsLayer();
    jMap.getLayers().add(graphicsLayerQueryResults);
    
    graphicsLayer = new GraphicsLayer();
    jMap.getLayers().add(graphicsLayer);

    jMap.addMapOverlay(new MouseOverlay(jMap, graphicsLayer, graphicsLayerQueryResults));

    // create the geodatabase and geodatabase feature table once
    try {
      geodatabase = new Geodatabase(GEODATABASE_PATH);
      table = geodatabase.getGdbFeatureTableByLayerId(LAYER_ID);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(contentPane, "Error: " + e.getLocalizedMessage() + "\r\nSee notes for this application.");
    }

    jMap.getLayers().add(new FeatureLayer(table));

    return jMap;
  }
}
