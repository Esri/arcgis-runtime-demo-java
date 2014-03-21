/* Copyright 2014 Esri

All rights reserved under the copyright laws of the United States
and applicable international laws, treaties, and conventions.

You may freely redistribute and use this sample code, with or
without modification, provided you include the original copyright
notice and use restrictions.

See the use restrictions.*/
package buildingapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.esri.core.gdb.Geodatabase;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.table.FeatureTable;
import com.esri.core.table.TableException;
import com.esri.core.tasks.gdb.GenerateGeodatabaseParameters;
import com.esri.core.tasks.gdb.GeodatabaseStatusCallback;
import com.esri.core.tasks.gdb.GeodatabaseStatusInfo;
import com.esri.core.tasks.gdb.GeodatabaseTask;
import com.esri.core.tasks.gdb.SyncGeodatabaseParameters;
import com.esri.core.tasks.gdb.SyncModel;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.map.ArcGISFeatureLayer;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.FeatureLayer;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.LocationOnMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.map.MapOverlay;
import com.esri.map.popup.MapPopup;
import com.esri.map.popup.PopupView;
import com.esri.map.popup.PopupViewEvent;
import com.esri.map.popup.PopupViewListener;
import com.esri.client.toolkit.overlays.NavigatorOverlay;
import com.esri.client.toolkit.overlays.ScaleBarOverlay;

/**
 * Creates a application with connected-disconnected workflow. This application walks through
 * <ul>
 * <li>Creating a map and adding layers.
 * <li>Using geometry engine for simple analysis.
 * <li>Downloading feature layer for offline mode.
 * <li>Using toolkit to perform editing offline.
 * <li>Routing offline.
 * <li>Synchronizing data when connected.
 * </ul> 
 */
public final class MyMapApp {

  private JMap               map;
  private GraphicsLayer      analysisGraphicsLayer;
  private GraphicsLayer      stopsGraphicsLayer;
  private GraphicsLayer      routeGraphicsLayer;
  private ArcGISFeatureLayer onlineLayer;
  private FeatureLayer       offlineLayer;

  private final String ONLINE_SERVICE_URL = 
      "http://sampleserver6.arcgisonline.com/arcgis/rest/services/Sync/WildfireSync/FeatureServer";
  private final String GEODATABASE_FILE_PATH = "data/building_apps/fire.gdb";

  private enum MapClickOperation { ADD_STOPS, ADD_BARRIERS, IMPACT_ANALYSIS }
  private MapClickOperation onMapClickOperation = MapClickOperation.ADD_STOPS;
  private int numStops;
  private NAFeaturesAsFeature stops = new NAFeaturesAsFeature();
  private NAFeaturesAsFeature barriers = new NAFeaturesAsFeature();

  private JToolBar toolbar;
  private JLabel  lblCurrentMode;
  private JButton btnDownload;
  private JButton btnOffline;
  private JButton btnAddStops;
  private JButton btnAddBarriers;
  private JButton btnRoute;
  private JButton btnLiveRoute;
  private JButton btnEdit;
  private JButton btnOnline;
  private JButton btnSync;

  //create task
  private RouteTask route;
  private boolean liveRoute = false;

  public MyMapApp() {
  }

