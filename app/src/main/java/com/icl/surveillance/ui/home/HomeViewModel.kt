package com.icl.surveillance.ui.home

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.icl.surveillance.R


class HomeViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {

    fun getLayoutList(): List<Layout> {
        return Layout.values().toList()
    }

    fun getDiseasesList(): List<Diseases> {
        return Diseases.values().filter { it.isCurrent }.toList()
    }

    fun getMonthlyList(): List<Diseases> {
        return Diseases.values().filter { it.isMonthly }.toList()
    }

    enum class Diseases(
        @DrawableRes val iconId: Int,
        @StringRes val textId: Int,
        val count: Int,
        val isCurrent: Boolean,
        val isWeekly: Boolean,
        val isMonthly: Boolean
    ) {

        MEASLES(
            R.drawable.searching,
            R.string.measles, 0, true, false, false
        ),
        AFP(
            R.drawable.searching,
            R.string.afp, 1, true, false, false
        ),
        VL(
            R.drawable.searching,
            R.string.vl_forms, 1, false, false, true
        ),
    }

    enum class Layout(
        @DrawableRes val iconId: Int,
        @StringRes val textId: Int,
        val count: Int,
    ) {

        MOH502FORM(
            R.drawable.searching,
            R.string.moh502, 0
        ),
        CONTACTTRACINGFORM(
            R.drawable.searching,
            R.string.contact_tracing, 1
        ),
        MONTHLYFORM(R.drawable.searching, R.string.monthly, 5),
        MOH505FORM(R.drawable.searching, R.string.moh505, 2),
        SOCIALFORM(R.drawable.searching, R.string.social_form, 3),
        RUMORTOOL(R.drawable.searching, R.string.rumor_tracking, 4),
    }
}