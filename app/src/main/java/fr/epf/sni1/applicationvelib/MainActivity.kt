package fr.epf.sni1.applicationvelib

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
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
                    val textView = TextView(this@MainActivity)
                    // Je dois forçer le système à accepter stationStatus
                    textView.text = stationStatus!!.numBikesAvailable.toString()
                    textView.textSize = 14f
                    textView.setTextColor(Color.BLACK)
                    textView.gravity = Gravity.CENTER
                    textView.setBackgroundResource(R.drawable.fond_cercle)

                    textView.setOnClickListener {
                        val bottomSheetDialog = BottomSheetDialog(this@MainActivity)

                        val layout = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(64, 64, 64, 64)

                            addView(TextView(context).apply {
                                text = station.name
                                textSize = 20f
                                setTypeface(null, android.graphics.Typeface.BOLD)
                            })

                            addView(TextView(context).apply {
                                // Le nombre de vélos électriques disponibes est à la position [1], je ne sais pas pourquoi
                                text = "${stationStatus.num_bikes_available_types[1].ebike} vélos électriques"
                                textSize = 24f
                                setPadding(0, 24, 0, 0)

                                gravity = android.view.Gravity.CENTER_VERTICAL

                                val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.velib_bleu)
                                drawable?.setBounds(0, 0, 120, 90)
                                setCompoundDrawables(drawable, null, null, null)
                                compoundDrawablePadding = 16
                            })

                            addView(TextView(context).apply {
                                text = "${stationStatus.num_bikes_available_types[0].mechanical} vélos mécaniques"
                                textSize = 24f
                                setPadding(0, 24, 0, 0)

                                gravity = android.view.Gravity.CENTER_VERTICAL

                                val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.logo_velib)
                                drawable?.setBounds(0, 0, 120, 90)
                                setCompoundDrawables(drawable, null, null, null)
                                compoundDrawablePadding = 16
                            })

                            addView(TextView(context).apply {
                                text = "${stationStatus.numDocksAvailable} places disponibles"
                                textSize = 24f
                                setPadding(0, 24, 0, 0)
                                gravity = android.view.Gravity.CENTER_VERTICAL

                                val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.parking)
                                drawable?.setBounds(0, 0, 120, 90)
                                setCompoundDrawables(drawable, null, null, null)
                                compoundDrawablePadding = 16
                            })
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

                map.invalidate()

            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors de la récupération des stations", e)
            }
        }
    }
}