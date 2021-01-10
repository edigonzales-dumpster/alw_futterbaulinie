package futterbaulinie

import geoscript.feature.Feature
import geoscript.feature.Field
import geoscript.feature.Schema
import geoscript.geom.Geometry
import geoscript.geom.MultiPolygon
import geoscript.layer.Format
import geoscript.layer.GeoTIFF
import geoscript.layer.Layer
import geoscript.layer.Raster
import geoscript.filter.Filter
import geoscript.workspace.GeoPackage
import geoscript.workspace.Memory
import geoscript.workspace.Workspace
import org.gdal.gdal.TranslateOptions

import java.nio.file.Paths

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstJNI

def DOWNLOAD_FOLDER = "/Volumes/Samsung_T5/geodata/ch.so.agi.lidar_2019.ndsm_vegetation/"
def DOWNLOAD_URL = "https://geo.so.ch/geodata/ch.so.agi.lidar_2019.ndsm_vegetation/"

def TILES_FOLDER = "/Volumes/Samsung_T5/alw_futterbaulinie/uncompressed/"
def RECLASSIFIED_RASTER_FOLDER = "/Volumes/Samsung_T5/alw_futterbaulinie/reclassified/"
def RECLASSIFIED_VECTOR_FOLDER = "/Volumes/Samsung_T5/alw_futterbaulinie/reclassified_vector/"
def RECLASSIFIED_VECTOR_FILTERED_FOLDER = "/Volumes/Samsung_T5/alw_futterbaulinie/reclassified_vector_filtered/"
def RECLASSIFIED_VECTOR_FILTERED_DISSOLVED_FOLDER = "/Volumes/Samsung_T5/alw_futterbaulinie/reclassified_vector_filtered_dissolved/"

// Read (gdal) VRT file to get a list of all tif files.
def vrt = new groovy.xml.XmlParser().parse("/vagrant/data/vegetation.vrt")
def tiles = vrt.VRTRasterBand[0].ComplexSource.collect { it ->
    it.SourceFilename.text().reverse().drop(4).reverse()
}

def directory = "/Users/stefan/Downloads/"

//def tiles = [
//    "2594000_1230000_vegetation",
//    "2594500_1230000_vegetation",
//    "2594000_1230500_vegetation"
//    ]

// Uncompress tif file since geotools/geoscript cannot handle 32bit and deflate/predictor compressed files.
gdal.AllRegister()
gdal.UseExceptions()
println("Running against GDAL " + gdal.VersionInfo())

//DOWNLOAD_FOLDER = "/vagrant/data/"
for (tile in tiles) {
    println "Uncompressing: " + tile
    if (new File(TILES_FOLDER, tile + ".tif").exists()) new File(TILES_FOLDER, tile + ".tif").delete()
    Dataset dataset = gdal.Open(DOWNLOAD_FOLDER + tile + ".tif", gdalconstJNI.GA_ReadOnly_get());
    Vector<String> optionsVector = new Vector<>();
    optionsVector.add("-co");
    optionsVector.add("TILED=TRUE");
    gdal.Translate(Paths.get(TILES_FOLDER, tile + ".tif").toFile().getAbsolutePath(), dataset, new TranslateOptions(optionsVector))
    dataset.delete()
}
gdal.GDALDestroyDriverManager();

for (tile in tiles) {
    println "Processing: " + tile

    // Raster wird umklassiert:
    // - Bestockt ja/nein.
    // - Tiefe Bestockung (<2m) wird als zu unbestockt umklassiert.
    println "Reclassify raster..."
    File file = Paths.get(TILES_FOLDER, tile + ".tif").toFile()
    GeoTIFF geotiff = new GeoTIFF(file)
    Raster raster = geotiff.read(tile)

    Raster reclassifiedRaster = raster.reclassify([
            [min:-9999, max:-9999, value: 2],
            [min:-9999, max:0,     value: 2],
            [min:0,     max:2,     value: 2],
            [min:2,     max:200,   value: 1]
    ])

    File outFile = Paths.get(RECLASSIFIED_RASTER_FOLDER, tile + ".tif").toFile()
    Format outFormat = Format.getFormat(outFile)
    outFormat.write(reclassifiedRaster)

    // Vektorisierung des Rasters
    Layer layer = reclassifiedRaster.polygonLayer
//    Workspace geopkg = new GeoPackage(Paths.get(RECLASSIFIED_VECTOR_FOLDER, tile + ".gpkg").toFile())
//    geopkg.add(layer, tile)

    // Bestockte Flächen <10m werden ignoriert.
    // Unbestockte Flächen <10 werden zu bestockten Flächen. Annahme: Es handelt sich um kleiner Löcher grösseren
    // bestockten Flächen.
    Filter filter = new Filter("(area(the_geom) > 10 AND value = 1) OR (value = 2 AND area(the_geom) < 10)")
    Layer filteredLayer = layer.filter(filter)
    filteredLayer.update(new Field("value", "double"), 1, new Filter("area(the_geom) < 10 AND value = 2"))

//    Workspace geopkg = new GeoPackage(Paths.get(RECLASSIFIED_VECTOR_FILTERED_FOLDER, tile + ".gpkg").toFile())
//    geopkg.add(filteredLayer, tile)


    // Geometrien dissolven.
    // CascadedUnion ist x-fach schneller und liefert korrekte Resultate.
    List<Geometry> geometries = filteredLayer.features.collect{ f -> f.geom }
    Geometry unionedGeometry = Geometry.cascadedUnion(geometries)

    Schema schema = new Schema("dummy", "geom:Polygon:srid=2056,id:String,value:Double")

    // TODO:
    // Keinen Memory-Workspace machen, sondern was Persistierendes.
    // Mit dem dann auch ganz am Ende nochmals cascade union.
    // Oder prüfen, ob GPGK existiert. Falls ja wird der Layer als "resultLayer" verwendet.


    Workspace workspace = new Memory()
    Layer dissolvedLayer = workspace.create(schema);

    if (unionedGeometry instanceof MultiPolygon) {
        List<Feature> featureList = unionedGeometry.geometries.collect {geom ->
            def uuid = UUID.randomUUID().toString()
            Feature feature = new Feature([
                    geom,
                    uuid,
                    1.0,
            ], uuid, schema)
            return feature
        }
        dissolvedLayer.add(featureList)
    } else {
        def uuid = UUID.randomUUID().toString()
        Feature feature = new Feature([
                unionedGeometry,
                uuid,
                1.0,
        ], uuid, schema)
        dissolvedLayer.add(feature)
    }

    Workspace geopkg = new GeoPackage(Paths.get(RECLASSIFIED_VECTOR_FILTERED_DISSOLVED_FOLDER, tile + ".gpkg").toFile())
    geopkg.add(dissolvedLayer, tile)
}



// function: export gpkg. dir=random. output text als parameter.




/*
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
*/

println("Hallo Welt.")

//File file = new File("/Users/stefan/Downloads/2594000_1230000_vegetation_uncompressed.tif")
//GeoTIFF geotiff = new GeoTIFF(file)
//Raster raster = geotiff.read("2594000_1230000_vegetation_uncompressed")
//
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