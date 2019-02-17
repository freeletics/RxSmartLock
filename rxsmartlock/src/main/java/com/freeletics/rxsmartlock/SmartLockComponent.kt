package com.freeletics.rxsmartlock

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.auth.api.credentials.CredentialRequestResult
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Status
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

internal class SmartLockComponent {

    private val activityResultSubject = PublishSubject.create<ActivityResult>()

    private var isDialogShown = false

    fun retrieveCredentialRequest(googleApiClient: GoogleApiClient?): Single<Credential> {
        return Single.create { emitter ->
            Timber.d("In retrieveCredentialRequest...")

            val credentialRequest = CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .build()

            Auth.CredentialsApi.request(googleApiClient, credentialRequest)
                .setResultCallback { credentialRequestResult ->
                    processCredentialsRetrieveCallback(
                        googleApiClient,
                        credentialRequestResult,
                        emitter
                    )
                }
        }
    }

    private fun processCredentialsRetrieveCallback(
        googleApiClient: GoogleApiClient?,
        credentialRequestResult: CredentialRequestResult,
        emitter: SingleEmitter<Credential>
    ) {
        val status = credentialRequestResult.status
        if (status.isSuccess) {
            onSuccessCredentialsRequest(credentialRequestResult.credential, emitter)
        } else if (status.statusCode == CommonStatusCodes.SIGN_IN_REQUIRED) {
            val message = "Credentials retrieve, sign in required. No credentials saved."
            onErrorCredentialsRequest(message, emitter)
        } else {
            Timber.w(
                "Credentials retrieve status: %s, message: %s, success: %b",
                status.toString(), status.statusMessage, status.isSuccess
            )
            if (status.hasResolution() && !isDialogShown) {
                try {
                    val disposable = activityResultSubject
                        .filter { it.requestCode == REQUEST_CODE_RESOLVE_REQUEST }
                        .subscribe { handleFetchCredentialsResult(it, emitter) }
                    emitter.setDisposable(disposable)

                    val activity = googleApiClient?.context as Activity
                    status.startResolutionForResult(activity, REQUEST_CODE_RESOLVE_REQUEST)
                    isDialogShown = true
                } catch (e: IntentSender.SendIntentException) {
                    val message = "Retrieve credential, startResolutionForResult failed: $e"
                    onErrorCredentialsRequest(message, emitter)
                }
            } else {
                // Possible reasons for this:
                // * "Network error"
                // * "No eligible accounts can be found"
                // * "At least one account on the device is in bad state"
                val message =
                    "Retrieve credential, no resolution. Message: ${credentialRequestResult.status.statusMessage}"
                onErrorCredentialsRequest(message, emitter)
            }
        }
    }

    private fun onErrorCredentialsRequest(
        message: String,
        emitter: SingleEmitter<Credential>
    ) {
        Timber.e(message)
        emitter.onError(Exception(message))
    }

    private fun onSuccessCredentialsRequest(
        credential: Credential,
        emitter: SingleEmitter<Credential>
    ) {
        Timber.d("Credentials retrieved for %s", credential.id)
        emitter.onSuccess(credential)
    }

    private fun handleFetchCredentialsResult(
        activityResult: ActivityResult,
        emitter: SingleEmitter<Credential>
    ) {
        if (activityResult.resultCode == Activity.RESULT_OK && activityResult.resultData != null) {
            val selectedCredentials =
                activityResult.resultData.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
            onSuccessCredentialsRequest(selectedCredentials, emitter)
        } else {
            val message = "Fetching credentials failed: ${activityResult.requestCode}"
            onErrorCredentialsRequest(message, emitter)
        }
    }

    fun saveCredentialsRequest(
        googleApiClient: GoogleApiClient?,
        credentialsToSave: Credential
    ): Completable {
        return Completable.create { emitter ->
            Timber.d("In save...")
            val username = credentialsToSave.id
            Auth.CredentialsApi
                .save(googleApiClient, credentialsToSave)
                .setResultCallback { result ->
                    processSaveCredentialsCallback(googleApiClient, result, username, emitter)
                }
        }
    }

