package com.example.mobilecomputinglecture7

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import kotlinx.android.synthetic.main.activity_tts.*

class SpeechSynthetizer : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var tts : TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tts)

        tts = TextToSpeech( applicationContext, this)

        btn_speak.setOnClickListener {
            tts.setSpeechRate(1f)
            tts.setPitch(1f)
            tts.speak(txt_tts.text, TextToSpeech.QUEUE_ADD, Bundle.EMPTY, "message")
        }

        btn_robot.setOnClickListener {
            tts.setSpeechRate(0.5f)
            tts.setPitch(0.5f)
            tts.speak(txt_tts.text, TextToSpeech.QUEUE_ADD, Bundle.EMPTY, "message")
        }

        btn_helium.setOnClickListener {
            tts.setSpeechRate(2f)
            tts.setPitch(2f)
            tts.speak(txt_tts.text, TextToSpeech.QUEUE_ADD, Bundle.EMPTY, "message")
        }
    }

    /**
     * Initialised Text-To-Speech service
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { //good to go, greet the user
            tts.speak("Hi, hello there!", TextToSpeech.QUEUE_ADD, Bundle.EMPTY, "greeting")
        } else {
            //Ask device to install missing Text-To-Speech library
            startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //release the background service now that we are done
        tts.shutdown()
    }

}
