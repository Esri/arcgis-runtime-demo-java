

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.esri.core.geometry.Envelope;
import com.esri.core.gps.FileGPSWatcher;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GPSLayer;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.map.GPSLayer.Mode;

/**
 * This sample shows how to create a basic map application.
 */
public class MyMapApp {

  private JFrame window;
  private JMap map;
  
  private FileGPSWatcher watcher;
  private GPSLayer gpsLayer;

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
