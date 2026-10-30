package pe.profelab.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

@Suppress("DEPRECATION")
internal fun installationReport(context:Context):String {
    val flags=if(Build.VERSION.SDK_INT>=28)PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    val info=context.packageManager.getPackageInfo(context.packageName,flags)
    val certificates=if(Build.VERSION.SDK_INT>=28)info.signingInfo?.apkContentsSigners.orEmpty()else info.signatures.orEmpty()
    val fingerprints=certificates.map {signature->MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString(":"){"%02X".format(it)}}
    val code=if(Build.VERSION.SDK_INT>=28)info.longVersionCode else info.versionCode.toLong()
    val debug=(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)!=0
    return "Paquete: ${context.packageName}\nVersión: ${info.versionName} ($code)\nTipo: ${if(debug)"DEBUG / prueba"else "RELEASE"}\nCertificado(s) SHA-256:\n${fingerprints.joinToString("\n")}"
}
@Composable
internal fun InstallationCard(){
    val context=LocalContext.current
    val report=remember {runCatching {installationReport(context)}.getOrElse {"No se pudo consultar la información de instalación."}}
    var copied by remember {mutableStateOf(false)}
    Card {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Mi instalación y actualizaciones",style=MaterialTheme.typography.titleMedium)
        SelectionContainer {Text(report,style=MaterialTheme.typography.bodySmall)}
        Text("Para actualizar conservando datos, las APK necesitan la misma identidad, una firma compatible y un código de versión mayor. Conserva tu respaldo antes de cambiar de origen de APK.",style=MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick={(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Diagnóstico ProfeLab",report));copied=true}){Text(if(copied)"Diagnóstico copiado"else "Copiar diagnóstico público")}
    }}
}
