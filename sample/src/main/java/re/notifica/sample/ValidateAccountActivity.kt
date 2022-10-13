package re.notifica.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.authentication.ktx.authentication
import timber.log.Timber

class ValidateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = Notificare.authentication().parseValidateUserToken(intent)

        if (token == null) {
            openSampleActivity()
            return
        }

        lifecycleScope.launch {
            try {
                Notificare.authentication().validateUser(token)
            } catch (e: Exception) {
                Timber.e(e, "Failed to validate user account.")
                Toast.makeText(baseContext, "Failed to validate user account.", Toast.LENGTH_SHORT).show()

                openSampleActivity()
                return@launch
            }

            Timber.i("User account validated successfully.")
            Toast.makeText(baseContext, "User account validated successfully.", Toast.LENGTH_SHORT).show()

            openSampleActivity()
        }
    }

    private fun openSampleActivity() {
        val intent = Intent(this, SampleActivity::class.java)
        startActivity(intent)
        this.finish()
    }
}
