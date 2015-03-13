package devsummit2015.advancedtopics;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.CompositeSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.core.symbol.Symbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.map.FeatureLayer;
import com.esri.map.GraphicsLayer;

public class ClusterLayer extends GraphicsLayer {
  
  public static enum ClusterType {
    POINT,
    BOUNDING_RECTANGLE,
    CONVEX_HULL
  }

  private List<ClusterableLocation> pointsToCluster = new ArrayList<ClusterableLocation>();
  private GraphicsLayer             graphicsLayer;
  private FeatureLayer              featureLayer;
  private double                    clusterDistanceFactor = 0.04;
  private double                    clusterDistance;
  private int                       minimumPoints = 20;
  private double                    minimumScale = 10000;
  private ClusterType               clusterType = ClusterType.POINT;

  public ClusterLayer(GraphicsLayer graphicsLayer) {
    this.graphicsLayer = graphicsLayer;
  }

  public ClusterLayer(FeatureLayer featureLayer) {
    this.featureLayer = featureLayer;
  }

  public void updateClusters() {
    if (!clustersNeedUpdate()) {
      return;
    }
    
    showInputLayer(false);
    DBSCANClusterer<ClusterableLocation> clusterer = 
        new DBSCANClusterer<ClusterableLocation>(clusterDistance, 0);
    add(clusterer.cluster(pointsToCluster));
  }
  
  private void add(List<Cluster<ClusterableLocation>> clusters) {
    int max = getMax(clusters);
    for (Cluster<ClusterableLocation> cluster : clusters) {
      Geometry geometry = null;
      Symbol symbol = null;
      Color color = cluster.getPoints().size() == max ? Color.RED : Color.GRAY;
      ClusterType thisClusterType = cluster.getPoints().size() < 2 ? ClusterType.POINT : clusterType;
      switch (thisClusterType) {
        default:
        case POINT:
          geometry = getCenter(cluster);
          symbol = createPointSymbol(color, cluster.getPoints().size());
          break;
        case BOUNDING_RECTANGLE:
          geometry = getBoundingRectangle(cluster);
          symbol = createPolygonSymbol(color, cluster.getPoints().size());
          break;
        case CONVEX_HULL:
          geometry = getConvexHull(cluster);
          symbol = createPolygonSymbol(color, cluster.getPoints().size());
          break;
      };
      addGraphic(new Graphic(geometry, symbol));
    }
  }
  
  private void showInputLayer(boolean show) {
    if (graphicsLayer != null) {
      graphicsLayer.setVisible(show);
    }
    if (featureLayer != null) {
      featureLayer.setVisible(show);
    }
  }

