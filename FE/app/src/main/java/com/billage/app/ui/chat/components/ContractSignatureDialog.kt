package com.billage.app.ui.chat.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.ByteArrayOutputStream
import kotlin.math.max

@Composable
fun ContractSignatureDialog(
    isVisible: Boolean,
    isSubmitting: Boolean,
    documentHash: String?,
    onDismissRequest: () -> Unit,
    onSaveSignature: (String) -> Unit
) {
    if (!isVisible) return

    val strokes = remember { mutableStateListOf<SnapshotStateList<Offset>>() }
    var currentStroke by remember { mutableStateOf<SnapshotStateList<Offset>?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "전자서명",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "지문 인증이 완료되었습니다. 아래 영역에 서명한 뒤 저장해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "이 문서에 서명합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .onSizeChanged { canvasSize = it }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .pointerInput(isSubmitting) {
                                if (isSubmitting) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { start ->
                                        val stroke = mutableStateListOf(start)
                                        strokes.add(stroke)
                                        currentStroke = stroke
                                    },
                                    onDragEnd = { currentStroke = null },
                                    onDragCancel = { currentStroke = null },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentStroke?.add(change.position)
                                    }
                                )
                            }
                    ) {
                        strokes.forEach { stroke ->
                            if (stroke.size == 1) {
                                drawCircle(
                                    color = Color.Black,
                                    radius = 3f,
                                    center = stroke.first()
                                )
                            } else {
                                stroke.windowed(2).forEach { (start, end) ->
                                    drawLine(
                                        color = Color.Black,
                                        start = start,
                                        end = end,
                                        strokeWidth = 6f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (strokes.isEmpty()) {
                        "서명을 입력해 주세요."
                    } else {
                        "서명이 준비되었습니다."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { strokes.clear() },
                        enabled = strokes.isNotEmpty() && !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("지우기")
                    }
                    OutlinedButton(
                        onClick = onDismissRequest,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }
                    Button(
                        onClick = {
                            val dataUrl = encodeSignatureAsDataUrl(
                                strokes = strokes.map { it.toList() },
                                sourceSize = canvasSize
                            )
                            onSaveSignature(dataUrl)
                        },
                        enabled = strokes.isNotEmpty() && !isSubmitting && canvasSize != IntSize.Zero,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("저장")
                        }
                    }
                }
            }
        }
    }
}

private fun encodeSignatureAsDataUrl(
    strokes: List<List<Offset>>,
    sourceSize: IntSize
): String {
    val outputWidth = 1200
    val outputHeight = 480
    val safeWidth = max(sourceSize.width, 1)
    val safeHeight = max(sourceSize.height, 1)
    val scaleX = outputWidth.toFloat() / safeWidth.toFloat()
    val scaleY = outputHeight.toFloat() / safeHeight.toFloat()

    val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 8f
    }

    strokes.forEach { stroke ->
        if (stroke.size == 1) {
            val point = stroke.first()
            canvas.drawCircle(point.x * scaleX, point.y * scaleY, 4f, paint)
        } else {
            stroke.windowed(2).forEach { (start, end) ->
                canvas.drawLine(
                    start.x * scaleX,
                    start.y * scaleY,
                    end.x * scaleX,
                    end.y * scaleY,
                    paint
                )
            }
        }
    }

    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    return "data:image/png;base64,$encoded"
}
