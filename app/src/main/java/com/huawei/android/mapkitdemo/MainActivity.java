package com.huawei.android.mapkitdemo;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.common.ResolvableApiException;
import com.huawei.hms.location.ActivityIdentification;
import com.huawei.hms.location.ActivityIdentificationService;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationCallback;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.location.LocationSettingsRequest;
import com.huawei.hms.location.LocationSettingsStatusCodes;
import com.huawei.hms.location.SettingsClient;
import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.MapsInitializer;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.SupportMapFragment;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.LatLngBounds;
import com.huawei.hms.maps.model.Marker;
import com.huawei.hms.maps.model.MarkerOptions;
import com.huawei.hms.maps.model.Polyline;
import com.huawei.hms.maps.model.PolylineOptions;
import com.huawei.hms.site.api.SearchResultListener;
import com.huawei.hms.site.api.SearchService;
import com.huawei.hms.site.api.SearchServiceFactory;
import com.huawei.hms.site.api.model.Coordinate;
import com.huawei.hms.site.api.model.LocationType;
import com.huawei.hms.site.api.model.NearbySearchRequest;
import com.huawei.hms.site.api.model.NearbySearchResponse;
import com.huawei.hms.site.api.model.SearchStatus;
import com.huawei.hms.site.api.model.Site;
import com.huawei.openalliance.ad.beans.metadata.Location;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private HuaweiMap hMap;
    private String apikey = "CgB6e3x9BFxwylCTq+W9lmsmjYHgFSb20i//wjubvhUTwI8waYVog5CrgDzMikdEN0zcQ4lqDsJbjh2l+ZYw7Sb0", name, address;
    private MapView mMapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private SupportMapFragment mSupportMapFragment;
    private List<Polyline> mPolylines = new ArrayList<>();
    private List<List<LatLng>> mPaths = new ArrayList<>();
    private LatLngBounds mLatLngBounds;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    renderRoute(mPaths, mLatLngBounds);
                    break;
                case 1:
                    Bundle bundle = msg.getData();
                    String errorMsg = bundle.getString("errorMsg");
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapsInitializer.setApiKey("CgB6e3x9BFxwylCTq+W9lmsmjYHgFSb20i//wjubvhUTwI8waYVog5CrgDzMikdEN0zcQ4lqDsJbjh2l+ZYw7Sb0");
        mSupportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapfragment_mapfragmentdemo);
        mSupportMapFragment.getMapAsync(this);
        mMapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle("MapViewBundleKey");
        }
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);
        ZoomControls zoom = (ZoomControls) findViewById(R.id.zoom);
        zoom.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hMap.animateCamera(com.huawei.hms.maps.CameraUpdateFactory.zoomOut());
            }
        });
        zoom.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hMap.animateCamera(com.huawei.hms.maps.CameraUpdateFactory.zoomIn());
            }
        });

        final Button btn_MapType = (Button) findViewById(R.id.btn_Sat);
        btn_MapType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hMap.getMapType() == HuaweiMap.MAP_TYPE_NORMAL) {
                    hMap.setMapType(HuaweiMap.MAP_TYPE_SATELLITE);
                    btn_MapType.setText("NORMAL GÖRÜNÜM");
                } else {
                    hMap.setMapType(HuaweiMap.MAP_TYPE_NORMAL);
                    btn_MapType.setText("UYDU GÖRÜNÜMÜ");
                }
            }
        });

    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    @Override
    public void onMapReady(HuaweiMap huaweiMap) {
        Log.d("onMapReady", huaweiMap.toString());
        hMap = huaweiMap;
        hMap.setMyLocationEnabled(true);// konumum katmanı
        hMap.getUiSettings().setCompassEnabled(true);
        hMap.getUiSettings().setMyLocationButtonEnabled(true);// konumum iconu
        LatLng huawei = new LatLng(41.028880, 29.117224);
        hMap.addMarker(new MarkerOptions().position(huawei).title("Huawei Türkiye"));
        hMap.moveCamera(com.huawei.hms.maps.CameraUpdateFactory.newLatLng(huawei));
    }

        private void renderRoute(List<List<LatLng>> paths, LatLngBounds latLngBounds) {
            if (null == paths || paths.size() <= 0 || paths.get(0).size() <= 0) {
                return;
            }
        for (int i = 0; i < paths.size(); i++) {
            List<LatLng> path = paths.get(i);
            PolylineOptions options = new PolylineOptions().color(Color.BLUE).width(5);
            for (LatLng latLng : path) {
                options.add(latLng);
            }

            Polyline polyline = hMap.addPolyline(options);
            mPolylines.add(i, polyline);
        }

            if (null != latLngBounds) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, 5);
                hMap.moveCamera(cameraUpdate);
            } else {
                hMap.moveCamera(CameraUpdateFactory.newLatLngZoom(paths.get(0).get(0), 13));
            }

        }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}