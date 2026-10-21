import java.util.*;
import java.util.concurrent.*;

final class CombinedWait {
 static void await(List<CombinedStream> results,List<? extends Future<?>> tasks,long budgetMillis,long settleMillis)throws InterruptedException{
  long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(budgetMillis),confirmedAt=0;
  synchronized(results){for(;;){
   boolean done=true;for(Future<?> task:tasks)if(!task.isDone())done=false;if(done)return;
   long now=System.nanoTime();if(confirmedAt==0)for(CombinedStream stream:results)if(stream.confirmed){confirmedAt=now;break;}
   long remaining=end-now;if(confirmedAt!=0)remaining=Math.min(remaining,confirmedAt+TimeUnit.MILLISECONDS.toNanos(settleMillis)-now);
   if(remaining<=0)return;results.wait(Math.max(1,Math.min(100,TimeUnit.NANOSECONDS.toMillis(remaining))));
  }}
 }
}
