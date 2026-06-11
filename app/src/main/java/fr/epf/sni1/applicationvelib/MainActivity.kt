package fr.epf.sni1.applicationvelib

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_main)


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

            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors de la récupération des stations", e)
            }
        }
    }
}