import com.lagradost.cloudstream3.metaproviders.TmdbProvider;
import kotlin.jvm.functions.Function1;
import kotlin.coroutines.Continuation;
import java.util.*;
import java.util.concurrent.*;

public final class CombinedProvider extends TmdbProvider {
 public String getName(){return "RoDub Combined [TV Test]";}
 public String getMainUrl(){return NativeCatalog.SITE;}
 public String getLang(){return "ro";}
 public boolean getHasMainPage(){return false;}
 public Object load(String input,Continuation continuation){NativeHttp.Session session=new NativeHttp.Session(12000);NativeHttp.CURRENT.set(session);try{return NativeMetadata.load(input,getName());}catch(Exception e){return null;}finally{session.stop();NativeHttp.CURRENT.remove();}}
 public Object loadLinks(final String data,final boolean casting,final Function1 subtitle,Function1 callback,final Continuation continuation){
  String saved="";try{if(CombinedPlugin.app!=null)saved=new CombinedStore(CombinedPlugin.app).read();}catch(Exception e){/* Device keystore unavailable: website lookup still works. */}
  boolean diagnostic=false;try{diagnostic=!new org.json.JSONObject(data).has("originalTitle");}catch(Exception e){}
  if(saved.isEmpty()||diagnostic)CombinedSetup.show();return lookup(data,casting,subtitle,callback,continuation,saved);
 }
 Object lookup(final String data,final boolean casting,final Function1 subtitle,Function1 callback,final Continuation continuation,final String manifest){
  final List<CombinedStream> results=Collections.synchronizedList(new ArrayList<CombinedStream>());
  ExecutorService workers=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"RoDub-combined");t.setDaemon(true);return t;});
  Future<?> web=workers.submit(()->new RoDubbedProvider().loadLinks(data,casting,subtitle,new Function1(){public Object invoke(Object value){if(value instanceof CombinedStream)results.add((CombinedStream)value);return null;}},continuation));
  final NativeHttp.Session remoteSession=new NativeHttp.Session(31000);
  Future<?> hosted=workers.submit(()->{if(manifest.isEmpty())return;NativeHttp.CURRENT.set(remoteSession);try{results.addAll(CombinedRemote.streams(manifest,data));}catch(Exception e){System.out.println("[RoDub Combined] hosted lookup unavailable");}finally{remoteSession.stop();NativeHttp.CURRENT.remove();}});
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);int count=0;
  try{
   for(Future<?> f:Arrays.asList(web,hosted)){long remaining=end-System.nanoTime();if(remaining<=0)break;try{f.get(remaining,TimeUnit.NANOSECONDS);}catch(ExecutionException e){/* Keep the other branch. */}catch(TimeoutException e){break;}}
   List<CombinedStream> snapshot; synchronized(results){snapshot=new ArrayList<>(results);}
   for(CombinedStream stream:CombinedStream.ordered(snapshot)){callback.invoke(stream.link());count++;}
  }catch(InterruptedException e){Thread.currentThread().interrupt();}catch(Exception e){System.out.println("[RoDub Combined] output stopped");}
  finally{remoteSession.stop();web.cancel(true);hosted.cancel(true);workers.shutdownNow();}
  System.out.println("[RoDub Combined] streams="+count);return count>0;
 }
}
