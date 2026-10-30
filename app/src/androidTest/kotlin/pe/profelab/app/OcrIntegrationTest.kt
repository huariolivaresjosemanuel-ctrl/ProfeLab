package pe.profelab.app

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.feature.ocr.LatinOcrEngine

@RunWith(AndroidJUnit4::class)
class OcrIntegrationTest {
    @Test fun bundledLatinModelRecognizesFixture()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().context
        val bitmap=context.assets.open("qa/EJEMPLO_ORIGEN_PARA_OCR.png").use {BitmapFactory.decodeStream(it)}
        LatinOcrEngine().use {engine->assertTrue(engine.recognize(bitmap).isNotBlank())}
        bitmap.recycle()
    }
}
