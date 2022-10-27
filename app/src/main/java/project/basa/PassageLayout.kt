package project.basa

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import org.json.JSONArray
import org.json.JSONObject

class PassageLayout : Defaults() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_layout)

        val test = "[\n" +
                "                 [\n" +
                "                  {\"title\":\"Passage1\" , \"passage\":\"This is a test passage. lorem ipsum\" , \"language\":\"english\" , \"word_count\":\"20\"},\n" +
                "                  {\"title\":\"Passage2\" , \"passage\":\"This is a test passage. lorem ipsum\" , \"language\":\"english\" , \"word_count\":\"22\"},\n" +
                "                  {\"title\":\"Basahin1\" , \"passage\":\"Eto ay test passage. lorem ipsum\" , \"lquanguage\":\"filipino\" , \"word_count\":\"20\"}\n" +
                "                 ],\n" +
                "\n" +
                "                 [\n" +
                "                  {\"passage_title\":\"Passage1\" , \"question\":\"This is a test question\" , \"choice1\":\"tomato\" , \"choice2\":\"tomato\" , \"choice3\":\"tomato\" , \"choice4\":\"tomato\"} ,\n" +
                "                  {\"passage_title\":\"Passage1\" , \"question\":\"This is a test question\" ,\"choice1\":\"tomato\" , \"choice2\":\"tomato\" , \"choice3\":\"tomato\" , \"choice4\":\"tomato\"},\n" +
                "                  {\"passage_title\":\"Passage1\" , \"question\":\"This is a test question\" ,\"choice1\":\"tomato\" , \"choice2\":\"tomato\" , \"choice3\":\"tomato\" , \"choice4\":\"tomato\"},\n" +
                "                  {\"passage_title\":\"Passage2\" , \"question\":\"This is a test question\" ,\"choice1\":\"tomato\" , \"choice2\":\"tomato\" , \"choice3\":\"tomato\" , \"choice4\":\"tomato\"},\n" +
                "                  {\"passage_title\":\"Basahin1\" , \"question\":\"This is a test question\" ,\"choice1\":\"kamatis\" , \"choice2\":\"kamatis\" , \"choice3\":\"kamatis\" , \"choice4\":\"kamatis\"}\n" +
                "                 ]\n" +
                "                ]"



        drawActionBar("Reading Comprehension")

        findViewById<LinearLayout>(R.id.quizView).addView(
            PassageQuizView(this, getPassageJSONArray())
        )

    }


    private fun getPassageJSONArray():JSONArray{
        val extras = intent.extras!!
        val language = extras.getString("language")

        val passageArray = JSONArray(extras.getString("passagesArray"))
        val passages = passageArray.getJSONArray(0)

        val filteredPassages = mutableListOf<JSONObject>()

        for (index in 0 until  passages.length()){
            val passageJSON = passages.getJSONObject(index)
            if (passageJSON.getString("language") == language) filteredPassages.add(passageJSON)
        }

        return JSONArray(arrayOf(JSONArray(filteredPassages),passageArray.getJSONArray(1)))
    }


    override fun onBackPressed() {
        showNormalDialog("Your quiz is not finished yet. Do you want to exit ?",
            {
                it.cancel()
                finish()
            },
            {
                it.cancel()
            }
        )
    }
}