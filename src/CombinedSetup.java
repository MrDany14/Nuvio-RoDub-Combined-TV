import android.app.*;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.*;
final class CombinedSetup {
 private static boolean open;
 static void show(){final Activity a=CombinedPlugin.currentActivity();if(a==null||a.isFinishing()||a.isDestroyed())return;
  a.runOnUiThread(()->{if(open)return;open=true;LinearLayout layout=new LinearLayout(a);layout.setOrientation(1);layout.setPadding(28,12,28,12);
   TextView status=new TextView(a);status.setText("Paste your private RoDub Instant installer URL once. It is encrypted on this TV. Website streams run locally using your TV connection. Results: confirmed Romanian first, then possible Romanian; quality within each group.");
   EditText input=new EditText(a);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setHint("Private manifest.json URL");layout.addView(status);layout.addView(input);
   AlertDialog dialog=new AlertDialog.Builder(a).setTitle("RoDub Combined — private setup").setView(layout).setPositiveButton("Save connection",null).setNegativeButton("Later",null).setNeutralButton("Forget connection",(d,w)->new CombinedStore(a).clear()).create();
   dialog.setOnDismissListener(d->{input.setText("");open=false;});dialog.show();dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
   dialog.getButton(-1).setOnClickListener(v->{final String value=input.getText().toString().trim();dialog.getButton(-1).setEnabled(false);status.setText("Checking connection…");Thread t=new Thread(()->{NativeHttp.Session session=new NativeHttp.Session(12000);NativeHttp.CURRENT.set(session);boolean ok=false;try{CombinedRemote.validate(value);new CombinedStore(a).save(value);ok=true;}catch(Exception e){}finally{session.stop();NativeHttp.CURRENT.remove();}final boolean connected=ok;a.runOnUiThread(()->{input.setText("");status.setText(connected?"Connected. Close this dialog and reopen the title to load combined streams.":"Could not connect. Paste your private RoDub Instant manifest URL and try again.");dialog.getButton(-1).setEnabled(true);});},"RoDub-private-setup");t.setDaemon(true);t.start();});
  });
 }
}
