package pe.profelab.feature.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.*

data class TextBox(val text:String,val left:Float,val top:Float,val right:Float,val bottom:Float)
object PdfEngine {
    suspend fun text(context:Context,uri:Uri,page:Int,password:String):List<TextBox> = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)!!.use {input->PDDocument.load(input,password).use {doc->
            val p=doc.getPage(page);val box=p.cropBox;val rotated=p.rotation%180!=0
            val w=if(rotated)box.height else box.width;val h=if(rotated)box.width else box.height
            val items=mutableListOf<TextBox>()
            val capture=object:PDFTextStripper(){
                override fun writeString(value:String,positions:MutableList<TextPosition>) {
                    positions.forEach {t->items.add(TextBox(t.unicode,(t.xDirAdj/w).coerceIn(0f,1f),((t.yDirAdj-t.heightDir)/h).coerceIn(0f,1f),((t.xDirAdj+t.widthDirAdj)/w).coerceIn(0f,1f),(t.yDirAdj/h).coerceIn(0f,1f)))}
                }
            }
            capture.startPage=page+1;capture.endPage=page+1;capture.sortByPosition=true;capture.getText(doc);items
        }}
    }
    // Display-to-PDF transform adapted from DocReader MIT PdfOps.toPdf, pinned commit in notices.
    private fun point(x:Float,y:Float,w:Float,h:Float,ox:Float,oy:Float,rotation:Int):Pair<Float,Float> {
        val (u,v)=when(((rotation%360)+360)%360){90->y to 1f-x;180->1f-x to 1f-y;270->1f-y to x;else->x to y}
        return ox+u*w to oy+(1f-v)*h
    }
    suspend fun export(context:Context,uri:Uri,password:String,annotations:List<JSONObject>):File=withContext(Dispatchers.IO){
        val folder=File(context.cacheDir,"exports").apply {mkdirs()};val output=File(folder,"${pe.profelab.core.model.newId()}.pdf")
        context.contentResolver.openInputStream(uri)!!.use {input->PDDocument.load(input,password).use {doc->
            doc.setAllSecurityToBeRemoved(true)
            annotations.groupBy {it.getInt("page")}.forEach { (index,marks)->
                if(index in 0 until doc.numberOfPages){val page=doc.getPage(index);val box=page.cropBox
                    PDPageContentStream(doc,page,PDPageContentStream.AppendMode.APPEND,true,true).use {cs->
                        marks.forEach {a->
                            cs.saveGraphicsState()
                            val color=a.optLong("color",0xFFFFD54FL);val r=((color shr 16)and 255)/255f;val g=((color shr 8)and 255)/255f;val b=(color and 255)/255f
                            cs.setStrokingColor(r,g,b);cs.setNonStrokingColor(r,g,b)
                            cs.setLineWidth((a.optDouble("width",.004)*box.width).toFloat().coerceIn(.5f,24f))
                            fun xy(x:Float,y:Float)=point(x,y,box.width,box.height,box.lowerLeftX,box.lowerLeftY,page.rotation)
                            val points=a.optJSONArray("points") ?: JSONArray()
                            if(a.optString("type")=="pen" && points.length()>=4){
                                val first=xy(points.getDouble(0).toFloat(),points.getDouble(1).toFloat());cs.moveTo(first.first,first.second)
                                for(i in 2 until points.length()-1 step 2){val pt=xy(points.getDouble(i).toFloat(),points.getDouble(i+1).toFloat());cs.lineTo(pt.first,pt.second)};cs.stroke()
                            }else {
                                val x1=a.optDouble("left").toFloat();val y1=a.optDouble("top").toFloat();val x2=a.optDouble("right",(x1+.1).coerceAtMost(1.0)).toFloat();val y2=a.optDouble("bottom",(y1+.03).coerceAtMost(1.0)).toFloat()
                                val corners=listOf(xy(x1,y1),xy(x2,y1),xy(x1,y2),xy(x2,y2));val left=corners.minOf {it.first};val bottom=corners.minOf {it.second};val width=corners.maxOf {it.first}-left;val height=corners.maxOf {it.second}-bottom
                                when(a.optString("type")){
                                    "underline"->{val p1=xy(x1,y2);val p2=xy(x2,y2);cs.moveTo(p1.first,p1.second);cs.lineTo(p2.first,p2.second);cs.stroke()}
                                    "note"->{cs.beginText();cs.setFont(PDType1Font.HELVETICA,10f);cs.newLineAtOffset(left,bottom);cs.showText("Nota (ver comentarios)");cs.endText()}
                                    else->{val state=PDExtendedGraphicsState();state.nonStrokingAlphaConstant=.30f;cs.setGraphicsStateParameters(state);cs.addRect(left,bottom,width.coerceAtLeast(1f),height.coerceAtLeast(1f));cs.fill()}
                                }
                            };cs.restoreGraphicsState()
                        }
                    }
                }
            }
            // Preserve full Unicode comments in printable appendix pages; anchor labels remain on original pages.
            val comments=annotations.map {it.optInt("page") to it.optString("comment")}.filter {it.second.isNotBlank()}.distinct()
            if(comments.isNotEmpty())appendComments(doc,comments)
            doc.save(output)
        }};output
    }
    private fun appendComments(doc:PDDocument,comments:List<Pair<Int,String>>) {
        val paint=android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {textSize=24f;color=android.graphics.Color.BLACK}
        val body="Comentarios de ProfeLab · copia anotada\n\n"+comments.joinToString("\n\n"){(page,text)->"Página original ${page+1}\n$text"}
        val layout=android.text.StaticLayout.Builder.obtain(body,0,body.length,paint,1070).setIncludePad(false).setLineSpacing(8f,1f).build()
        val available=1500;var line=0
        while(line<layout.lineCount){
            val top=layout.getLineTop(line);var end=line
            while(end+1<layout.lineCount && layout.getLineBottom(end+1)-top<=available)end++
            val bottom=layout.getLineBottom(end);val bitmap=android.graphics.Bitmap.createBitmap(1190,1684,android.graphics.Bitmap.Config.ARGB_8888)
            try{
                val canvas=android.graphics.Canvas(bitmap);canvas.drawColor(android.graphics.Color.WHITE);canvas.save();canvas.clipRect(60,70,1130,70+(bottom-top));canvas.translate(60f,70f-top);layout.draw(canvas);canvas.restore()
                val page=PDPage(PDRectangle.A4);doc.addPage(page)
                val image=LosslessFactory.createFromImage(doc,bitmap)
                PDPageContentStream(doc,page).use {it.drawImage(image,0f,0f,PDRectangle.A4.width,PDRectangle.A4.height)}
            }finally{bitmap.recycle()}
            line=end+1
        }
    }
}
