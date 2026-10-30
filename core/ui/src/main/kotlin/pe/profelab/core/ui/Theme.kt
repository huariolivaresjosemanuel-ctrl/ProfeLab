package pe.profelab.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ProfeColors {
    val Green = Color(0xFF135B46)
    val Ink = Color(0xFF1D3035)
    val Cream = Color(0xFFF8F5EE)
    val Mint = Color(0xFFE7EEE3)
    val Coral = Color(0xFFF4D5C9)
    val Blue = Color(0xFFD9EAF2)
    val Paper = Color(0xFFFFF0D8)
    val Muted = Color(0xFF55665E)
}
@Composable
fun ProfeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary=ProfeColors.Green, onPrimary=Color.White,
            secondary=ProfeColors.Green,onSecondary=Color.White,secondaryContainer=ProfeColors.Mint,onSecondaryContainer=ProfeColors.Ink,
            primaryContainer=ProfeColors.Mint, onPrimaryContainer=ProfeColors.Ink,
            background=ProfeColors.Cream, onBackground=ProfeColors.Ink,
            surface=ProfeColors.Cream, onSurface=ProfeColors.Ink, surfaceVariant=ProfeColors.Mint,
            onSurfaceVariant=ProfeColors.Muted, outline=Color(0xFF78877E)),
        shapes = Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp)),
        typography = Typography(
            headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=30.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold),
            headlineSmall=TextStyle(fontSize=23.sp,lineHeight=29.sp,fontWeight=FontWeight.Bold),
            titleLarge=TextStyle(fontSize=20.sp,lineHeight=26.sp,fontWeight=FontWeight.Bold),
            titleMedium=TextStyle(fontSize=16.sp,lineHeight=22.sp,fontWeight=FontWeight.SemiBold),
            bodyLarge=TextStyle(fontSize=16.sp,lineHeight=23.sp),
            bodyMedium=TextStyle(fontSize=14.sp,lineHeight=20.sp),
            labelLarge=TextStyle(fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.SemiBold),
        ), content=content,
    )
}