  /**
   * Starting point of this application
   * 
   * @param args any arguments
   */
  public static void main(String[] args) {
    //ArcGISRuntime.setClientID("");
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          MyMapApp application = new MyMapApp();
          application.createUI();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public void createUI() {
    // application window. dispose map when application is closed.
    JFrame window = UI.createWindow();
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowEvent) {
        super.windowClosing(windowEvent);
        map.dispose();
      }
    });

    // ---------------------------------------------------------------------------------------------
    // Map with a base layer
    // ---------------------------------------------------------------------------------------------
    map = new JMap();
    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
        "http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer");
    map.getLayers().add(tiledLayer);

    map.addMapEventListener(new MapEventListenerAdapter() {
      @Override
      public void mapReady(final MapEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            Point sanFrancisco = GeometryEngine.project(
                -122.4167, 37.7833, map.getSpatialReference());
            map.setExtent(new Envelope(sanFrancisco, 5000, 5000));
          }
        });
      }

      @Override
      public void mapExtentChanged(final MapEvent arg0) {
      }
    });

    // online feature layer
    onlineLayer = new ArcGISFeatureLayer(ONLINE_SERVICE_URL + "/0");
    map.getLayers().add(onlineLayer);

    window.getContentPane().add(map, BorderLayout.CENTER);

    // ---------------------------------------------------------------------------------------------
    // Graphics Layers
    // ---------------------------------------------------------------------------------------------
    analysisGraphicsLayer = new GraphicsLayer();
    map.getLayers().add(analysisGraphicsLayer);

    stopsGraphicsLayer = new GraphicsLayer();
    map.getLayers().add(stopsGraphicsLayer);
    routeGraphicsLayer = new GraphicsLayer();
    map.getLayers().add(routeGraphicsLayer);

    // ---------------------------------------------------------------------------------------------
    // Map Overlay - to capture mouse click on top of map
    // ---------------------------------------------------------------------------------------------
    map.addMapOverlay(onMapClick);


    // toolbar ...

    toolbar = UI.createToolbar();
    window.getContentPane().add(toolbar, BorderLayout.WEST);

    // ---------------------------------------------------------------------------------------------
    // Current online/offline status
    // ---------------------------------------------------------------------------------------------
    UI.addBigSeparator(toolbar); 
    lblCurrentMode = UI.createLabel("             ONLINE              ");
    toolbar.add(lblCurrentMode);

    // ---------------------------------------------------------------------------------------------
    // Analysis
    // ---------------------------------------------------------------------------------------------
    UI.addBigSeparator(toolbar);    
    JButton btnAnalysis = UI.createButton("IMPACT ANALYSIS");
    btnAnalysis.addActionListener(onBtnImpactAnalysisClick);
    toolbar.add(btnAnalysis);

    // ---------------------------------------------------------------------------------------------
    // Download/Sync
    // ---------------------------------------------------------------------------------------------
    UI.addBigSeparator(toolbar);
    btnDownload = UI.createButton("DOWNLOAD");
    btnDownload.addActionListener(onBtnDownloadClick);
    toolbar.add(btnDownload);

    // ---------------------------------------------------------------------------------------------
    // Go offline
    // ---------------------------------------------------------------------------------------------
    btnOffline = UI.createButton("GO OFFLINE");
    btnOffline.addActionListener(onBtnGoOfflineClick);
    toolbar.add(btnOffline);

    // ---------------------------------------------------------------------------------------------
    // Offline routing
    // ---------------------------------------------------------------------------------------------
    UI.addSeparator(toolbar);
    btnAddStops = UI.createButton("ADD STOPS");
    btnAddStops.addActionListener(onBtnAddStopsClick);
    toolbar.add(btnAddStops);

    btnAddBarriers = UI.createButton("ADD BARRIERS");
    btnAddBarriers.addActionListener(onBtnAddBarriersClick);
    toolbar.add(btnAddBarriers);

    btnRoute = UI.createButton("ROUTE");
    btnRoute.addActionListener(onBtnRouteClick);
    toolbar.add(btnRoute);

    btnLiveRoute = UI.createButton("LIVE ROUTE");
    btnLiveRoute.addActionListener(onBtnLiveRouteClick);
    toolbar.add(btnLiveRoute);

    // ---------------------------------------------------------------------------------------------
    // Edit
    // ---------------------------------------------------------------------------------------------
    UI.addBigSeparator(toolbar);

    btnEdit = UI.createButton("EDIT");
    btnEdit.addActionListener(onBtnEditClick);
    toolbar.add(btnEdit);

    // ---------------------------------------------------------------------------------------------
    // Go online
    // ---------------------------------------------------------------------------------------------
    btnOnline = UI.createButton("GO ONLINE");
    btnOnline.addActionListener(onBtnGoOnlineClick);
    toolbar.add(btnOnline);

    // ---------------------------------------------------------------------------------------------
    // Sync
    // ---------------------------------------------------------------------------------------------
    btnSync = UI.createButton("UPLOAD / SYNC");
    btnSync.addActionListener(onBtnSyncClick);
    toolbar.add(btnSync);

    // ---------------------------------------------------------------------------------------------
    // Exit
    // ---------------------------------------------------------------------------------------------
    UI.addBigSeparator(toolbar);
    JButton btnExit = UI.createButton("EXIT");
    btnExit.addActionListener(onBtnExitClick); 
    toolbar.add(btnExit);

    // ---------------------------------------------------------------------------------------------
    // Toolkit - Overlays
    // ---------------------------------------------------------------------------------------------
    ScaleBarOverlay scaleBar = new ScaleBarOverlay();
    scaleBar.setLocation(LocationOnMap.BOTTOM_RIGHT);
    map.addMapOverlay(scaleBar);

    NavigatorOverlay navigator = new NavigatorOverlay();
    navigator.setForeground(Color.WHITE);
    navigator.setBackground(UI.BACKGROUND);
    map.addMapOverlay(navigator);

    updateUI(true);
    window.setVisible(true);
  }

  // Event handlers...

  //---------------------------------------------------------------------------------------------
  // Map click
  //---------------------------------------------------------------------------------------------
  private MapOverlay onMapClick = new MapOverlay() {
    private static final long serialVersionUID = 1L;
    @Override
    public void onMouseClicked(MouseEvent event) {
      super.onMouseClicked(event);
      Point clickedPoint = map.toMapPoint(event.getX(), event.getY());
      switch (onMapClickOperation) {
        case IMPACT_ANALYSIS:
          // create impact area at clicked point
          Geometry impactArea = GeometryEngine.buffer(
              clickedPoint, 
              map.getSpatialReference(), 
              500, 
              map.getSpatialReference().getUnit());

          // merge with existing impact areas
          Geometry totalImpactArea = impactArea;
          int[] ids = analysisGraphicsLayer.getGraphicIDs();
          if (ids != null) {
            Geometry[] impactAreas = new Geometry[ids.length + 1];
            impactAreas[0] = impactArea;
            int numImpactAreas = 1;
            for (int id : ids) {
              impactAreas[numImpactAreas++] = 
                  analysisGraphicsLayer.getGraphic(id).getGeometry();
            }
            totalImpactArea = GeometryEngine.union(impactAreas, map.getSpatialReference());
          }

          // add result to graphics layer
          analysisGraphicsLayer.removeAll();
          analysisGraphicsLayer.addGraphic(new Graphic(totalImpactArea, UI.SYM_LIGHT));
          break;
        case ADD_STOPS:
          Graphic graphic = new Graphic(clickedPoint, UI.STOP_SYM);
          stopsGraphicsLayer.addGraphic(graphic);
          stops.addFeature(graphic);

          TextSymbol symPoint = new TextSymbol(16, ++numStops + "", Color.WHITE);
          stopsGraphicsLayer.addGraphic(new Graphic(clickedPoint, symPoint));
          break;
        case ADD_BARRIERS:
          Graphic barrierGraphic = new Graphic(clickedPoint, UI.BARRIER_SYM);
          stopsGraphicsLayer.addGraphic(barrierGraphic);
          barriers.addFeature(barrierGraphic);
          break;
        default:
          break;
      }
    }

    @Override
    public void onMouseMoved(MouseEvent event) {
      if (!liveRoute) {
        return;
      }
      Point mousePoint = map.toMapPoint(event.getX(), event.getY());
      Graphic origin = stops.getFeatures().get(0);
      stops.clearFeatures();

      stops = new NAFeaturesAsFeature();
      stops.addFeature(origin);
      stops.addFeature(new Graphic(mousePoint, UI.STOP_SYM));

      findRoute();
    }
  };

  //---------------------------------------------------------------------------------------------
  // Analysis
  //---------------------------------------------------------------------------------------------
  private ActionListener onBtnImpactAnalysisClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      updateOverlays(false);
      onMapClickOperation = MapClickOperation.IMPACT_ANALYSIS;
    }
  };

  //---------------------------------------------------------------------------------------------
  // Download
  //---------------------------------------------------------------------------------------------
  private ActionListener onBtnDownloadClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      GeodatabaseTask geodatabaseSyncTask = new GeodatabaseTask(ONLINE_SERVICE_URL, null);

      //set up the parameters
      int[] layers = {0};
      GenerateGeodatabaseParameters params = new GenerateGeodatabaseParameters(
          layers, 
          map.getExtent(), 
          map.getSpatialReference(), 
          false, 
          SyncModel.LAYER,
          map.getSpatialReference());

      // status callback
      GeodatabaseStatusCallback statusCallback = new GeodatabaseStatusCallback() {
        @Override
        public void statusUpdated(GeodatabaseStatusInfo status) {
          //updateProgressBarUI("Latest status: " + status.getStatus(), true);
        }
      };

      // response callback
      CallbackListener<Geodatabase> responseCallback = new CallbackListener<Geodatabase>() {
        @Override
        public void onError(Throwable ex) {
          showError(ex);
        }

        @Override
        public void onCallback(Geodatabase geodatabase) {
          showMessage("File downloaded to: " + geodatabase.getPath());
        }
      };

      // Generate the geodatabase from the service and download
      geodatabaseSyncTask.submitGenerateGeodatabaseJobAndDownload(params, GEODATABASE_FILE_PATH, statusCallback, responseCallback);
      //updateProgressBarUI("Creating geodatabase from service...", true);
    }
  };

  //---------------------------------------------------------------------------------------------
  // Go Offline
  //---------------------------------------------------------------------------------------------
  private ActionListener onBtnGoOfflineClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      // remove online layer
      map.getLayers().remove(onlineLayer);

      // add offline layer
      try {
        Geodatabase offlineSource = new Geodatabase(GEODATABASE_FILE_PATH);
        FeatureTable offlineData = offlineSource.getGdbFeatureTableByLayerId(0);
        offlineLayer = new FeatureLayer(offlineData);
        map.getLayers().add(offlineLayer);

        // add extra tools
        addOnMapClickEdit(map, offlineLayer);
        if (route == null) {
          route = RouteTask.createLocalRouteTask(
              "data/building_apps/route/SanFrancisco/RuntimeSanFrancisco.geodatabase", "Streets_ND");
        }

        // set UI to offline mode
        updateUI(false);
      } catch (Exception ex) {
        showError(ex);
      }
    }
  };
  private MapOverlay onMapClickEdit;

  /**
   * Adds an overlay to the jMap to display an infopopup for every feature clicked 
   * by the mouse. 
   * @param jMap map to which the overlay will be added to as a map overlay.
   * @param featureLayer feature layer to associate with the overlay - attributes of 
   * clicked features in this layer will be displayed in a popup dialog
   */
  private void addOnMapClickEdit(JMap jMap, final FeatureLayer featureLayer) {
    onMapClickEdit = new MapOverlay() {
      private static final long serialVersionUID = 1L;
      @Override
      public void onMouseClicked(MouseEvent event) {
        // get first (top-most) graphic hit by the mouse
        final long featureId = featureLayer.getFeatureIDs(event.getX(), event.getY(), 5)[0];
        Feature hitFeature;
        try {
          hitFeature = featureLayer.getFeatureTable().getFeature(featureId);
          // create an editing popup view
          PopupView content = PopupView.createEditView("Edit Attributes", featureLayer);
          content.setSize(300, 200);
          content.setFeature(map, null, hitFeature);
          // create a popup dialog passing in the editing popup view
          final MapPopup popup = featureLayer.getMap().createPopup(new JComponent[]{content}, hitFeature);
          popup.setTitle("Edit Attributes");
          popup.setVisible(true);
          content.addPopupViewListener(new PopupViewListener() {
            @Override
            public void onCommitEdit(PopupViewEvent popupViewEvent, Feature feature) {
              try {
                featureLayer.getFeatureTable().updateFeature(featureId, feature);
              } catch (Exception ex) {
                ex.printStackTrace();
              }
              popup.close();
            }

            @Override
            public void onCancelEdit(PopupViewEvent popupViewEvent, Feature feature) {
              popup.close();
            }
          });
        } catch (TableException e) {
          showError(e);
        }
      }
    };
    jMap.addMapOverlay(onMapClickEdit);
    updateOverlays(true);
  }

  //---------------------------------------------------------------------------------------------
  // Add stops & barriers
  //---------------------------------------------------------------------------------------------
  private ActionListener onBtnAddStopsClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      updateOverlays(false);
      onMapClickOperation = MapClickOperation.ADD_STOPS;
    }
  };

  private ActionListener onBtnAddBarriersClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      updateOverlays(false);
      onMapClickOperation = MapClickOperation.ADD_BARRIERS;
    }
  };

  //---------------------------------------------------------------------------------------------
  // Route
  //---------------------------------------------------------------------------------------------
  private ActionListener onBtnRouteClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      liveRoute = false;
      findRoute();
    }
  };

  private ActionListener onBtnLiveRouteClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      liveRoute = true;
    }
  };

  private void findRoute() {
    try {
      // setup parameters
      RouteParameters routeParams = route.retrieveDefaultRouteTaskParameters();
      stops.setSpatialReference(map.getSpatialReference());
      routeParams.setStops(stops);

      barriers.setSpatialReference(map.getSpatialReference());
      routeParams.setPointBarriers(barriers);

      routeParams.setOutSpatialReference(map.getSpatialReference());

      // execute
      route.solve(routeParams, new CallbackListener<RouteResult>() {

        @Override
        public void onError(Throwable ex) {
          // showError(ex);
        }

        @Override
        public void onCallback(RouteResult result) {
          // remove existing graphics
          routeGraphicsLayer.removeAll();	

          StringBuilder msg = new StringBuilder();
          msg.append("Route completed.");
          Route topRoute = result.getRoutes().get(0);
          Graphic routeGraphic = new Graphic(topRoute.getRouteGraphic().getGeometry(), UI.createRouteSym());
          routeGraphicsLayer.addGraphic(routeGraphic);
          msg.append("\nTotal distance: " + " (" + topRoute.getTotalMiles() + " miles.)");
          for (RouteDirection direction : topRoute.getRoutingDirections()) {
            msg.append("\n" + direction.getText() + " (" + direction.getLength() + " miles.)");
          }
          //          showMessage(msg.toString());
        }
      });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  //---------------------------------------------------------------------------------------------
  // Edit
  //---------------------------------------------------------------------------------------------
  private ActionListener onBtnEditClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      updateOverlays(true);
    }
  };

  //---------------------------------------------------------------------------------------------
  // Go online
  //---------------------------------------------------------------------------------------------
  private ActionListener onBtnGoOnlineClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      updateUI(true);
    }
  };

  //---------------------------------------------------------------------------------------------
  // Sync
  //---------------------------------------------------------------------------------------------
  private ActionListener onBtnSyncClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        Geodatabase geodatabase = new Geodatabase(GEODATABASE_FILE_PATH);

        GeodatabaseTask syncTask = new GeodatabaseTask(ONLINE_SERVICE_URL, null);

        //set up the parameters
        SyncGeodatabaseParameters params = geodatabase.getSyncParameters();

        // status & response callbacks
        GeodatabaseStatusCallback statusCallback = new GeodatabaseStatusCallback() {
          @Override
          public void statusUpdated(GeodatabaseStatusInfo status) {
            //updateProgressBarUI("Latest status: " + status.getStatus(), true);
          }
        };

        CallbackListener<Geodatabase> responseCallback = 
            new CallbackListener<Geodatabase>() {
          @Override
          public void onError(Throwable ex) {
            showError(ex);
          }

          @Override
          public void onCallback(Geodatabase gdb) {
            showMessage("Data uploaded.");
          }
        };

        // sync
        syncTask.submitSyncJobAndApplyResults(params, geodatabase, statusCallback, responseCallback);
      } catch (Exception ex) {
        showError(ex);
      }
    }
  };

  //---------------------------------------------------------------------------------------------
  // Exit
  //---------------------------------------------------------------------------------------------
  private ActionListener onBtnExitClick = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      System.exit(1);
    }
  };

  private void showError(Throwable ex) {
    JOptionPane.showMessageDialog(map, "An error occured: "+ ex.getLocalizedMessage(), 
        "", JOptionPane.ERROR_MESSAGE);
    //updateProgressBarUI(null, false);
  }

  private void showMessage(String msg) {
    JOptionPane.showMessageDialog(map, msg, "", JOptionPane.INFORMATION_MESSAGE);
    //updateProgressBarUI(null, false);
  }

  private void updateUI(boolean isOnline) {
    lblCurrentMode.setText(isOnline ? 
        "             ONLINE              " : "             OFFLINE              ");
    lblCurrentMode.setBackground(isOnline ? UI.ON_HOVER : Color.RED);

    btnDownload.setVisible(isOnline);
    btnOffline.setVisible(isOnline);

    btnAddStops.setVisible(!isOnline);
    btnAddBarriers.setVisible(!isOnline);
    btnRoute.setVisible(!isOnline);
    btnLiveRoute.setVisible(!isOnline);
    btnEdit.setVisible(!isOnline);
    btnOnline.setVisible(!isOnline);
    btnSync.setVisible(offlineLayer != null && isOnline);
  }

  private void updateOverlays(boolean allowEdit) {
    onMapClick.setActive(!allowEdit);
    if (onMapClickEdit != null) {
      onMapClickEdit.setActive(allowEdit);
    }
  }

} 
