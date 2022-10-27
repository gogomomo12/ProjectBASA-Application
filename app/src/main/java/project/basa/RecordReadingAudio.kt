package project.basa

import android.content.Intent
import android.media.MediaRecorder
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class RecordReadingAudio(val context: Defaults,val firstName:String,val middleName:String,val lastName:String , val year:String , val language:String) {
    val temporaryAudioPath = context.filesDir.absolutePath + "audios/"
    var mediaRecorder: MediaRecorder? = null
    var currentAudioName:String? = null
    var currentWord:String? = null
    var currentLevel:String? = null

    var answerLists = HashMap<String,String>()

    init {
        val audioFolder = File(temporaryAudioPath)
        if (!audioFolder.exists()) audioFolder.mkdir()
        else deleteTemporaryAudios()
    }

    private fun gotoPassage(){
        val intent = Intent(context,PassageLayout::class.java)
        val extras = context.intent.extras
        intent.putExtra("language",extras!!.getString("language")!!)
        intent.putExtra("passagesArray", extras.getString("passagesArray")!!)
        context.startActivity(intent)
    }


    fun deleteTemporaryAudios(){
        val audioFolder = File(temporaryAudioPath)
        audioFolder.listFiles().forEach {
            it.delete()
        }
    }


    fun recordAudio(word:String,level:String){
        if (mediaRecorder != null)  stopAudio(false)

        currentWord = word
        currentLevel = level

        currentAudioName = temporaryAudioPath +  "${UUID.randomUUID()}.mp3"
        mediaRecorder = MediaRecorder().also {
            it.setAudioSource(MediaRecorder.AudioSource.MIC)
            it.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            it.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
            it.setOutputFile(currentAudioName);
        }

        mediaRecorder?.prepare()
        mediaRecorder?.start()
    }



    fun stopAudio(saveAudio: Boolean){
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        }catch (e:Exception){

        }

        mediaRecorder = null

        if(currentAudioName != null && currentWord != null && currentLevel != null){
            val audioFile = File(currentAudioName!!)
            if (!saveAudio && audioFile.exists()) audioFile.delete()
            else if (saveAudio) answerLists[currentAudioName!!] = "${currentWord!!} ${currentLevel!!}"
        }

        currentAudioName = null
        currentLevel = null
    }




    fun uploadAudios(){
        val waitingDialog = context.showWaitingMessage("Saving response. Please wait.")

        Executors.newFixedThreadPool(1).execute{
            val done = {
                answerLists.clear()
                deleteTemporaryAudios()
                waitingDialog.cancel()
            }

            try {
                for (audio:String in answerLists.keys) {
                    val values = answerLists[audio]!!

                    val headers: MutableMap<String?, String?> = HashMap()
                    headers["User-Agent"] =
                        System.setProperty("http.agent", "")    //using this user agent to bypass local tunnel reminder page

                    val splitted = values.split(" ")
                    val word = splitted[0]
                    val level = splitted[1]
                    val multipart = HttpPostMultipart(context.serverApi + "uploadAudio", "utf-8", headers)

                    multipart.addFormField("firstName", firstName)
                    multipart.addFormField("middleName", middleName)
                    multipart.addFormField("lastName", lastName)
                    multipart.addFormField("level", level)
                    multipart.addFormField("language", language)
                    multipart.addFormField("word", word)
                    multipart.addFormField("year", year)
                    multipart.addFilePart("audio", File(audio))

                    val response = multipart.finish()
                    context.testLog(response)
                }

                context.runOnUiThread{
                    context.makeToast("Your recordings uploaded successfully")
                    done()
                    gotoPassage()
                    context.finish()
                }


            } catch (e:Exception){
                context.runOnUiThread{
                    done()
                    context.testLog(e)
                    context.showErrorDialog("Error occurred submitting your response. Please restart the application.")
                }
            }
        }





    }


}