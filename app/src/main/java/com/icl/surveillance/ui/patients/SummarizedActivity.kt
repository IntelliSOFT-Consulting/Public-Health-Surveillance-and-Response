package com.icl.surveillance.ui.patients

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.icl.surveillance.R
import com.icl.surveillance.models.ChildItem
import com.icl.surveillance.models.OutputGroup
import com.icl.surveillance.models.OutputItem
import com.icl.surveillance.models.QuestionnaireItem
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class SummarizedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_summarized)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val resultJson = parseFromAssets(this) // 'this' is Context
        Log.d("ParsedJson", resultJson)
    }


    fun parseFromAssets(context: Context): String {
        // Read JSON file from assets
        val jsonContent = context.assets.open("afp-case.json")
            .bufferedReader()
            .use { it.readText() }

        val gson = Gson()
        val questionnaire = gson.fromJson(jsonContent, QuestionnaireItem::class.java)

        val outputGroups = questionnaire.item.map { group ->
            OutputGroup(
                linkId = group.linkId,
                text = group.text,
                type = group.type,
                items = group.item?.flatMap { flattenItems(it) } ?: emptyList()
            )
        }

        return gson.toJson(outputGroups)
    }


    fun flattenItems(item: ChildItem): List<OutputItem> {
        val current = OutputItem(
            linkId = item.linkId,
            text = item.text,
            type = item.type
        )
        val children = item.item?.flatMap { flattenItems(it) } ?: emptyList()
        return listOf(current) + children
    }
}