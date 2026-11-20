package com.sintesistudio.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sintesistudio.data.AppContainer
import com.sintesistudio.ui.screens.DocumentScreen
import com.sintesistudio.ui.screens.HomeScreen
import com.sintesistudio.ui.screens.SettingsScreen
import com.sintesistudio.ui.viewmodel.DocumentViewModel
import com.sintesistudio.ui.viewmodel.DocumentViewModelFactory
import com.sintesistudio.ui.viewmodel.HomeViewModel
import com.sintesistudio.ui.viewmodel.HomeViewModelFactory
import com.sintesistudio.ui.viewmodel.SettingsViewModel
import com.sintesistudio.ui.viewmodel.SettingsViewModelFactory

@Composable
fun AppNav(container: AppContainer, initialImportUri: Uri? = null) {
    val nav = rememberNavController()
    val homeVm: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(container.documentRepository)
    )

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            homeVm.importDocument(it) { id -> nav.navigate("doc/$id") }
        }
    }

    LaunchedEffect(initialImportUri) {
        initialImportUri?.let { uri ->
            homeVm.importDocument(uri) { id -> nav.navigate("doc/$id") }
        }
    }

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = homeVm,
                onOpenDocument = { nav.navigate("doc/$it") },
                onSettings = { nav.navigate("settings") },
                onImport = {
                    picker.launch(
                        arrayOf(
                            "application/pdf",
                            "application/epub+zip",
                            "text/plain",
                            "*/*"
                        )
                    )
                }
            )
        }
        composable("settings") {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(container.settingsStore)
            )
            SettingsScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
        composable(
            route = "doc/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val vm: DocumentViewModel = viewModel(
                factory = DocumentViewModelFactory(container.documentRepository, id)
            )
            DocumentScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
    }
}
