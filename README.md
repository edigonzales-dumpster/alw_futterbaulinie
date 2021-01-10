# alw_futterbaulinie

## Bemerkungen

- 32bit und predictor=2 funktioniert nicht -> Gdal
- Reclassify: value=0 wird als nodata interpretiert? (Ich glaube, ich habe zufälligerweise was im Code dazu gesehen.)
- Namen von Shapefiles? Es wird der Name des Schemas (welcher read-only ist) verwendet.
- Shapefile.dump() wird in QGIS nicht angezeigt. fid missing?
- layer.dissolve() geht sehr lange und liefert teilweise ganz krude Geometrien zurück. -> Bug melden mit Beispiel

## Develop

### Eclipse
Code Completion ist käsig. Ich habe unter Preferences - Java - Editor - Content Assistant - Advanced die "Groovy Template Proposals" ausgeschaltet. Scheint auf den ersten Blick reaktiver zu sein. Anscheinend bringt das Deaktivieren von "Fill method arguments and show guessed arguments" auch etwas. Möchte ich aber (noch) nicht machen.

### IntelliJ
Viel weniger käsig.


### Snippets

Download files
```
tiles.each {tile ->
    Paths.get(DOWNLOAD_FOLDER, tile + ".tif").toFile().withOutputStream {out ->
        out << new URL(DOWNLOAD_URL + tile + ".tif").openStream()
    }
}
```
