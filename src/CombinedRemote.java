import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

final class CombinedRemote {
 static String base(String manifest)throws Exception{
  URI u=new URI(manifest.trim());
  if(!"https".equals(u.getScheme())||!"rodub-instant-test.vercel.app".equals(u.getHost())||u.getPort()!=-1||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||!u.getPath().matches("/[a-f0-9]{64}/manifest\\.json"))throw new Exception("Use your private RoDub Instant installer URL");
  return manifest.trim().substring(0,manifest.trim().length()-"manifest.json".length());
 }
 static JSONObject read(String url)throws Exception{return new JSONObject(new String(NativeHttp.request(url,Collections.<String,String>emptyMap(),512*1024,false,null,28000),StandardCharsets.UTF_8));}
 static void validate(String manifest)throws Exception{JSONObject m=read(base(manifest)+"manifest.json");if(!"org.mrdany.rodub.instant.test".equals(m.optString("id")))throw new Exception("Unexpected addon");}
 static List<CombinedStream> streams(String manifest,String data)throws Exception{
  String root=base(manifest);JSONObject q=new JSONObject(data);boolean tv=q.has("season")&&q.has("episode");String imdb=q.optString("imdbID",q.optString("imdbId",""));
  if(!imdb.matches("tt\\d+")){
   int id=q.optInt("tmdbID",q.optInt("tmdbId",0));if(id<=0)return Collections.emptyList();
   imdb=read("https://api.themoviedb.org/3/"+(tv?"tv/":"movie/")+id+"/external_ids?api_key="+NativeCatalog.TMDB_KEY).optString("imdb_id","");
  }
  if(!imdb.matches("tt\\d+"))return Collections.emptyList();
  String id=imdb+(tv?":"+q.getInt("season")+":"+q.getInt("episode"):"");
  return parseStreams(read(root+"tvstream/"+(tv?"series/":"movie/")+id+".json"),root);
 }
 static List<CombinedStream> parseStreams(JSONObject response,String root)throws Exception{
  JSONArray values=response.optJSONArray("streams");List<CombinedStream> result=new ArrayList<>();if(values==null)return result;
  for(int i=0;i<Math.min(12,values.length());i++){
   JSONObject s=values.getJSONObject(i);String url=s.optString("url","");URI u=new URI(url);
   if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||u.getFragment()!=null)continue;
   boolean addon="rodub-instant-test.vercel.app".equals(u.getHost())&&url.startsWith(root)&&(u.getPath().contains("/torrentplay/")||u.getPath().contains("/play/"));
   boolean direct="130.61.16.159.nip.io".equals(u.getHost())&&u.getPath().matches("/media/[a-f0-9]{64}");if(!addon&&!direct)continue;
   String title=s.optString("title",""),name=s.optString("name","");boolean confirmed="confirmed".equals(s.optString("roConfidence"))&&title.startsWith("RO Dub (audio metadata)");
   String transport=name.contains("Usenet Direct")?"Usenet Direct":name.contains("Usenet")?"Usenet cached":"TorBox cached";
   String filename=title.contains("\n")?title.substring(title.indexOf('\n')+1):"";
   int quality=s.optInt("resolution",0);String label=(quality>0?quality+"p | ":"")+transport+" | "+(confirmed?"RO Dub (audio metadata)":"Possible RO Dub");if(!filename.isEmpty())label+="\n"+filename;
   JSONObject hints=s.optJSONObject("behaviorHints");String identity=hints==null?null:hints.optString("filename","");if(identity!=null&&!identity.isEmpty())identity="hosted:"+transport+":"+identity+":"+hints.optLong("videoSize",0);else identity=null;
   result.add(new CombinedStream(label,url,"",quality,s.optInt("releaseQuality",0),confirmed,false,Collections.<String,String>emptyMap(),identity));
  }return result;
 }
}
