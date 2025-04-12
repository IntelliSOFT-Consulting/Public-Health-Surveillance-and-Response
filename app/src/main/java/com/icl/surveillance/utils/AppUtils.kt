package com.icl.surveillance.utils

import android.content.Context

fun Context.readFileFromAssets(fileName: String): String =
    assets.open(fileName).bufferedReader().use { it.readText() }
