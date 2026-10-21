import java.util.*;
import java.lang.reflect.Constructor;

/** Confidence is independent of resolution. Website declarations are labelled explicitly. */
final class CombinedStream {
 final String name,url,referer,identity;final int quality,releaseQuality;final boolean confirmed,hls;final Map<String,String> headers;
 CombinedStream(String name,String url,String referer,int quality,int releaseQuality,boolean confirmed,boolean hls,Map<String,String> headers,String identity){
  this.name=name;this.url=url;this.referer=referer;this.quality=quality;this.releaseQuality=releaseQuality;this.confirmed=confirmed;this.hls=hls;this.headers=headers;this.identity=identity;
 }
 static List<CombinedStream> ordered(List<CombinedStream> input){
  List<CombinedStream> out=new ArrayList<>(input);
  Collections.sort(out,new Comparator<CombinedStream>(){public int compare(CombinedStream a,CombinedStream b){int c=Boolean.compare(b.confirmed,a.confirmed);if(c==0)c=Integer.compare(b.quality,a.quality);if(c==0)c=Integer.compare(b.releaseQuality,a.releaseQuality);return c;}});
  Set<String> urls=new HashSet<>(),ids=new HashSet<>();List<CombinedStream> unique=new ArrayList<>();
  for(CombinedStream s:out){if(urls.contains(s.url)||s.identity!=null&&ids.contains(s.identity))continue;urls.add(s.url);if(s.identity!=null)ids.add(s.identity);unique.add(s);}return unique;
 }
 Object link()throws Exception{
  Constructor<?> c=Class.forName("com.lagradost.cloudstream3.utils.ExtractorLink").getConstructor(String.class,String.class,String.class,String.class,int.class,boolean.class,Map.class,String.class);
  return c.newInstance("RoDub Combined",name,url,referer,quality,hls,headers,null);
 }
}
