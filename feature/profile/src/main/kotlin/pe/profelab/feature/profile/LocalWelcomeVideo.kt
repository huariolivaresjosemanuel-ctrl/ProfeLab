package pe.profelab.feature.profile

import android.animation.ValueAnimator
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.PowerManager
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.profelab.core.model.*
import pe.profelab.core.ui.CanonicalMascot

/** One local decoder, no URL, audio, loop, wake lock or view-owned persistent data. */
private class LocalVideoSession(private val context:Context,private val view:TextureView,private val clip:WelcomeClip,
    initialPosition:Int,private val position:(Int)->Unit,private val complete:()->Unit,private val failure:()->Unit,
    private val rendered:()->Unit):TextureView.SurfaceTextureListener {
    private var player:MediaPlayer?=null;private var surface:Surface?=null;private var prepared=false
    private var active=false;private var closed=false;private var terminal=false;private var saved=initialPosition
    init {view.surfaceTextureListener=this}
    fun foreground(){active=true;if(view.isAvailable)view.surfaceTexture?.let(::open)}
    fun background(){active=false;releasePlayer()}
    private fun open(texture:SurfaceTexture){
        if(closed || terminal || !active || player!=null)return
        try {
            val media=MediaPlayer();player=media;surface=Surface(texture)
            media.setSurface(surface);media.setVolume(0f,0f);media.isLooping=false;media.setScreenOnWhilePlaying(false)
            media.setOnInfoListener {_,what,_->if(what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START)rendered();false}
            media.setOnErrorListener {_,_,_->terminal=true;releasePlayer();failure();true}
            media.setOnCompletionListener {terminal=true;saved=clip.durationMs.toInt();position(saved);releasePlayer();complete()}
            media.setOnSeekCompleteListener {if(player===it && active && !closed && !terminal)it.start()}
            media.setOnPreparedListener {if(player!==it || closed)return@setOnPreparedListener;prepared=true;it.setVolume(0f,0f);if(active){if(saved>0)it.seekTo(saved.toLong(),MediaPlayer.SEEK_CLOSEST)else it.start()}}
            context.assets.openFd(clip.asset).use {media.setDataSource(it.fileDescriptor,it.startOffset,it.length)}
            media.prepareAsync()
        }catch(_:Exception){terminal=true;releasePlayer();failure()}
    }
    private fun releasePlayer(){
        player?.let {media->if(prepared && !terminal)try {saved=media.currentPosition;position(saved)}catch(_:Exception){}
            media.setOnCompletionListener(null);media.setOnPreparedListener(null);media.setOnSeekCompleteListener(null);media.setOnErrorListener(null);media.setOnInfoListener(null)
            try{media.release()}catch(_:Exception){}
        };player=null;prepared=false;surface?.release();surface=null
    }
    fun close(){closed=true;releasePlayer();view.surfaceTextureListener=null}
    override fun onSurfaceTextureAvailable(texture:SurfaceTexture,width:Int,height:Int){open(texture)}
    override fun onSurfaceTextureSizeChanged(texture:SurfaceTexture,width:Int,height:Int){}
    override fun onSurfaceTextureUpdated(texture:SurfaceTexture){}
    override fun onSurfaceTextureDestroyed(texture:SurfaceTexture):Boolean {releasePlayer();return true}
}

@Composable
private fun motionAllowed(prefs:UserPreferences):Boolean {
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle
    val power=context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var allowed by remember {mutableStateOf(false)}
    DisposableEffect(context,lifecycle,prefs){
        fun refresh(){allowed=WelcomeMediaPolicy.canPlay(prefs,power.isPowerSaveMode,ValueAnimator.areAnimatorsEnabled())}
        val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,intent:Intent?){refresh()}}
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_START || event==Lifecycle.Event.ON_RESUME)refresh()}
        refresh();lifecycle.addObserver(observer)
        val registered=try {ContextCompat.registerReceiver(context,receiver,IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),ContextCompat.RECEIVER_NOT_EXPORTED);true}catch(_:Exception){false}
        onDispose {lifecycle.removeObserver(observer);if(registered)try {context.unregisterReceiver(receiver)}catch(_:IllegalArgumentException){}}
    }
    return allowed
}

@Composable
fun LocalWelcomeVideo(clip:WelcomeClip,state:WelcomeState,modifier:Modifier=Modifier,
    onPosition:(WelcomeClip,Int)->Unit,onComplete:(WelcomeClip)->Unit,onFailure:(WelcomeClip)->Unit){
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle
    val motion=motionAllowed(state.prefs)
    val poster by produceState<android.graphics.Bitmap?>(null,clip,motion){value=if(!motion)null else withContext(Dispatchers.IO){try {context.assets.open(clip.poster).use {BitmapFactory.decodeStream(it)}}catch(e:CancellationException){throw e}catch(_:Exception){null}}}
    val latestPosition=rememberUpdatedState(onPosition);val latestComplete=rememberUpdatedState(onComplete);val latestFailure=rememberUpdatedState(onFailure)
    var view by remember(clip){mutableStateOf<TextureView?>(null)};var firstFrame by remember(clip){mutableStateOf(false)}
    Box(modifier.testTag("welcome-media-${clip.name}")){
        if(!motion || state.videoFailed)CanonicalMascot(Modifier.fillMaxSize(),reduceMotion=true,fullBody=true)
        else {
            if(!state.videoComplete)AndroidView(factory={TextureView(it).also {texture->view=texture}},modifier=Modifier.fillMaxSize().semantics {stateDescription=if(firstFrame)"Vídeo sin audio en reproducción"else "Cargando vídeo sin audio"})
            if(!firstFrame || state.videoComplete)poster?.let {Image(it.asImageBitmap(),"Vídeo de bienvenida de Flow, sin audio",Modifier.fillMaxSize())}
        }
    }
    DisposableEffect(view,clip,lifecycle,motion,state.videoComplete,state.videoFailed){
        val texture=view
        if(texture==null || !motion || state.videoComplete || state.videoFailed)onDispose {}
        else {
            val session=LocalVideoSession(context,texture,clip,state.positionMs,{latestPosition.value(clip,it)},{latestComplete.value(clip)},{latestFailure.value(clip)},{firstFrame=true})
            val observer=LifecycleEventObserver {_,event->when(event){Lifecycle.Event.ON_START,Lifecycle.Event.ON_RESUME->session.foreground();Lifecycle.Event.ON_STOP,Lifecycle.Event.ON_PAUSE->{firstFrame=false;session.background()};else->Unit}}
            lifecycle.addObserver(observer);if(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))session.foreground()
            onDispose {lifecycle.removeObserver(observer);session.close()}
        }
    }
}
