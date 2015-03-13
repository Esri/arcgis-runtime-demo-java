package devsummit2015.javafx;

import java.awt.Color;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.symbol.Symbol;
import com.esri.map.ArcGISTiledMapServiceLayer;
import com.esri.map.FXMap;
import com.esri.map.GraphicsLayer;
import com.esri.map.LayerInitializeCompleteEvent;
import com.esri.map.LayerInitializeCompleteListener;


public class SampleController {
	
	  @FXML
	  private Parent root;

	  @FXML
	  private FXMap map;
	  
	  @FXML
	  private Button btnAdd;
	  
	  @FXML
	  private Button btnExit;
	  
	  @FXML
	  private Button btnIdentify;
	  
	  @FXML
	  private HBox boxSymbolContainer;
	  
	  @FXML
	  private VBox boxMenu;
	  
	  @FXML
	  private Button btnMenu;
	  
	  private MapMode mapMode = MapMode.NONE;
	  private GraphicsLayer graphicsLayer;
	  private SimpleMarkerSymbol symbol;
	  private boolean isMenuVisible = false;
	  
	  // default constructor
	  public SampleController() { }
	
	  //This method is invoked by JavaFX framework
	  public void initialize() {
		map.setWrapAroundEnabled(true);  
		 
	    // Create a tiled map service layer with an ArcGIS Online map service URL
	    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(
	      "http://services.arcgisonline.com/arcgis/rest/services/NatGeo_World_Map/MapServer");
	    tiledLayer.addLayerInitializeCompleteListener(new LayerInitializeCompleteListener() {
			
			@Override
			public void layerInitializeComplete(LayerInitializeCompleteEvent arg0) {
				map.zoomByFactor(4);
				
			}
		});

	    // Add the layer to the list of layers in the map
	    map.getLayerList().add(tiledLayer);
	    
	    graphicsLayer = new GraphicsLayer();
		map.getLayerList().add(graphicsLayer);
		
		map.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (mapMode == MapMode.ADD_GRAPHIC) {
					Point mapPoint = map.screenPointToMapPoint((int) event.getX(), (int) event.getY());
					Map<String, Object> attributes = new HashMap<String, Object>();
					attributes.put("Color", symbol.getColor());
					attributes.put("Added by", "Vijay");
					attributes.put("Added on", new Date().toString());
					graphicsLayer.addGraphic(new Graphic(mapPoint, symbol, attributes));
				} else if (mapMode == MapMode.IDENTIFY_GRAPHIC) {
					int[] ids = graphicsLayer.getGraphicIDs(
							(int) event.getX(), (int) event.getY(), 10);
					if (ids.length == 0) {
						return;
					}
					final Stage dialog = new Stage();
	                dialog.initModality(Modality.APPLICATION_MODAL);
	                dialog.initOwner(root.getScene().getWindow());
	                VBox dialogVbox = new VBox(20);
	                
	                Graphic hitGraphic = graphicsLayer.getGraphic(ids[0]);
	                
	                StringBuilder sb = new StringBuilder();
	                for (Map.Entry<String, Object> attr: hitGraphic.getAttributes().entrySet()) {
	                	sb.append(attr.getKey() + ": " + attr.getValue());
	                	sb.append("\n");
	                }
	                
	                dialogVbox.getChildren().add(new Text(sb.toString()));
	                Scene dialogScene = new Scene(dialogVbox, 300, 200);
	                dialog.setScene(dialogScene);
	                dialog.show();
				} else {
					map.zoomByFactor(2);
				}
				event.consume();

			}
		});
		
		//map.setExtent(new Point(0, 0));
	  }
	  
	  @FXML
	  private void setModeToAdd(ActionEvent event) {
		  mapMode = MapMode.ADD_GRAPHIC;
		  boxSymbolContainer.setVisible(true);
	  }
	  
	  @FXML
	  private void setModeToIdentify(ActionEvent event) {
		  mapMode = MapMode.IDENTIFY_GRAPHIC;
	  }
	  
	  @FXML
	  private void exit(ActionEvent event) {
		  System.exit(0);
	  }
	  
	  @FXML
	  private void selectRedSymbol(ActionEvent event) {
		  symbol = new SimpleMarkerSymbol(Color.RED, 12, Style.CIRCLE);
		  mapMode = MapMode.ADD_GRAPHIC;
	  }
	  
	  @FXML
	  private void selectGreenSymbol(ActionEvent event) {
		  symbol = new SimpleMarkerSymbol(Color.GREEN, 12, Style.CIRCLE);
		  mapMode = MapMode.ADD_GRAPHIC;
	  }
	  
	  @FXML
	  private void selectBlueSymbol(ActionEvent event) {
		  symbol = new SimpleMarkerSymbol(Color.BLUE, 12, Style.CIRCLE);
		  mapMode = MapMode.ADD_GRAPHIC;
	  }
	  
	  @FXML
	  private void showMenu(ActionEvent event) {
		  isMenuVisible = !isMenuVisible;
		  boxSymbolContainer.setVisible(isMenuVisible);
		  btnIdentify.setVisible(isMenuVisible);
		  btnExit.setVisible(isMenuVisible);
		  if (isMenuVisible) {
			  mapMode = MapMode.ADD_GRAPHIC;
		  } else {
			  mapMode = MapMode.NONE;
		  }
	  }
	  
	  @FXML
	  private void hideMenu(ActionEvent event) {
		  boxSymbolContainer.setVisible(false);
	  }
}

enum MapMode {
	NONE, ADD_GRAPHIC, IDENTIFY_GRAPHIC
}
