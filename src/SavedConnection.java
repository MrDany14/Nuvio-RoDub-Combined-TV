import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Read-only, bounded access to this app's current profile addon URLs. No settings are edited. */
final class SavedConnection {
 static final int LIMIT=2*1024*1024;
 static final class Wire {
  final byte[] bytes;int pos;
  Wire(byte[] bytes){this.bytes=bytes;}
  long number()throws IOException{long v=0;for(int shift=0;shift<64;shift+=7){if(pos>=bytes.length)throw new IOException();int b=bytes[pos++]&255;v|=(long)(b&127)<<shift;if((b&128)==0)return v;}throw new IOException();}
  byte[] block()throws IOException{long n=number();if(n<0||n>LIMIT||n>bytes.length-pos)throw new IOException();byte[] out=Arrays.copyOfRange(bytes,pos,pos+(int)n);pos+=(int)n;return out;}
  void skip(int type)throws IOException{if(type==0){number();return;}if(type==2){block();return;}int n=type==1?8:type==5?4:-1;if(n<0||n>bytes.length-pos)throw new IOException();pos+=n;}
 }
 // AndroidX Preferences proto: map<string,Value>=1; Value integer=3, string=5, string_set=6.
 // https://github.com/androidx/androidx/blob/androidx-main/datastore/datastore-preferences-proto/src/main/proto/preferences.proto
 static Map<String,Object> read(File file)throws Exception{
  Map<String,Object> out=new HashMap<>();if(!file.isFile())return out;if(file.length()>LIMIT)throw new IOException();
  byte[] bytes;try(InputStream in=new FileInputStream(file);ByteArrayOutputStream buffer=new ByteArrayOutputStream()){byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1){if(buffer.size()+n>LIMIT)throw new IOException();buffer.write(chunk,0,n);}bytes=buffer.toByteArray();}
  Wire w=new Wire(bytes);while(w.pos<bytes.length){int tag=(int)w.number();if(tag!=10){w.skip(tag&7);continue;}Wire entry=new Wire(w.block());String key=null;byte[] value=null;while(entry.pos<entry.bytes.length){int t=(int)entry.number();if(t==10)key=new String(entry.block(),StandardCharsets.UTF_8);else if(t==18)value=entry.block();else entry.skip(t&7);}
   if(key==null||value==null||!Arrays.asList("active_profile_id","profiles_json","installed_addon_urls_ordered","installed_addon_urls").contains(key))continue;
   Wire v=new Wire(value);Object decoded=null;while(v.pos<value.length){int t=(int)v.number();if(t==24)decoded=(int)v.number();else if(t==42)decoded=new String(v.block(),StandardCharsets.UTF_8);else if(t==50){Wire set=new Wire(v.block());List<String> values=new ArrayList<>();while(set.pos<set.bytes.length){int st=(int)set.number();if(st==10)values.add(new String(set.block(),StandardCharsets.UTF_8));else set.skip(st&7);}decoded=values;}else v.skip(t&7);}out.put(key,decoded);
  }return out;
 }
 static int profile(File filesDir)throws Exception{
  Map<String,Object> prefs=read(new File(filesDir,"datastore/profile_settings.preferences_pb"));Object raw=prefs.get("active_profile_id");int id=raw==null?1:raw instanceof Integer?(Integer)raw:0;if(id<1||id>6)throw new IOException();if(id==1)return 1;
  Object json=prefs.get("profiles_json");if(!(json instanceof String))throw new IOException();JSONArray profiles=new JSONArray((String)json);for(int i=0;i<profiles.length();i++){JSONObject p=profiles.getJSONObject(i);if(p.optInt("id")==id)return p.optBoolean("usesPrimaryAddons",false)?1:id;}throw new IOException();
 }
 static String find(File filesDir){try{
  int id=profile(filesDir);File file=new File(filesDir,"datastore/addon_preferences"+(id==1?"":"_p"+id)+".preferences_pb");Map<String,Object> prefs=read(file);List<String> urls=new ArrayList<>();
  if(prefs.containsKey("installed_addon_urls_ordered")){Object raw=prefs.get("installed_addon_urls_ordered");if(!(raw instanceof String))return "";JSONArray values=new JSONArray((String)raw);for(int i=0;i<values.length();i++)urls.add(values.getString(i));}
  else{Object raw=prefs.get("installed_addon_urls");if(raw instanceof List)for(Object u:(List<?>)raw)if(u instanceof String)urls.add((String)u);}
  Set<String> matches=new LinkedHashSet<>();for(String url:urls)try{String manifest=url.trim().replaceAll("/+$","");if(!manifest.endsWith("/manifest.json"))manifest+="/manifest.json";CombinedRemote.base(manifest);matches.add(manifest);}catch(Exception ignored){}
  return matches.size()==1?matches.iterator().next():"";
 }catch(Exception unavailable){return "";}}
}
