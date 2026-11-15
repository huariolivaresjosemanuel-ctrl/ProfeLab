package pe.profelab.feature.students.guardians

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.core.model.newId
import pe.profelab.feature.students.*
import pe.profelab.feature.students.communication.FamilyReportPreview
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun GuardianContacts(student:EduStudent,all:List<WorkRecord>,store:WorkspaceStore){
    var editing by remember(student.id){mutableStateOf(false)}
    var editContact by remember(student.id){mutableStateOf<EduGuardianContact?>(null)}
    var revoke by remember(student.id){mutableStateOf<EduGuardianContact?>(null)}
    var archive by remember(student.id){mutableStateOf<EduGuardianContact?>(null)}
    var report by remember(student.id){mutableStateOf(false)}
    val actions=rememberEducationActions();val repo=remember(store){Education32Repository(store)}
    val contacts=Education32Data.guardians(all).filter {it.studentId==student.id && !it.archived}
    EducationCard("Contactos familiares"){
        Text("Cada contacto tiene permisos propios. Ningún destinatario se elige automáticamente.")
        EducationState(actions)
        if(contacts.isEmpty())Text("Sin contactos registrados. Teléfono y correo son opcionales al guardar.")
        contacts.forEach {contact->EducationCard("${contact.name} · ${contact.relationship}"){
            Text(contact.preferredChannel.label)
            Text(Education32Data.shareBlockReason(contact) ?: "Autorización verificada y vigente para informe de aprendizaje")
            if(!student.archived){TextButton(onClick={editContact=contact;editing=true}){Text("Editar contacto y permiso")};if(contact.revokedAt==null)TextButton(onClick={revoke=contact}){Text("Revocar autorización")};TextButton(onClick={archive=contact}){Text("Archivar contacto")}}
        }}
        if(!student.archived){
            OutlinedButton(onClick={editContact=null;editing=true},modifier=Modifier.fillMaxWidth()){Text("Añadir contacto")}
            Button(onClick={report=true},enabled=contacts.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text("Preparar informe individual")}
            if(contacts.isEmpty())Text("Añade un contacto para preparar un borrador. Compartir requiere permiso vigente y revisión.")
        }
    }
    if(editing)GuardianEditor(student.id,editContact,store){editing=false}
    if(report)FamilyReportPreview(student,all,store){report=false}
    revoke?.let {contact->AlertDialog(onDismissRequest={revoke=null},title={Text("¿Revocar autorización?")},text={Text("${contact.name} dejará de estar habilitado para compartir informes. Se conserva el historial privado.")},confirmButton={TextButton(onClick={actions.perform {repo.revokeGuardian(contact.id);revoke=null}},enabled=!actions.busy){Text("Revocar")}},dismissButton={TextButton(onClick={revoke=null}){Text("Cancelar")}})}
    archive?.let {contact->AlertDialog(onDismissRequest={archive=null},title={Text("¿Archivar contacto?")},text={Text("El contacto quedará deshabilitado para nuevos informes. Los borradores anteriores se conservan privados.")},confirmButton={TextButton(onClick={actions.perform {repo.archive(contact.id);archive=null}},enabled=!actions.busy){Text("Archivar")}},dismissButton={TextButton(onClick={archive=null}){Text("Cancelar")}})}
}

@Composable
private fun GuardianEditor(studentId:String,existing:EduGuardianContact?,store:WorkspaceStore,dismiss:()->Unit){
    var name by remember(studentId,existing?.id){mutableStateOf(existing?.name.orEmpty())}
    var relation by remember(studentId,existing?.id){mutableStateOf(existing?.relationship.orEmpty())}
    var phone by remember(studentId,existing?.id){mutableStateOf(existing?.phone.orEmpty())}
    var email by remember(studentId,existing?.id){mutableStateOf(existing?.email.orEmpty())}
    var channel by remember(studentId,existing?.id){mutableStateOf(existing?.preferredChannel ?: EduFamilyChannel.WHATSAPP)}
    var authorized by remember(studentId,existing?.id){mutableStateOf(existing?.authorized ?: false)}
    var verified by remember(studentId,existing?.id){mutableStateOf(existing?.verified ?: false)}
    fun date(value:Long)=java.time.Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    var from by remember(studentId,existing?.id){mutableStateOf(existing?.let {date(it.validFrom)} ?: LocalDate.now().toString())}
    var until by remember(studentId,existing?.id){mutableStateOf(existing?.let {date(it.validUntil)}.orEmpty())}
    val actions=rememberEducationActions();val repo=remember(store){Education32Repository(store)}
    EducationDialog(if(existing==null)"Nuevo contacto familiar"else "Editar contacto familiar",{if(!actions.busy)dismiss()}){
        EducationState(actions)
        Text("Guarda solo la información necesaria para este estudiante. No se solicitan documentos de identidad ni dirección.")
        OutlinedTextField(name,{name=it.take(100);verified=false;authorized=false},label={Text("Nombre del contacto")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(relation,{relation=it.take(100);verified=false;authorized=false},label={Text("Relación: madre, padre, tutor u otra")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(phone,{phone=it.take(30);verified=false;authorized=false},label={Text("Teléfono con código de país (opcional)")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(email,{email=it.take(254);verified=false;authorized=false},label={Text("Correo (opcional)")},modifier=Modifier.fillMaxWidth())
        EducationChoices(EduFamilyChannel.entries.map {it.name to it.label},channel.name){channel=EduFamilyChannel.valueOf(it);verified=false;authorized=false}
        Text("Permiso para recibir informes de aprendizaje",style=MaterialTheme.typography.titleSmall)
        Row {Checkbox(verified,{verified=it;if(!it)authorized=false},enabled=existing?.revokedAt==null);Text("He verificado la autorización y su alcance",Modifier.weight(1f))}
        Row {Checkbox(authorized,{authorized=it},enabled=verified && existing?.revokedAt==null);Text("Autorizar informes individuales de aprendizaje",Modifier.weight(1f))}
        if(existing?.revokedAt!=null)Text("Permiso revocado. Para una nueva autorización, registra un nuevo contacto y verifica el permiso.")
        OutlinedTextField(from,{from=it.take(10);verified=false;authorized=false},label={Text("Vigencia desde (AAAA-MM-DD)")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(until,{until=it.take(10);verified=false;authorized=false},label={Text("Vigencia hasta (AAAA-MM-DD)")},modifier=Modifier.fillMaxWidth())
        if(!verified && existing!=null)Text("Vuelve a verificar la autorización si cambiaste destinatario, canal o vigencia.")
        Text("Sin permiso verificado y vigente, compartir queda bloqueado. Registrar un contacto no envía mensajes.")
        Button(onClick={actions.perform {
            val start=runCatching {LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()}.getOrElse {error("Revisa la fecha inicial (AAAA-MM-DD)")}
            val end=runCatching {LocalDate.parse(until).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()-1}.getOrElse {error("Revisa la fecha final (AAAA-MM-DD)")}
            repo.saveGuardian(studentId,name,relation,phone,email,channel,authorized,"LEARNING_REPORT",start,end,verified,existing?.id ?: newId());dismiss()
        }},enabled=name.isNotBlank() && relation.isNotBlank() && from.isNotBlank() && until.isNotBlank() && !actions.busy,modifier=Modifier.fillMaxWidth()){Text("Guardar contacto y permiso")}
    }
}
