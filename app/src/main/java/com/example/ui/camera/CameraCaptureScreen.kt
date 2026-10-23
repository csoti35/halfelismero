package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.OceanDeepDark
import com.example.ui.theme.TurquoisePrimary

@Composable
fun CameraCaptureScreen(
    onPhotoCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_capture_screen")
    ) {
        if (hasCameraPermission) {
            CameraXViewfinder(
                onPhotoCaptured = onPhotoCaptured,
                onClose = onClose
            )
        } else {
            CameraPermissionFallback(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onClose = onClose
            )
        }
    }
}

@Composable
private fun CameraXViewfinder(
    onPhotoCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Bind Camera lifecycle
    LaunchedEffect(lensFacing) {
        try {
            val cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            boundCamera = camera
            camera.cameraControl.enableTorch(isTorchEnabled)
        } catch (exc: Exception) {
            Log.e("CameraCaptureScreen", "Camera binding failed", exc)
        }
    }

    // Handle Torch Toggle
    LaunchedEffect(isTorchEnabled) {
        boundCamera?.cameraControl?.enableTorch(isTorchEnabled)
    }

    // Viewfinder
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Aquarium Fish Reticle Overlay
        AquariumFocusReticle(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 90.dp)
        )

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("camera_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Vissza",
                        tint = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Akváriumi Fényképezés",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch toggle
                Surface(
                    shape = CircleShape,
                    color = if (isTorchEnabled) TurquoisePrimary.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = { isTorchEnabled = !isTorchEnabled },
                        modifier = Modifier.testTag("camera_torch_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Vaku ki/be",
                            tint = if (isTorchEnabled) Color(0xFF002220) else Color.White
                        )
                    }
                }

                // Lens switch
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier.testTag("camera_flip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Kamera váltás",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Controls with Shutter Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Helyezd a halat a kijelölt keretbe oldalról",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Shutter Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(84.dp)
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .border(4.dp, TurquoisePrimary, CircleShape)
                )

                // Inner trigger button
                Surface(
                    shape = CircleShape,
                    color = if (isCapturing) TurquoisePrimary.copy(alpha = 0.5f) else Color.White,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isCapturing) {
                            isCapturing = true
                            takePicture(
                                context = context,
                                imageCapture = imageCapture,
                                onPhotoCaptured = { bitmap ->
                                    isCapturing = false
                                    onPhotoCaptured(bitmap)
                                },
                                onError = {
                                    isCapturing = false
                                }
                            )
                        }
                        .testTag("camera_shutter_button")
                ) {
                    if (isCapturing) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = TurquoisePrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun takePicture(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val rawBitmap = imageProxy.toBitmap()
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                    val orientedBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(
                            rawBitmap,
                            0,
                            0,
                            rawBitmap.width,
                            rawBitmap.height,
                            matrix,
                            true
                        )
                    } else {
                        rawBitmap
                    }

                    onPhotoCaptured(orientedBitmap)
                } catch (e: Exception) {
                    Log.e("CameraCaptureScreen", "Kép feldolgozási hiba", e)
                } finally {
                    imageProxy.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraCaptureScreen", "Fénykép készítési hiba: ${exception.message}", exception)
                onError(exception)
            }
        }
    )
}

@Composable
private fun AquariumFocusReticle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val cornerLength = 36.dp.toPx()
        val strokeWidth = 3.dp.toPx()
        val cornerRadius = 20.dp.toPx()
        val reticleColor = Color(0xFF00E5C9)

        // Draw rounded focus frame corners
        // Top-Left
        drawLine(
            color = reticleColor,
            start = Offset(0f, cornerLength),
            end = Offset(0f, 0f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = reticleColor,
            start = Offset(0f, 0f),
            end = Offset(cornerLength, 0f),
            strokeWidth = strokeWidth
        )

        // Top-Right
        drawLine(
            color = reticleColor,
            start = Offset(width - cornerLength, 0f),
            end = Offset(width, 0f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = reticleColor,
            start = Offset(width, 0f),
            end = Offset(width, cornerLength),
            strokeWidth = strokeWidth
        )

        // Bottom-Left
        drawLine(
            color = reticleColor,
            start = Offset(0f, height - cornerLength),
            end = Offset(0f, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = reticleColor,
            start = Offset(0f, height),
            end = Offset(cornerLength, height),
            strokeWidth = strokeWidth
        )

        // Bottom-Right
        drawLine(
            color = reticleColor,
            start = Offset(width - cornerLength, height),
            end = Offset(width, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = reticleColor,
            start = Offset(width, height),
            end = Offset(width, height - cornerLength),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = TurquoisePrimary.copy(alpha = 0.15f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = TurquoisePrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Kamera hozzáférés szükséges",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Ahhoz, hogy lefotózhasd az akváriumodban úszó halakat és az AI azonosítani tudja a fajt, engedélyezned kell a kamera használatát a készüléken.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = TurquoisePrimary,
                contentColor = Color(0xFF002220)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("request_camera_permission_button")
        ) {
            Text("Kamera engedélyezése", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.12f),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Mégse, vissza a galériához")
        }
    }
}
