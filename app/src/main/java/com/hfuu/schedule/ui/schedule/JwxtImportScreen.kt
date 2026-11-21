package com.hfuu.schedule.ui.schedule

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hfuu.schedule.data.model.HfuuDefaults
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.viewmodel.MainViewModel

/**
 * 合肥大学教务处 WebView 导入页（参考 ShiGuangSchedule 的 JS 适配脚本注入机制）
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JwxtImportScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf(HfuuDefaults.HFUU_JWC_URL) }
    var progress by remember { mutableIntStateOf(0) }
    var statusTip by remember {
        mutableStateOf("步骤：在下方登录合肥大学教务系统 → 打开「个人课表」页面 → 点击底部「一键导入课表」")
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val parserScript = remember {
        runCatching {
            context.assets.open("hfuu_jwxt_parser.js").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    val colors = AppTheme.colors
    val metrics = AppTheme.metrics

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = colors.textPrimary)
                    }
                },
                title = {
                    Text("合肥大学教务处导入", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                },
                actions = {
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新", tint = colors.textPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 地址栏（方便用户切换到校内教务直连地址或 WebVPN）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    singleLine = true,
                    placeholder = { Text("输入合肥大学教务系统网址") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val target = if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
                        webViewRef?.loadUrl(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = metrics.buttonShape
                ) {
                    Text("前往")
                }
            }

            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = colors.primary
                )
            }

            // 内置 WebView
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onScheduleParsed(json: String) {
                                    post {
                                        viewModel.importFromJwxtJson(json) { ok, msg ->
                                            statusTip = msg
                                            if (ok) onBack()
                                        }
                                    }
                                }
                            },
                            "HfuuBridge"
                        )

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (!url.isNullOrBlank()) {
                                    urlInput = url
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                        loadUrl(HfuuDefaults.HFUU_JWC_URL)
                        webViewRef = this
                    }
                }
            )

            // 底部导入操作条
            Surface(
                color = colors.surface,
                shadowElevation = if (metrics.isMasses) 0.dp else 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = statusTip,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 17.sp
                    )
                    Button(
                        onClick = {
                            if (parserScript.isBlank()) {
                                statusTip = "解析脚本加载失败"
                            } else {
                                statusTip = "正在识别合肥大学教务课表..."
                                webViewRef?.evaluateJavascript(parserScript, null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = metrics.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text(
                            text = "一键解析并导入当前页课表",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
