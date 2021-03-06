package com.freeletics.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.freeletics.example.databinding.ActivityMainBinding
import com.freeletics.rxsmartlock.RxGoogleSmartLockManager
import com.google.android.gms.auth.api.credentials.Credential
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.saveBtn.setOnClickListener {
            doSaveCredentials()
        }

        viewBinding.retrieveBtn.setOnClickListener {
            doRetrieveCredentials()
        }

        viewBinding.deleteBtn.setOnClickListener {
            doDeleteCredentials()
        }

        viewBinding.hintsBtn.setOnClickListener {
            doGetHints()
        }

        viewBinding.disableBtn.setOnClickListener {
            doDisableSmartLock()
        }
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    private fun doSaveCredentials() {
        val login = viewBinding.loginInput.text.toString()
        val password = viewBinding.passwordInput.text.toString()

        val credential = Credential.Builder(login).setName(login)
            .setPassword(password)
            .build()
        disposables += RxGoogleSmartLockManager
            .storeCredentials(this, credential)
            .subscribe(
                { Toast.makeText(this, "Stored successfully", Toast.LENGTH_SHORT).show() },
                { Toast.makeText(this, "Failed storing: $it", Toast.LENGTH_SHORT).show() }
            )
    }

    private fun doRetrieveCredentials() {
        disposables += RxGoogleSmartLockManager.retrieveCredentials(this)
            .subscribe(
                {
                    Toast.makeText(this, "Retrieved successfully", Toast.LENGTH_SHORT).show()
                    viewBinding.loginInput.setText(it.name)
                    viewBinding.passwordInput.setText(it.password)
                },
                { Toast.makeText(this, "Failed retrieving: $it", Toast.LENGTH_SHORT).show() }
            )
    }

    private fun doDeleteCredentials() {
        val login = "example"
        val password = "0000"
        val credential = Credential.Builder(login).setName(login)
            .setPassword(password)
            .build()

        disposables += RxGoogleSmartLockManager.deleteStoredCredentials(this, credential)
            .subscribe(
                {
                    Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
                },
                {
                    Toast.makeText(this, "Failed deleting: $it", Toast.LENGTH_SHORT).show()
                }
            )
    }

    private fun doGetHints() {
        disposables += RxGoogleSmartLockManager.retrieveSignInHints(this)
            .subscribe(
                {
                    Toast.makeText(this, "Retrieved successfully", Toast.LENGTH_SHORT).show()
                    viewBinding.loginInput.setText(it.email)
                },
                {
                    Toast.makeText(this, "Failed retrieving: $it", Toast.LENGTH_SHORT).show()
                }
            )

    }

    private fun doDisableSmartLock() {
        disposables += RxGoogleSmartLockManager.disableAutoSignIn(this)
            .subscribe(
                {
                    Toast.makeText(this, "Disabled successfully", Toast.LENGTH_SHORT).show()
                },
                {
                    Toast.makeText(this, "Failed disabling: $it", Toast.LENGTH_SHORT).show()
                }
            )

    }
}
