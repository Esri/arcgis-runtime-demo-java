/*
COPYRIGHT 1995-2013 ESRI

TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
Unpublished material - all rights reserved under the
Copyright Laws of the United States.

For additional information, contact:
Environmental Systems Research Institute, Inc.
Attn: Contracts Dept
380 New York Street
Redlands, California, USA 92373

email: contracts@esri.com
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

import com.esri.client.local.ArcGISLocalTiledLayer;
import com.esri.client.local.LocalFeatureService;
import com.esri.client.local.LocalServiceStartCompleteEvent;
import com.esri.client.local.LocalServiceStartCompleteListener;
import com.esri.client.toolkit.editing.JEditToolsPicker;
import com.esri.client.toolkit.editing.JTemplatePicker;
import com.esri.client.toolkit.overlays.HitTestEvent;
import com.esri.client.toolkit.overlays.HitTestListener;
import com.esri.client.toolkit.overlays.HitTestOverlay;
import com.esri.core.geometry.Envelope;
import com.esri.core.map.Graphic;
import com.esri.map.ArcGISFeatureLayer;
import com.esri.map.JMap;
import com.esri.map.LayerList;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.map.popup.MapPopup;
import com.esri.map.popup.PopupView;
import com.esri.map.popup.PopupViewEvent;
import com.esri.map.popup.PopupViewListener;

/**
 * Application for a tech session at Esri Dev Summit 2013.
 */
public class DataCollectorApp {

  private JPanel jPanel;

  private LocalFeatureService featureService;

  private ArcGISFeatureLayer pointsLayer;

  private JMap map;

  private HitTestOverlay hitTestOverlay;

  private static String FSP = System.getProperty("file.separator");

  /**
   * Constructor
   */
  public DataCollectorApp() {
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

    // set default extent
    map.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            ((JMap) arg0.getSource()).setExtent(new Envelope(-122.54045325841207, 37.66851428227277,
                -122.33909138949045, 37.84935330192468));
          }
        });
      }

      @Override
      public void mapExtentChanged(final MapEvent arg0) {
      }
    });

    // add a tiled layer to map
    final LayerList layers = map.getLayers();
    ArcGISLocalTiledLayer tiledLayed = new ArcGISLocalTiledLayer("data" + FSP + "SanFrancisco.tpk");
    layers.add(tiledLayed);

    // add template picker
    final JTemplatePicker templatePicker = new JTemplatePicker(map);
    jPanel.add(templatePicker, BorderLayout.WEST);
    templatePicker.setIconWidth(36);
    templatePicker.setIconHeight(36);
    templatePicker.setShowNames(true);
    templatePicker.setWatchMap(true);

    // add edit tools picker
    final JEditToolsPicker editToolsPicker = new JEditToolsPicker(map);
    editToolsPicker.setCreationOverlay(templatePicker.getOverlay());

    // attribute editor
    JButton editButton = new JButton("View/Edit a feature's attributes");
    editButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        templatePicker.getOverlay().setActive(false);
        hitTestOverlay.setActive(true);
      }
    });
    
    JPanel topPanel = new JPanel();
    topPanel.add(editToolsPicker);
    topPanel.add(editButton);    
    jPanel.add(topPanel, BorderLayout.NORTH);

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
            // get first (top-most) graphic hit by the mouse
            HitTestOverlay overlay = (HitTestOverlay) event.getSource();
            Graphic hitGraphic = overlay.getHitGraphics().get(0);
            try {
              PopupView content = PopupView.createEditView("Edit Attributes", pointsLayer);
              content.setSize(300, 200);
              content.setGraphic(pointsLayer, hitGraphic);
              final MapPopup popup = pointsLayer.getMap().createPopup(new JComponent[] { content }, hitGraphic, true);
              popup.setMovingWithMap(true);
              popup.setTitle("Edit Attributes");
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
        hitTestOverlay.setActive(false);
        map.addMapOverlay(hitTestOverlay);
      }
    });
    // start the service
    featureService.startAsync();
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
          DataCollectorApp app = new DataCollectorApp();
          JFrame window = app.createWindow();
          window.add(app.createUI());
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
    JFrame window = new JFrame("Data Collection Application");
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
