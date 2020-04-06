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
        return Single.create<Credential> { emitter ->
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
        }.doOnError {
            Timber.e(it)
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
            val error = SmartLockException(
                reason = SmartLockException.SIGN_IN_REQUIRED,
                message = "Credentials retrieve, sign in required. No credentials saved."
            )
            emitter.onError(error)
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
                    val error = SmartLockException(
                        reason = SmartLockException.START_RESOLUTION_FOR_RESULT_FAILED,
                        message = "Retrieve credential, startResolutionForResult failed",
                        throwable = e
                    )
                    emitter.onError(error)
                }
            } else {
                // Possible reasons for this:
                // * "Network error"
                // * "No eligible accounts can be found"
                // * "At least one account on the device is in bad state"
                val error = SmartLockException(
                    reason = SmartLockException.NO_RESOLUTION,
                    message = "Retrieve credential, no resolution. " +
                        "Message: ${credentialRequestResult.status.statusMessage}"
                )
                emitter.onError(error)
            }
        }
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
            val error = SmartLockException(
                reason = SmartLockException.FETCHING_CREDENTIALS_FAILED,
                message = "Fetching credentials failed: ${activityResult.requestCode}"
            )
            emitter.onError(error)
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
        }.doOnError {
            Timber.e(it)
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
                val error = SmartLockException(
                    reason = SmartLockException.START_RESOLUTION_FOR_RESULT_FAILED,
                    message = "Save credential startResolutionForResult failed",
                    throwable = e
                )
                emitter.onError(error)
            }
        } else {
            val error = SmartLockException(
                reason = SmartLockException.FAILED_TO_SAVE,
                message = "Credentials cannot be saved for $username"
            )
            emitter.onError(error)
        }
    }

    private fun handleSaveCredentialsResult(resultCode: Int, emitter: CompletableEmitter) {
        if (resultCode == Activity.RESULT_OK) {
            onSuccessSaveCredentials(emitter)
        } else {
            val error = SmartLockException(
                reason = SmartLockException.FAILED_TO_SAVE,
                message = "Saving credentials failed: $resultCode"
            )
            emitter.onError(error)
        }
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
                val error = SmartLockException(
                    reason = SmartLockException.FAILED_TO_SHOW_HINT_PICKER,
                    message = "Could not start hint picker Intent",
                    throwable = e
                )
                emitter.onError(error)
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
            emitter.onError(
                SmartLockException(
                    reason = SmartLockException.RETRIEVING_HINTS_FAILED,
                    message = "Retrieving hints failed: ${activityResult.resultCode}"
                )
            )
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
                        emitter.onError(
                            SmartLockException(
                                reason = SmartLockException.DELETING_CREDENTIALS_FAILED,
                                message = "Deleting credentials failed: ${status.statusCode}"
                            )
                        )
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
                    emitter.onError(
                        SmartLockException(
                            reason = SmartLockException.FAILED_DISABLE_AUTO_SIGN_IN,
                            message = "Auto sign disable failed: ${status.statusCode}"
                        )
                    )
                }
            }
        }
    }

    fun deliverResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        activityResultSubject.onNext(ActivityResult(requestCode, resultCode, resultData))
        isDialogShown = false
    }

    /**
     * Smart lock operation error.
     */
    class SmartLockException(
        val reason: Int,
        message: String,
        throwable: Throwable? = null
    ) : Exception(message, throwable) {
        companion object {
            const val SIGN_IN_REQUIRED = 0
            const val START_RESOLUTION_FOR_RESULT_FAILED = 1
            const val NO_RESOLUTION = 2
            const val FETCHING_CREDENTIALS_FAILED = 3
            const val FAILED_TO_SAVE = 4
            const val FAILED_TO_SHOW_HINT_PICKER = 5
            const val RETRIEVING_HINTS_FAILED = 6
            const val DELETING_CREDENTIALS_FAILED = 7
            const val FAILED_DISABLE_AUTO_SIGN_IN = 8
        }
    }
}
