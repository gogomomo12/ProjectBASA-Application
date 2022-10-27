package project.basa

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class ReadingLevelAdapter(val context:Defaults,val levels:MutableList<ReadingLevel>,val oralReadingJsonArray:JSONArray): RecyclerView.Adapter<ReadingLevelAdapter.CustomViewHolder>(){

    class CustomViewHolder(v: View): RecyclerView.ViewHolder(v){
        val image = v.findViewById<ImageView>(R.id.image)
        val description = v.findViewById<TextView>(R.id.description)
        val timeLimit = v.findViewById<TextView>(R.id.timeLimit)
        val mainView = v
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val readingLevelBox = LayoutInflater.from(parent.context).inflate(R.layout.reading_level_box,parent,false)
        return  CustomViewHolder(readingLevelBox.also {
            (it.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0,0,0,10)
        })
    }



    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val level = levels[position]
        val language = if (level.isEnglish) "english" else "filipino"
        val title = (if (language == "english") "Level " else "Baitang ") + level.level

        holder.image.setImageDrawable(context.getDrawable(level.imageRes))
        holder.description.text = title
        holder.timeLimit.text = "${context.getOralLevelTimeLimit(level.level)} sec"


        holder.mainView.setOnClickListener{
            val intent = Intent(context,ReadingVoiceRecordActivity::class.java)
            val words = mutableListOf<String>()

            for (index in 0 until oralReadingJsonArray.length()){
                val json = oralReadingJsonArray.getJSONObject(index)

                if (json.getInt("level") == level.level &&
                    (json.getString("language") == "english") == level.isEnglish){
                    words.add(json.getString("word"))
                }
            }

            intent.putExtra("jsonWords",words.toTypedArray())
            intent.putExtra("level",level.level.toString())
            intent.putExtra("language",language )
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = levels.size




    class ReadingLevel(val level:Int,val isEnglish:Boolean , val imageRes:Int)

}