package com.interactimuse.spatial.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.interactimuse.spatial.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
