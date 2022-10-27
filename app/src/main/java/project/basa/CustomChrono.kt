package project.basa

import android.os.CountDownTimer
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class CustomChrono(val millisLimit:Long, val displayView:TextView){
    var currentSeconds = 0L
    var onFinish:(()->Unit)? = null



    private val countDownTimer = object : CountDownTimer(millisLimit,1000){
        override fun onTick(millisUntilFinished: Long) {
            currentSeconds++
            displayTime()
        }

        override fun onFinish() {
            onFinish?.invoke()
        }

    }




    private fun displayTime(){
        val format = SimpleDateFormat("mm:ss")
        Calendar.getInstance().also {
            it.timeInMillis = currentSeconds * 1000
            displayView.setText(format.format(it.time))
        }

    }




    fun start() {
        displayTime()
        countDownTimer.start()
    }

    fun stop() {
        countDownTimer.cancel()
        currentSeconds = 0L
        displayTime()

    }






}