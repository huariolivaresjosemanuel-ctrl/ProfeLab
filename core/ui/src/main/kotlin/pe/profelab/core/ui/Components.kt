package pe.profelab.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

fun <T : ViewModel> modelFactory(create: () -> T): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
}

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment=Alignment.TopCenter) {
        Column(Modifier.widthIn(max=1000.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
    }
}
@Composable
fun PageTitle(title: String, subtitle: String? = null, back: (() -> Unit)? = null) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        if (back != null) IconButton(onClick=back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Volver") }
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.headlineSmall)
            if (subtitle != null) Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=ProfeColors.Muted)
        }
    }
}
@Composable
fun SectionHeading(title:String,subtitle:String,icon:ImageVector){
    Row(Modifier.fillMaxWidth().testTag("section-$title"),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.headlineLarge);Text(subtitle,style=MaterialTheme.typography.bodyLarge,color=ProfeColors.Muted)}
        if(LocalDensity.current.fontScale<1.4f)Surface(color=ProfeColors.Mint,shape=MaterialTheme.shapes.large){Icon(icon,null,Modifier.padding(18.dp).size(44.dp),tint=ProfeColors.Green)}
    }
}
@Composable
fun ActionTile(title:String,subtitle:String,icon:ImageVector,color:Color,modifier:Modifier=Modifier,onClick:()->Unit){
    Card(onClick=onClick,modifier=modifier,colors=CardDefaults.cardColors(containerColor=color)){
        Column(Modifier.fillMaxWidth().padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
            Icon(icon,null,Modifier.size(34.dp),tint=ProfeColors.Green)
            Text(title,style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,textAlign=TextAlign.Center,color=ProfeColors.Muted)
        }
    }
}
@Composable
fun SoftCard(title: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick=onClick, modifier=modifier.heightIn(min=108.dp), colors=CardDefaults.cardColors(containerColor=color)) {
        BoxWithConstraints {
            if (maxWidth < 230.dp) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Icon(icon,null,Modifier.size(28.dp),tint=ProfeColors.Green)
                        Text(title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    }
                    Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=ProfeColors.Muted)
                }
            } else Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(icon,null,Modifier.size(32.dp),tint=ProfeColors.Green)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(title,style=MaterialTheme.typography.titleMedium)
                    Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=ProfeColors.Muted)
                }
                Icon(Icons.Outlined.ChevronRight,null,Modifier.size(20.dp))
            }
        }
    }
}
@Composable
fun InfoCard(title: String, message: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=.65f))) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium)
            Text(message,style=MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
fun ErrorNotice(message: String, retry: (() -> Unit)? = null) {
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(message,color=MaterialTheme.colorScheme.onErrorContainer)
            if (retry != null) TextButton(onClick=retry) { Text("Reintentar") }
        }
    }
}
