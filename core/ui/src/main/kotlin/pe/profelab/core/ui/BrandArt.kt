package pe.profelab.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale

/** Original vector illustration; no pixels taken from the supplied mockups. */
@Composable
fun BookMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width/64f,size.height/64f,pivot=Offset.Zero) {
            drawPath(Path().apply { moveTo(4f,20f); quadraticTo(19f,15f,30f,24f); lineTo(30f,57f); quadraticTo(17f,48f,4f,52f); close() },ProfeColors.Green)
            drawPath(Path().apply { moveTo(34f,24f); quadraticTo(47f,15f,60f,20f); lineTo(60f,52f); quadraticTo(45f,48f,34f,57f); close() },Color(0xFF78A187))
            drawLine(Color(0xFFD8A34E),Offset(32f,53f),Offset(32f,15f),3f,StrokeCap.Round)
            drawPath(Path().apply { moveTo(32f,19f); cubicTo(24f,8f,38f,2f,44f,3f); cubicTo(45f,14f,36f,20f,32f,19f); close() },Color(0xFFD8A34E))
        }
    }
}
@Composable
fun DeskPlant(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width/120f,size.height/120f,pivot=Offset.Zero) {
            val stem = Color(0xFF547E60)
            drawLine(stem,Offset(45f,102f),Offset(51f,28f),3f,StrokeCap.Round)
            drawLine(stem,Offset(47f,75f),Offset(24f,53f),3f,StrokeCap.Round)
            drawPath(Path().apply { moveTo(45f,68f); cubicTo(12f,65f,9f,35f,12f,32f); cubicTo(34f,28f,49f,47f,45f,68f); close() },Color(0xFF8EAF92))
            drawPath(Path().apply { moveTo(52f,50f); cubicTo(25f,28f,51f,7f,54f,5f); cubicTo(75f,23f,71f,43f,52f,50f); close() },Color(0xFF78A187))
            drawPath(Path().apply { moveTo(27f,88f); lineTo(64f,88f); lineTo(59f,118f); lineTo(32f,118f); close() },stem)
            rotate(15f,pivot=Offset(91f,96f)) {
                drawLine(Color(0xFFDCA548),Offset(87f,96f),Offset(87f,42f),7f,StrokeCap.Round)
                drawLine(ProfeColors.Green,Offset(97f,96f),Offset(97f,36f),7f,StrokeCap.Round)
            }
            drawPath(Path().apply { moveTo(71f,82f); lineTo(110f,82f); lineTo(105f,118f); lineTo(76f,118f); close() },Color(0xFFD9A047))
            drawLine(Color(0xFFE8BD6E),Offset(86f,21f),Offset(92f,10f),4f,StrokeCap.Round)
            drawLine(Color(0xFFE8BD6E),Offset(97f,28f),Offset(109f,24f),4f,StrokeCap.Round)
        }
    }
}
