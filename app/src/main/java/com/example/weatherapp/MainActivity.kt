package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
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
    private val Base_URL = "https://api.weatherapi.com/v1"
    private val Current_weather = "/current.json"

    private val REQUEST_PERMISSION_FINE_LOCATION = 1

    //private val MY_API_KEY = "7242d5381f68418c8ff93444210203"
    lateinit var MY_API_KEY : String
    lateinit var editTextField: EditText

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

        var prefs : SharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val s : String? = prefs.getString("apyKey", "")
        MY_API_KEY = prefs.getString("apyKey", "") ?: ""

        if (MY_API_KEY == "") {
            showApiDialog()
        } else if (ActivityCompat.checkSelfPermission(
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
            getWeather()
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
                val fbDialogue = Dialog(this)
                //fbDialogue.getWindow().setBackgroundDrawable(ColorDrawable(Color.argb(100, 0, 0, 0)))
                fbDialogue.setContentView(R.layout.fragment_second)
                fbDialogue.setCancelable(true)
                fbDialogue.show()

                //val fragmentSec: SecondFragment = supportFragmentManager.fragments.first().childFragmentManager.fragments.first() as SecondFragment
                //fragmentSec.apiKey.setText(MY_API_KEY)
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

    @SuppressLint("ResourceType")
    private fun showApiDialog(){
        /*val fbDialogue = Dialog(this)
        //fbDialogue.getWindow().setBackgroundDrawable(ColorDrawable(Color.argb(100, 0, 0, 0)))
        fbDialogue.setContentView(R.layout.fragment_second)
        fbDialogue.setCancelable(true)
        fbDialogue.show()*/

        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.api_key_dialog_title)
                .setCancelable(false)
        // Get the layout inflater
        val inflater = this.layoutInflater;

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        //val inf = inflater.inflate(R.layout.fragment_second, null)
        //builder.setView(inf)

        editTextField = EditText(this)
        editTextField.setHint((R.string.api_key_hint))
        editTextField.setTextSize(15F)
        builder.setView(editTextField)

        // Add action buttons
        builder.setPositiveButton(R.string.save,
                        DialogInterface.OnClickListener { dialog, id ->
                            //val s : String = inf.text as String
                            val editTextInput = editTextField.text.toString()
                            if(editTextInput != ""){
                                GlobalScope.launch {
                                    var response : Response
                                    withContext(Dispatchers.IO) {// IO: Optimized for network and disk operations
                                        val url = Base_URL + Current_weather
                                        response = khttp.get(url = url, params = mapOf("key" to editTextInput, "q" to "London"))
                                    }
                                    println(response)
                                    if(response.statusCode >= 400){
                                        //Toast.makeText(applicationContext, "Your API key is invalid", Toast.LENGTH_SHORT).show()
                                        editTextField.setText("")
                                    } else {
                                        var prefs : SharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE)
                                        var editor : SharedPreferences.Editor = prefs.edit()
                                        editor.putString("apyKey", editTextInput)
                                        editor.apply()
                                        //Toast.makeText(applicationContext, R.string.save, Toast.LENGTH_SHORT).show()
                                        dialog.cancel()
                                    }
                                }
                            }
                            //dialog.cancel()
                        })
                .setNegativeButton(R.string.cancel,
                        DialogInterface.OnClickListener { dialog, id ->
                            if (MY_API_KEY == "") finishAffinity()
                            dialog.cancel()
                        })
        builder.create().show()
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

            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.DOWN
            var lat : String
            var lon : String
            if (locationGps.accuracy > locationNetwork.accuracy) {
                lat = df.format(locationGps.latitude)
                lon = df.format(locationGps.longitude)
            } else {
                lat = df.format(locationNetwork.latitude)
                lon = df.format(locationNetwork.longitude)
            }
            lat = lat.replace(',', '.')
            lon = lon.replace(',', '.')
            location = "$lat,$lon"
        }
    }

    // A készüléken beállított nyelvet adjuk át. (a rövid szöveges leírás ezen a nyelven fog érkezni)
    private fun getLanguage(){
        language = Locale.getDefault().getLanguage()
    }

    private suspend fun networkRequest(): Response {
        lateinit var response : Response
        withContext(Dispatchers.IO) {// IO: Optimized for network and disk operations
            val url = Base_URL + Current_weather
            response = khttp.get(url = url, params = mapOf("key" to MY_API_KEY, "q" to location, "lang" to language))
        }
        return response
    }

    private fun getWeather() {
        GlobalScope.launch {
            val response = networkRequest()
            var errorCode = ""
            val navController = findNavController(R.id.nav_host_fragment)
            if (navController.currentDestination?.id == R.id.FirstFragment) {
                val fragment: FirstFragment = supportFragmentManager.fragments.first().childFragmentManager.fragments.first() as FirstFragment
                when(response.statusCode) {
                    200 -> {
                        val obj: JSONObject = response.jsonObject
                        val location : JSONObject = obj.get("location") as JSONObject
                        val current : JSONObject = obj.get("current") as JSONObject

                        val city : String = location.get("name") as String
                        val last_update : String = current.get("last_updated") as String
                        // Temperature in celsius
                        val temp: Double = current.get("temp_c") as Double
                        val temperature : Int = temp.toInt()
                        // Feels like temperature in celsius
                        val temperature2: Double = current.get("feelslike_c") as Double
                        // Wind speed in miles per hour
                        val wind_speed: Double = current.get("wind_mph") as Double
                        // Wind direction in degrees
                        val wind_direction: Int = current.get("wind_degree") as Int

                        val condition : JSONObject = current.get("condition") as JSONObject
                        val condition_text : String = condition.get("text") as String
                        val icon_url : String = condition.get("icon") as String
                        var imageBitmap = getImageBitmap("https:$icon_url") //http://cdn.weatherapi.com/weather/64x64/day/113.png
                        withContext(Dispatchers.Main){ //switched to Main thread
                            fragment.city.setText(city)
                            fragment.date.setText("Friss: $last_update")
                            fragment.icon.setImageBitmap(imageBitmap);
                            fragment.temperature.setText("$temperature°C")
                            fragment.wind_speed.setText(" $wind_speed m/h")
                            fragment.wind_direction.setText(" $wind_direction°")
                            fragment.short_description.setText(condition_text)
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
                        fragment.short_description.setText(errorCode)
                    }
                }
            }
        }
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