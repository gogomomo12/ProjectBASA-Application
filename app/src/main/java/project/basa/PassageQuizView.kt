package project.basa

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.children
import com.opencsv.CSVWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors

class PassageQuizView(val defaults: Defaults, val passageJSON:JSONArray):LinearLayout(defaults){

    var currentItemDisplayNumber = 1    //the display number on question items

    var currentPassageIndex = 0
    val passages = passageJSON.getJSONArray(0)
    val passageDisplay = LayoutInflater.from(defaults).inflate(R.layout.passage_display_layout,null)

    val matchparent = LayoutParams.MATCH_PARENT

    val answers = mutableListOf<JSONObject>()

    init {
        post{
            layoutParams = LayoutParams(matchparent,matchparent)
            passageDisplay.layoutParams = LayoutParams(matchparent,matchparent)
        }

        nextPassage(null)
    }


    private fun nextPassage(clickedView:View?){
        clickedView?.isClickable = false

        if (currentPassageIndex < passages.length()){
            removeAllViews()

            addView(passageDisplay)
            val passageJSON = JSONObject(passages.getString(currentPassageIndex))

            passageDisplay.findViewById<TextView>(R.id.passageTitle).text = passageJSON.getString("title")
            passageDisplay.findViewById<TextView>(R.id.passageText).text = passageJSON.getString("passage")
            passageDisplay.findViewById<View>(R.id.goToQuestions).setOnClickListener{gotoQuestionView->
                gotoQuestionView.isClickable = false

                val next = {
                    removeAllViews()
                    addView(PassageQuestion())
                }

                defaults.showNormalDialog("You won't be able to go back to read this passage again. Go to the questions ?",
                    {
                        it.cancel()
                        next()
                    },
                    {
                        it.cancel()
                    }
                )

                gotoQuestionView.isClickable = true
            }

            currentPassageIndex++
        }
        else {
            defaults.testLog(answers.size)
            submitAnswers()
        }


        clickedView?.isClickable = true


    }


    
    private fun submitAnswers(){
        val saveAsCSV = {
            val csvName = defaults.filesDir.absolutePath + "/${UUID.randomUUID()}.csv"

            val fileWriter = FileWriter(csvName)
            val writer = CSVWriter(fileWriter)

            val keys = mutableListOf<String>()
            for (key in answers[0].keys()){
                keys.add(key)
            }

            writer.writeNext(keys.toTypedArray())

            val jsonValues = mutableListOf<Array<String>>()
            answers.forEach{jsonObject ->
                val values = mutableListOf<String>()
                keys.forEach{key->
                    values.add(jsonObject.getString(key))
                }
                jsonValues.add(values.toTypedArray())
            }

            writer.writeAll(jsonValues)

            writer.flush()
            writer.close()

            csvName
        }



        val uploadAnswers = {csvName:String ->
            val headers: MutableMap<String?, String?> = HashMap()
            headers["User-Agent"] = System.setProperty("http.agent", "")    //using this user agent to bypass local tunnel reminder page

            val multipart = HttpPostMultipart(defaults.serverApi + "uploadAnswer",
                "utf-8",headers)

            val name = defaults.getMyName()

            multipart.addFilePart("csvFile",File(csvName))
            multipart.addFormField("firstName",name!!["firstName"]!!)
            multipart.addFormField("middleName", name["middleName"]!!)
            multipart.addFormField("lastName", name["lastName"]!!)
            multipart.addFormField("year",name["yearLevel"]!!)

            val response = multipart.finish()
            defaults.testLog(response)
        }


        defaults.showNormalDialog("This is the last question. Do you want to submit your quiz ?",
            {
                it.cancel()

                val waiting = defaults.showWaitingMessage("Uploading response")
                Executors.newFixedThreadPool(1).execute{
                    val csvName = saveAsCSV()
                    val done = {
                        val csvFile = File(csvName)
                        if (csvFile.exists()) csvFile.delete()
                        waiting.cancel()
                    }

                    try {
                        uploadAnswers(csvName)
                        defaults.runOnUiThread{
                            done()
                            defaults.makeToast("Quiz uploaded successfully")
                            defaults.finish()
                        }
                    }catch (e:Exception){
                        defaults.runOnUiThread{
                            done()
                            defaults.showErrorDialog("Error occurred while uploading response")
                            defaults.testLog(e)
                        }
                    }




                }



            },
            { it.cancel() }
        )
    }







