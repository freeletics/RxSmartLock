package com.freeletics.rxsmartlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.common.api.GoogleApiClient
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import io.reactivex.Observable

internal const val REQUEST_CODE_RESOLVE_REQUEST = 64357

internal const val REQUEST_CODE_RESOLVE_SAVE = 64358

internal const val REQUEST_CODE_RESOLVE_HINTS = 64359

/**
 * Exception indicating that there was a failure while connecting to the GoogleApiClient.
 */
class GoogleApiConnectionError(message: String) : Exception(message)

object RxGoogleSmartLockManager : SmartLockManager {

    private var googleApiClientSubject = PublishSubject.create<GoogleApiClient>()
    private var googleApiClient: GoogleApiClient? = null

    private val smartLockComponent = SmartLockComponent()

    override fun retrieveCredentials(
        context: Context
    ): Single<Credential> = apiClient(context)
        .doOnSubscribe {
            Timber.d("RetrieveCredentials started...")
        }
        .flatMapSingle {
            smartLockComponent.retrieveCredentialRequest(googleApiClient)
        }
        .firstOrError()

    override fun storeCredentials(
        context: Context,
        credential: Credential
    ): Completable = apiClient(context)
        .doOnSubscribe {
            Timber.d("storeCredentials started...")
        }
        .flatMap {
            smartLockComponent
                .saveCredentialsRequest(googleApiClient, credential)
                .andThen(Observable.just(Unit))
        }
        .take(1)
        .ignoreElements()

    override fun deleteStoredCredentials(
        context: Context,
        credential: Credential
    ): Completable = apiClient(context)
        .doOnSubscribe {
            Timber.d("deleteStoredCredential started...")
        }
        .flatMap {
            smartLockComponent
                .deleteCredentialsRequest(googleApiClient, credential)
                .andThen(Observable.just(Unit))
        }
        .take(1)
        .ignoreElements()


    override fun retrieveSignInHints(
        context: Context
    ): Single<Hint> = apiClient(context)
        .doOnSubscribe {
            Timber.d("CredentialsClient retrieveSignInHints started...")
        }
        .flatMapSingle {
            smartLockComponent.retrieveSignInHintsRequest(googleApiClient)
        }
        .firstOrError()

    override fun disableAutoSignIn(
        context: Context
    ): Completable = apiClient(context)
        .doOnSubscribe {
            Timber.d("disableAutoSignIn started...")
        }
        .flatMap {
            smartLockComponent
                .disableAutoSignInRequest(googleApiClient)
                .andThen(Observable.just(Unit))
        }
        .take(1)
        .ignoreElements()

    private fun apiClient(
        context: Context
    ): Observable<GoogleApiClient> = Completable
        .fromAction {
            context.startActivity(HiddenSmartLockActivity.newIntent(context))
        }
        .andThen(googleApiClientSubject)
        .doFinally { dispose() }
        .publish()
        .refCount()

    private fun dispose() {
        Timber.d("Disposing google api client")
        val activity = googleApiClient?.context as? FragmentActivity
        if (activity != null) {
            googleApiClient?.stopAutoManage(activity)
            activity.finish()
        }
        googleApiClient = null
    }

    internal fun handleActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        smartLockComponent.deliverResult(requestCode, resultCode, resultData)
    }

    internal fun performAction(activity: FragmentActivity) {
        check(googleApiClient == null) { "Google api client is still connected!" }

        val options = CredentialsOptions.Builder().forceEnableSaveDialog().build()

        googleApiClient = GoogleApiClient.Builder(activity)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    Timber.d("CredentialsApiClient connected")
                    googleApiClientSubject.onNext(googleApiClient!!)
                }

                override fun onConnectionSuspended(i: Int) {
                    val message = "CredentialsApiClient connection suspended, i: $i"
                    Timber.d(message)
                    googleApiClientSubject.onError(Exception(message))
                }
            })
            .enableAutoManage(activity) {
                val message =
                    "CredentialsApiClient connection failed, result: ${it.errorMessage}"
                Timber.d(message)
                googleApiClientSubject.onError(GoogleApiConnectionError(message))
            }
            .addApi(Auth.CREDENTIALS_API, options)
            .build()
    }
}
