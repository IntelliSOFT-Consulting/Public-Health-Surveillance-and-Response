package com.icl.surveillance.models

import com.icl.surveillance.R

data class QuestionnaireAnswer(
    val linkId: String,
    val text: String,
    val answer: String
)


enum class UrlData(var message: Int) {
    BASE_URL(R.string.base_url),
}

data class DbSignIn(
    val idNumber: String,
    val password: String,
    val location: String,
)
data class DbResetPasswordData(val idNumber: String, val email: String)


data class DbSetPasswordReq(val resetCode: String, val idNumber: String, val password: String)
data class DbSignInResponse(
    val access_token: String,
    val expires_in: String,
    val refresh_expires_in: String,
    val refresh_token: String,
)

data class DbResetPassword(
    val status: String,
    val response: String,
)

data class DbResponseError(
    val status: String,
    val error: String,
)

data class DbUserInfoResponse(
    val user: DbUser?,
)

data class DbUser(
    val firstName: String,
    val lastName: String,
    val role: String,
    val id: String,
    val idNumber: String,
    val fullNames: String,
    val phone: String?, // nullable
    val email: String
)

data class CaseOption(
    val title: String,
    val showCount: Boolean = false,
    val count: Int = 0
)
