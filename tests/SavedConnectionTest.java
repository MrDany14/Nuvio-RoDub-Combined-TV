import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class SavedConnectionTest {
 static byte[] field(int tag,byte[] value)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();number(out,tag);number(out,value.length);out.write(value);return out.toByteArray();}
 static void number(ByteArrayOutputStream out,long value){do{int b=(int)value&127;value>>>=7;out.write(value==0?b:b|128);}while(value!=0);}
 static byte[] join(byte[]... values)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] value:values)out.write(value);return out.toByteArray();}
 static byte[] text(String value)throws Exception{return field(42,value.getBytes("UTF-8"));}
 static byte[] integer(int value)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();number(out,24);number(out,value);return out.toByteArray();}
 static byte[] entry(String key,byte[] value)throws Exception{return field(10,join(field(10,key.getBytes("UTF-8")),field(18,value)));}
 static void write(File root,String name,byte[] value)throws Exception{Files.write(new File(root,"datastore/"+name+".preferences_pb").toPath(),value);}
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("rodub-prefill-").toFile();new File(root,"datastore").mkdir();
  String a="https://rodub-instant-test.vercel.app/"+String.join("",Collections.nCopies(64,"a")),b=a.replace("/aaaa","/bbbb");
  byte[] primary=entry("installed_addon_urls_ordered",text("[\"https://v3-cinemeta.strem.io\",\""+a+"\"]"));
  write(root,"addon_preferences",primary);check(SavedConnection.find(root).equals(a+"/manifest.json"));
  check(Arrays.equals(primary,Files.readAllBytes(new File(root,"datastore/addon_preferences.preferences_pb").toPath())));
  write(root,"profile_settings",join(entry("active_profile_id",integer(2)),entry("profiles_json",text("[{\"id\":2,\"usesPrimaryAddons\":false}]"))));
  check(SavedConnection.find(root).isEmpty());write(root,"addon_preferences_p2",entry("installed_addon_urls_ordered",text("[\""+b+"/manifest.json\"]")));check(SavedConnection.find(root).equals(b+"/manifest.json"));
  write(root,"profile_settings",join(entry("active_profile_id",integer(2)),entry("profiles_json",text("[{\"id\":2,\"usesPrimaryAddons\":true}]"))));check(SavedConnection.find(root).equals(a+"/manifest.json"));
  write(root,"addon_preferences",entry("installed_addon_urls_ordered",text("[\""+a+"\",\""+b+"\"]")));check(SavedConnection.find(root).isEmpty());
  write(root,"addon_preferences",join(entry("installed_addon_urls_ordered",text("[]")),entry("installed_addon_urls",field(50,field(10,a.getBytes("UTF-8"))))));check(SavedConnection.find(root).isEmpty());
  write(root,"addon_preferences",entry("installed_addon_urls",field(50,field(10,a.getBytes("UTF-8")))));check(SavedConnection.find(root).equals(a+"/manifest.json"));
  write(root,"addon_preferences",entry("installed_addon_urls_ordered",text("[\""+a.replace("vercel.app","vercel.app.evil.invalid")+"\"]")));check(SavedConnection.find(root).isEmpty());
  write(root,"profile_settings",new byte[]{10,127});check(SavedConnection.find(root).isEmpty());
  if(args.length>0){File destination=new File(args[0]);new File(destination,"datastore").mkdirs();write(destination,"addon_preferences",primary);}
  System.out.println("SAVED_CONNECTION_OK automatic_prefill_profile_isolation_inheritance_ambiguity_removal_legacy_corruption_readonly");
 }
}
