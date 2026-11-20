package com.sintesistudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sintesistudio.ui.navigation.AppNav
import com.sintesistudio.ui.theme.SintesiStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SintesiStudioApp
        val deepLink = intent?.data

        setContent {
            SintesiStudioTheme {
                AppNav(
                    container = app.container,
                    initialImportUri = deepLink
                )
            }
        }
    }
}
