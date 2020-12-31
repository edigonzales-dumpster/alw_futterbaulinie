package futterbaulinie

import geoscript.feature.Feature
import geoscript.feature.Field
import geoscript.filter.Filter
import geoscript.geom.Geometry
import geoscript.geom.Point
import geoscript.layer.Format
import geoscript.layer.GeoTIFF
import geoscript.layer.Layer
import geoscript.layer.Raster
import geoscript.layer.Shapefile
import geoscript.workspace.Directory
import geoscript.workspace.GeoPackage
import geoscript.workspace.Workspace
import java.nio.file.Paths

/*
class App {
    String getGreeting() {
        return 'Hello World!'
    }

    static void main(String[] args) {
        println new App().greeting        
    }
}
*/

//Bemerkungen:
// - 32bit und predictor=2 funktioniert nicht
// - Reclassify: value=0 wird als nodata interpretiert?
// - Namen von Shapefiles? Es wird der Name des Schemas (read-only) verwendet.
// - Shapefile.dump() wird in QGIS nicht angezeigt. fid missing?

def directory = "/Users/stefan/Downloads/"

def tiles = [
    "2594000_1230000_vegetation_uncompressed",
    "2594500_1230000_vegetation_uncompressed",
    "2594000_1230500_vegetation_uncompressed"
    ]

for (tile in tiles) {
    println "Processing: " + tile
    
    File file = Paths.get(directory, tile + ".tif").toFile()
    GeoTIFF geotiff = new GeoTIFF(file)
    Raster raster = geotiff.read(tile)
    
    Raster reclassifiedRaster = raster.reclassify([
        [min:-9999, max:-9999, value: 2],
        [min:-9999, max:0,     value: 2],
        [min:0,     max:200,   value: 1]
    ])
    
    File outFile = Paths.get(directory, tile + "_step01.tif").toFile()
    Format outFormat = Format.getFormat(outFile)
    outFormat.write(reclassifiedRaster)
    
    Layer layer = reclassifiedRaster.polygonLayer    
    Workspace geopkg = new GeoPackage(Paths.get(directory, tile + "_step02.gpkg").toFile())
    geopkg.add(layer, tile)
 
    
    
    Filter filter = new Filter("(area(the_geom) > 10 AND value = 1) OR (value = 2 AND area(the_geom) < 10)")
    Layer layer3 = layer.filter(new Filter("(area(the_geom) > 10 AND value = 1) OR (value = 2 AND area(the_geom) < 10)"))
    layer3.update(new Field("value", "double"), 1, new Filter("area(the_geom) < 10 AND value = 2"))
    
    Workspace geopkg3 = new GeoPackage(Paths.get(directory, tile + "_step03.gpkg").toFile())
    geopkg3.add(layer3, tile)
    
}
    
    

//File file = new File("/Users/stefan/Downloads/2594000_1230000_vegetation_uncompressed.tif")
//GeoTIFF geotiff = new GeoTIFF(file)
//Raster raster = geotiff.read("2594000_1230000_vegetation_uncompressed")
//
//// value 0 ist nicht gut, wird als NULL/nodata interpretiert?
//Raster reclassifiedRaster = raster.reclassify([
//    [min:-9999, max:-9999, value: 2],
//    [min:-9999, max:0,     value: 2],
//    [min:0,     max:200,   value: 1]
//])
//
//File outFile = new File("/Users/stefan/Downloads/A_pa1.tif")
//Format outFormat = Format.getFormat(outFile)
//println outFormat.name
//outFormat.write(reclassifiedRaster)
//
//Layer layer = reclassifiedRaster.polygonLayer
//println layer.schema
//println layer.workspace // -> Memory Workspace
//
////Workspace geopkg = new GeoPackage(new File("/Users/stefan/Downloads/A_pa1.gpkg"))
////geopkg.add(layer, "A_pa1")
//
//// Ich verstehe den Filter nicht wirklich. Warum werden nicht bestockte Fläche (value=2) kleiner 10m2 nicht gelöscht,
//// sondern zu bestockten Flächen gemacht?
//// Variante 1
///*
//Layer layer3 = new Layer("A_pa3", layer.schema)
//layer.filter("(area(the_geom) > 10 AND value = 1) OR (value = 2 AND area(the_geom) < 10)").features.each { f -> 
//    Feature feat = new Feature(f.attributes, f.id, f.schema)
//    if (f.get("value") == 2 && f.geom.area < 10) {
//        feat.set("value", 1)
//    } 
//    layer3.add(feat)
//}
//Workspace geopkg3 = new GeoPackage(new File("/Users/stefan/Downloads/A_pa3.gpkg"))
//geopkg3.add(layer3, "A_pa3")
//*/
//
//// Variante 2
//Filter filter = new Filter("(area(the_geom) > 10 AND value = 1) OR (value = 2 AND area(the_geom) < 10)")
//Layer layer3 = layer.filter(filter)
//layer3.update(new Field("value", "double"), 1, new Filter("area(the_geom) < 10 AND value = 2")) 
//
//Workspace geopkg3 = new GeoPackage(new File("/Users/stefan/Downloads/A_pa3_v2.gpkg"))
//geopkg3.add(layer3, "A_pa3_v2")
//
//Geometry g = (Geometry)layer3.getFeatures().get(0).geom
//
//
////Directory workspace = Shapefile.dump(new File("/Users/stefan/Downloads"), layer)
//
////Workspace geopkg = new GeoPackage(new File("/Users/stefan/Downloads/A_pa1.gpkg"))
////geopkg.add(layer, "A_pa1")

println("Hallo Welt.")
