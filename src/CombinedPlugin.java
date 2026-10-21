import android.app.Activity;
import android.content.Context;
import java.lang.ref.WeakReference;
import java.util.*;
import com.lagradost.cloudstream3.metaproviders.TmdbProvider;
public final class CombinedPlugin {
 static Context app;static WeakReference<Activity> activity=new WeakReference<>(null);
 public void load(Context context){app=context.getApplicationContext();if(context instanceof Activity)activity=new WeakReference<>((Activity)context);}
 static Activity currentActivity(){Activity a=activity.get();if(a==null||a.isFinishing()||a.isDestroyed())try{a=(Activity)Class.forName("com.lagradost.cloudstream3.AcraApplication").getMethod("getActivity").invoke(null);}catch(Exception e){a=null;}return a;}
 public List<TmdbProvider> getRegisteredMainAPIs(){return Collections.<TmdbProvider>singletonList(new CombinedProvider());}
 public List<Object> getRegisteredExtractorAPIs(){return Collections.emptyList();}
}
