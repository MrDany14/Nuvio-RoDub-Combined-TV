import java.util.*;import java.util.concurrent.*;
public class CombinedWaitTest {
 static CombinedStream stream(boolean confirmed){return new CombinedStream("test","https://example.invalid/","",1080,0,confirmed,false,Collections.emptyMap(),"test");}
 public static void main(String[] args)throws Exception{
  List<CombinedStream> results=Collections.synchronizedList(new ArrayList<>());FutureTask<Void> stalled=new FutureTask<>(()->null);
  results.add(stream(true));CombinedWait.await(results,Arrays.asList(stalled),1000,5);if(stalled.isDone()||results.size()!=1)throw new AssertionError("Confirmed result must survive a stalled branch");
  results.clear();results.add(stream(false));ExecutorService worker=Executors.newSingleThreadExecutor();try{Future<?> done=worker.submit(()->{try{Thread.sleep(25);}catch(InterruptedException e){Thread.currentThread().interrupt();}});CombinedWait.await(results,Arrays.asList(done),1000,0);if(!done.isDone())throw new AssertionError("Unconfirmed-only lookup ended early");}finally{worker.shutdownNow();}
  stalled.cancel(true);System.out.println("COMBINED_WAIT_OK confirmed_grace_unconfirmed_full_search");
 }
}
