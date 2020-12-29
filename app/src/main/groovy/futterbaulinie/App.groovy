package futterbaulinie

import geoscript.geom.Point
import geoscript.layer.Format
import geoscript.layer.GeoTIFF
import geoscript.layer.Layer
import geoscript.layer.Raster

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


//Point point = new Point(20,20)
//println(point.buffer(10))

File file = new File("/Users/stefan/Downloads/2594000_1230000_vegetation_uncompressed.tif")
GeoTIFF geotiff = new GeoTIFF(file)
Raster raster = geotiff.read("2594000_1230000_vegetation_uncompressed")

// value 0 ist nicht gut, wird als NULL/nodata interpretiert?
Raster reclassifiedRaster = raster.reclassify([
    [min:-9999, max:-9999, value: 2],
    [min:0,     max:0,     value: 2],
    [min:0,     max:200,   value: 1]
])

File outFile = new File("/Users/stefan/Downloads/A_pa1.tif")
Format outFormat = Format.getFormat(outFile)
println outFormat.name
outFormat.write(reclassifiedRaster)

Layer layer = reclassifiedRaster.polygonLayer
println layer.schema

layer.features.each { f ->
    
    println f.get("value").toString()
    
    if (f.get("value") < 2.0) {
        println f.geom.toString()
        println f.get("value").toString()
    
    }
}

println("Hallo Welt.")
