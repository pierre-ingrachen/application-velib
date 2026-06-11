package fr.epf.sni1.applicationvelib

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NearbyStationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userLat = intent.getDoubleExtra("USER_LAT", 0.0)
        val userLon = intent.getDoubleExtra("USER_LON", 0.0)
        val radiusMeters = intent.getFloatExtra("SEARCH_RADIUS", 1000f)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(32, 64, 32, 32)
        }

        val backButton = TextView(this).apply {
            text = "←"
            textSize = 32f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(0, 0, 32, 0)
            setOnClickListener {
                finish()
            }
        }
        headerLayout.addView(backButton)

        val titleText = TextView(this).apply {
            text = "À moins de ${radiusMeters.toInt()}m"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerLayout.addView(titleText)

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

            layoutParams = LinearLayout.LayoutParams(120, 120)

            setOnClickListener { view ->
                val popup = PopupMenu(this@NearbyStationsActivity, view)
                popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_carte -> {
                            val intent = Intent(this@NearbyStationsActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            true
                        }
                        R.id.action_sauvegardes -> {
                            startActivity(Intent(this@NearbyStationsActivity, SavedStationsActivity::class.java))
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
        headerLayout.addView(menuButton)

        rootLayout.addView(headerLayout)

        val listView = ListView(this).apply {
            setPadding(32, 0, 32, 32)
        }
        rootLayout.addView(listView)

        setContentView(rootLayout)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://velib-metropole-opendata.smovengo.cloud/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val infoService = retrofit.create(VelibStationInformationService::class.java)

        lifecycleScope.launch {
            try {
                val infoResponse = infoService.getStations()
                val allStations = infoResponse.data.stations

                val nearbyStations = allStations.filter { station ->
                    val distance = calculateDistance(userLat, userLon, station.lat, station.lon)
                    distance <= radiusMeters
                }.sortedBy { station ->
                    calculateDistance(userLat, userLon, station.lat, station.lon)
                }

                if (nearbyStations.isEmpty()) {
                    Toast.makeText(this@NearbyStationsActivity, "Aucune station trouvée dans ce rayon", Toast.LENGTH_LONG).show()
                }

                val stationDisplayList = nearbyStations.map {
                    val dist = calculateDistance(userLat, userLon, it.lat, it.lon).toInt()
                    "${it.name}\n📍 à $dist mètres"
                }

                val adapter = ArrayAdapter(this@NearbyStationsActivity, android.R.layout.simple_list_item_1, stationDisplayList)
                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val selectedStation = nearbyStations[position]

                    val mapIntent = Intent(this@NearbyStationsActivity, MainActivity::class.java).apply {
                        putExtra("STATION_TO_OPEN", selectedStation.station_id.toString())
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(mapIntent)
                }

            } catch (e: Exception) {
                Toast.makeText(this@NearbyStationsActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}