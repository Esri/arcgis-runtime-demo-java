

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.esri.client.toolkit.overlays.DrawingCompleteEvent;
import com.esri.client.toolkit.overlays.DrawingCompleteListener;
import com.esri.client.toolkit.overlays.DrawingOverlay;
import com.esri.client.toolkit.overlays.DrawingOverlay.DrawingMode;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.gps.FileGPSWatcher;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.tasks.ags.geocode.Locator;
import com.esri.core.tasks.ags.geocode.LocatorFindParameters;
import com.esri.core.tasks.ags.geocode.LocatorGeocodeResult;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GPSLayer;
import com.esri.map.GPSLayer.Mode;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;

/**
 * This sample shows how to create a basic map application.
 */
public class MyMapApp {

  private JFrame window;
  private JMap map;
  
  private FileGPSWatcher watcher;
  private GPSLayer gpsLayer;
  
  private JTextField textField;
  private GraphicsLayer addressGraphics;
  private Symbol geocodeSymbol;
  
  private GraphicsLayer stopGraphics;
  private Symbol symRoutingStops;
  
  public MyMapApp() {
    window = new JFrame();
    window.setBounds(0, 0, 1000, 700);
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

    map = new JMap();
    
    map.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            // default extent to Edinburgh, Scotland
            map.setExtent(new Envelope(-377278, 7533440, -339747, 7558472.79));
          }
        });
      }
    });

    window.getContentPane().add(map, BorderLayout.CENTER);
    
    // create toolbar
    JToolBar toolbar = new JToolBar();
    window.getContentPane().add(toolbar, BorderLayout.NORTH);

    // Start/stop GPS button
    JButton btnLocation = new JButton("GPS on/off");
    toolbar.add(btnLocation);

    btnLocation.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {
        if (!map.getLayers().contains(gpsLayer)) {
          addGPSLayer();
        } else {
          map.getLayers().remove(gpsLayer);
        }
      }
    });

    // create and add tiled layer
    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
        "http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer");
    map.getLayers().add(tiledLayer);
    
    // GEOCODING
    addressGraphics = new GraphicsLayer();
    map.getLayers().add(addressGraphics);
    geocodeSymbol = createGeocodeSymbol();
    
    textField = new JTextField("100 holyrood, Edinburgh");
    JButton findButton = new JButton("Find");

    toolbar.add(textField);
    toolbar.add(findButton);

    findButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {
        onFind();
      }
    });
    
    // graphics layer to add stops
    stopGraphics = new GraphicsLayer();
    map.getLayers().add(stopGraphics);
    symRoutingStops = createRoutingSymbol();
    
    // create drawing overlay, setup, and add to map
    final DrawingOverlay drawingOverlay = new DrawingOverlay();
    map.addMapOverlay(drawingOverlay);
    drawingOverlay.setUp(
        DrawingMode.POINT,
        symRoutingStops,
        null);
    drawingOverlay.setActive(false);
    drawingOverlay.addDrawingCompleteListener(new DrawingCompleteListener() {

      @Override
      public void drawingCompleted(DrawingCompleteEvent arg0) {
        Graphic graphic = drawingOverlay.getAndClearGraphic();

        Point pt = (Point) graphic.getGeometry();
        System.out.println("x: " +pt.getX() +", y: "+ pt.getY());
        
        stopGraphics.addGraphic(graphic);
      }
    });

    JButton addStops = new JButton("Add stops");
    addStops.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        drawingOverlay.setActive(true);
      }
    });
    
    toolbar.add(addStops);
    
  }
  
  protected void onFind() {
    Locator locator = new Locator(
        "http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
    LocatorFindParameters params = new LocatorFindParameters(textField.getText());
    params.setOutSR(map.getSpatialReference());

    // additional parameters optionally, could grab the latest point from a GPS feed for example
    params.setLocation(new Point(-356903.5435, 7546014.500), map.getSpatialReference());
    params.setDistance(10000);

    // run the locator task asynchronously
    locator.findAsync(params, new CallbackListener<List<LocatorGeocodeResult>>() {

      @Override
      public void onError(Throwable e) {
        JOptionPane.showMessageDialog(map.getParent(), e.getMessage());
      }

      @Override
      public void onCallback(List<LocatorGeocodeResult> results) {
        // display top result
        if (results != null) {
          // get the top result to display on map
          LocatorGeocodeResult highestScoreResult = results.get(0);

          // create and populate attribute map
          Map<String, Object> attributes = new HashMap<String, Object>();
          for (Entry<String, String> entry : highestScoreResult.getAttributes().entrySet())
          {
            attributes.put(entry.getKey(), entry.getValue());
          }

          // create a graphic at this location
          Graphic addressGraphic = new Graphic(
              highestScoreResult.getLocation(), 
              geocodeSymbol, 
              attributes, 
              null);
          addressGraphics.addGraphic(addressGraphic);

          // centre the map at this location
          Envelope extent = map.getExtent();
          extent.centerAt(highestScoreResult.getLocation());
          map.zoomTo(extent);
        }
      }
    });
  }
  
  private PictureMarkerSymbol createRoutingSymbol() {
    PictureMarkerSymbol symPoint = new PictureMarkerSymbol(
        "http://static.arcgis.com/images/Symbols/Basic/BlueStickpin.png");
    symPoint.setSize(44, 44);
    symPoint.setOffsetY(22.0f);
    return symPoint;
  }
  
  private PictureMarkerSymbol createGeocodeSymbol() {
    PictureMarkerSymbol symPoint = new PictureMarkerSymbol(
        "http://static.arcgis.com/images/Symbols/Basic/RedShinyPin.png");
    symPoint.setSize(40, 40);
    symPoint.setOffsetY(20.0f);
    return symPoint;
  }
  
  private void addGPSLayer() {
    watcher = new FileGPSWatcher("data" + System.getProperty("file.separator") + "meadows.txt", 800, true);
    gpsLayer = new GPSLayer(watcher);
    gpsLayer.setMode(Mode.OFF);
    gpsLayer.setShowTrackPoints(false);
    gpsLayer.setShowTrail(false);
    map.getLayers().add(gpsLayer);
  }
  
  /**
   * Starting point of this application
   * 
   * @param args any arguments
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          MyMapApp application = new MyMapApp();
          application.window.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
