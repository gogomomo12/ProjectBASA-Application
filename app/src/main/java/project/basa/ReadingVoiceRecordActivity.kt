package project.basa

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class ReadingVoiceRecordActivity : Defaults() {
    val AUDIO_PERSMISSION = 2000
    var wordPosition = 0

    var voiceRecord:RecordReadingAudio? = null
    var timer:CustomChrono? = null

    var oralArray = JSONArray()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading_voice_record)

        drawActionBar("Oral Reading")

        val extras = intent.extras!!

        initializeJSONData(extras)
        initializeTimer()


        enableAudioSubmission(false)
        isRecording(false)
        val name = getMyName()
        voiceRecord = RecordReadingAudio(this,
            name!!["firstName"]!!,
            name["middleName"]!!,
            name["lastName"]!!,
            name["yearLevel"]!!,
            extras.getString("language")!!
        )

        checkAudioPermission()

    }


    private fun initializeJSONData(extras:Bundle){
        val origArray = JSONArray(extras.getString("oralArray"))
        val tempList = mutableListOf<JSONObject>()
        for (pos in 0 until origArray.length()){
            val jsonObject = origArray.getJSONObject(pos)
            if (extras.getString("language")!! == jsonObject.getString("language"))
                tempList.add(jsonObject)
        }

        tempList.sortBy { it.getString("level").toInt() }

        oralArray = JSONArray(tempList.toTypedArray())
        displayWord()

    }


    private fun initializeTimer(){
        val level = getMyName()!!["yearLevel"]!!.toInt()

        timer = CustomChrono(getOralLevelTimeLimit(level)*1000L,findViewById(R.id.timer))
        timer!!.onFinish = { onClickNextWord(null)}
    }


    private fun displayWord(){
        val json = oralArray.getJSONObject(wordPosition)
        findViewById<TextView>(R.id.wordDisplay).text = json.getString("word")
        findViewById<TextView>(R.id.levelDisplay).text = json.getString("level")
    }



    private fun checkAudioPermission(){
        val recordAudioPermission = android.Manifest.permission.RECORD_AUDIO
        if (checkSelfPermission(recordAudioPermission) == PackageManager.PERMISSION_DENIED)
            requestPermissions(arrayOf(recordAudioPermission), AUDIO_PERSMISSION)

    }


    private fun enableAudioSubmission(enabled:Boolean){
        findViewById<View>(R.id.submitButton).visibility = if (enabled) View.VISIBLE else View.GONE
        //findViewById<View>(R.id.nextWordButton).visibility = if (enabled) View.GONE else View.VISIBLE
    }


    private fun isRecording(recording:Boolean){
        if (recording) timer!!.start()
        else timer!!.stop()

        findViewById<View>(R.id.recordButton).visibility = if (recording) View.GONE else View.VISIBLE
        findViewById<View>(R.id.stopButton).visibility = if (recording) View.VISIBLE else View.GONE
    }


    fun onClickStartRecord(v: View){
        isRecording(true)
        val jsonObject = oralArray.getJSONObject(wordPosition)
        voiceRecord!!.recordAudio(jsonObject.getString("word"),jsonObject.getString("level"))

        if(wordPosition >= oralArray.length()-1) enableAudioSubmission(true)
    }


    fun onClickStopRecord(v: View?){
        enableAudioSubmission(false)
        isRecording(false)

        if (oralArray.length() > wordPosition+1){
            voiceRecord!!.stopAudio(true)
            wordPosition++
            displayWord()
        }
        else {
            voiceRecord!!.stopAudio(false)
            displayWord()
        }
    }


    fun onClickNextWord(v: View?){

        if(wordPosition < oralArray.length()-1 && voiceRecord!!.currentAudioName != null){
            voiceRecord!!.stopAudio(true)
            wordPosition++
            displayWord()
            isRecording(false)
        }

        else if(voiceRecord!!.currentAudioName == null) makeToast("Please record something first")




    }



    fun onClickSubmit(v:View){
        if (voiceRecord!!.currentAudioName == null) makeToast("Please record something first")
        else{
            v.isClickable = false
            showNormalDialog("Do you want to submit your recordings ?",
                {
                    it.cancel()
                    voiceRecord!!.stopAudio(true)
                    timer!!.stop()
                    voiceRecord!!.uploadAudios()
                },
                {
                    onClickStopRecord(null)
                    it.cancel()
                }
            )

        }


        v.isClickable = true

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERSMISSION && grantResults.isNotEmpty()){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) onClickNextWord(null)
            else {
                makeToast("Please accept record audio permission to use the application")
                finish()
            }

        }
    }


    override fun onBackPressed() {
        showNormalDialog("Your recordings has not been submitted yet. Do you want to exit?",
            {
                voiceRecord!!.stopAudio(false)
                voiceRecord!!.deleteTemporaryAudios()
                it.cancel()
                finish()
            },
            {
                it.cancel()
            }
        )
    }


}