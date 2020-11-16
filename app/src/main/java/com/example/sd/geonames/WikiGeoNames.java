package com.example.sd.geonames;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class WikiGeoNames extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, LocationListener {

    private static final String GEOMAP_WIKI_URL = "http://api.geonames.org/wikipediaBoundingBoxJSON?north=%f&south=%f&east=%f&west=%f&username=joaovicente&lang=pt";
    private static final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=5614f6f3901a0691bbb959b2a1066269&units=metric&lang=pt";
    private static final long MIN_TIME_BW_UPDATES = 1 /*1000 * 60 * 1*/;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private GoogleMap mMap;
    private LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wiki_geo_names);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Mudar estilo do mapa
        ImageButton imageMapMode = (ImageButton) findViewById(R.id.imageMapMode);
        imageMapMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
            }
        });


        //Obtem uma referencia para o Location Manager do sistema
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Boolean isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        if (!isGPSEnabled && !isNetworkEnabled) {
            Log.d("MAP", "Without Location...");
            showBox();
            //Toast.makeText(this, "Please enabled GPS", Toast.LENGTH_LONG).show();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS))
                Log.d("MAP", "shouldShowRequest");
            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            }
            return;
        }
        //Inibe a geração de actualizações do gps (Location.GPS_PROVIDER:
        //  1º Só gera ectualizações da localização ao fim de 1 minuto ou
        //  2º Sempre que haja um deslocamento de pelo menos 10m
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);


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

        LatLng init = new LatLng(-7.43038, 39.2957);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(init));
        /* Icons de zoom e minha localização */
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        /* Activar o mexer da camera */
        mMap.setOnCameraIdleListener(this);
        /* Activar o click no marcador*/
        mMap.setOnMarkerClickListener(this);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

    }

    /**
     * Função onCameraIdle
     * <p>
     * Tem o objetivo adicionar marcadores numa determinada area
     */
    @Override
    public void onCameraIdle() {
        /* Tamanho da vista do mapa */
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        /* Adicionar marcadores no mapa numa determinada area  */
        String url = String.format(GEOMAP_WIKI_URL, bounds.northeast.latitude, bounds.southwest.latitude, bounds.northeast.longitude, bounds.southwest.longitude);
        url = url.replace(",", ".");
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("MAP", "Response is: " + response);
                try {
                    JSONArray jsonArray = (JSONArray) response.getJSONArray("geonames");

                    mMap.clear();
                    // Adicionar os marcadores
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonOb = (JSONObject) jsonArray.get(i);

                        LatLng local = new LatLng(jsonOb.getDouble("lat"), jsonOb.getDouble("lng"));
                        /* Criar um marcador */
                        MarkerOptions markerOp = new MarkerOptions().position(local)
                                .title(jsonOb.getString("title"))
                                .snippet(jsonOb.getString("summary"));

                        //Verde -> cidade; azul -> cidade;
                        if (jsonOb.has("feature")) {
                            switch (jsonOb.getString("feature")) {
                                case "city":
                                    markerOp.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_city));
                                    break;
                                case "country":
                                    markerOp.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_country));
                                    break;
                                default:
                                    markerOp.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_mapmarker));
                            }

                        }


                        /* Adicioanr marcador ao mapa */
                        mMap.addMarker(markerOp);


                    }

                } catch (JSONException error) {
                    Log.d("MAP", "onResponse error: " + error);
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("MAP", "That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(jsonRequest);
    }

    /**
     * Função onMarkerClick
     * <p>
     * Tem como objetivo mostar informação sobre o marcador carregado
     *
     * @param marker
     * @return
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return true;
    }

    /**
     * Função onLocationChanged
     * <p>
     * Temo como objetivo alterar a posição do utilizador
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.d("MAP", "onLocationChanged: " + location);
        /* Serviço de actualização do tempo */
        weather(location);
        /* Nova localização */
        LatLng local = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(local));

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /**
     * Função weather
     * <p>
     * Tem como objetivo actualizar os dados do tempo da localização do utilizador
     *
     * @param location : Localização do utilizador
     */
    public void weather(Location location) {
        String url = String.format(WEATHER_URL, location.getLatitude(), location.getLongitude());
        url = url.replace(",", ".");

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("MAP", "Response is: " + response);
                TextView descricao = (TextView) findViewById(R.id.descricao);
                TextView tempo = (TextView) findViewById(R.id.tempo);
                TextView localidade = (TextView) findViewById(R.id.localidade);
                ImageView imageView = (ImageView) findViewById(R.id.imageView);

                try {
                    /* Buscar Tempo*/
                    JSONArray jsonAWeather = (JSONArray) response.getJSONArray("weather");
                    /* Buscar descrição */
                    JSONObject jsonWeather = (JSONObject) response.getJSONObject("main");

                    /* Descrição */
                    descricao.setText(jsonAWeather.getJSONObject(0).getString("description"));
                    /* Tempo */
                    tempo.setText(jsonWeather.getString("temp") + "ºC");
                    /* Localidade */
                    localidade.setText(response.getString("name"));
                    /* Imagem */

                    Glide.with(imageView).load("http://openweathermap.org/img/w/" +
                            jsonAWeather.getJSONObject(0).getString("icon") + ".png")
                            .into(imageView);

                } catch (JSONException error) {
                    Log.d("MAP", "onResponse error: " + error);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("MAP", "That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(jsonRequest);
    }

    /**
     * Função showBox
     * <p>
     * Tem como objetivo mostrar caixa para ativar GPS
     * Todo o codigo abaixo tirardo de "https://stackoverflow.com/questions/33295610/enable-gps-using-dialog-box"
     */
    public void showBox() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        /* Botao de ativar GPS */
        alertDialogBuilder
                .setMessage("GPS esta desligado, deseja activalo?")
                .setCancelable(false)
                .setPositiveButton("Activar GPS", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        /** Here it's leading to GPS setting options*/
                        Intent callGPSSettingIntent = new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                    }
                });
        /* Botao de cancelar */
        alertDialogBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }


}