    private inner class PassageQuestion:LinearLayout(defaults){
        val passagesQuestions = mutableListOf<JSONObject>()
        val currentPassageJSON = passages.getJSONObject(currentPassageIndex-1)

        var questionIndex = 0
        val passageQuestionDisplay = LayoutInflater.from(defaults).inflate(R.layout.passage_question_layout,null)
        val currentPassageResponse = mutableMapOf<String,Map<Int,String>>()

        init {

            post{
                layoutParams = LayoutParams(matchparent,matchparent)
            }

            passageQuestionDisplay.layoutParams = LayoutParams(matchparent,matchparent)
            addView(passageQuestionDisplay)

            passageQuestionDisplay.findViewById<View>(R.id.nextQuestion).setOnClickListener{nextQuestionView->
                if (questionIndex < passagesQuestions.size-1) nextQuestion()
                else {
                    defaults.showNormalDialog("No questions left. Go to the next passage ?",
                        {
                            it.cancel()
                            saveAnswers()
                            saveAnswersInMain()
                            nextPassage(nextQuestionView)
                            currentItemDisplayNumber++
                        },
                        {it.cancel()}
                    )
                }
            }



            passageQuestionDisplay.findViewById<View>(R.id.previouQuestion).setOnClickListener{
                if (questionIndex > 0) previousQuestion()
                else defaults.makeToast("No previous questions")
            }





            getPassageQuestions()
            displayQuestion()

        }


        private fun getPassageQuestions(){
            val mainArray = passageJSON.getJSONArray(1)
            for (index in 0 until mainArray.length()){
                val json = mainArray.getJSONObject(index)

                if (json.getString("passage_title") == currentPassageJSON.getString("title"))
                    passagesQuestions.add(json)
            }
        }



        private fun previousQuestion(){
            saveAnswers()
            questionIndex--
            currentItemDisplayNumber--
            if (questionIndex <0 ) questionIndex = 0
            else displayQuestion()
        }



        private fun nextQuestion(){
            saveAnswers()
            questionIndex++
            currentItemDisplayNumber++
            if (questionIndex < passagesQuestions.size) displayQuestion()
            else questionIndex = passagesQuestions.size


        }



        private fun saveAnswers(){
            if ((currentPassageResponse.isEmpty() && passagesQuestions.isNotEmpty())
                || (questionIndex >= 0 && questionIndex<passagesQuestions.size)){
                val questionChoices = findViewById<RadioGroup>(R.id.questionChoices)
                val checked = questionChoices.checkedRadioButtonId
                currentPassageResponse["$questionIndex"] = mapOf(
                    checked to findViewById<RadioButton>(checked).text.toString())

            }

        }


        private fun saveAnswersInMain(){
            //commit answers to the the main answer lists
            currentPassageResponse.forEach{answer->
                try{
                    answers.add(JSONObject().also {
                        it.put("firstName","John")
                        it.put("middleName","Kyle")
                        it.put("lastName","Ismael")
                        it.put("language",currentPassageJSON.getString("language"))
                        it.put("passageTitle",currentPassageJSON.getString("title"))
                        it.put("question",passagesQuestions[answer.key.toInt()].getString("question"))
                        it.put("answer",answer.value.values.toTypedArray()[0])
                    })

                }catch (e:Exception){
                    defaults.testLog(e)
                }
            }

        }




        private fun displayQuestion(){
            val questionJSON = passagesQuestions[questionIndex]


            passageQuestionDisplay.findViewById<TextView>(R.id.question).text = "$currentItemDisplayNumber. ${questionJSON.getString("question")}"
            passageQuestionDisplay.findViewById<RadioButton>(R.id.choice1).text = questionJSON.getString("choice1")
            passageQuestionDisplay.findViewById<RadioButton>(R.id.choice2).text = questionJSON.getString("choice2")
            passageQuestionDisplay.findViewById<RadioButton>(R.id.choice3).text = questionJSON.getString("choice3")
            passageQuestionDisplay.findViewById<RadioButton>(R.id.choice4).text = questionJSON.getString("choice4")

            val questionChoices = findViewById<RadioGroup>(R.id.questionChoices)
            val response = currentPassageResponse["$questionIndex"]

            var radioSelected = R.id.choice1
            if (response != null) radioSelected = response.keys.toIntArray()[0]

            questionChoices.check(radioSelected)


        }



    }




}