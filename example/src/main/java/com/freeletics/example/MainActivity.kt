package com.freeletics.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.freeletics.rxsmartlock.RxGoogleSmartLockManager
import com.google.android.gms.auth.api.credentials.Credential
import kotlinx.android.synthetic.main.activity_main.deleteBtn
import kotlinx.android.synthetic.main.activity_main.disableBtn
import kotlinx.android.synthetic.main.activity_main.hintsBtn
import kotlinx.android.synthetic.main.activity_main.loginInput
import kotlinx.android.synthetic.main.activity_main.passwordInput
import kotlinx.android.synthetic.main.activity_main.retrieveBtn
import kotlinx.android.synthetic.main.activity_main.saveBtn

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        saveBtn.setOnClickListener {
            doSaveCredentials()
        }

        retrieveBtn.setOnClickListener {
            doRetrieveCredentials()
        }

        deleteBtn.setOnClickListener {
            doDeleteCredentials()
        }

        hintsBtn.setOnClickListener {
            doGetHints()
        }

        disableBtn.setOnClickListener {
            doDisableSmartLock()
        }
    }

    private fun doSaveCredentials() {
        val login = loginInput.text.toString()
        val password = passwordInput.text.toString()

        val credential = Credential.Builder(login).setName(login)
            .setPassword(password)
            .build()
        RxGoogleSmartLockManager.storeCredentials(this, credential)
            .subscribe({ Toast.makeText(this, "Stored successfully", Toast.LENGTH_SHORT).show() },
                { Toast.makeText(this, "Failed storing: $it", Toast.LENGTH_SHORT).show() })
    }

    private fun doRetrieveCredentials() {
        RxGoogleSmartLockManager.retrieveCredentials(this)
            .subscribe({
                Toast.makeText(this, "Retrieved successfully", Toast.LENGTH_SHORT).show()
                loginInput.setText(it.name)
                passwordInput.setText(it.password)
            }, { Toast.makeText(this, "Failed retrieving: $it", Toast.LENGTH_SHORT).show() })
    }

    private fun doDeleteCredentials() {
        val login = "example"
        val password = "0000"
        val credential = Credential.Builder(login).setName(login)
            .setPassword(password)
            .build()

        RxGoogleSmartLockManager.deleteStoredCredentials(this, credential)
            .subscribe({
                Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
            }, { Toast.makeText(this, "Failed deleting: $it", Toast.LENGTH_SHORT).show() })
    }

    private fun doGetHints() {
        RxGoogleSmartLockManager.retrieveSignInHints(this)
            .subscribe({
                Toast.makeText(this, "Retrieved successfully", Toast.LENGTH_SHORT).show()
                loginInput.setText(it.email)
            }, { Toast.makeText(this, "Failed retrieving: $it", Toast.LENGTH_SHORT).show() })

    }

    private fun doDisableSmartLock() {
        RxGoogleSmartLockManager.disableAutoSignIn(this)
            .subscribe({
                Toast.makeText(this, "Disabled successfully", Toast.LENGTH_SHORT).show()
            }, { Toast.makeText(this, "Failed disabling: $it", Toast.LENGTH_SHORT).show() })

    }
}
