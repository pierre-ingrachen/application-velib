package fr.epf.sni1.applicationvelib

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

data class StationSauvegardee(
    val id: String,
    val nom: String,
    val velosMecaniques: Int,
    val velosElectriques: Int,
    val placesDisponibles: Int
)

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var searchRadiusMeters = 1000f

    // Obligation de demander l'autorisation pour la localisation
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            locateUserAndOpenNearbyStations()
        } else {
            Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val menuButton = TextView(this).apply {
            text = "⋮"
            textSize = 28f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            elevation = 12f

            layoutParams = FrameLayout.LayoutParams(120, 120).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, 64, 48, 0)
            }

            setOnClickListener { view ->
                val popup = PopupMenu(this@MainActivity, view)
                popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_carte -> true
                        R.id.action_sauvegardes -> {
                            startActivity(Intent(this@MainActivity, SavedStationsActivity::class.java))
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
        addContentView(menuButton, menuButton.layoutParams)

        val searchBar = AutoCompleteTextView(this).apply {
            hint = "Rechercher une station..."
            textSize = 16f
            setPadding(48, 0, 48, 0)
            setSingleLine(true)
            setTextColor(Color.BLACK)

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60f
                setColor(Color.WHITE)
            }
            elevation = 12f

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                120
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(48, 64, 200, 0)
            }
        }
        addContentView(searchBar, searchBar.layoutParams)

        val locationButton = TextView(this).apply {
            text = "Stations à proximité"
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(48, 0, 48, 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30f
                setColor(Color.parseColor("#007BFF"))
            }
            elevation = 16f

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                120
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setMargins(0, 0, 0, 64)
            }

            setOnClickListener {
                showRadiusDialog()
            }
        }
        addContentView(locationButton, locationButton.layoutParams)

        val map = findViewById<MapView>(R.id.map)
        map.apply {
            setMultiTouchControls(true)
            controller.setZoom(14.5)
            controller.setCenter(GeoPoint(48.7891474, 2.3268263))
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://velib-metropole-opendata.smovengo.cloud/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val infoService = retrofit.create(VelibStationInformationService::class.java)
        val statusService = retrofit.create(VelibStationStatusService::class.java)

        lifecycleScope.launch {
            try {
                val infoResponse = infoService.getStations()
                val statusResponse = statusService.getStationStatus()

                val stationList = infoResponse.data.stations
                val markersMap = mutableMapOf<String, TextView>()

                val stationNames = stationList.map { it.name }.toTypedArray()
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, stationNames)
                searchBar.setAdapter(adapter)

                searchBar.setOnItemClickListener { parent, _, position, _ ->
                    val selectedName = parent.getItemAtPosition(position) as String
                    val selectedStation = stationList.find { it.name == selectedName }

                    if (selectedStation != null) {
                        val point = GeoPoint(selectedStation.lat, selectedStation.lon)
                        map.controller.animateTo(point)
                        map.controller.setZoom(18.0)

                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(searchBar.windowToken, 0)
                        searchBar.clearFocus()

                        markersMap[selectedStation.station_id.toString()]?.performClick()
                    }
                }

                infoResponse.data.stations.forEach { station ->
                    val stationStatus = statusResponse.data.stations.find {
                        it.station_id == station.station_id
                    }

                    if (stationStatus != null) {
                        val textView = TextView(this@MainActivity)

                        textView.text = stationStatus.numBikesAvailable.toString()
                        textView.textSize = 14f
                        textView.setTextColor(Color.BLACK)
                        textView.gravity = Gravity.CENTER
                        textView.setBackgroundResource(R.drawable.fond_cercle)

                        markersMap[station.station_id.toString()] = textView

                        textView.setOnClickListener {
                            val bottomSheetDialog = BottomSheetDialog(this@MainActivity)

                            val layout = LinearLayout(this@MainActivity).apply {
                                orientation = LinearLayout.VERTICAL
                                setPadding(64, 64, 64, 64)

                                val headerLayout = LinearLayout(context).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    gravity = Gravity.CENTER_VERTICAL

                                    val textContainer = LinearLayout(context).apply {
                                        orientation = LinearLayout.VERTICAL
                                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                                        addView(TextView(context).apply {
                                            text = station.name
                                            textSize = 24f
                                            setTypeface(null, android.graphics.Typeface.BOLD)
                                            setTextColor(Color.BLACK)
                                        })

                                        // J'utilise des emojis (plus facile à intégrer que les images)
                                        addView(TextView(context).apply {
                                            text = """
                                                🚴 ${stationStatus.numBikesAvailable} vélos disponibles
                                                Mécaniques : ${stationStatus.num_bikes_available_types[0].mechanical} | Électriques : ${stationStatus.num_bikes_available_types[1].ebike}
                                                🅿️ Places libres : ${stationStatus.numDocksAvailable}
                                            """.trimIndent()
                                            textSize = 22f
                                            setTextColor(Color.DKGRAY)
                                            setPadding(0, 16, 0, 0)
                                        })
                                    }

                                    addView(textContainer)

                                    addView(ImageView(context).apply {
                                        setPadding(16, 16, 16, 16)

                                        val gson = Gson()
                                        val fichier = File(context.filesDir, "stations_favorites.json")

                                        val listeInitiale = if (fichier.exists()) {
                                            gson.fromJson(fichier.readText(), Array<StationSauvegardee>::class.java).toMutableList()
                                        } else {
                                            mutableListOf()
                                        }

                                        var isLiked = listeInitiale.any { it.id == station.station_id.toString() }

                                        if (isLiked) {
                                            setImageResource(R.drawable.baseline_favorite_24)
                                            setColorFilter(Color.RED)
                                        } else {
                                            setImageResource(R.drawable.baseline_favorite_border_24)
                                            clearColorFilter()
                                        }

                                        setOnClickListener {
                                            isLiked = !isLiked

                                            val listeStations = if (fichier.exists()) {
                                                gson.fromJson(fichier.readText(), Array<StationSauvegardee>::class.java).toMutableList()
                                            } else {
                                                mutableListOf()
                                            }

                                            if (isLiked) {
                                                setImageResource(R.drawable.baseline_favorite_24)
                                                setColorFilter(Color.RED)

                                                val nouvelleStation = StationSauvegardee(
                                                    id = station.station_id.toString(),
                                                    nom = station.name,
                                                    velosMecaniques = stationStatus.num_bikes_available_types[0].mechanical,
                                                    velosElectriques = stationStatus.num_bikes_available_types[1].ebike,
                                                    placesDisponibles = stationStatus.numDocksAvailable
                                                )

                                                listeStations.removeAll { it.id == nouvelleStation.id }
                                                listeStations.add(nouvelleStation)
                                            } else {
                                                setImageResource(R.drawable.baseline_favorite_border_24)
                                                clearColorFilter()

                                                listeStations.removeAll { it.id == station.station_id.toString() }
                                            }

                                            fichier.writeText(gson.toJson(listeStations))
                                        }
                                    })
                                }

                                addView(headerLayout)
                            }

                            bottomSheetDialog.setContentView(layout)
                            bottomSheetDialog.show()
                        }

                        val layoutParams = MapView.LayoutParams(
                            80,
                            80,
                            GeoPoint(station.lat, station.lon),
                            MapView.LayoutParams.CENTER,
                            0,
                            0
                        )

                        map.addView(textView, layoutParams)
                    }
                }

                map.invalidate()

                val stationToOpenId = intent.getStringExtra("STATION_TO_OPEN")
                if (stationToOpenId != null) {
                    val targetStation = stationList.find { it.station_id.toString() == stationToOpenId }
                    if (targetStation != null) {
                        val point = org.osmdroid.util.GeoPoint(targetStation.lat, targetStation.lon)
                        map.controller.setCenter(point)
                        map.controller.setZoom(18.0)

                        markersMap[stationToOpenId]?.performClick()
                    }
                    intent.removeExtra("STATION_TO_OPEN")
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors de la récupération des stations", e)
            }
        }
    }

    private fun showRadiusDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recherche à proximité")
        builder.setMessage("Entrez le rayon de recherche en mètres :")

        val container = FrameLayout(this)
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(searchRadiusMeters.toInt().toString())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(64, 0, 64, 0)
            }
        }
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Rechercher") { dialog, _ ->
            val newRadius = input.text.toString().toFloatOrNull()
            if (newRadius != null && newRadius > 0) {
                searchRadiusMeters = newRadius
                checkLocationPermissionAndLocate()
            } else {
                Toast.makeText(this, "Veuillez entrer un rayon valide", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Annuler") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun checkLocationPermissionAndLocate() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                locateUserAndOpenNearbyStations()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun locateUserAndOpenNearbyStations() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val intent = Intent(this, NearbyStationsActivity::class.java).apply {
                    putExtra("USER_LAT", location.latitude)
                    putExtra("USER_LON", location.longitude)
                    putExtra("SEARCH_RADIUS", searchRadiusMeters)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Impossible de récupérer votre position actuelle", Toast.LENGTH_SHORT).show()
            }
        }
    }
}