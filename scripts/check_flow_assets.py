"""Read-only ISO BMFF/H.264 structure checks. This is NOT entropy decoding or Android playback."""
from pathlib import Path
import json,struct,hashlib

def u32(b,p):return struct.unpack_from('>I',b,p)[0]
def boxes(b,start=0,end=None):
    end=len(b) if end is None else end;result=[];offset=start
    while offset<end:
        assert offset+8<=end
        size=u32(b,offset);kind=b[offset+4:offset+8].decode('ascii');header=8
        if size==1:size=struct.unpack_from('>Q',b,offset+8)[0];header=16
        if size==0:size=end-offset
        assert size>=header and offset+size<=end,(kind,offset,size,end)
        result.append((kind,offset,offset+header,offset+size));offset+=size
    assert offset==end;return result
def child(b,parent,kind):return next(x for x in boxes(b,parent[2],parent[3]) if x[0]==kind)
class Bits:
    def __init__(self,b):self.bits=''.join(f'{x:08b}' for x in b);self.p=0
    def get(self,n):v=int(self.bits[self.p:self.p+n],2);self.p+=n;return v
    def ue(self):
        zeros=0
        while self.get(1)==0:zeros+=1;assert zeros<32
        return (1<<zeros)-1+(self.get(zeros) if zeros else 0)
def inspect(path):
    b=Path(path).read_bytes();root=boxes(b);moov=next(x for x in root if x[0]=='moov');mdat=next(x for x in root if x[0]=='mdat')
    assert moov[1]<mdat[1],'faststart required';tracks=[]
    for track in [x for x in boxes(b,moov[2],moov[3]) if x[0]=='trak']:
        mdia=child(b,track,'mdia');hdlr=child(b,mdia,'hdlr');handler=b[hdlr[2]+8:hdlr[2]+12].decode('ascii');assert handler=='vide','No audio/extra track allowed'
        mdhd=child(b,mdia,'mdhd');p=mdhd[2];version=b[p]
        timescale=u32(b,p+(20 if version==1 else 12));duration=struct.unpack_from('>Q',b,p+24)[0] if version==1 else u32(b,p+16)
        table=child(b,child(b,mdia,'minf'),'stbl');stsd=child(b,table,'stsd');assert u32(b,stsd[2]+4)==1
        sample=boxes(b,stsd[2]+8,stsd[3])[0];assert sample[0] in ['avc1','avc3']
        width,height=struct.unpack_from('>HH',b,sample[1]+32);assert (width,height)==(540,960)
        avcc=next(x for x in boxes(b,sample[1]+86,sample[3]) if x[0]=='avcC');p=avcc[2];nal_length=(b[p+4]&3)+1;assert b[p+5]&31
        sps_length=struct.unpack_from('>H',b,p+6)[0];sps=b[p+8:p+8+sps_length];rbsp=sps[1:].replace(b'\x00\x00\x03',b'\x00\x00');bits=Bits(rbsp)
        profile=bits.get(8);bits.get(8);level=bits.get(8);bits.ue();chroma=1;luma_depth=chroma_depth=8
        if profile in {100,110,122,244,44,83,86,118,128,138,139,134,135}:
            chroma=bits.ue()
            if chroma==3:bits.get(1)
            luma_depth=8+bits.ue();chroma_depth=8+bits.ue()
        assert (chroma,luma_depth,chroma_depth)==(1,8,8),'8-bit yuv420 required'
        stts=child(b,table,'stts');entries=u32(b,stts[2]+4);frames=ticks=0
        for i in range(entries):count,delta=struct.unpack_from('>II',b,stts[2]+8+i*8);frames+=count;ticks+=count*delta
        assert abs(frames*timescale/ticks-24)<0.001
        stsz=child(b,table,'stsz');fixed=u32(b,stsz[2]+4);count=u32(b,stsz[2]+8);sizes=[fixed]*count if fixed else [u32(b,stsz[2]+12+i*4) for i in range(count)];assert count==frames
        offsets=next(x for x in boxes(b,table[2],table[3]) if x[0] in {'stco','co64'});n=u32(b,offsets[2]+4);wide=offsets[0]=='co64';positions=[struct.unpack_from('>Q' if wide else '>I',b,offsets[2]+8+i*(8 if wide else 4))[0] for i in range(n)]
        stsc=child(b,table,'stsc');mapping=[struct.unpack_from('>III',b,stsc[2]+8+i*12) for i in range(u32(b,stsc[2]+4))]
        sample_index=0;nals=0
        for chunk_index,offset in enumerate(positions,1):
            _,samples,_=next(row for row in reversed(mapping) if row[0]<=chunk_index)
            for _ in range(samples):
                size=sizes[sample_index];sample_index+=1;assert mdat[2]<=offset and offset+size<=mdat[3]
                end=offset+size
                while offset<end:
                    length=int.from_bytes(b[offset:offset+nal_length],'big');offset+=nal_length;assert length>0 and offset+length<=end
                    assert b[offset]&31 in range(1,24);offset+=length;nals+=1
                assert offset==end
        assert sample_index==frames
        tracks.append(dict(codec='h264',width=width,height=height,fps=24,frames=frames,nals=nals,duration_seconds=duration/timescale,pixel_format='yuv420p',profile=profile,level=level))
    assert len(tracks)==1
    return dict(path=str(path),sha256=hashlib.sha256(b).hexdigest(),bytes=len(b),faststart=True,audio_tracks=0,track=tracks[0],scope='Container/SPS and all sample NAL bounds only; full video decoding NOT_RUN')

if __name__=='__main__':
    root=Path(__file__).resolve().parents[1]/'feature/profile/src/main/assets/welcome'
    print(json.dumps([inspect(root/name) for name in ['flow_entry_v1.mp4','flow_greeting_v1.mp4']],indent=2))
