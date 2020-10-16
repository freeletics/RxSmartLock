package com.freeletics.rxsmartlock

import android.content.Context
import com.google.android.gms.auth.api.credentials.Credential
import io.reactivex.Completable
import io.reactivex.Single

interface SmartLockManager {

    /**
     * Retrieves stored credentials from Smartlock
     *
     * @return Observable that emits stored credentials if there are any
     */
    fun retrieveCredentials(
        context: Context
    ): Single<Credential>

    /**
     * Retrieves sign in hints data
     *
     * @return Observable that emits sign in data
     */
    fun retrieveSignInHints(
        context: Context
    ): Single<Hint>

    /**
     * Stores credentials to the SmartLock
     *
     * @param credential Credentials to store
     * @return Completable
     */
    fun storeCredentials(
        context: Context,
        credential: Credential
    ): Completable

    /**
     * Deletes credentials from the SmartLock
     *
     * @param credential Credentials to delete
     * @return Completable
     */
    fun deleteStoredCredentials(
        context: Context,
        credential: Credential
    ): Completable

    /**
     * Disables auto sign in feature
     *
     * @return Completable
     */
    fun disableAutoSignIn(
        context: Context
    ): Completable

    /**
     * Smart lock operation error.
     */
    class SmartLockException(
        val reason: Int,
        message: String,
        throwable: Throwable? = null
    ) : Exception(message, throwable) {
        companion object {
            const val FETCHING_CREDENTIALS_FAILED = 3
            const val FAILED_TO_SAVE = 4
            const val FAILED_TO_SHOW_HINT_PICKER = 5
            const val RETRIEVING_HINTS_FAILED = 6
            const val DELETING_CREDENTIALS_FAILED = 7
            const val FAILED_DISABLE_AUTO_SIGN_IN = 8
        }
    }
}
