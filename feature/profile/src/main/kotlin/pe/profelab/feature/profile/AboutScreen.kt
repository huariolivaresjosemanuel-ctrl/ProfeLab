package pe.profelab.feature.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import pe.profelab.core.ui.*

@Composable
fun AboutScreen(back: ()->Unit) {
    val context=LocalContext.current
    val mit=remember {runCatching {context.assets.open("licenses/DocReader-MIT.txt").bufferedReader().use {it.readText()}}.getOrDefault("Licencia MIT disponible en las fuentes: licenses/DocReader-MIT.txt")}
    Page {
        PageTitle("Acerca de ProfeLab",back=back)
        InfoCard("ProfeLab · 1.0.0","Organización local para estudiantes y docentes de Educación Primaria. Planificación, proyectos, notas y consulta de documentos propios.")
        InfoCard("Independiente del MINEDU","Los títulos y enlaces de Biblioteca proceden del catálogo entregado para este proyecto. No hay PDFs oficiales incluidos. Los materiales y trabajos del usuario no tienen aprobación oficial.")
        InfoCard("Créditos de software","AndroidX, Compose, Room, CameraX, WorkManager, Kotlin y PDFBox-Android: Apache 2.0. ML Kit: condiciones de Google. PdfDocumentHandle y transformación de coordenadas adaptados de DocReader, licencia MIT. Ilustraciones editoriales ProfeLab. Avisos completos en las fuentes.")
        Text("No se incluyen claves privadas ni código de ScanPro. La aplicación es independiente del MINEDU.")
        Text(mit)
    }
}
