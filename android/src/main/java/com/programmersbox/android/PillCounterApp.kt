package com.programmersbox.android

import android.app.Application
import com.splendo.kaluga.base.ApplicationHolder

class PillCounterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ApplicationHolder.application = this
    }

}