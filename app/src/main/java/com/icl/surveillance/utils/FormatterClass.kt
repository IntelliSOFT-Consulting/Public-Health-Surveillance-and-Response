package com.icl.surveillance.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.icl.surveillance.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormatterClass {
  private val dateInverseFormatSeconds: SimpleDateFormat =
      SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

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

  fun formatCurrentDateTime(date: Date): String {
    return dateInverseFormatSeconds.format(date)
  }
}
