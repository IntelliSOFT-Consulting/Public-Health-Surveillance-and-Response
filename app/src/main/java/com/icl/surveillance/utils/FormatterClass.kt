package com.icl.surveillance.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.icl.surveillance.R
import com.icl.surveillance.network.Constants.FIRST_LAUNCH_KEY
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import kotlin.contracts.contract


class FormatterClass {
    private val dateInverseFormatSeconds: SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    fun String.toSlug(): String {
        return this
            .trim() // remove leading/trailing spaces
            .lowercase() // make all lowercase
            .replace("[^a-z0-9\\s-]".toRegex(), "") // remove special characters
            .replace("\\s+".toRegex(), "-") // replace spaces with hyphens
            .replace("-+".toRegex(), "-") // collapse multiple hyphens
    }

    fun saveSharedPref(key: String, value: String, context: Context) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getSharedPref(key: String, context: Context): String? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        return sharedPreferences.getString(key, null)
    }

    fun deleteSharedPref(key: String, context: Context) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(key)
        editor.apply()
    }

    fun isFirstLaunch(context: Context): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        return sharedPreferences.getBoolean(FIRST_LAUNCH_KEY, true)
    }

    fun setFirstLaunchCompleted(context: Context) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(FIRST_LAUNCH_KEY, false)
        editor.apply()
    }

    /**
     * Formats the provided [date] into a string using the pattern
     * `yyyy-MM-dd HH:mm:ss` and the English locale.
     */
    fun formatDateTime(date: Date): String {
        return dateInverseFormatSeconds.format(date)
    }

    fun getTimeOfDay(): String {

        val currentTime = LocalTime.now()
        return when (currentTime.hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }
}
