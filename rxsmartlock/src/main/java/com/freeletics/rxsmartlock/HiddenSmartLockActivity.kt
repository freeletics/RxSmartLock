package com.freeletics.rxsmartlock

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import timber.log.Timber

class HiddenSmartLockActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i(
            "SmartLock: HiddenSmartLockActivity::onCreate fresh=%b",
            savedInstanceState == null
        )
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val hintIntent: PendingIntent? = intent.getParcelableExtra(EXTRA_HINT_INTENT)
        if (hintIntent != null) {
            try {
                startIntentSenderForResult(
                        hintIntent.intentSender,
                        REQUEST_CODE_RESOLVE_HINTS, null, 0, 0, 0
                )
            } catch (e: ActivityNotFoundException) {
                Timber.e(e,"SmartLock: Activity not found for intent - $hintIntent")
                // may happen on some ROMs and we don't want a crash to happen
                RxGoogleSmartLockManager.handleActivityResult(RESULT_CANCELED, null)
                finish()
            }
        } else {
            // finish with error
            RxGoogleSmartLockManager.handleActivityResult(RESULT_CANCELED, null)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RESOLVE_HINTS) {
            RxGoogleSmartLockManager.handleActivityResult(resultCode, data)
            finish()
        } else {
            Timber.w("Unknown request code: $requestCode")
        }
    }

    companion object {
        private const val REQUEST_CODE_RESOLVE_HINTS = 64359
        private const val EXTRA_HINT_INTENT = "hidden_smart_lock_activity_hint_intent"

        fun newIntent(
            context: Context,
            hintIntent: PendingIntent
        ): Intent =
            Intent(context, HiddenSmartLockActivity::class.java).apply {
                putExtra(EXTRA_HINT_INTENT, hintIntent)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
    }
}
