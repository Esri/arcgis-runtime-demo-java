For DataCollector/DataAnalysis App:

Note that local server unpacks the .mpk to the following directory on Windows:
C:\Users\<user>\AppData\Local\ArcGISRuntime\Documents\ArcGIS\Packages\<package-name>\v101

Whenever a feature is updated, the .mxd and file .gdb at the unpacked folder are updated.

To create a new .mpk from ArcMap that contains the updated features, use the .mxd from the 
unpacked location.

Also, note that local server will not unpack the .mpk if its unpacked folder already exists.

