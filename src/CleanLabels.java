import java.util.*;
import java.util.regex.*;

final class CleanLabels {
 static String size(long bytes){
  if(bytes<=0)return "";
  if(bytes>=1000000000L)return String.format(Locale.ROOT,"%.1f GB",bytes/1000000000.0);
  if(bytes>=1000000L)return Math.round(bytes/1000000.0)+" MB";
  return Math.max(1,Math.round(bytes/1000.0))+" KB";
 }
 static String hosted(int quality,String transport,boolean confirmed,String filename,long bytes){
  boolean usenet=transport.startsWith("Usenet"),direct="Usenet Direct".equals(transport);
  String first=(quality>0?quality+"p · ":"")+(usenet?"Usenet":"TorBox");
  List<String> facts=new ArrayList<>();if(!confirmed)facts.add("Unverified");facts.add(direct?"Direct streaming":"Cached");
  Matcher extension=Pattern.compile("\\.(mkv|mp4|m4v|avi)$",Pattern.CASE_INSENSITIVE).matcher(filename==null?"":filename.trim());if(extension.find())facts.add(extension.group(1).toUpperCase(Locale.ROOT));
  String size=size(bytes);if(!size.isEmpty())facts.add(size);
  return first+"\n"+(confirmed?"Romanian audio verified":"Possible Romanian dub")+"\n"+String.join(" · ",facts);
 }
 static String website(String host,String site,int quality){
  return (quality>0?quality+"p · ":"")+host+"\nRomanian · website label"+(site==null||site.trim().isEmpty()?"":"\n"+site.trim());
 }
}
