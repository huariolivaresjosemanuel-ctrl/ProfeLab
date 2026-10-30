package pe.profelab.core.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The approved master, unchanged. A finite entrance is not a character rig or a new pose. */
@Composable
fun CanonicalMascot(modifier:Modifier=Modifier,reduceMotion:Boolean=true,entrance:Boolean=false){
    val context=LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null){value=withContext(Dispatchers.IO){runCatching {
        val options=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        context.assets.open("brand/concept_c_original.png").use {BitmapFactory.decodeStream(it,null,options)}
        var sample=1;while(options.outWidth/sample>650 || options.outHeight/sample>800)sample*=2
        context.assets.open("brand/concept_c_original.png").use {BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=sample})}
    }.getOrNull()}}
    val power=context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val animated=entrance && !reduceMotion && ValueAnimator.areAnimatorsEnabled() && !power.isPowerSaveMode
    val alpha=remember {Animatable(1f)}
    LaunchedEffect(animated,bitmap){if(animated && bitmap!=null){alpha.snapTo(.35f);alpha.animateTo(1f,tween(600))}else alpha.snapTo(1f)}
    bitmap?.let {Image(it.asImageBitmap(),"La maestra de ProfeLab, con lazo menta y delantal verde",modifier.graphicsLayer(alpha=alpha.value),contentScale=ContentScale.Fit)}
        ?: Text("Tu compañera de ProfeLab",modifier=modifier)
}
