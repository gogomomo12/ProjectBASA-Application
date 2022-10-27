package project.basa


import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.util.concurrent.Executors

class MainActivity : Defaults() {

    var passagesArray = ""
    var oralArray = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawActionBar("BASA")

        val waiting = showWaitingMessage("Connecting to server")
        mainProcess(waiting)



    }


    private fun mainProcess(waiting: AlertDialog){
        Executors.newFixedThreadPool(1).execute{
            try {
                val oralQuestions = "getOralReadingQuestions"
                val passageQuestions = "getPassageQuestions"
                val versions = "getServerVersion"

                val jsonsMap = ServerJsons(this, listOf(oralQuestions,passageQuestions,versions))
                    .getServerJSONS()


                val appVersionName = packageManager.getPackageInfo(packageName,0).versionName
                if (JSONObject(jsonsMap[versions]).getString("latestAppVersion") == appVersionName){

                    runOnUiThread{
                        if (getMyName() == null) showNameDialog(false)

                        passagesArray = jsonsMap[passageQuestions]!!
                        oralArray = jsonsMap[oralQuestions]!!
                    //    initializeReadingRecycler(JSONArray(jsonsMap[oralQuestions]))

                    }

                }
                else runOnUiThread{ showNewVersionAvailable() }

            }catch (e:Exception){
                runOnUiThread{  showErrorDialog("Unexpected error. Please restart the application") }
            }


            waiting.cancel()

        }
    }





    private fun initializeReadingRecycler(oralReadingJson:JSONArray){
        val readingRecycler = findViewById<RecyclerView>(R.id.readingRecycler)
        readingRecycler.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)

        val readingLevel = {title:Int , isEnglish:Boolean , imageRes:Int ->
            ReadingLevelAdapter.ReadingLevel(title,isEnglish,imageRes)
        }

        val levels = mutableListOf(
            readingLevel(1, true,R.drawable.one),
            readingLevel(2, true,R.drawable.two),
            readingLevel(3, true,R.drawable.three),
            readingLevel(4, true,R.drawable.four),
            readingLevel(5, true,R.drawable.five),
            readingLevel(6, true,R.drawable.six),
            readingLevel(7, true,R.drawable.seven),
            readingLevel(1, false,R.drawable.one),
            readingLevel(2, false,R.drawable.two),
            readingLevel(3, false,R.drawable.three),
            readingLevel(4, false,R.drawable.four),
            readingLevel(5, false,R.drawable.five),
            readingLevel(6, false,R.drawable.six),
            readingLevel(7, false,R.drawable.seven),
        )

        readingRecycler.adapter = ReadingLevelAdapter(this,levels,oralReadingJson)
    }






    private fun showNameDialog(dialogCancelable:Boolean){
        val mainView = LayoutInflater.from(this).inflate(R.layout.name_box,null)
        val firstName = mainView.findViewById<EditText>(R.id.firstName)
        val middleName = mainView.findViewById<EditText>(R.id.middleName)
        val lastName = mainView.findViewById<EditText>(R.id.lastName)
        val yearLevel = mainView.findViewById<Spinner>(R.id.yearLevel)

        val existingName = getMyName()
        if (existingName != null){
            firstName.setText(existingName["firstName"])
            middleName.setText(existingName["middleName"])
            lastName.setText(existingName["lastName"])
            yearLevel.setSelection(existingName["yearLevel"]!!.toInt() - 1)
        }


        val dialog = showViewInDialog(mainView).also {
            it.setCancelable(dialogCancelable)
        }

        mainView.findViewById<View>(R.id.setNameButton).setOnClickListener{
            val cleanInput = {editable:Editable -> editable.toString().replace("'","''").capitalize()}

            val rawFirstName = firstName.text
            val f = cleanInput(rawFirstName)
            val m = cleanInput(middleName.text)
            val l = cleanInput(lastName.text)

            if (f.isEmpty() || m.isEmpty() || l.isEmpty()) makeToast("Please fill out all the fields")
            else {
                val nameTable = gStr(R.string.name_table)
                db.execSQL("DELETE FROM $nameTable")
                db.execSQL("INSERT INTO $nameTable(firstName,middleName,lastName,year)" +
                        "VALUES('${f}','${m}','${l}',${yearLevel.selectedItem})"
                )
                makeToast("Hello $rawFirstName !!")
                dialog.cancel()
            }
        }
    }



    private fun gotoOralReading(language:String){
        val intent = Intent(this,ReadingVoiceRecordActivity::class.java)
        intent.putExtra("oralArray",oralArray )
        intent.putExtra("language",language)
        intent.putExtra("passagesArray",passagesArray)
        startActivity(intent)
    }

    fun onClickEnglishQuiz(v:View){
        gotoOralReading("english")
    }


    fun onClickFilipinoQuiz(v:View){
        gotoOralReading("filipino")
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_act_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.me -> showNameDialog(true)
        }
        return super.onOptionsItemSelected(item)
    }







}