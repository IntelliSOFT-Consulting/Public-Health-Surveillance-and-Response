package com.icl.surveillance.network

import android.content.Context

object Constants {
    const val BASE_URL = "https://dsrfhir.intellisoftkenya.com/hapi/fhir/"
    const val LOCATION_STARTER = "${BASE_URL}Location?_count=200&_offset=0"
    const val NOTIFICATION_CODE = 1001

    const val FIRST_LAUNCH_KEY = "is_first_launch"
    private const val PREF_NAME = "pagination_pref"
    private const val KEY_NEXT_URL = "next_url"

    fun saveNextUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NEXT_URL, url).apply()
    }

    fun getNextUrl(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NEXT_URL, null)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_NEXT_URL).apply()
    }
}