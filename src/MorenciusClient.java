import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.regex.*;import org.json.*;import org.jsoup.Jsoup;import org.jsoup.nodes.Element;
/** Declarative player configuration only: never evaluates website JavaScript. */
final class MorenciusClient {
 static boolean playerUrl(String value){try{URL u=new URL(value);return u.getProtocol().equals("https")&&u.getHost().equals("morencius.com")&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&u.getQuery()==null&&u.getRef()==null&&u.getPath().matches("/embed/[a-z0-9]{12}/?");}catch(Exception e){return false;}}
 static String literal(String value)throws Exception{
  StringBuilder out=new StringBuilder();for(int i=0;i<value.length();i++){char c=value.charAt(i);if(c!='\\'){out.append(c);continue;}if(++i>=value.length())throw new Exception("Incomplete JS string");char n=value.charAt(i);if(n=='n')out.append('\n');else if(n=='r')out.append('\r');else if(n=='t')out.append('\t');else if(n=='\\'||n=='\''||n=='"'||n=='/')out.append(n);else if(n=='x'||n=='u'){int count=n=='x'?2:4;if(i+count>=value.length())throw new Exception("Incomplete string escape");out.append((char)Integer.parseInt(value.substring(i+1,i+count+1),16));i+=count;}else throw new Exception("Unsupported JS escape");}return out.toString();
 }
 static final class Cursor {
  final String s;int p;Cursor(String s,int p){this.s=s;this.p=p;}
  void space(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;}
  void take(char c)throws Exception{space();if(p>=s.length()||s.charAt(p++)!=c)throw new Exception("Invalid packed syntax");}
  int number()throws Exception{space();int start=p;while(p<s.length()&&Character.isDigit(s.charAt(p)))p++;if(p==start||p-start>5)throw new Exception("Invalid packed number");return Integer.parseInt(s.substring(start,p));}
  String string()throws Exception{take('\'');int start=p;while(p<s.length()){char c=s.charAt(p++);if(c=='\\'){if(p>=s.length())break;p++;}else if(c=='\'')return literal(s.substring(start,p-1));}throw new Exception("Incomplete packed string");}
 }
 static String unpack(String script)throws Exception{
  if(script.length()>512*1024)throw new Exception("Packed player too large");int start=script.indexOf("}('");if(start<0)throw new Exception("Packed player configuration missing");Cursor c=new Cursor(script,start+2);String payload=c.string();c.take(',');int radix=c.number();c.take(',');int count=c.number();c.take(',');String dictionary=c.string();if(!script.startsWith(".split('|'))",c.p))throw new Exception("Invalid packed dictionary");if(radix<2||radix>36||count<1||count>4096)throw new Exception("Invalid player dictionary");String[] words=dictionary.split("\\|",-1);if(words.length!=count)throw new Exception("Player dictionary size mismatch");Matcher token=Pattern.compile("\\b[0-9a-z]+\\b").matcher(payload);StringBuffer out=new StringBuffer();while(token.find()){String word=token.group(),replace=word;try{int index=Integer.parseInt(word,radix);if(index>=0&&index<count&&!words[index].isEmpty()&&Integer.toString(index,radix).equals(word))replace=words[index];}catch(NumberFormatException ignored){}token.appendReplacement(out,Matcher.quoteReplacement(replace));}token.appendTail(out);return out.toString();
 }
 static JSONObject config(String html,String embed)throws Exception{
  if(!playerUrl(embed))throw new Exception("Invalid Morencius player URL");String code=null;for(Element e:Jsoup.parse(html).select("script")){String text=e.data();if(text.startsWith("eval(function(p,a,c,k,e,d)")){String decoded=unpack(text);if(decoded.contains("var links=")){if(code!=null)throw new Exception("Ambiguous player configuration");code=decoded;}}}
  if(code==null)throw new Exception("Morencius player configuration unavailable");String id=new URL(embed).getPath().replaceFirst("/$","").substring(7);if(!code.contains("file_code="+id)&&!code.contains("file_code:'"+id+"'")&&!code.contains("file_code: '"+id+"'"))throw new Exception("Morencius video identity mismatch");
  Matcher links=Pattern.compile("var\\s+links\\s*=\\s*(\\{[^;]+\\})\\s*;").matcher(code);if(!links.find())throw new Exception("Morencius links missing");JSONObject data=new JSONObject(links.group(1));Matcher duration=Pattern.compile("(?:[,\\{])duration\\s*:\\s*[\"']?(\\d+(?:\\.\\d+)?)").matcher(code);if(!duration.find())throw new Exception("Morencius duration missing");data.put("duration",Double.parseDouble(duration.group(1)));return data;
 }
 static String source(JSONObject data)throws Exception{
  String value=data.optString("hls2","");URL u=new URL(value);if(!u.getProtocol().equals("https")||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!(u.getHost().endsWith(".datainnovation.space")||u.getHost().endsWith(".dramiyos-cdn.com"))||!u.getPath().contains("/hls")||!u.getPath().endsWith(".m3u8"))throw new Exception("Unverified Morencius media route");return value;
 }
 static String romanianVariant(String master,String url)throws Exception{
  if(!master.startsWith("#EXTM3U"))throw new Exception("Morencius master playlist missing");String audio=null,group=null;
  for(String line:master.split("\\r?\\n"))if(line.startsWith("#EXT-X-MEDIA:")&&"AUDIO".equals(HlsProbe.attribute(line,"TYPE"))){String language=HlsProbe.attribute(line,"LANGUAGE");if(!RomanianAudio.isRomanian(language))continue;String uri=HlsProbe.attribute(line,"URI");Matcher track=Pattern.compile("^index-a(\\d+)\\.m3u8(?:\\?.*)?$").matcher(uri);if(!track.matches())continue;if(audio!=null)throw new Exception("Ambiguous Romanian audio tracks");audio=track.group(1);group=HlsProbe.attribute(line,"GROUP-ID");}
  if(audio==null)throw new Exception("No selectable Romanian audio track");String[] lines=master.split("\\r?\\n");String selected=null;int best=0;
  for(int i=0;i+1<lines.length;i++)if(lines[i].startsWith("#EXT-X-STREAM-INF:")&&group.equals(HlsProbe.attribute(lines[i],"AUDIO"))){String uri=lines[i+1].trim();Matcher path=Pattern.compile("^index-v\\d+-a"+audio+"\\.m3u8(?:\\?.*)?$").matcher(uri);if(!path.matches())continue;String resolution=HlsProbe.attribute(lines[i],"RESOLUTION");int height;try{height=Integer.parseInt(resolution.substring(resolution.indexOf('x')+1));}catch(Exception e){continue;}if(height>best&&height<=1080){best=height;selected=new URL(new URL(url),uri).toString();}}
  if(selected==null)throw new Exception("No combined video/Romanian variant");return selected;
 }
 static ResolvedStream resolve(NativeCatalog.Candidate c)throws Exception{
  if(!FdCatalog.titleUrl(c.page)||c.expected==null||!playerUrl(c.embed))throw new Exception("Morencius site/audio provenance not enabled");
  String html=new String(NativeHttp.request(c.embed,NativeHttp.headers(c.page),1024*1024,false,null,24000),StandardCharsets.UTF_8);String finalUrl=NativeHttp.session().resolvedUrls.get(c.embed);if(!c.embed.equals(finalUrl))throw new Exception("Unexpected Morencius redirect");JSONObject data=config(html,c.embed);double duration=data.getDouble("duration"),expected=c.expected.optDouble("durationSeconds");if(expected<90||duration<90||Math.abs(duration-expected)>Math.max(60,expected*.10))throw new Exception("Morencius duration mismatch/error clip");
  String masterUrl=source(data);if(!new URL(masterUrl).getPath().contains("/"+new URL(c.embed).getPath().substring(7).replaceFirst("/$","")+"_h/"))throw new Exception("Morencius media identity mismatch");String master=NativeHttp.text(masterUrl,NativeHttp.headers(c.embed));String selected=romanianVariant(master,masterUrl);
  return new ResolvedStream("Morencius",selected,c.embed,c.identity(),0,"FilmeDublate");
 }
}
