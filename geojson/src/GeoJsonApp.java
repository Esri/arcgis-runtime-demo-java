
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
import java.io.File;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import com.esri.core.map.Feature;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.CompositeSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;
import com.esri.toolkit.overlays.HitTestEvent;
import com.esri.toolkit.overlays.HitTestListener;
import com.esri.toolkit.overlays.HitTestOverlay;
import com.esri.toolkit.overlays.InfoPopupOverlay;

/**
 * This sample shows how to add GeoJSON features to a graphics layer.
 * <p>
 * It involves using the {@link GeoJsonParser} to parse GeoJSON data from a string
 * or a file, then adding the parsed features to a {@link GraphicsLayer}.
 * <p>
 * For information on GeoJSON, visit <a href="http://geojson.org">http://geojson.org</a>
 * <p>
 * Credits:<br>
 * Data file "countries.geojson" is from <a href="https://github.com/johan/">https://github.com/johan/</a>.
 */
public final class GeoJsonApp {

  private static final String GEOJSON_DATA_FILE = "countries.geojson";
  
  private JMap map;

  public GeoJsonApp() { }
  
  // ------------------------------------------------------------------------
  // Core functionality
  // ------------------------------------------------------------------------
  /**
   * Parse GeoJSON file and add the features to a graphics layer.
   * @param graphicsLayer layer to which the features should be added.
   */
  private void addGeoJsonFeatures(GraphicsLayer graphicsLayer) {
    try {
      // create an instance of the parser
      GeoJsonParser geoJsonParser = new GeoJsonParser();
      
      // provide the symbology for the features
      CompositeSymbol symbol = new CompositeSymbol();
      symbol.add(new SimpleFillSymbol(new Color(0, 255, 0, 70)));
      symbol.add(new SimpleLineSymbol(Color.BLACK, 2));
      geoJsonParser.setSymbol(symbol).setOutSpatialReference(map.getSpatialReference());
      
      // parse geojson data
      File geoJsonFile = new File(GEOJSON_DATA_FILE);
      List<Feature> features = geoJsonParser.parseFeatures(geoJsonFile);
      
      // add parsed features to a layer
      for (Feature f : features) {
       graphicsLayer.addGraphic(new Graphic(f.getGeometry(), f.getSymbol(), f.getAttributes()));
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
  
  // ------------------------------------------------------------------------
  // Static methods
  // ------------------------------------------------------------------------
  /**
   * Starting point of this application.
   * 
   * @param args arguments to this application.
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          GeoJsonApp app = new GeoJsonApp();
          JFrame appWindow = app.createWindow();
          appWindow.add(app.createUI());
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
    JComponent contentPane = createContentPane();
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
    contentPane.setBounds(100, 100, 1000, 700);
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
    JFrame window = new JFrame("Add GeoJSON Features Application");
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
   * Creates a map with a graphics layer and required overlays.
   * @return a map instance.
   */
  private JMap createMap() {

    MapOptions options = new MapOptions(MapType.TOPO);
    final JMap jMap = new JMap(options);
    
    // create the graphics layer to which the features will be added
    final GraphicsLayer graphicsLayer = new GraphicsLayer();
    jMap.getLayers().add(graphicsLayer);
    
    // add a overlay to highlight features at the clicked location
    final HitTestOverlay hitTestOverlay = new HitTestOverlay(graphicsLayer, onFeatureClick);
    jMap.addMapOverlay(hitTestOverlay);
    
    // add a overlay to show popup with features' info at the clicked location 
    InfoPopupOverlay infoPopupOverlay = new InfoPopupOverlay();
    infoPopupOverlay.setPopupTitle("Selected Country");
    infoPopupOverlay.addLayer(graphicsLayer);
    jMap.addMapOverlay(infoPopupOverlay);

    // add features after map is ready
    jMap.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            addGeoJsonFeatures(graphicsLayer);
          }
        });
      }
    });

    return jMap;
  }
  
  private final HitTestListener onFeatureClick = new HitTestListener() {
    @Override
    public void featureHit(HitTestEvent event) {
      List<Feature> hitFeatures = event.getOverlay().getHitFeatures();
      GraphicsLayer graphicsLayer = (GraphicsLayer) event.getOverlay().getLayer();
      for (Feature feature : hitFeatures) {
        int id = (int) feature.getId();
        if (graphicsLayer.isGraphicSelected(id)) {
          // if graphic is selected in the layer, unselect it
          graphicsLayer.unselect(id);
        } else {
          // otherwise select graphic in the layer
          graphicsLayer.select(id);
        }
      }
    }
  };
}
