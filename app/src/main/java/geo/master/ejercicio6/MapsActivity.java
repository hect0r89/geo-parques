package geo.master.ejercicio6;

import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.wkb.geom.Geometry;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    File geoPackageFile;
    ArrayList<LatLng> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void addHeatMap() {
        List<LatLng> listHeat = null;

        // Get the data: latitude/longitude positions of police stations.

        listHeat = list;


        // Create a heat map tile provider, passing it the latlngs of the police stations.
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                .data(listHeat)
                .build();
        // Add a tile overlay to the map, using the heat map tile provider.
        TileOverlay mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }



    private void readDatabaseData() {
        File dir = Environment.getExternalStorageDirectory();
        geoPackageFile = new File(dir, "geoPackage" + File.separator + "parques.gpkg");

        GeoPackageManager manager = GeoPackageFactory.getManager(this);
        if (manager.databases().size() == 0) {
            boolean imported = manager.importGeoPackage(geoPackageFile);
        }
        List<String> databases = manager.databases();
        GeoPackage geoPackage = manager.open(databases.get(0));


        // Feature and tile tables
        List<String> features = geoPackage.getFeatureTables();

        // Query Features
        String featureTable = features.get(0);
        FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
        GoogleMapShapeConverter converter = new GoogleMapShapeConverter(
                featureDao.getProjection());
        FeatureCursor featureCursor = featureDao.queryForAll();
        CoordinateConversion c = new CoordinateConversion();
        try {
            while (featureCursor.moveToNext()) {
                FeatureRow featureRow = featureCursor.getRow();
                GeoPackageGeometryData geometryData = featureRow.getGeometry();
                Geometry geometry = geometryData.getGeometry();
                GoogleMapShape shape = converter.toShape(geometry);
                GoogleMapShape mapShape = GoogleMapShapeConverter
                        .addShapeToMap(mMap, shape);
                double [] latlong = c.utm2LatLon("30 T "+ featureRow.getValue("UTM_X")+ " "+featureRow.getValue("UTM_Y") );;
                double lat = latlong[0];
                double lon = latlong[1];
                list.add(new LatLng(lat, lon));

            }
        } finally {
            featureCursor.close();
        }

        geoPackage.close();
        addHeatMap();
        mMap.setMyLocationEnabled(true);


    }





    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        Dexter.withActivity(this)
                .withPermissions(
                        ACCESS_FINE_LOCATION,
                        WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {
                readDatabaseData();

            }
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();

    }
}
