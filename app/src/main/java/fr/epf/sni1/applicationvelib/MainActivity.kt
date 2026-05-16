package fr.epf.sni1.applicationvelib

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_main)

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
                    val marker = Marker(map)
                    marker.position = GeoPoint(station.lat, station.lon)
                    marker.title = station.name
                    marker.snippet = "Nombre de vélos disponibles : ${stationStatus!!.numBikesAvailable}"

                    map.overlays.add(marker)
                }

                map.invalidate()

            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors de la récupération des stations", e)
            }
        }
    }
}