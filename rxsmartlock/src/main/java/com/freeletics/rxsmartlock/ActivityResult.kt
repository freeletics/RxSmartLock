package com.freeletics.rxsmartlock

import android.content.Intent

internal data class ActivityResult(val requestCode: Int, val resultCode: Int, val resultData: Intent?)
