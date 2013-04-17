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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import com.esri.core.geometry.Envelope;
import com.esri.map.ArcGISFeatureLayer;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListener;
import com.esri.map.WebMap;

/**
 * Demo used for a tech session at Esri Dev Summit 2013.
 */
public class MapsAndLayersApp {

  private JMap map;

  public MapsAndLayersApp() {
  }

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
          // instance of this application
          MapsAndLayersApp webMapApp = new MapsAndLayersApp();

          // create the UI, including the map, for the application.
          JFrame appWindow = webMapApp.createWindow();
          appWindow.add(webMapApp.createUI());
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

  public JComponent createUI() throws Exception {
    // application content
    JComponent contentPane = createContentPane();

    // create the map
    map = createMap();
    contentPane.add(map);

    return contentPane;
  }

  // ------------------------------------------------------------------------
  // Private methods
  // ------------------------------------------------------------------------

  /**
   * Creates the JMap, initializes the webMap
   * 
   * @return a JMap
   */
  private JMap createMap() throws Exception {
    final JMap jMap = new JMap();

    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
        "http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer");
    jMap.getLayers().add(tiledLayer);

    ArcGISFeatureLayer featureLayer = new ArcGISFeatureLayer(
        "http://sampleserver6.arcgisonline.com/arcgis/rest/services/WorldTimeZones/MapServer/0");
    jMap.getLayers().add(featureLayer);

    ArcGISFeatureLayer featureLayer2 = new ArcGISFeatureLayer(
        "http://sampleserver6.arcgisonline.com/arcgis/rest/services/WorldTimeZones/MapServer/1");
    jMap.getLayers().add(featureLayer2);

    jMap.addMapEventListener(new MapEventListener() {
      @Override
      public void mapReady(MapEvent arg0) {
        Envelope initialExtent = new Envelope(-1.7179059443386406E7, 454212.81402631383, 
         -4019659.610190239, 9301865.407287464);
        jMap.setExtent(initialExtent);
      }

      @Override
      public void mapExtentChanged(MapEvent arg0) {
      }

      @Override
      public void mapDispose(MapEvent arg0) {
      }
    });

    // This WebMap contains the above 3 layers + the initial extent
    /*
     * String WEB_MAP_ID = "90699a203b6b483989fa8e8930115775"; // arcgis.com 
     * WebMap webMap = new WebMap(WEB_MAP_ID);
     * webMap.initializeMap(jMap);
     */

    return jMap;
  }

  /**
   * Creates the application window.
   * 
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
}
