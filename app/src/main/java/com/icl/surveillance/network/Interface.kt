package com.icl.surveillance.network

import com.icl.surveillance.models.DbResetPassword
import com.icl.surveillance.models.DbSetPasswordReq
import com.icl.surveillance.models.DbSignIn
import com.icl.surveillance.models.DbSignInResponse
import com.icl.surveillance.models.DbUserInfoResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface Interface {

    @POST("provider/login")
    suspend fun signInUser(@Body dbSignIn: DbSignIn): Response<DbSignInResponse>

    @PUT("Patient/{id}")
    @Headers("Content-Type: application/json")
    suspend fun sendPatientToServer(
        @Path("id") id: String,
        @Body payload: RequestBody
    ): Response<Any>

    @POST("https://hapi.fhir.org/baseR4/")
    @Headers("Content-Type: application/json")
    suspend fun sendBundleToServer(@Body payload: RequestBody): Response<Any>

    @GET("provider/me")
    suspend fun getUserInfo(
        @Header("Authorization") token: String,
    ): Response<DbUserInfoResponse>

    @GET("provider/reset-password")
    suspend fun resetPassword(
        @Query("idNumber") idNumber: String,
        @Query("email", encoded = true) email: String,
    ): Response<DbResetPassword>

    @POST("provider/reset-password")
    suspend fun setNewPassword(@Body dbSetPasswordReq: DbSetPasswordReq): Response<Any>
}