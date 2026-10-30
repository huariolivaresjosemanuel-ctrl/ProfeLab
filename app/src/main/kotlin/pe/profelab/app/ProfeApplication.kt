package pe.profelab.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class ProfeApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
    override fun onCreate() {
        super.onCreate()
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
        graph.startReminderSync()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { graph.refreshReminders() }
        })
    }
}
