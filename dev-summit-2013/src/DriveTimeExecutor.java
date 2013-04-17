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

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.MouseInputAdapter;

import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.tasks.ags.geoprocessing.GPFeatureRecordSetLayer;
import com.esri.core.tasks.ags.geoprocessing.GPParameter;
import com.esri.core.tasks.ags.geoprocessing.GPString;
import com.esri.core.tasks.ags.geoprocessing.Geoprocessor;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;

/**
 * Class that executes a remote geoprocessing service to calculate zones with different drive times.
 */
public class DriveTimeExecutor extends MouseInputAdapter {

  // URL to remote geoprocessing service
  private final String URL_GEOPROCESSING_SERVICE = 
    "http://sampleserver1.arcgisonline.com/ArcGIS/rest/services/Network/ESRI_DriveTime_US/GPServer/CreateDriveTimePolygons";

  // symbology
  private final SimpleMarkerSymbol SYM_START_POINT = new SimpleMarkerSymbol(Color.RED, 16, Style.DIAMOND);

  private final SimpleLineSymbol SYM_ZONE_BORDER = new SimpleLineSymbol(Color.WHITE, 1);

  private final SimpleFillSymbol[] zoneFillSymbols = new SimpleFillSymbol[] { 
    new SimpleFillSymbol(new Color(0, 50, 0, 50), SYM_ZONE_BORDER), 
    new SimpleFillSymbol(new Color(0, 50, 0, 100), SYM_ZONE_BORDER), 
    new SimpleFillSymbol(new Color(0, 50, 0, 150), SYM_ZONE_BORDER) };

  JMap jMap;

  GraphicsLayer graphicsLayer;

  boolean enabled = true;

  /**
   * Creates an object that executes the remote geoprocessing service to calculate zones with different drive times.
   * 
   * @param jMap map to get the start point of drive, and to get the spatial reference.
   * @param graphicsLayer graphics layer to which the result will be added.
   */
  public DriveTimeExecutor(JMap jMap, GraphicsLayer graphicsLayer) {
    this.jMap = jMap;
    this.graphicsLayer = graphicsLayer;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Computes the drive time zones on click of the mouse.
   */
  @Override
  public void mouseClicked(MouseEvent mapEvent) {

    super.mouseClicked(mapEvent);

    if (!enabled) {
      return;
    }

    if (mapEvent.getButton() == MouseEvent.BUTTON3) {
      // remove zones from previous computation
      graphicsLayer.removeAll();
      return;
    }

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
    geoprocessor.setOutSR(jMap.getSpatialReference());
    geoprocessor.setProcessSR(jMap.getSpatialReference());

    // initialize the required input parameters: refer to help link in the
    // geoprocessing service URL for a list of required parameters
    List<GPParameter> gpInputParams = new ArrayList<GPParameter>();

    GPFeatureRecordSetLayer gpInputStartpoint = new GPFeatureRecordSetLayer("Input_Location");
    gpInputStartpoint.addGraphic(startPointGraphic);

    GPString gpInputDriveTimes = new GPString("Drive_Times");
    gpInputDriveTimes.setValue("1 2 3");

    gpInputParams.add(gpInputStartpoint);
    gpInputParams.add(gpInputDriveTimes);

    geoprocessor.executeAsync(gpInputParams, new CallbackListener<GPParameter[]>() {

      @Override
      public void onError(Throwable th) {
        th.printStackTrace();
      }

      @Override
      public void onCallback(GPParameter[] result) {
        processResult(result);
      }
    });

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
          Graphic theGraphic = new Graphic(graphic.getGeometry(), zoneFillSymbols[zone++]);
          // add to the graphics layer
          graphicsLayer.addGraphic(theGraphic);
        }
      }
    }
  }
}