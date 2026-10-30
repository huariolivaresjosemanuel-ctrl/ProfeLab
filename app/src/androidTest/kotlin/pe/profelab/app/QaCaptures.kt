package pe.profelab.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File

/** Target app storage is always available; the instrumentation package's external directory may be null. */
internal fun qaCaptureDirectory():File=File(ApplicationProvider.getApplicationContext<Context>().filesDir,"qa-captures/visual").apply {
    check(isDirectory || mkdirs()) {"No se pudo crear la carpeta de evidencia QA"}
}

internal fun captureQaWindow(name:String){
    require(name.matches(Regex("[A-Z0-9_]+")))
    val bitmap=requireNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
    try {File(qaCaptureDirectory(),"$name.png").outputStream().use {check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}}
    finally{bitmap.recycle()}
}
