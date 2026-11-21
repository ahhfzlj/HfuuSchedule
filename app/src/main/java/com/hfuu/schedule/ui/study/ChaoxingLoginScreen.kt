package com.hfuu.schedule.ui.study

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hfuu.schedule.data.model.HfuuDefaults
import com.hfuu.schedule.ui.theme.AppTheme
import com.hfuu.schedule.ui.viewmodel.MainViewModel

/**
 * 超星学习通 WebView 登录接入页：
 * 支持手机号/密码/学校机构账号登录，登录成功后自动提取 Cookie 加密保存在本地。
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChaoxingLoginScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var progress by remember { mutableIntStateOf(0) }
    var tipText by remember { mutableStateOf("请登录超星学习通（支持手机号或合肥大学机构账号），登录进入首页后点击下方「完成接入」") }
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
                    Text("接入超星学习通", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = colors.primary
                )
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val cookie = collectChaoxingCookies()
                                if (cookie.contains("UID=") && cookie.contains("vc3=")) {
                                    tipText = "已检测到学习通登录凭证，可点击下方「完成接入并同步待办」"
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                        loadUrl(HfuuDefaults.CHAOXING_LOGIN_URL)
                    }
                }
            )

            Surface(
                color = colors.surface,
                shadowElevation = if (metrics.isMasses) 0.dp else 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tipText, fontSize = 12.sp, color = colors.textSecondary, lineHeight = 17.sp)
                    Button(
                        onClick = {
                            val cookie = collectChaoxingCookies()
                            if (cookie.contains("UID=") || cookie.contains("_uid=")) {
                                viewModel.saveChaoxingSession("学习通账号", cookie)
                                onBack()
                            } else {
                                tipText = "尚未检测到登录状态，请先完成学习通登录再点击接入"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = metrics.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("完成接入并同步待办", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun collectChaoxingCookies(): String {
    val cm = CookieManager.getInstance()
    val domains = listOf(
        "https://i.chaoxing.com",
        "https://mooc1-api.chaoxing.com",
        "https://mobilelearn.chaoxing.com",
        "https://passport2.chaoxing.com"
    )
    val merged = linkedMapOf<String, String>()
    domains.forEach { d ->
        cm.getCookie(d)?.split(";")?.forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val k = pair.substring(0, idx).trim()
                val v = pair.substring(idx + 1).trim()
                if (k.isNotEmpty()) merged[k] = v
            }
        }
    }
    return merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
}
