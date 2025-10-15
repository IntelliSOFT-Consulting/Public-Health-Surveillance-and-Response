package com.icl.nphi.network

import android.content.Context

object Constants {
    //        const val BASE_URL = "https://dsrfhir.intellisoftkenya.com/hapi/fhir/"
    const val BASE_URL = "http://45.79.161.190:8085/fhir/"

    //    const val BASE_URL ="https://auth.nphiis.nphl.go.ke/fhir/"
    const val LOCATION_STARTER = "${BASE_URL}Location?_count=200&_offset=0"
    const val NOTIFICATION_CODE = 1001
    const val FIRST_LAUNCH_KEY = "is_first_launch"
    private const val PREF_NAME = "pagination_pref"
    private const val KEY_NEXT_URL = "next_url"

    const val TEST_TOKEN =
        "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJxbTVfeFpQWFVHV1I0YTdwbkpLZ1VORVRNbERMMlpHeXhUSndNSEx5UkhjIn0.eyJleHAiOjE3NzYwNTkxMTMsImlhdCI6MTc2MDUwNzExMywianRpIjoiYmQ5YTdiOGItOWFlNS00ZmJkLWFiY2YtODY4MTFmNzhlYjJhIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL21hc3RlciIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiIwZGMzM2ExYi1iNmJjLTRjZTgtYjU5MS1iYTU4ZDZmYTFhZTYiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJjaGFuam8tY2xpZW50LWFwaXMiLCJzaWQiOiIxZmNlOTRlNi1lYjRjLTRkMjItYThmNS00ZjhmZTIxYTIxMDMiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgb3JnYW5pemF0aW9uIHByb2ZpbGUgb3BlbmlkIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiS2lwcm90aWNoIEphcGhldGgiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiI0MTQxNDEiLCJnaXZlbl9uYW1lIjoiS2lwcm90aWNoIiwiZmFtaWx5X25hbWUiOiJKYXBoZXRoIiwiZW1haWwiOiJqa2lwcm90aWMuaEBpbnRlbGxpc29mdGtlbnlhLmNvbSJ9.ZrQm4Pzd-AtjbbMkWj6DR_cK2CZTtQ2f1mE58AavL9FoReCAUKX2ze1TsqRe93NbtWfOUSwUee7ks-SlD9JaxmsEuVcY6YI5uA4rFw_CJ4zu9NB6oCCOzf1ZEQRkgeqHFKw1KF2ciDvnJaQwsDJjW8Hd6MAHzX8GI4iFBp7h_LT1zL_oCb6jMzPOfatOi5RAGisdy3bUFHzRL5IUZlOwqjMA4QxGHeSeAV-Hbegrjtfx-ufUdiITLVU4DYXYwReKth_D9iitFh8ul5kx8CZH8sUPbD5d2iqbw6HMH6_-Rb_sjj3mnJTjrct98UCOHIUrAmM4UOmNxgD9xYc0ZA4puw"

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