package com.billage.app.ui.contract.detail

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.billage.app.BuildConfig
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDocumentScreen(
    viewModel: ContractDetailViewModel,
    onBackClick: () -> Unit
) {
    val contractHtml by viewModel.contractHtml.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val detail by viewModel.contractDetail.collectAsState()
    val pdfDownloadState by viewModel.pdfDownloadState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pdfDownloadState) {
        when (pdfDownloadState) {
            is PdfDownloadState.Success -> {
                snackbarHostState.showSnackbar("Downloads 폴더에 저장되었어요")
                viewModel.clearPdfDownloadState()
            }
            is PdfDownloadState.Error -> {
                snackbarHostState.showSnackbar("다운로드에 실패했어요. 다시 시도해주세요")
                viewModel.clearPdfDownloadState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val isError = data.visuals.message.contains("실패")
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isError) BillageDanger else BillagePrimary,
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "차용증 전문",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = BillageTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BillageSurface)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        detail?.contractSeq?.let { viewModel.downloadPdf(it) }
                    },
                    enabled = pdfDownloadState !is PdfDownloadState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
                ) {
                    if (pdfDownloadState is PdfDownloadState.Loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "차용증을 다운로드하고 있어요...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "PDF로 저장",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            errorMessage != null && contractHtml == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = errorMessage!!, color = BillageDanger, fontSize = 14.sp)
                }
            }

            isLoading || contractHtml == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BillagePrimary)
                }
            }

            contractHtml != null -> {
                ContractHtmlWebView(
                    htmlContent = contractHtml!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ContractHtmlWebView(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                BuildConfig.BACKEND_BASE_URL,
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = modifier
    )
}
