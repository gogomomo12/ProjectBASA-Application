package project.basa

import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.HashMap

class ServerJsons(val context:Defaults,val apiName:List<String>) {


    private fun getApiResponse(api:String):String{
        val url = URL(context.serverApi + api)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent",System.setProperty("http.agent", "") )
        conn.connect()

        var data = ""
        val sc = Scanner(url.openStream())
        while (sc.hasNextLine()) data+=sc.nextLine()

        conn.disconnect()
        return data
    }


    fun getServerJSONS():Map<String,String>{
        var jsons = HashMap<String,String>()
        for (api:String in apiName){
            jsons[api] = getApiResponse(api)
        }

        return jsons
    }



}