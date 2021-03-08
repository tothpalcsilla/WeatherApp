package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import khttp.responses.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.math.RoundingMode
import java.net.URL
import java.net.URLConnection
import java.text.DecimalFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val baseURL = "https://api.weatherapi.com/v1"
    private val currentWeather = "/current.json"

    private val REQUEST_PERMISSION_FINE_LOCATION = 1
    private val REQUEST_CHECK_CODE = 2
    companion object {
        const val LOCATION_SETTING_REQUEST = 999
    }

    //private val MY_API_KEY = "7242d5381f68418c8ff93444210203"
    private lateinit var myApiKey : String
    private lateinit var editTextField: EditText

    private lateinit var locationManager: LocationManager
    private var locationGps: Location? = null
    private var locationNetwork: Location? = null
    private lateinit var location : String
    private lateinit var language : String
    private var hasGps = false
    private var hasNetwork = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val prefs : SharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        myApiKey = prefs.getString("apyKey", "") ?: ""

        getPermission()
    }

    private fun getPermission(){
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
        } else enableLocation()
    }

    private fun enableLocation(){
        // initialize location request object
        val request = LocationRequest.create()
        request.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 3000
            setFastestInterval(1500)
        }

        // initialize location setting request builder object
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
        val locationSettingsRequest = builder.build()

        // initialize location service object
        val settingsClient = LocationServices.getSettingsClient(this)
        val result: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(locationSettingsRequest)

        result.addOnSuccessListener { response ->
            val states = response.locationSettingsStates
            if(states.isLocationPresent){
                getApiKey()
            }

            /*val states : LocationSettingsStates = LocationSettingsStates.fromIntent(intent)
            when (requestCode){
                REQUEST_CHECK_CODE -> {
                    when(resultCode){
                        Activity.RESULT_OK -> {
                            // All required changes were successfully made
                        }
                        Activity.RESULT_CANCELED -> {
                            // The user was asked to change settings, but chose not to
                        }
                    }
                }
            }*/
        }
        result.addOnFailureListener { e ->
            if(e is ResolvableApiException){
                try{
                    //Handle result in onActivityResult()
                    e.startResolutionForResult(this, MainActivity.LOCATION_SETTING_REQUEST)
                } catch (sendEx: IntentSender.SendIntentException){

                }
            }
        }

        /*result.addOnCompleteListener(OnCompleteListener<LocationSettingsResponse?> { task ->
            try {
                val response: LocationSettingsResponse? = task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location requests here.
                getApiKey()
            } catch (e: ApiException) {
                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            val resolvableApiException = e as ResolvableApiException

                            // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                            resolvableApiException.startResolutionForResult(
                                this@MainActivity,
                                REQUEST_CHECK_CODE
                            )

                            val getContent = resolvableApiException.startResolutionForResult(
                                this@MainActivity,
                                REQUEST_CHECK_CODE
                            )

                        } catch (ex: IntentSender.SendIntentException) {
                            // Ignore the error.
                            ex.printStackTrace()
                        } catch (ex: java.lang.ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                    }
                }
            }
        })*/
    }

    private fun getApiKey(){
        if (myApiKey == "") {
            showApiDialog()
        } else {
            updateWeather()
        }
    }

    private fun updateWeather(){
        getLanguage()
        getLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_FINE_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation()
            } else {
                this.getPermission()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        if (menu is MenuBuilder) {
            val m: MenuBuilder = menu
            m.setOptionalIconsVisible(true)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_APIkey_setting -> {
                showApiDialog(myApiKey)
                true
            }
            R.id.action_update -> {
                updateWeather()
                true
            }
            R.id.action_exit -> {
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("ResourceType")
    private fun showApiDialog(oldApiKey : String = ""){
        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.api_key_dialog_title)
                .setCancelable(false)
        editTextField = EditText(this)
        editTextField.setHint(R.string.api_key_hint)
        editTextField.setSingleLine()
        editTextField.textSize = 15F
        editTextField.background.clearColorFilter()
        editTextField.setPadding(40, 60,40, 20)
        if(oldApiKey != "") editTextField.setText(oldApiKey)
        builder.setView(editTextField)

        // Add action buttons
        builder.setPositiveButton(R.string.save) { _, _ ->
                    //has to be empty -> override after dialog show
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    if (myApiKey == "") finishAffinity()
                    dialog.cancel()
                }
        val dialog = builder.create()
        dialog.show()
        //Overriding the handler immediately after show
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val editTextInput = editTextField.text.toString()
            if(editTextInput != ""){
                // check that the set api key is correct
                GlobalScope.launch {
                    var response : Response
                    withContext(Dispatchers.IO) {// IO: Optimized for network and disk operations
                        val url = baseURL + currentWeather
                        response = khttp.get(url = url, params = mapOf("key" to editTextInput, "q" to "London"))
                    }
                    if(response.statusCode >= 400){
                        withContext(Dispatchers.Main){ //switched to Main thread
                            Toast.makeText(applicationContext, R.string.invalid_api_key, Toast.LENGTH_SHORT).show()
                        }
                        editTextField.setText("")
                    } else {
                        withContext(Dispatchers.Main){ //switched to Main thread
                            myApiKey = editTextInput
                            val editor : SharedPreferences.Editor = getSharedPreferences("prefs", Context.MODE_PRIVATE).edit()
                            editor.putString("apyKey", editTextInput)
                            editor.apply()
                            Toast.makeText(applicationContext, R.string.save, Toast.LENGTH_SHORT).show()
                            updateWeather()
                        }
                        dialog.dismiss()
                    }
                }
            } else if (editTextInput == ""){
                Toast.makeText(applicationContext, R.string.empty_api_key, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Az API felé a helyet geo koordinátákkal adjuk át, amit a készülék GPS adataiból határozzunk meg.
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
                    100,
                    100F,
                    object : LocationListener {
                        override fun onLocationChanged(p0: Location) {
                            locationGps = p0
                            setLarLon(locationGps)
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
                if (localGpsLocation != null) {
                    locationGps = localGpsLocation
                    setLarLon(locationGps)
                }

            }

            else if (hasNetwork) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    100,
                    100F,
                    object : LocationListener {
                        override fun onLocationChanged(p0: Location) {
                            locationNetwork = p0
                            setLarLon(locationNetwork)
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
                val localNetworkLocation : Location? =
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (localNetworkLocation != null){
                    locationNetwork = localNetworkLocation
                    setLarLon(locationNetwork)
                }
            }
        }
    }

    private fun setLarLon(locationGT: Location?){
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.DOWN
        var lat : String = df.format(locationGT!!.latitude)
        var lon : String = df.format(locationGT!!.longitude)
        lat = lat.replace(',', '.')
        lon = lon.replace(',', '.')
        location = "$lat,$lon"
        getWeather()
    }

    // A készüléken beállított nyelvet adjuk át. (a rövid szöveges leírás ezen a nyelven fog érkezni)
    private fun getLanguage(){
        language = Locale.getDefault().language
    }

    private suspend fun networkRequest(): Response {
        lateinit var response : Response
        withContext(Dispatchers.IO) {// IO: Optimized for network and disk operations
            val url = baseURL + currentWeather
            response = khttp.get(url = url, params = mapOf("key" to myApiKey, "q" to location, "lang" to language))
        }
        return response
    }

    @SuppressLint("SetTextI18n")
    private fun getWeather() {
        GlobalScope.launch {
            val response = networkRequest()
            val navController = findNavController(R.id.nav_host_fragment)
            if (navController.currentDestination?.id == R.id.FirstFragment) {
                val fragment: FirstFragment = supportFragmentManager.fragments.first().childFragmentManager.fragments.first() as FirstFragment
                if(response.statusCode == 200){
                    val obj: JSONObject = response.jsonObject
                    val location : JSONObject = obj.get("location") as JSONObject
                    val current : JSONObject = obj.get("current") as JSONObject

                    val city : String = location.get("name") as String
                    val lastUpdate : String = current.get("last_updated") as String
                    // Temperature in celsius
                    val temp: Double = current.get("temp_c") as Double
                    val temperature : Int = temp.toInt()
                    // Wind speed in miles per hour
                    val windSpeed: Double = current.get("wind_mph") as Double
                    // Wind direction in degrees
                    val windDirection: Int = current.get("wind_degree") as Int

                    val condition : JSONObject = current.get("condition") as JSONObject
                    val conditionText : String = condition.get("text") as String
                    val iconUrl : String = condition.get("icon") as String
                    val imageBitmap = getImageBitmap("https:$iconUrl")
                    withContext(Dispatchers.Main){ //switched to Main thread
                        fragment.table.setBackgroundResource(R.color.whitetrans)
                        fragment.city.text = city
                        fragment.lastUpdate.text = getString(R.string.update, lastUpdate)
                        fragment.icon.setImageBitmap(imageBitmap)
                        fragment.temperature.text = "$temperature°C"
                        fragment.wind.setText(R.string.wind)
                        fragment.wind_icon.setImageResource(R.drawable.wind4_g)
                        fragment.wind_speed_title.setText(R.string.speed)
                        fragment.wind_speed.text = " $windSpeed m/h"
                        fragment.wind_direction_title.setText(R.string.direction)
                        fragment.wind_direction.text = " $windDirection°"
                        fragment.short_description.text = conditionText
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        checkError(response)
                    }
                }
            }
        }
    }

    private fun checkError(response:Response){
        val error = response.jsonObject.get("error") as JSONObject
        val errorCode : String = error.get("message") as String
        Toast.makeText(applicationContext, errorCode, Toast.LENGTH_SHORT).show()
    }
    private fun getImageBitmap(url: String): Bitmap? {
        var bm: Bitmap? = null
        try {
            val aURL = URL(url)
            val conn: URLConnection = aURL.openConnection()
            conn.connect()
            val `is`: InputStream = conn.getInputStream()
                val bis = BufferedInputStream(`is`)
                bm = BitmapFactory.decodeStream(bis)
                bis.close()
            `is`.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error getting bitmap", e)
        }
        return bm
    }
}