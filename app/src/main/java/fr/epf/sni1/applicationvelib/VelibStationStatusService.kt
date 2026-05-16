package fr.epf.sni1.applicationvelib

import retrofit2.http.GET

interface VelibStationStatusService {

    // J'utilise la même méthode qu'en cours
    @GET("opendata/Velib_Metropole/station_status.json")
    suspend fun getStationStatus() : GetStationStatusResponse

}

data class GetStationStatusResponse(val data: StationStatusData, val lastUpdatedOther: Long, val ttl: Int)
data class StationStatusData(val stations: List<StationStatus>)
data class StationStatus(val is_installed: Int, val is_renting: Int, val is_returning: Int, val last_reported: Long, val numBikesAvailable: Int, val numDocksAvailable: Int, val num_bikes_available: Int, val num_bikes_available_types: List<BikeTypes>, val num_docks_available: Int, val station_id: Long)
data class BikeTypes(val ebike: Int, val mechanical: Int)