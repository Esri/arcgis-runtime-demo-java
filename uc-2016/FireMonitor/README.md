## Create a Map and MapView
```java
  private void createMapAndMapView() {
    // create a map
    map = new ArcGISMap();

    //make the basemap for streets
    map.setBasemap(Basemap.createStreets());

    //create the MapView JavaFX control and assign its map
    mapView = new MapView();
    mapView.setMap(map);
  }
```

## Add a feature layer

## Geocode

## Download feature data

## Geometry operation

## Edit offline

## Sync edits

## 3D