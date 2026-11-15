package pe.profelab.feature.students.communication

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.feature.students.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** RAM-only editing and explicit single-recipient confirmation; no send API or delivery claim. */
@Composable
internal fun FamilyReportPreview(student:EduStudent,all:List<WorkRecord>,store:WorkspaceStore,dismiss:()->Unit){
    var contactId by remember(student.id){mutableStateOf("")}
    var selected by remember(student.id){mutableStateOf(emptySet<String>())}
    var selectedAcademic by remember(student.id){mutableStateOf(emptySet<String>())}
    var body by remember(student.id){mutableStateOf("Estimada familia:\n\nFecha: ${LocalDate.now()}\nActividad y avances observados:\n\nOrientación para casa:\n\n")}
    var channel by remember(student.id){mutableStateOf<EduFamilyChannel?>(null)}
    var confirm by remember(student.id){mutableStateOf(false)}
    var savedId by remember(student.id){mutableStateOf<String?>(null)}
    var status by remember(student.id){mutableStateOf<String?>(null)}
    val contacts=Education32Data.guardians(all).filter {it.studentId==student.id && !it.archived}
    val contact=contacts.find {it.id==contactId}
    var reviewed by remember(student.id,contactId,contact,body,selected,selectedAcademic){mutableStateOf(false)}
    LaunchedEffect(contact){confirm=false}
    val observations=Education32Data.timeline(all,student.id)
    val academic=Education32Data.academicAssessments(all).filter {it.studentId==student.id && !it.archived}
    val reason=Education32Data.shareBlockReason(contact)
    val destinationReady=when(channel){EduFamilyChannel.EMAIL->!contact?.email.isNullOrBlank();EduFamilyChannel.SMS,EduFamilyChannel.WHATSAPP->!contact?.phone.isNullOrBlank();null->false}
    val actions=rememberEducationActions();val repo=remember(store){Education32Repository(store)}
    val context=LocalContext.current
    EducationDialog("Informe familiar · vista previa",{if(!actions.busy)dismiss()}){
        Text("Un solo estudiante: ${student.displayName}",style=MaterialTheme.typography.titleMedium)
        Text("Selecciona registros y revisa cada frase. No incluyas nombres, notas o imágenes de otras personas. No se adjuntan fotos automáticamente.")
        EducationState(actions)
        status?.let {Text(it)}
        if(observations.isEmpty() && academic.isEmpty())Text("Sin evaluaciones ni observaciones individuales. Puedes redactar un informe revisado sin atribuir evidencias inexistentes.")
        academic.forEach {evaluation->Row {
            Checkbox(evaluation.id in selectedAcademic,{checked->selectedAcademic=if(checked)selectedAcademic+evaluation.id else selectedAcademic-evaluation.id;savedId=null;status=null})
            Text("${evaluation.taskKind.label} · ${evaluation.period}\n${evaluation.competency}\n${evaluation.scale.label}: ${evaluation.value}",Modifier.weight(1f))
        }}
        observations.forEach {observation->Row {
            Checkbox(observation.id in selected,{checked->selected=if(checked)selected+observation.id else selected-observation.id;savedId=null;status=null})
            Text("${observation.criterion.label} · ${Instant.ofEpochMilli(observation.at).atZone(ZoneId.systemDefault()).toLocalDate()}\n${observation.status.label}",Modifier.weight(1f))
        }}
        TextButton(onClick={
            val records=observations.filter {it.id in selected}
            val academicText=academic.filter {it.id in selectedAcademic}.joinToString("\n\n"){e->"${e.taskKind.label} · ${e.period}\n${e.competency}\nCriterio: ${e.criterion}\nValoración docente (${e.scale.label}): ${e.value}\n${e.observation}\nOrientación docente: ${e.improvementAction}"}
            val workshopText=records.joinToString("\n\n"){o->"Actividad observada · ${o.criterion.label}\n${o.note.ifBlank {o.status.label}}"}
            body="Estimada familia:\n\nFecha: ${LocalDate.now()}\n\n"+listOf(academicText,workshopText).filter(String::isNotBlank).joinToString("\n\n")+"\n\nOrientación para casa:\n";savedId=null;status=null
        },enabled=selected.isNotEmpty() || selectedAcademic.isNotEmpty()){Text("Preparar texto con registros seleccionados")}
        OutlinedTextField(body,{body=it.take(10000);savedId=null;status=null},label={Text("Mensaje editable · revisa antes de compartir")},minLines=5,modifier=Modifier.fillMaxWidth())
        Text("Contacto autorizado · selección obligatoria",style=MaterialTheme.typography.titleSmall)
        contacts.forEach {item->Row {
            RadioButton(contactId==item.id,{contactId=item.id;channel=null;savedId=null;status=null},modifier=Modifier.testTag("family-contact:${item.id}"))
            Column(Modifier.weight(1f)){Text("${item.name} · ${item.relationship}");Text(Education32Data.shareBlockReason(item) ?: "Permiso vigente",style=MaterialTheme.typography.bodySmall)}
        }}
        reason?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        contact?.let {item->
            Text("Destinatario revisado: ${item.name}")
            EducationChoices(EduFamilyChannel.entries.map {it.name to it.label},channel?.name.orEmpty()){channel=EduFamilyChannel.valueOf(it);reviewed=false}
            Text(when(channel){EduFamilyChannel.EMAIL->item.email.ifBlank {"Este contacto no tiene correo"};EduFamilyChannel.SMS,EduFamilyChannel.WHATSAPP->item.phone.ifBlank {"Este contacto no tiene teléfono"};null->"Selecciona un canal"})
        }
        Row {Checkbox(reviewed,{reviewed=it},enabled=reason==null && destinationReady);Text("He revisado destinatario, permiso, canal y contenido individual",Modifier.weight(1f))}
        OutlinedButton(onClick={actions.perform {savedId=repo.saveDraft(student.id,contactId,selected,body,savedId ?: pe.profelab.core.model.newId(),selectedAcademic);status="Borrador guardado en este dispositivo."}},
            enabled=contact!=null && body.isNotBlank() && !actions.busy,modifier=Modifier.fillMaxWidth()){Text("Guardar borrador privado")}
        Button(onClick={confirm=true},enabled=reason==null && reviewed && destinationReady && body.isNotBlank() && !actions.busy,modifier=Modifier.fillMaxWidth()){Text("Revisar y abrir aplicación externa")}
        Text("Abrir otra aplicación no significa enviado, entregado ni leído. El envío final se realiza manualmente fuera de ProfeLab.",style=MaterialTheme.typography.bodySmall)
        val drafts=Education32Data.drafts(all).filter {it.studentId==student.id && !it.archived}
        if(drafts.isNotEmpty()){
            Text("Borradores guardados",style=MaterialTheme.typography.titleSmall)
            drafts.sortedByDescending {it.createdAt}.forEach {draft->TextButton(onClick={
                // Loading a draft never selects its previous recipient: the teacher must review again.
                body=draft.body;selected=draft.observationIds;selectedAcademic=draft.academicAssessmentIds;contactId="";channel=null;savedId=null;status=null
            }){Text("Revisar borrador · ${Instant.ofEpochMilli(draft.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()} · "+if(draft.state==EduShareState.EXTERNAL_APP_OPENED)"app externa abierta"else "preparado")}}
        }
    }
    if(confirm)AlertDialog(onDismissRequest={if(!actions.busy)confirm=false},title={Text("¿Abrir ${channel?.label.orEmpty()}?")},text={Column {
        Text("Destinatario: ${contact?.name.orEmpty()} · ${contact?.relationship.orEmpty()}")
        Text(if(channel==EduFamilyChannel.EMAIL)contact?.email.orEmpty()else contact?.phone.orEmpty())
        Text("Se abrirá el mensaje revisado de ${student.displayName}. Verifica nuevamente el destinatario en la aplicación externa. Tú decides si lo envías.")
    }},confirmButton={TextButton(onClick={actions.perform {
        val selectedChannel=requireNotNull(channel)
        val id=repo.saveDraft(student.id,contactId,selected,body,savedId ?: pe.profelab.core.model.newId(),selectedAcademic);savedId=id
        val request=repo.prepareShare(id,selectedChannel,reviewed)
        val intent=Intent(if(selectedChannel==EduFamilyChannel.WHATSAPP)Intent.ACTION_VIEW else Intent.ACTION_SENDTO,Uri.parse(request.uri))
        if(selectedChannel==EduFamilyChannel.SMS)intent.putExtra("sms_body",request.body)
        try {context.startActivity(intent)}catch(_:android.content.ActivityNotFoundException){error("No hay una aplicación disponible para este canal. El borrador permanece guardado.")}
        repo.markExternalOpened(id,selectedChannel,reviewed);status="Aplicación externa abierta. ProfeLab no confirma envío ni entrega.";confirm=false;reviewed=false
    }},enabled=!actions.busy && reviewed && destinationReady && Education32Data.shareBlockReason(contact)==null){Text("Abrir aplicación")}},dismissButton={TextButton(onClick={confirm=false},enabled=!actions.busy){Text("Cancelar")}})
}
