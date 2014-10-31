## GeoJSON sample with ArcGIS Runtime for Java

![Screenshot](/geojson/screenshot.png?raw=true "Screenshot")

## About

This [application](/geojson/src/GeoJsonApp.java) uses a [parser](/geojson/src/GeoJsonParser.java) that can 
parse [GeoJson](http://geojson.org/), and convert the [GeoJson data](/geojson/countries.geojson) to 
ArcGISRuntime Feature and Geometry. After parsing, the features are added to a GraphicsLayer.

## Instructions to run the sample
1. Download & Install [ArcGIS Runtime SDK for Java](https://developers.arcgis.com/java/)
2. Open the Eclipse project in this folder.
3. Run GeoJsonApp.java.

## Credits
Data file countries.geojson is from https://github.com/johan/.

## GeoJSON
GeoJSON is an open standard for representing geographical features and their attributes. Based on the JSON (JavaScript Object Notation) format, GeoJSON inherits the advantages of being readable, simple and lightweight. Many GIS technologies and services now support GeoJSON.

At its core, it defines a type called Feature. A Feature has a geometry and optional properties. The geometry types supported are Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon. Properties are basically name-value pairs. A set of features is represented by a FeatureCollection.

The following example represents the Los Angeles airport as a GeoJSON Feature. The feature geometry is of type Point with a long-lat value of (-118.40, 33.93). The airport code LAX and its elevation 38 (meters) are specified as properties.
```
{ 
  "type": "Feature",
  "geometry": {
    "type": "Point", 
    "coordinates": [-118.40, 33.93]
  },
  "properties": {
    "code": "LAX",
    "elevation": 38,
  }
}
```

## Mapping GeoJSON Feature to ArcGIS Runtime Feature
The structure of a Feature in ArcGIS Runtime is very similar to that of a GeoJSON Feature - it consists of a Geometry and an optional map of name-value pairs called attributes. The properties of a GeoJSON feature can be directly mapped to the attributes of a ArcGIS Runtime feature. 

The mapping between the GeoJSON geometry and ArcGIS Runtime geometry is as per table below.

GeoJSON         | ArcGIS Runtime SDK (com.esri.core.geometry)
-------         | ------------------        
Point           | Point
MultiPoint      | MultiPoint
LineString      | Polyline
MultiLineString | Polyline
Polygon         | Polygon
MultiPolygon    | Polygon
  
Geometry types of GeoJSON and corresponding type in ArcGIS Runtime

## Adding GeoJSON features to a ArcGIS Runtime application
This requires parsing GeoJSON data to ArcGIS Runtime features and then adding the features to the application. 
The class GeoJsonParser can parse GeoJSON data and return data as a collection of ArcGIS Runtime Feature. The example below shows how to use the parser.

```
// create an instance of the parser
GeoJsonParser geoJsonParser = new GeoJsonParser();

// parse the input file in GeoJSON format
List<Feature> features = geoJsonParser.parseFeatures(<GeoJSON file>);
```

After the GeoJSON data is parsed into ArcGISRuntime features, they can be added to a ArcGISRuntime GraphicsLayer.

```
// add parsed features to a layer
GraphicsLayer graphicsLayer = new GraphicsLayer();
for (Feature f : features) {
  graphicsLayer.addGraphic(new Graphic(f.getGeometry(), f.getSymbol(), f.getAttributes()));
}
```

## References:
1. GeoJSON - http://geojson.org/
2. ArcGIS Runtime SDK for Java - https://developers.arcgis.com/java/


