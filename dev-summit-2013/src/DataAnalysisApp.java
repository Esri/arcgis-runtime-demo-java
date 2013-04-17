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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.esri.client.local.LocalFeatureService;
import com.esri.client.local.LocalServiceStartCompleteEvent;
import com.esri.client.local.LocalServiceStartCompleteListener;
import com.esri.client.toolkit.overlays.HitTestEvent;
import com.esri.client.toolkit.overlays.HitTestListener;
import com.esri.client.toolkit.overlays.HitTestOverlay;
import com.esri.core.geometry.Envelope;
import com.esri.core.map.Graphic;
import com.esri.map.ArcGISFeatureLayer;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.LayerList;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.map.popup.MapPopup;
import com.esri.map.popup.PopupView;
import com.esri.map.popup.PopupViewEvent;
import com.esri.map.popup.PopupViewListener;

/***
 * Application for a tech session at Esri Dev Summit 2013.
 */
public class DataAnalysisApp {

  private JPanel jPanel;

  private LocalFeatureService featureService;

  private ArcGISFeatureLayer pointsLayer;

  private JMap map;

  private HitTestOverlay hitTestOverlay;

  private DriveTimeExecutor driveTimeExecutor;

  private static String FSP = System.getProperty("file.separator");

  /**
   * Constructor
   */
  public DataAnalysisApp() {
    initialize();
  }

  public JPanel createUI() {
    return jPanel;
  }

  private void initialize() {

    // create UI
    jPanel = new JPanel();
    jPanel.setLayout(new BorderLayout(5, 0));
    jPanel.setBackground(Color.WHITE);

    // add map
    map = new JMap();
    jPanel.add(map, BorderLayout.CENTER);
    map.setMinimumSize(new Dimension(512, 512));

    // attribute editor
    JButton driveTimeButton = new JButton("Calculate Drive Time");
    driveTimeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        hitTestOverlay.setActive(false);
        driveTimeExecutor.setEnabled(true);
      }
    });

    JButton viewAttributesButton = new JButton("View Attributes");
    viewAttributesButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        hitTestOverlay.setActive(true);
        driveTimeExecutor.setEnabled(false);
      }
    });
    
    JPanel topPanel = new JPanel();
    topPanel.add(driveTimeButton);
    topPanel.add(viewAttributesButton);
    jPanel.add(topPanel, BorderLayout.NORTH);

    // set default extent
    map.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            ((JMap) arg0.getSource()).setExtent(
              new Envelope(-122.51716971480532, 37.700866040901786, -122.34825490495734, 37.8145058824865));
          }
        });
      }

      @Override
      public void mapExtentChanged(final MapEvent arg0) {
      }
    });

    // add a tiled layer to map
    final LayerList layers = map.getLayers();
    layers.add(new ArcGISTiledMapServiceLayer(
        "http://services.arcgisonline.com/ArcGIS/rest/services/ESRI_StreetMap_World_2D/MapServer"));

    // create the local feature service
    featureService = new LocalFeatureService("data" + FSP + "MyRestaurants.mpk");
    featureService.addLocalServiceStartCompleteListener(new LocalServiceStartCompleteListener() {

      @Override
      public void localServiceStartComplete(LocalServiceStartCompleteEvent arg0) {
        // once service started, create and add feature layers to the map
        pointsLayer = new ArcGISFeatureLayer(arg0.getUrl() + "/0");
        layers.add(pointsLayer);

        hitTestOverlay = new HitTestOverlay(pointsLayer);
        hitTestOverlay.addHitTestListener(new HitTestListener() {
          @Override
          public void graphicHit(HitTestEvent event) {
            // / get first (top-most) graphic hit by the mouse
            HitTestOverlay overlay = (HitTestOverlay) event.getSource();
            Graphic hitGraphic = overlay.getHitGraphics().get(0);
            try {
              PopupView content = PopupView.createEditView("Attributes", pointsLayer);
              content.setSize(300, 200);
              content.setGraphic(pointsLayer, hitGraphic);
              final MapPopup popup = pointsLayer.getMap().createPopup(new JComponent[] { content }, hitGraphic, true);
              popup.setMovingWithMap(true);
              popup.setTitle("Attributes");
              popup.setVisible(true);
              content.addPopupViewListener(new PopupViewListener() {

                @Override
                public void onCommitEdit(PopupViewEvent popupViewEvent, Graphic _graphic) {
                  popup.close();
                }

                @Override
                public void onCancelEdit(PopupViewEvent popupViewEvent, Graphic _graphic) {
                  popup.close();
                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
        hitTestOverlay.setActive(true);
        map.addMapOverlay(hitTestOverlay);
      }
    });
    // start the service
    featureService.startAsync();

    // graphics layer for drive time
    GraphicsLayer graphicsLayer = new GraphicsLayer();
    driveTimeExecutor = new DriveTimeExecutor(map, graphicsLayer);
    map.addMouseListener(driveTimeExecutor);
    layers.add(graphicsLayer);
  }

  /**
   * Starting point of this application.
   * 
   * @param args
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          DataAnalysisApp templatePickerApp = new DataAnalysisApp();
          JFrame window = templatePickerApp.createWindow();
          window.add(templatePickerApp.createUI());
          window.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Creates a window.
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
}