    private fun processSaveCredentialsCallback(
        googleApiClient: GoogleApiClient?,
        result: Status,
        username: String,
        emitter: CompletableEmitter
    ) {
        val status = result.status
        if (status.isSuccess) {
            onSuccessSaveCredentials(emitter)
        } else if (status.hasResolution() && !isDialogShown) {
            Timber.d("Credentials for $username has status: $status")
            try {
                val disposable = activityResultSubject
                    .filter { it.requestCode == REQUEST_CODE_RESOLVE_SAVE }
                    .subscribe {
                        handleSaveCredentialsResult(
                            it.resultCode,
                            emitter
                        )
                    }
                emitter.setDisposable(disposable)

                val activity = googleApiClient?.context as Activity
                status.startResolutionForResult(activity, REQUEST_CODE_RESOLVE_SAVE)
                isDialogShown = true
            } catch (e: IntentSender.SendIntentException) {
                val message = "Save credential startResolutionForResult failed: $e"
                onErrorSaveCredentials(message, emitter)
            }
        } else {
            val message = "Credentials cannot be saved for $username"
            onErrorSaveCredentials(message, emitter)
        }
    }

    private fun handleSaveCredentialsResult(resultCode: Int, emitter: CompletableEmitter) {
        if (resultCode == Activity.RESULT_OK) {
            onSuccessSaveCredentials(emitter)
        } else {
            val message = "Saving credentials failed: $resultCode"
            onErrorSaveCredentials(message, emitter)
        }
    }

    private fun onErrorSaveCredentials(message: String, emitter: CompletableEmitter) {
        Timber.e(message)
        emitter.onError(Exception(message))
    }

    private fun onSuccessSaveCredentials(emitter: CompletableEmitter) {
        Timber.d("Credentials saved successfully")
        emitter.onComplete()
    }

    fun retrieveSignInHintsRequest(googleApiClient: GoogleApiClient?): Single<Hint> {
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
            val intent = Auth.CredentialsApi.getHintPickerIntent(googleApiClient, hintRequest)
            try {
                if (!isDialogShown) {
                    val disposable = activityResultSubject
                        .filter { it.requestCode == REQUEST_CODE_RESOLVE_HINTS }
                        .subscribe {
                            handleFetchHintsResult(it, emitter)
                        }
                    emitter.setDisposable(disposable)

                    val activity = googleApiClient?.context as Activity
                    activity.startIntentSenderForResult(
                        intent.intentSender,
                        REQUEST_CODE_RESOLVE_HINTS, null, 0, 0, 0
                    )
                    isDialogShown = true
                }
            } catch (e: IntentSender.SendIntentException) {
                val message = "Could not start hint picker Intent"
                Timber.e(e, message)
                emitter.onError(Exception(message))
            }
        }
    }

    private fun handleFetchHintsResult(
        activityResult: ActivityResult,
        emitter: SingleEmitter<Hint>
    ) {
        if (activityResult.resultCode == Activity.RESULT_OK && activityResult.resultData != null) {
            val credential =
                activityResult.resultData.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
            Timber.d("Hints retrieved for %s", credential.id)
            emitter.onSuccess(Hint(credential))
        } else {
            val message = "Retrieving hints failed: ${activityResult.resultCode}"
            Timber.d(message)
            emitter.onError(Exception(message))
        }
    }

    fun deleteCredentialsRequest(
        googleApiClient: GoogleApiClient?,
        credentialToDelete: Credential
    ): Completable {
        return Completable.create { emitter ->
            Timber.d("In deleteCredentialsRequest...")
            Auth.CredentialsApi.delete(googleApiClient, credentialToDelete)
                .setResultCallback { status ->
                    // Credential may be deleted via another device/app. So there is a
                    // possibility that status is not success
                    if (status.isSuccess) {
                        Timber.d("Credential deleted successfully")
                        emitter.onComplete()
                    } else {
                        val message = "Deleting credentials failed: ${status.statusCode}"
                        Timber.d(message)
                        emitter.onError(Exception(message))
                    }
                }
        }
    }

    fun disableAutoSignInRequest(googleApiClient: GoogleApiClient?): Completable {
        return Completable.create { emitter ->
            Timber.d("In disableAutoSignInRequest")
            Auth.CredentialsApi.disableAutoSignIn(googleApiClient).setResultCallback { status ->
                if (status.isSuccess) {
                    Timber.d("Auto sign disabled successfully")
                    emitter.onComplete()
                } else {
                    val message = "Auto sign disable failed: ${status.statusCode}"
                    Timber.d(message)
                    emitter.onError(Exception(message))
                }
            }
        }
    }

    fun deliverResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        activityResultSubject.onNext(ActivityResult(requestCode, resultCode, resultData))
        isDialogShown = false
    }
}
