package com.freeletics.rxsmartlock

import android.content.Context
import com.google.android.gms.auth.api.credentials.Credential
import io.reactivex.Completable
import io.reactivex.Single

class EmptySmartLockManager : SmartLockManager {

    override fun storeCredentials(context: Context, credential: Credential): Completable = Completable.complete()

    override fun retrieveCredentials(context: Context): Single<Credential> = Single.never()

    override fun deleteStoredCredentials(context: Context, credential: Credential): Completable =
        Completable.complete()

    override fun retrieveSignInHints(context: Context): Single<Hint> = Single.never()

    override fun disableAutoSignIn(context: Context): Completable = Completable.complete()
}
