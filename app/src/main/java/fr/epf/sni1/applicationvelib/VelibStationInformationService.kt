package fr.epf.sni1.applicationvelib

import retrofit2.http.GET

interface VelibStationInformationService {

    // J'utilise la même méthode qu'en cours
    @GET("opendata/Velib_Metropole/station_information.json")
    suspend fun getStations() : VelibStationResponse

}

data class VelibStationResponse(val data: VelibData, val lastUpdatedOther: Long, val ttl: Int)
data class VelibData(val stations: List<Station>)
data class Station(val capacity: Int, val lat: Double, val lon: Double, val name: String, val station_id: Long)