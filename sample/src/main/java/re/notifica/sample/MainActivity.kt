package re.notifica.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import re.notifica.Notificare

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Notificare.launch()
    }
}
