package project.basa

import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

open class Defaults: AppCompatActivity() {
    var serverApi = ""
    var db:SQLiteDatabase = SQLiteDatabase.create(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serverApi = gStr(R.string.server_api)
        db = openOrCreateDatabase("main_db", MODE_PRIVATE,null)
        db.execSQL("CREATE TABLE IF NOT EXISTS ${gStr(R.string.name_table)}(firstName VARCHAR,middleName VARCHAR,lastName VARCHAR , year INT)")
    }



    fun makeToast(message:Any?){
       Toast.makeText(this,"$message",Toast.LENGTH_SHORT).show()
    }


    fun drawActionBar(title:String){
        supportActionBar?.title = title
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColorFromAttr(R.attr.primaryColor)))
    }


    fun getColorFromAttr(res:Int):Int{
        val typedValue = TypedValue()
        theme.resolveAttribute(res,typedValue,true)
        return typedValue.data
    }

    fun testLog(message:Any?){
        Log.e("Log Message","$message")
    }


    fun gStr(res:Int):String = getString(res)

    fun showViewInDialog(v: View):AlertDialog{
        val dialog = AlertDialog.Builder(this)
        dialog.setView(v)
        dialog.create()

        return dialog.show().also {
            it.window?.setBackgroundDrawable(null)
        }
    }


    fun getMyName():MutableMap<String,String>?{
        var name:MutableMap<String,String>? = null

        db.rawQuery("SELECT * FROM ${gStr(R.string.name_table)}",null).also { c->
            if (c.moveToFirst()){
                name = mutableMapOf()
                name!!["firstName"] = (c.getString(0)).capitalize()
                name!!["middleName"] = (c.getString(1)).capitalize()
                name!!["lastName"] = (c.getString(2)).capitalize()
                name!!["yearLevel"] = c.getString(3)
            }
        }

        return name
    }

    fun showErrorDialog(message:String){
        val errorView = LayoutInflater.from(this).inflate(R.layout.error_dialog,null)
        errorView.findViewById<TextView>(R.id.errorMessage).text = message

        val dialog = showViewInDialog(errorView)

        errorView.findViewById<View>(R.id.restartApplication).setOnClickListener{
            dialog.cancel()
            finishAffinity()
            startActivity(Intent(this,MainActivity::class.java))
        }
    }


    fun showNewVersionAvailable(){
        val latestView = LayoutInflater.from(this).inflate(R.layout.new_version_available,null)

        showViewInDialog(latestView).also {dialog->
            dialog.setCancelable(false)
            latestView.findViewById<View>(R.id.exitApplication).setOnClickListener{
                dialog.cancel()
                finishAffinity()
            }
        }

    }


    fun showWaitingMessage(message:String):AlertDialog{
        val waitingView = LayoutInflater.from(this).inflate(R.layout.waiting_dialog,null)
        waitingView.findViewById<TextView>(R.id.message).text = message
        return showViewInDialog(waitingView).also {
            it.setCancelable(false)
        }
    }



    fun showNormalDialog(message:String, onYes:((dialog:DialogInterface)->Unit)? ,  onNo:((dialog:DialogInterface)->Unit)?){
        val dialog = AlertDialog.Builder(this)
        dialog.setMessage(message)

        val setColor = {string:String->
            Html.fromHtml("<font color=${getColorFromAttr(R.attr.secondaryColor)}>$string</font>")
        }

        dialog.setPositiveButton(setColor("Yes")){ dialog, which ->
            onYes?.invoke(dialog)

        }


        dialog.setNegativeButton(setColor("No")){ dialog, _ ->
            onNo?.invoke(dialog)
        }

        dialog.create()
        dialog.show()
    }



    fun getOralLevelTimeLimit(level:Int):Int = when(level){
        1->10
        2->9
        3->8
        4->7
        5->6
        6->5
        7->4
        else ->0
    }



}