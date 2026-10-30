package pe.profelab.feature.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import pe.profelab.core.ui.*
import java.io.File

@Composable
fun CaptureScreen(onImage:(Uri)->Unit,back:()->Unit) {
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    var granted by remember {mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
    var message by remember {mutableStateOf<String?>(null)};var torch by remember {mutableStateOf(false)}
    val controller=remember {LifecycleCameraController(context).apply {cameraSelector=CameraSelector.DEFAULT_BACK_CAMERA}}
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it;if(!it)message="Cámara denegada. Puedes seleccionar una imagen."}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(onImage)}
    DisposableEffect(granted,owner){
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_STOP){runCatching {controller.enableTorch(false)};torch=false};if(event==Lifecycle.Event.ON_RESUME){granted=ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED}}
        owner.lifecycle.addObserver(observer)
        if(granted)runCatching {controller.bindToLifecycle(owner)}.onFailure {message="No se pudo abrir la cámara; elige una imagen."}
        onDispose {runCatching {controller.enableTorch(false)};runCatching {controller.unbind()};owner.lifecycle.removeObserver(observer)}
    }
    Page {
        PageTitle("Foto / OCR","Encuadra un documento sin datos sensibles",back)
        message?.let {ErrorNotice(it)}
        if(!granted)Button(onClick={request.launch(Manifest.permission.CAMERA)}){Text("Autorizar cámara al capturar")}
        if(granted){
            AndroidView(factory={PreviewView(it).apply {this.controller=controller}},modifier=Modifier.fillMaxWidth().height(380.dp))
            Button(onClick={
                val file=File(context.cacheDir,"capture-${pe.profelab.core.model.newId()}.jpg")
                try{controller.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(context),object:ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result:ImageCapture.OutputFileResults){controller.enableTorch(false);torch=false;onImage(Uri.fromFile(file))}
                    override fun onError(error:ImageCaptureException){message="No se pudo capturar la foto; revisa permiso y almacenamiento."}
                })}catch(_:Exception){message="La cámara no está disponible; prueba seleccionar una imagen."}
            }){Text("Capturar documento")}
            TextButton(onClick={if(controller.cameraInfo?.hasFlashUnit()==true){torch=!torch;controller.enableTorch(torch)}else message="Este dispositivo no tiene flash disponible."}){Text(if(torch)"Apagar linterna" else "Linterna (si hay hardware)")}
        }
        OutlinedButton(onClick={picker.launch(arrayOf("image/*"))}){Text("Elegir imagen del dispositivo")}
        Text("Toca la vista previa para enfocar. La revisión permite girar y recortar antes del OCR.")
    }
}
