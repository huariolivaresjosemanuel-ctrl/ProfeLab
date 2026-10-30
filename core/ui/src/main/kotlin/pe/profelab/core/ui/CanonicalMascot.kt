package pe.profelab.core.ui

import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class MascotGesture { IDLE, WAVE, EXPLAIN, CELEBRATE }
object MascotCapabilities {
    // A fade is not a greeting. Arm and eyelid rigs still require canonical/anatomical approval.
    const val approvedWave=false
    fun supported(gesture:MascotGesture)=gesture==MascotGesture.IDLE
}

/** Supplied canonical art with baked 2.5D shadow, without an additional Compose shadow. Never the full sheet. Unsupported gestures use this static fallback. */
@Composable
fun CanonicalMascot(modifier:Modifier=Modifier,reduceMotion:Boolean=true,entrance:Boolean=false,
    gesture:MascotGesture=MascotGesture.IDLE,fullBody:Boolean=false){
    val context=LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null,fullBody){value=withContext(Dispatchers.IO){try {
        fun decode(asset:String,shadow:Boolean):android.graphics.Bitmap?=try {
            context.assets.open(asset).use {input->
                @Suppress("DEPRECATION") val decoder=requireNotNull(BitmapRegionDecoder.newInstance(input,false))
                try {
                    val region=Rect(0,0,decoder.width,if(fullBody)decoder.height else minOf(if(shadow)1600 else 980,decoder.height))
                    var sample=1;while(region.width()/sample>600 || region.height()/sample>800)sample*=2
                    decoder.decodeRegion(region,BitmapFactory.Options().apply {inSampleSize=sample})
                }finally{decoder.recycle()}
            }
        }catch(_:Exception){null}
        decode("brand/concept_c_assistant_shadow_v1.webp",true) ?: decode("brand/concept_c_individual.png",false)
    }catch(e:CancellationException){throw e}catch(_:Exception){null}}}
    // Legacy entrance, gesture and reduceMotion never activate an unapproved motion or endless loop.
    bitmap?.let {Image(it.asImageBitmap(),"Maestra Concepto C de ProfeLab, con lazo menta y delantal verde",
        modifier.testTag("mascot-canonical-static"),contentScale=ContentScale.Fit)}
        ?: Text("Tu compañera de ProfeLab",modifier=modifier)
}
