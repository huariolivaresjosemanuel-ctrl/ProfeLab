package pe.profelab.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.first
import pe.profelab.core.ui.*
import pe.profelab.feature.profile.WelcomeFlowModel

class MainActivity : ComponentActivity() {
    private lateinit var welcome:WelcomeFlowModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph=(application as ProfeApplication).graph
        val external=if(savedInstanceState==null)receivePdf(intent)else pdfUri(intent)!=null || graph.incomingPdf.value!=null
        val sameProcess=savedInstanceState?.getString("profelab.welcome.process")==graph.welcomeSessionToken
        val accessibility=getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        welcome=ViewModelProvider(this,modelFactory {WelcomeFlowModel(graph.preferences,graph.repository,{
            graph.transfers.recoverPendingPreferences()
            graph.preferences.migrateLegacyProfile(graph.repository.observeProfile().first())
        },sameProcess && savedInstanceState?.getBoolean("profelab.welcome.session.handled")==true,external,!accessibility.isTouchExplorationEnabled)})[WelcomeFlowModel::class.java]
        setContent {ProfeTheme {ProfeRoot(graph,welcome)}}
    }
    override fun onSaveInstanceState(outState:Bundle){outState.putString("profelab.welcome.process",(application as ProfeApplication).graph.welcomeSessionToken);outState.putBoolean("profelab.welcome.session.handled",welcome.sessionHandled);super.onSaveInstanceState(outState)}
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);if(receivePdf(intent))welcome.externalContentRequested()}
    @Suppress("DEPRECATION")
    private fun pdfUri(source:Intent):Uri? {
        if(source.type!="application/pdf")return null
        val uri=when(source.action){Intent.ACTION_VIEW->source.data;Intent.ACTION_SEND->source.getParcelableExtra<Uri>(Intent.EXTRA_STREAM);else->null}
        return uri?.takeIf {it.scheme=="content"}
    }
    private fun receivePdf(source:Intent):Boolean {
        val uri=pdfUri(source) ?: return false
        (application as ProfeApplication).graph.incomingPdf.value=uri.toString();return true
    }
}
