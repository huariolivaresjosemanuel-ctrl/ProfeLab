package pe.profelab.feature.students

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.feature.students.classrooms.ClassroomDirectory
import pe.profelab.feature.students.classrooms.ClassroomDetail
import pe.profelab.feature.students.students.StudentProfile

/** A private, hierarchical Cuaderno workspace; contextual callers keep their original route. */
@Composable
fun StudentsWorkspace(all:List<WorkRecord>,store:WorkspaceStore,back:()->Unit,
    initialClassroomId:String?=null,initialStudentId:String?=null,backLabel:String="Volver a Cuaderno") {
    PrivateEducationSurface()
    var classroomId by rememberSaveable {mutableStateOf(initialClassroomId)}
    var studentId by rememberSaveable {mutableStateOf(initialStudentId)}
    var tools by rememberSaveable {mutableStateOf(false)}
    fun up(){when {studentId!=null->studentId=null;classroomId!=null->classroomId=null;tools->tools=false;else->back()}}
    BackHandler(onBack=::up)
    val classroom=EducationData.classrooms(all).find {it.id==classroomId}
    val student=EducationData.students(all).find {it.id==studentId}
    Column(Modifier.fillMaxSize().testTag("students-workspace").padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(when {student!=null->"Ficha del estudiante";classroom!=null->classroom.name;tools->"Recursos de seguimiento";else->"Mis aulas"},
                Modifier.weight(1f).padding(top=12.dp),style=MaterialTheme.typography.headlineSmall)
            TextButton(onClick=::up){Text(if(student!=null)"Volver al aula"else if(classroom!=null || tools)"Mis aulas"else backLabel)}
        }
        when {
            student!=null->key(student.id){StudentProfile(student,classroomId,all,store)}
            classroom!=null->key(classroom.id){ClassroomDetail(classroom,all,store,{studentId=it})}
            tools->EducationTools(all,store)
            else->ClassroomDirectory(all,store,{classroomId=it},{tools=true})
        }
    }
}
