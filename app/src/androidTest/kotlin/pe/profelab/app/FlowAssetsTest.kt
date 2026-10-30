package pe.profelab.app

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import pe.profelab.core.content.AssistantCatalog
import pe.profelab.core.model.*

@RunWith(AndroidJUnit4::class)
class FlowAssetsTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun decode(clip:WelcomeClip){
        val extractor=MediaExtractor();var codec:MediaCodec?=null
        try {
            context.assets.openFd(clip.asset).use {extractor.setDataSource(it.fileDescriptor,it.startOffset,it.length)}
            assertEquals(1,extractor.trackCount);val format=extractor.getTrackFormat(0)
            assertEquals("video/avc",format.getString(MediaFormat.KEY_MIME));assertEquals(540,format.getInteger(MediaFormat.KEY_WIDTH));assertEquals(960,format.getInteger(MediaFormat.KEY_HEIGHT))
            assertTrue(kotlin.math.abs(format.getLong(MediaFormat.KEY_DURATION)/1000-clip.durationMs)<100)
            extractor.selectTrack(0);val decoder=MediaCodec.createDecoderByType("video/avc");codec=decoder
            decoder.configure(format,null,null,0);decoder.start()
            val info=MediaCodec.BufferInfo();var inputEnd=false;var outputEnd=false;var frames=0
            val deadline=SystemClock.elapsedRealtime()+20000
            while(!outputEnd && SystemClock.elapsedRealtime()<deadline){
                if(!inputEnd){val index=decoder.dequeueInputBuffer(10000)
                    if(index>=0){val buffer=requireNotNull(decoder.getInputBuffer(index));buffer.clear();val size=extractor.readSampleData(buffer,0)
                        if(size<0){decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true}
                        else {decoder.queueInputBuffer(index,0,size,extractor.sampleTime,0);extractor.advance()}
                    }
                }
                val index=decoder.dequeueOutputBuffer(info,10000)
                if(index>=0){if(info.size>0)frames++;outputEnd=(info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;decoder.releaseOutputBuffer(index,false)}
            }
            assertTrue("Decoder must reach end of ${clip.asset}",outputEnd);assertTrue("Decoded frames expected",frames>70)
        }finally{codec?.release();extractor.release()}
    }
    @Test fun entryClipHasNoAudioAndFullyDecodes(){decode(WelcomeClip.ENTRY)}
    @Test fun greetingClipHasNoAudioAndFullyDecodes(){decode(WelcomeClip.GREETING)}
    @Test fun editorialContractLoadsAndRejectsArbitraryActions(){
        val text=context.assets.open("assistant/contract_v1.json").bufferedReader().use {it.readText()}
        assertEquals(19,AssistantCatalog.parse(text).size)
        val changed=JSONObject(text);changed.getJSONArray("messages").getJSONObject(0).getJSONObject("action").put("target","https://invalid.example/execute")
        var rejected=false;try{AssistantCatalog.parse(changed.toString())}catch(_:IllegalStateException){rejected=true};assertTrue(rejected)
    }
}
