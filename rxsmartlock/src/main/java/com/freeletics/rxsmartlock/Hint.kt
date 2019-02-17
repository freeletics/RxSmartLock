package com.freeletics.rxsmartlock

import com.google.android.gms.auth.api.credentials.Credential

private const val BLANK_SPACE = " "

class Hint(credential: Credential) {

    val firstName: String

    val lastName: String

    val email: String = credential.id

    init {
        val namePair = splitName(credential.name)
        firstName = namePair.first
        lastName = namePair.second
    }

    private fun splitName(name: String?): Pair<String, String> {
        var names = Pair("", "")
        name?.let {
            names = fetchUserNames(name)
        }
        return names
    }

    private fun fetchUserNames(name: String): Pair<String, String> {
        var firstName = ""
        var lastName = ""
        val firstBlankSpaceIndex = name.indexOf(BLANK_SPACE)
        val lastBlankSpaceIndex = name.lastIndexOf(BLANK_SPACE)
        if (firstBlankSpaceIndex > -1 && firstBlankSpaceIndex == lastBlankSpaceIndex) {
            firstName = name.substring(0, firstBlankSpaceIndex)
            if (firstBlankSpaceIndex < name.length) {
                lastName = name.substring(firstBlankSpaceIndex + 1)
            }
        }
        return Pair(firstName, lastName)
    }
}
