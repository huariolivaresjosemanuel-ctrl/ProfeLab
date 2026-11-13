package pe.profelab.feature.students

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Sharing/copying are separate, explicit, reviewed actions. No contacts or automatic messaging. */
@Composable
internal fun EducationFeedbackDialog(initial:String,dismiss:()->Unit){
    var text by rememberSaveable(initial){mutableStateOf(initial)};var confirmed by rememberSaveable(initial){mutableStateOf(false)};var message by remember {mutableStateOf<String?>(null)}
    val context=LocalContext.current
    EducationDialog("Revisar retroalimentación privada",dismiss){
        Text("Comprueba destinatario y contenido. Puedes retirar el nombre o adaptar el texto antes de compartir.")
        OutlinedTextField(text,{text=it.take(30000);confirmed=false},label={Text("Resumen editable de registros reales")},minLines=5,modifier=Modifier.fillMaxWidth())
        Row {Checkbox(confirmed,{confirmed=it});Text("He revisado el texto y confirmo su copia o compartición.",Modifier.weight(1f))}
        Button(onClick={try{(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Retroalimentación ProfeLab",text));message="Texto copiado"}catch(_:Exception){message="No se pudo copiar. Revisa el acceso del dispositivo."}},enabled=confirmed && text.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Copiar texto revisado")}
        OutlinedButton(onClick={try{context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"Compartir texto revisado"));confirmed=false}catch(_:android.content.ActivityNotFoundException){message="No hay una aplicación disponible; puedes copiar el texto."}},enabled=confirmed && text.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Compartir con selector de aplicaciones")}
        message?.let {Text(it)}
    }
}
