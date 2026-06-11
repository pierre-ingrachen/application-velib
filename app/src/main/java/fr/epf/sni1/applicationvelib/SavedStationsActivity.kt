package fr.epf.sni1.applicationvelib

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.File

class SavedStationsActivity : AppCompatActivity() {

    private lateinit var layout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }


        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            elevation = 8f
        }


        val backArrow = TextView(this).apply {
            text = "←"
            textSize = 32f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 32, 0)
            setOnClickListener {
                finish()
            }
        }

        val titleView = TextView(this).apply {
            text = "Stations enregistrées"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

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
            layoutParams = LinearLayout.LayoutParams(120, 120)
            elevation = 12f

            setOnClickListener { view ->
                val popup = PopupMenu(this@SavedStationsActivity, view)
                popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_carte -> {
                            finish()
                            true
                        }
                        R.id.action_sauvegardes -> true
                        else -> false
                    }
                }
                popup.show()
            }
        }


        headerLayout.addView(backArrow)
        headerLayout.addView(titleView)
        headerLayout.addView(menuButton)

        rootLayout.addView(headerLayout)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(64, 64, 64, 64)
        }

        scrollView.addView(layout)
        rootLayout.addView(scrollView)

        setContentView(rootLayout)
    }

    override fun onResume() {
        super.onResume()
        chargerStationsFavorites()
    }

    private fun chargerStationsFavorites() {
        layout.removeAllViews()

        val fichier = File(filesDir, "stations_favorites.json")

        if (fichier.exists()) {
            try {
                val gson = Gson()
                val jsonString = fichier.readText()

                if (jsonString.isNotEmpty() && jsonString != "[]") {
                    val stations = gson.fromJson(jsonString, Array<StationSauvegardee>::class.java).toList()

                    stations.forEach { station ->
                        val stationContainer = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(0, 24, 0, 24)

                            addView(TextView(this@SavedStationsActivity).apply {
                                text = station.nom
                                textSize = 22f
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                setTextColor(Color.BLACK)
                            })

                            addView(TextView(this@SavedStationsActivity).apply {
                                val totalVelos = station.velosMecaniques + station.velosElectriques
                                text = """
                                    🚴 $totalVelos vélos disponibles
                                    Méca : ${station.velosMecaniques} | Élec : ${station.velosElectriques}
                                    🅿️ Places libres : ${station.placesDisponibles}
                                """.trimIndent()
                                textSize = 18f
                                setTextColor(Color.DKGRAY)
                                setPadding(0, 16, 0, 0)
                            })
                        }
                        layout.addView(stationContainer)

                        val divider = android.view.View(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                2
                            ).apply { setMargins(0, 16, 0, 16) }
                            setBackgroundColor(Color.LTGRAY)
                        }
                        layout.addView(divider)
                    }
                } else {
                    afficherMessageVide()
                }
            } catch (e: Exception) {
                Log.e("SavedStationsActivity", "Erreur lors de la lecture du JSON", e)
                afficherMessageVide("Erreur lors de la lecture des données.")
            }
        } else {
            afficherMessageVide()
        }
    }

    private fun afficherMessageVide(message: String = "Vous n'avez pas encore de stations enregistrées.") {
        layout.addView(TextView(this).apply {
            text = message
            textSize = 18f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 0)
        })
    }
}