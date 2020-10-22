package com.freeletics.rxsmartlock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.auth.api.credentials.Credentials
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.android.gms.auth.api.credentials.HintRequest
import io.reactivex.SingleEmitter

object RxGoogleSmartLockManager : SmartLockManager {
    private val activityResultSubject = PublishSubject.create<ActivityResult>()

    private var isDialogShown = false

    private val Context.apiClient get() = Credentials.getClient(this, CredentialsOptions.DEFAULT)

    override fun retrieveCredentials(
        context: Context
    ): Single<Credential> {
        return Single.create<Credential> { emitter ->
            Timber.d("In retrieveCredentialRequest...")

            val credentialRequest = CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .build()

            context.apiClient.request(credentialRequest)
                .addOnCompleteListener {
                    if (it.isSuccessful && it.result != null) {
                        Timber.d("Credentials retrieved for %s", it.result?.credential?.id)
                        emitter.onSuccess(it.result?.credential!!)
                    } else {
                        val error = SmartLockManager.SmartLockException(
                            reason = SmartLockManager.SmartLockException.FETCHING_CREDENTIALS_FAILED,
                            message = "Retrieve credential, no resolution.",
                            throwable = it.exception
                        )
                        emitter.tryOnError(error)
                    }
                }
        }.doOnError {
            Timber.e(it)
        }
    }

    override fun storeCredentials(
        context: Context,
        credential: Credential
    ): Completable {
        return Completable.create { emitter ->
            Timber.d("In save...")
            val username = credential.id
            context.apiClient
                .save(credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Timber.d("Credentials saved successfully")
                        emitter.onComplete()
                    } else {
                        val error = SmartLockManager.SmartLockException(
                            reason = SmartLockManager.SmartLockException.FAILED_TO_SAVE,
                            message = "Credentials cannot be saved for $username",
                            throwable = it.exception
                        )
                        emitter.tryOnError(error)
                    }
                }
        }.doOnError {
            Timber.e(it)
        }
    }

    override fun deleteStoredCredentials(
        context: Context,
        credential: Credential
    ): Completable {
        return Completable.create { emitter ->
            Timber.d("In deleteCredentialsRequest...")
            context.apiClient.delete(credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Timber.d("Credential deleted successfully")
                        emitter.onComplete()
                    } else {
                        emitter.tryOnError(
                            SmartLockManager.SmartLockException(
                                reason = SmartLockManager.SmartLockException.DELETING_CREDENTIALS_FAILED,
                                message = "Deleting credentials failed.",
                                throwable = it.exception
                            )
                        )
                    }
                }
        }
    }

    override fun retrieveSignInHints(
        context: Context
    ): Single<Hint> {
        return Single.create { emitter ->
            Timber.d("In retrieveSignInHintsRequest...")
            val hintRequest = HintRequest.Builder()
                .setHintPickerConfig(
                    CredentialPickerConfig.Builder()
                        .setShowCancelButton(true)
                        .build()
                )
                .setEmailAddressIdentifierSupported(true)
                .build()
            val intent = context.apiClient.getHintPickerIntent(hintRequest)
            try {
                if (!isDialogShown) {
                    val disposable = activityResultSubject
                        .subscribe {
                            handleFetchHintsResult(it, emitter)
                        }
                    emitter.setDisposable(disposable)

                    context.startActivity(
                        HiddenSmartLockActivity.newIntent(
                            context,
                            intent
                        )
                    )

                    isDialogShown = true
                }
            } catch (e: IntentSender.SendIntentException) {
                val error = SmartLockManager.SmartLockException(
                    reason = SmartLockManager.SmartLockException.FAILED_TO_SHOW_HINT_PICKER,
                    message = "Could not start hint picker Intent",
                    throwable = e
                )
                emitter.tryOnError(error)
            }
        }
    }

    override fun disableAutoSignIn(
        context: Context
    ): Completable {
        return Completable.create { emitter ->
            Timber.d("In disableAutoSignInRequest")
            context.apiClient
                .disableAutoSignIn()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Timber.d("Auto sign disabled successfully")
                        emitter.onComplete()
                    } else {
                        emitter.tryOnError(
                            SmartLockManager.SmartLockException(
                                reason = SmartLockManager.SmartLockException.FAILED_DISABLE_AUTO_SIGN_IN,
                                message = "Auto sign disable failed.",
                                throwable = it.exception
                            )
                        )
                    }
                }
        }
    }

    private fun handleFetchHintsResult(
        activityResult: ActivityResult,
        emitter: SingleEmitter<Hint>
    ) {
        if (activityResult.resultCode == Activity.RESULT_OK && activityResult.resultData != null) {
            val credential =
                activityResult.resultData.getParcelableExtra<Credential>(Credential.EXTRA_KEY)!!
            Timber.d("Hints retrieved for %s", credential.id)
            emitter.onSuccess(Hint(credential))
        } else {
            emitter.tryOnError(
                SmartLockManager.SmartLockException(
                    reason = SmartLockManager.SmartLockException.RETRIEVING_HINTS_FAILED,
                    message = "Retrieving hints failed: ${activityResult.resultCode}"
                )
            )
        }
    }

    internal fun handleActivityResult(
        resultCode: Int,
        resultData: Intent?
    ) {
        activityResultSubject.onNext(ActivityResult(resultCode, resultData))
        isDialogShown = false
    }

    private data class ActivityResult(val resultCode: Int, val resultData: Intent?)
}