  private boolean clustersNeedUpdate() {
    removeAll();
    pointsToCluster.clear();
    
    if (graphicsLayer.getMap().getScale() < minimumScale) {
      showInputLayer(true);
      return false;
    }
    
    if (graphicsLayer != null) {
      clusterDistance = 
          graphicsLayer.getMap().getExtent().getWidth() * clusterDistanceFactor;
      Envelope extent = graphicsLayer.getMap().getExtent();
      
      //TODO: better API to get graphics in current extent.
      for (int id : graphicsLayer.getGraphicIDs()) {
        Graphic g = graphicsLayer.getGraphic(id);
        if (g.getGeometry() instanceof Point && extent.contains((Point) g.getGeometry())) {
          pointsToCluster.add(new ClusterableLocation(id, g));
        }
      }
    }
    try {
      if (featureLayer != null) {
        clusterDistance = featureLayer.getMap().getExtent().getWidth() * clusterDistanceFactor;
        QueryParameters params = new QueryParameters();
        params.setGeometry(featureLayer.getMap().getExtent());
        Future<FeatureResult> featuresFuture = featureLayer.getFeatureTable().queryFeatures(params, null);
        for (Object feature : featuresFuture.get()) {
          Feature f = (Feature) feature;
          pointsToCluster.add(new ClusterableLocation((int) f.getId(), f));
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException("Failed to intitialize cluster");
    }
    
    if (pointsToCluster.size() < minimumPoints) {
      showInputLayer(true);
      pointsToCluster.clear();
      return false;
    }
    
    return true;
  }
  
  private Point getCenter(Cluster<ClusterableLocation> cluster) {
    double sumx = 0;
    double sumy = 0;
    for (ClusterableLocation p : cluster.getPoints()) {
      sumx += p.getPoint()[0];
      sumy += p.getPoint()[1];
    }
    Point center = new Point(sumx / cluster.getPoints().size(), sumy/ cluster.getPoints().size());
    return center;
  }
  
  private Envelope getBoundingRectangle(Cluster<ClusterableLocation> cluster) {
    double xmin = cluster.getPoints().get(0).getPoint()[0];
    double xmax = xmin;
    double ymin = cluster.getPoints().get(0).getPoint()[1];
    double ymax = ymin;
    for (ClusterableLocation p : cluster.getPoints()) {
      if (p.getPoint()[0] < xmin) {
        xmin = p.getPoint()[0];
      }
      if (p.getPoint()[0] > xmax) {
        xmax = p.getPoint()[0];
      }
      if (p.getPoint()[1] < ymin) {
        ymin = p.getPoint()[1];
      }
      if (p.getPoint()[1] > ymax) {
        ymax = p.getPoint()[1];
      }
    }
    Envelope boundingRectangle = new Envelope(xmin, ymin, xmax, ymax);
    return boundingRectangle;
  }
  
  private Polygon getConvexHull(Cluster<ClusterableLocation> cluster) {
    List<ClusterableLocation> pointsList = cluster.getPoints();
    ClusterableLocation[] pointsArray = pointsList.toArray(new ClusterableLocation[pointsList.size()]);
    ClusterableLocation[] pointsConvexHull = ConvexHullGenerator.generateHull(pointsArray);
    Polygon convexHull = new Polygon();
    convexHull.startPath(pointsConvexHull[0].getPoint()[0], pointsConvexHull[0].getPoint()[1]);
    for (int i = 1; i < pointsConvexHull.length; i++) {
      ClusterableLocation p = pointsConvexHull[i];
      convexHull.lineTo(p.getPoint()[0], p.getPoint()[1]);
    }
    convexHull.closePathWithLine();
    return convexHull;
  }
      
  private int getMax(List<Cluster<ClusterableLocation>> clusters) {
    int max = Integer.MIN_VALUE;
    for (Cluster<ClusterableLocation> cluster : clusters) {
      if (cluster.getPoints().size() > max) {
        max = cluster.getPoints().size();
      }
    }
    return max;
  }
  
  private Symbol createPolygonSymbol(Color color, int num) {
    List<Symbol> symbols = new ArrayList<Symbol>();
    symbols.add(new SimpleFillSymbol(new Color(color.getRed(),
        color.getGreen(), color.getBlue(), 70)));
    symbols.add(new TextSymbol(14, "" + num, Color.WHITE));
    CompositeSymbol cs = new CompositeSymbol(symbols);
    return cs;
  }

  private Symbol createPointSymbol(Color color, int num) {
    if (num < 2) {
      Symbol sms = new SimpleMarkerSymbol(color, 16, Style.CIRCLE);
      return sms;
    }
    List<Symbol> symbols = new ArrayList<Symbol>();
    symbols.add(new SimpleMarkerSymbol(color, 32, Style.CIRCLE));
    symbols.add(new TextSymbol(14, "" + num, Color.WHITE));
    CompositeSymbol cs = new CompositeSymbol(symbols);
    return cs;
  }
}

abstract class PointComparable implements Comparable<ClusterableLocation> {

}

class ClusterableLocation extends PointComparable implements Clusterable {

  double[]    xy = new double[2];
  private int id;

  ClusterableLocation(int id, Feature f) {
    Point p = (Point) f.getGeometry();
    xy[0] = p.getX();
    xy[1] = p.getY();
    this.id = id;
  }
  
  ClusterableLocation(double x, double y) {
    xy[0] = x;
    xy[1] = y;
  }

  @Override
  public double[] getPoint() {
    return xy;
  }

  public int getId() {
    return id;
  }

  // sort first on x then on y
  public int compareTo(ClusterableLocation other) {
    if (xy[0] == other.xy[0])
      return (int) (xy[1] - other.xy[1]);
    else
      return (int) (xy[0] - other.xy[0]);
  }

  // cross product of two vectors
  public double cross(ClusterableLocation p) {
    return xy[0] * p.xy[1] - xy[1] * p.xy[0];
  }

  // subtraction of two points
  public ClusterableLocation sub(ClusterableLocation p) {
    return new ClusterableLocation(xy[0] - p.xy[0], xy[1] - p.xy[1]);
  }
}
