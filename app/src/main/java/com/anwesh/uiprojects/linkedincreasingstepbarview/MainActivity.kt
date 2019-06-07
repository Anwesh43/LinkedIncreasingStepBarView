package com.anwesh.uiprojects.linkedincreasingstepbarview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.increasingstepbarview.IncreasingStepBarView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IncreasingStepBarView.create(this)
    }
}
