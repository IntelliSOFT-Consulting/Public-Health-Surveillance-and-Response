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

    fun getDiseasesList(int: Int): List<Diseases> {
        return Diseases.values().filter { it.count == int }.toList()
    }


    fun getNotifiableList(): List<Diseases> {
        return Diseases.values().filter { it.count == 0 }.toList()
    }

    fun getMassList(): List<Diseases> {
        return Diseases.values().filter { it.count == 1 }.toList()
    }

    fun getCaseList(): List<Diseases> {
        return Diseases.values().filter { it.count == 2 }.toList()
    }

    fun getSocialList(): List<Diseases> {
        return Diseases.values().filter { it.count == 3 }.toList()
    }

    fun getAssessmentList(): List<Diseases> {
        return Diseases.values().filter { it.count == 4 }.toList()
    }


    enum class Diseases(
        @DrawableRes val iconId: Int,
        @StringRes val textId: Int,
        val count: Int,
        val level: Int
    ) {

        RUMOR_TOOL(
            R.drawable.searching,
            R.string.rumor_tool, 3, 0
        ),
        SOCIAL_INV(
            R.drawable.searching,
            R.string.social_inv, 3, 1
        ),
        VL_FORM(
            R.drawable.searching,
            R.string.vl_form, 2, 0
        ),

        // Mass
        POLIO(
            R.drawable.searching,
            R.string.polio, 1, 0
        ),
        MEASLES_IMM(
            R.drawable.searching,
            R.string.measles_imm, 1, 1
        ),
        CHOLERA(
            R.drawable.searching,
            R.string.cholera, 1, 2
        ),
        // Top Layer

        IMMEDIATE(
            R.drawable.searching,
            R.string.immediate_reportable, 0, 0
        ),
        WEEKLY(
            R.drawable.searching,
            R.string.weekly_reported, 0, 1
        ),
        MONTHLY(
            R.drawable.searching,
            R.string.monthly_reported, 0, 2
        ),


        MEASLES(
            R.drawable.searching,
            R.string.measles, 6, 0
        ),
        AFP(
            R.drawable.searching,
            R.string.afp, 6, 0
        ),

        RUMOR(
            R.drawable.searching,
            R.string.rumor_tracking, 7, 0
        ),
    }

    enum class Layout(
        @DrawableRes val iconId: Int,
        @StringRes val textId: Int,
        val count: Int,
    ) {

        //        MOH502FORM(
//            R.drawable.searching,
//            R.string.moh502, 0
//        ),
//        CONTACTTRACINGFORM(
//            R.drawable.searching,
//            R.string.contact_tracing, 1
//        ),
//        MONTHLYFORM(R.drawable.searching, R.string.monthly, 5),
//        MOH505FORM(R.drawable.searching, R.string.moh505, 2),
//        SOCIALFORM(R.drawable.searching, R.string.social_form, 3),
//        RUMORTOOL(R.drawable.searching, R.string.rumor_tracking, 4),
        NOTIFIABLE(
            R.drawable.searching,
            R.string.notifiable,
            0
        ),
        MASS(
            R.drawable.searching,
            R.string.mass, 1
        ),
        CASE(
            R.drawable.searching,
            R.string.case_management, 2
        ),
        SOCIAL(
            R.drawable.searching,
            R.string.social, 3
        ),
        SURVEY(
            R.drawable.searching,
            R.string.surveys, 4
        )
    }
}