MyMapApp.java
-------------
This demonstrates the power of the API to create rich applications easily.
It shows 
- how to create a application with a basemap
- add a layer with GPS functionality
- perform geocoding
- perform routing
- add built-in controls (such as scalebar).

MapsAndLayersApp.java
---------------------
This application
- shows how to add layers to a map
- uses map's event listeners
- highlights differences between a regular Map and WebMap.

DataCollectorApp.java
---------------------
This application shows how to work with map offline, and perform local editing.

DataAnalysisApp.java, DriveTimeExecutor.java
--------------------------------------------
This application shows how to work perform analysis with geoprocessing.

DemoTheatreApp.java, DemoTheatreAppImproved.java
------------------------------------------------
The DemoeTheatreApp.java is an example of an application that does not
follow best practices. The DemoTheatreAppImproved.java shows the same
application following the best practices. The history of check-ins of this
file reveals the improvements made.

Notes:
------

For DataCollector/DataAnalysis App:

Note that local server unpacks the .mpk to the following directory on Windows:
C:\Users\<user>\AppData\Local\ArcGISRuntime\Documents\ArcGIS\Packages\<package-name>\v101

Whenever a feature is updated, the .mxd and file .gdb at the unpacked folder are updated.

To create a new .mpk from ArcMap that contains the updated features, use the .mxd from the 
unpacked location.

Also, note that local server will not unpack the .mpk if its unpacked folder already exists.

