package pe.profelab.feature.students

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/** Protects private records and recents previews only while this surface is on screen. */
@Composable
fun PrivateEducationSurface(){
    val context=LocalContext.current
    DisposableEffect(context){
        val activity=context.educationActivity()
        val window=activity?.window
        if(window!=null)EducationSecureWindows.acquire(window)
        onDispose {if(window!=null)EducationSecureWindows.release(window)}
    }
}
private fun Context.educationActivity():Activity?=when(this){is Activity->this;is ContextWrapper->baseContext.educationActivity();else->null}

/** Nested private panels must not clear protection while another private surface is visible. */
private object EducationSecureWindows {
    private data class State(var users:Int,val original:Boolean)
    private val states=java.util.WeakHashMap<Window,State>()
    fun acquire(window:Window){
        val state=states[window]
        if(state!=null)state.users++ else states[window]=State(1,window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE!=0)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    fun release(window:Window){
        val state=states[window] ?: return
        state.users--
        if(state.users==0){states.remove(window);if(!state.original)window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)}
    }
}
