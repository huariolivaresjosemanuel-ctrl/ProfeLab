package pe.profelab.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File

/** Target app storage is always available; the instrumentation package's external directory may be null. */
internal fun qaCaptureDirectory(scope:String?=null):File {
    require(scope==null || scope.matches(Regex("[a-z0-9-]{1,80}")))
    val root=File(ApplicationProvider.getApplicationContext<Context>().filesDir,"qa-captures/visual")
    return (if(scope==null)root else File(root,scope)).apply {
    check(isDirectory || mkdirs()) {"No se pudo crear la carpeta de evidencia QA"}
    }
}

internal fun captureQaWindow(name:String){
    require(name.matches(Regex("[A-Z0-9_]+")))
    val bitmap=requireNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
    try {File(qaCaptureDirectory(),"$name.png").outputStream().use {check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}}
    finally{bitmap.recycle()}
}
