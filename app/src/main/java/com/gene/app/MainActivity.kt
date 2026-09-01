package com.gene.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialPersonId = savedInstanceState?.getLong("person_id", intent?.getLongExtra("person_id", -1L) ?: -1L)
            ?: intent?.getLongExtra("person_id", -1L)
            ?: -1L
        setContent {
            GeneApp(initialPersonId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}
