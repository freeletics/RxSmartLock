package com.freeletics.rxsmartlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.common.api.GoogleApiClient
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

internal const val REQUEST_CODE_RESOLVE_REQUEST = 64357

internal const val REQUEST_CODE_RESOLVE_SAVE = 64358

internal const val REQUEST_CODE_RESOLVE_HINTS = 64359

object RxGoogleSmartLockManager : SmartLockManager {

    private var googleApiClientSubject = PublishSubject.create<GoogleApiClient>()
    private var googleApiClient: GoogleApiClient? = null

    internal val smartLockComponent = SmartLockComponent()

    override fun retrieveCredentials(context: Context): Single<Credential> {
        Timber.d("RetrieveCredentials started...")
        startHiddenActivity(context)
        return googleApiClientSubject
            .concatMapSingle {
                smartLockComponent.retrieveCredentialRequest(googleApiClient)
                    .doFinally { dispose() }
            }
            .singleOrError()
    }

    override fun storeCredentials(context: Context, credential: Credential): Completable {
        Timber.d("storeCredentials started...")
        startHiddenActivity(context)
        return googleApiClientSubject
            .concatMapCompletable {
                smartLockComponent.saveCredentialsRequest(googleApiClient, credential)
                    .doFinally { dispose() }
            }
    }

    override fun deleteStoredCredentials(context: Context, credential: Credential): Completable {
        Timber.d("deleteStoredCredential started...")
        startHiddenActivity(context)
        return googleApiClientSubject
            .concatMapCompletable {
                smartLockComponent.deleteCredentialsRequest(googleApiClient, credential)
                    .doFinally { dispose() }
            }
    }

    override fun retrieveSignInHints(context: Context): Single<Hint> {
        Timber.d("CredentialsClient retrieveSignInHints started...")
        startHiddenActivity(context)
        return googleApiClientSubject
            .concatMapSingle {
                smartLockComponent.retrieveSignInHintsRequest(googleApiClient)
                    .doFinally { dispose() }
            }
            .singleOrError()
    }

    override fun disableAutoSignIn(context: Context): Completable {
        Timber.d("disableAutoSignIn")
        startHiddenActivity(context)
        return googleApiClientSubject.concatMapCompletable {
            smartLockComponent.disableAutoSignInRequest(googleApiClient).doFinally { dispose() }
        }
    }

    private fun startHiddenActivity(context: Context) {
        context.startActivity(HiddenSmartLockActivity.newIntent(context))
    }

    private fun dispose() {
        val activity = googleApiClient?.context as? FragmentActivity
        if (activity != null) {
            googleApiClient?.stopAutoManage(activity)
            activity.finish()
        }
        googleApiClientSubject.onComplete()
        googleApiClientSubject = PublishSubject.create()
        googleApiClient = null
    }

    internal fun handleActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        smartLockComponent.deliverResult(requestCode, resultCode, resultData)
    }

    internal fun performAction(activity: FragmentActivity) {
        if (googleApiClient == null) {
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
                    googleApiClientSubject.onError(Exception(message))
                }
                .addApi(Auth.CREDENTIALS_API)
                .build()
        }
    }
}
