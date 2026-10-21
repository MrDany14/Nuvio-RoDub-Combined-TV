import java.io.*;
import java.util.*;
import org.json.*;
import kotlin.jvm.functions.Function1;
public final class CombinedDeviceProbe {
 public static void main(String[] args)throws Exception{
  String manifest=new BufferedReader(new InputStreamReader(System.in,"UTF-8")).readLine();
  String data=args.length>0?args[0]:"{\"tmdbID\":82728,\"imdbID\":\"tt7678620\",\"movieName\":\"Bluey\",\"originalTitle\":\"Bluey\",\"localTitle\":\"Bluey\",\"releaseYear\":2018,\"season\":1,\"episode\":1}";
  long start=System.nanoTime();final JSONArray rows=new JSONArray();final List<String> urls=new ArrayList<>();
  Object result=new CombinedProvider().lookup(data,false,new Function1(){public Object invoke(Object value){return null;}},new Function1(){public Object invoke(Object value){try{
   Class<?> c=value.getClass();String name=(String)c.getMethod("getName").invoke(value),url=(String)c.getMethod("getUrl").invoke(value);int q=(Integer)c.getMethod("getQuality").invoke(value);String label=name.contains("website")?"website":name.contains("Usenet Direct")?"directUsenet":name.contains("Usenet cached")?"cachedUsenet":"torrent";
   rows.put(new JSONObject().put("kind",label).put("quality",q).put("confirmed",!name.contains("Possible")));urls.add(url);
  }catch(Exception e){throw new RuntimeException("SDK link conversion failed");}return null;}},null,manifest);
  boolean sorted=true;int lastConfidence=2,lastQuality=10000;for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);int confidence=r.getBoolean("confirmed")?1:0,q=r.getInt("quality");if(confidence>lastConfidence||confidence==lastConfidence&&q>lastQuality)sorted=false;lastConfidence=confidence;lastQuality=q;}
  System.out.println("COMBINED_DEVICE_RESULT "+new JSONObject().put("success",result).put("elapsedMs",(System.nanoTime()-start)/1000000).put("streams",rows).put("sorted",sorted).put("uniqueUrls",new HashSet<String>(urls).size()==urls.size()));
 }
}
