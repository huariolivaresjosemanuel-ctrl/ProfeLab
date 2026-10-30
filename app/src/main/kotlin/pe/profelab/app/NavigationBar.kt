package pe.profelab.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.profelab.core.ui.ProfeColors

data class TopDestination(val route: String,val label: String,val icon: ImageVector)
val topDestinations=listOf(
    TopDestination("home","Inicio",Icons.Outlined.Home),
    TopDestination("taller","Taller",Icons.Outlined.Brush),
    TopDestination("sessions","Sesiones",Icons.Outlined.Groups),
    TopDestination("notebook","Cuaderno",Icons.Outlined.Article),
    TopDestination("library","Biblioteca",Icons.AutoMirrored.Outlined.MenuBook),
)
@Composable
fun ProfeNavigationBar(route: String?, navigate: (String)->Unit) {
    Surface(tonalElevation=2.dp,shadowElevation=2.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical=8.dp)) {
            topDestinations.forEach { destination ->
                val selected=route==destination.route || (route=="practices" && destination.route=="notebook")
                Column(Modifier.weight(1f).heightIn(min=64.dp).testTag("nav_${destination.route}").selectable(selected=selected,role=Role.Tab,onClick={navigate(destination.route)}).padding(horizontal=2.dp,vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Icon(destination.icon,null,tint=if(selected) ProfeColors.Green else ProfeColors.Muted)
                    Text(destination.label,fontSize=12.sp,color=if(selected) ProfeColors.Green else ProfeColors.Muted)
                    if(selected) Box(Modifier.width(20.dp).height(3.dp).background(ProfeColors.Green))
                }
            }
        }
    }
}
