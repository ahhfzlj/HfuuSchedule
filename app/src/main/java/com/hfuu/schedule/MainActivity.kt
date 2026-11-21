package com.hfuu.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hfuu.schedule.ui.navigation.AppNavGraph
import com.hfuu.schedule.ui.theme.HfuuScheduleTheme
import com.hfuu.schedule.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val uiStyle by mainViewModel.uiStyle.collectAsState()
            HfuuScheduleTheme(uiStyle = uiStyle) {
                AppNavGraph(viewModel = mainViewModel)
            }
        }
    }
}
