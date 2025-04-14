package com.icl.surveillance.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.icl.surveillance.R

class FormatterClass {

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
}
