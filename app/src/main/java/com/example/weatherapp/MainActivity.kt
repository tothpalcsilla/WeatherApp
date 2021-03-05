package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import khttp.responses.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private val MY_API_KEY = "7242d5381f68418c8ff93444210203"
    private val Base_URL = "http://api.weatherapi.com/v1"
    private val Current_weather = "/current.json"

    private val REQUEST_PERMISSION_FINE_LOCATION = 1

    lateinit var locationManager: LocationManager
    lateinit var locationGps: Location
    lateinit var locationNetwork: Location
    lateinit var location : String
    lateinit var language : String
    var hasGps = false
    var hasNetwork = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION_FINE_LOCATION
            )

            /*AlertDialog.Builder(this).setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again")
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setIcon(android.R.drawable.ic_dialog_alert).show()*/

        } else {
            getLocation()
            getLanguage()
            GlobalScope.launch {
                val response = networkRequest()
                var errorCode = ""
                val navController = findNavController(R.id.nav_host_fragment)
                if (navController.currentDestination?.id == R.id.FirstFragment) {
                    val fragment: FirstFragment = supportFragmentManager.fragments.first().childFragmentManager.fragments.first() as FirstFragment
                    when(response.statusCode) {
                        200 -> {
                            val obj: JSONObject = response.jsonObject
                            val message: String = response.text
                            val imageData: ByteArray = response.content
                            withContext(Dispatchers.Main){ //switched to Main thread
                                fragment.location.setText(obj.toString())
                            }
                        }
                        400 -> {
                            when(response.jsonObject.get("errorCode")){
                                1003 -> errorCode = "Parameter 'q' not provided."
                                1005 -> errorCode = "API request url is invalid."
                                1006 -> errorCode = "No location found matching parameter 'q'."
                                9999 -> errorCode = "Internal application error."
                            }
                        }
                        401 -> {
                            when(response.jsonObject.get("errorCode")){
                                1002 -> errorCode = "API key not provided."
                                2006 -> errorCode = "API key provided is invalid."
                            }
                        }
                        403 -> {
                            when(response.jsonObject.get("errorCode")){
                                2007 -> errorCode = "API key has exceeded calls per month quota."
                                2008 -> errorCode = "API key has been disabled."
                            }
                        }
                        else -> errorCode = "Unknown error."                        }
                    if(errorCode != ""){
                        withContext(Dispatchers.Main){
                            fragment.location.setText(errorCode)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on the Home/Up button,
        // so long as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_APIkey_setting -> {
                //this.findNavController(R.id.nav_graph).navigate(R.id.action_FirstFragment_to_SecondFragment)
                true
            }
            R.id.action_update -> true
            R.id.action_exit -> {
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION_FINE_LOCATION
            )
        } else {
            if (hasGps) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    60000,
                    100F,
                    object : LocationListener {
                        override fun onLocationChanged(p0: Location) {
                            locationGps = p0
                        }

                        override fun onProviderEnabled(provider: String) {

                        }

                        override fun onProviderDisabled(provider: String) {

                        }

                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {

                        }
                    })
                val localGpsLocation =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (localGpsLocation != null) locationGps = localGpsLocation
            }

            if (hasNetwork) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    60000,
                    100F,
                    object : LocationListener {
                        override fun onLocationChanged(p0: Location) {
                            locationNetwork = p0
                        }

                        override fun onProviderEnabled(provider: String) {

                        }

                        override fun onProviderDisabled(provider: String) {

                        }

                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {

                        }
                    })
                val localNetworkLocation =
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (localNetworkLocation != null) locationNetwork = localNetworkLocation
            }

            if (locationGps.accuracy > locationNetwork.accuracy) {
                location =  locationGps.latitude.toString() + "," + locationGps.longitude.toString()
            } else {
                location = locationNetwork.latitude.toString() + "," + locationNetwork.longitude.toString()
            }

        }
    }

    private fun getLanguage(){
        language = "hu"//TODO()
    }

    private suspend fun networkRequest(): Response {
        lateinit var response : Response
        withContext(Dispatchers.IO) {// IO: Optimized for network and disk operations
            val url = Base_URL + Current_weather
            response = khttp.get(url = url, params = mapOf("key" to MY_API_KEY, "q" to location, "lang" to language))
        }
        return response
    }

}