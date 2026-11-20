package com.sintesistudio

import android.app.Application
import com.sintesistudio.data.AppContainer

class SintesiStudioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
