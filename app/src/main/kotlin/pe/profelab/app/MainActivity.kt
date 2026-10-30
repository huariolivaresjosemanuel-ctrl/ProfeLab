package pe.profelab.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.profelab.core.ui.ProfeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph=(application as ProfeApplication).graph
        if(savedInstanceState==null)receivePdf(intent)
        setContent { ProfeTheme { ProfeApp(graph) } }
    }
    override fun onNewIntent(intent: Intent) {super.onNewIntent(intent);setIntent(intent);receivePdf(intent)}
    @Suppress("DEPRECATION")
    private fun receivePdf(source:Intent) {
        if(source.type!="application/pdf")return
        val uri=when(source.action){Intent.ACTION_VIEW->source.data;Intent.ACTION_SEND->source.getParcelableExtra<Uri>(Intent.EXTRA_STREAM);else->null}
        if(uri?.scheme=="content") (application as ProfeApplication).graph.incomingPdf.value=uri.toString()
    }
}
