import java.util.*;
import org.json.*;
public final class CombinedPolicyTest {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 static CombinedStream stream(String name,int q,boolean confirmed){return new CombinedStream(name,"https://example.invalid/"+name,"ref",q,0,confirmed,false,Collections.singletonMap("Referer","ref"),name);}
 public static void main(String[] args)throws Exception{
  CombinedStream fallback=stream("possible4k",2160,false),website=stream("website720",720,true),audio=stream("audio1080",1080,true);
  List<CombinedStream> sorted=CombinedStream.ordered(Arrays.asList(fallback,website,audio,audio));
  check(sorted.size()==3&&sorted.get(0)==audio&&sorted.get(1)==website&&sorted.get(2)==fallback);check(sorted.get(1).headers.get("Referer").equals("ref"));
  String root="https://rodub-instant-test.vercel.app/"+String.join("",Collections.nCopies(64,"a"))+"/";
  check(CombinedRemote.base(root+"manifest.json").equals(root));
  for(String bad:Arrays.asList(root.replace("https:","http:")+"manifest.json",root.replace("vercel.app","vercel.app.evil.invalid")+"manifest.json",root+"manifest.json?x=1",root+"manifest.json#x"))try{CombinedRemote.base(bad);throw new AssertionError();}catch(AssertionError e){throw e;}catch(Exception expected){}
  JSONArray rows=new JSONArray().put(new JSONObject().put("name","1080p Usenet").put("title","RO Dub (audio metadata)\nBluey").put("roConfidence","confirmed").put("resolution",1080).put("url",root+"play/ticket"));
  rows.put(new JSONObject().put("title","Possible RO Dub").put("resolution",2160).put("url",root+"torrentplay/ticket"));
  rows.put(new JSONObject().put("url","https://127.0.0.1/media/a"));
  rows.put(new JSONObject().put("url","https://rodub-instant-test.vercel.app.evil.invalid/play/ticket"));
  List<CombinedStream> parsed=CombinedRemote.parseStreams(new JSONObject().put("streams",rows),root);check(parsed.size()==2&&parsed.get(0).confirmed&&!parsed.get(1).confirmed);check(parsed.get(0).name.contains("Usenet cached"));
  JSONObject metadata=new JSONObject().put("name","Bluey").put("original_name","Bluey").put("first_air_date","2018-10-01").put("external_ids",new JSONObject().put("imdb_id","tt7678620"));
  check(NativeMetadata.link(82728,metadata,true).getString("imdbID").equals("tt7678620"));
  System.out.println("COMBINED_POLICY_OK confidence_quality_dedupe_headers_identity_configuration");
 }
}
