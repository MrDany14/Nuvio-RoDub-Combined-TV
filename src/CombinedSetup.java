import android.app.*;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.*;
final class CombinedSetup {
 private static boolean open;
 static void show(final String prefill){final Activity a=CombinedPlugin.currentActivity();if(a==null||a.isFinishing()||a.isDestroyed())return;
  a.runOnUiThread(()->{if(open)return;open=true;LinearLayout layout=new LinearLayout(a);layout.setOrientation(1);layout.setPadding(28,12,28,12);
   TextView status=new TextView(a);status.setText(prefill.isEmpty()?"No RoDub Instant connection found in this Nuvio profile. Keep RoDub Instant installed, then reopen this setup to fill the connection automatically. You can also paste your private installer URL below.":"Your RoDub Instant connection is already filled in. Press OK to connect. The private link will be saved encrypted on this TV.");
   EditText input=new EditText(a);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setHint("Private manifest.json URL");input.setText(prefill);if(!prefill.isEmpty())input.setFocusable(false);layout.addView(status);layout.addView(input);
   AlertDialog dialog=new AlertDialog.Builder(a).setTitle("RoDub Combined — private setup").setView(layout).setPositiveButton("OK",null).setNegativeButton("Later",null).setNeutralButton("Forget connection",(d,w)->new CombinedStore(a).clear()).create();
   dialog.setOnDismissListener(d->{input.setText("");open=false;});dialog.show();dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
   dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);if(!prefill.isEmpty())dialog.getButton(-1).requestFocus();
   dialog.getButton(-1).setOnClickListener(v->{final String value=input.getText().toString().trim();dialog.getButton(-1).setEnabled(false);status.setText("Checking connection…");Thread t=new Thread(()->{NativeHttp.Session session=new NativeHttp.Session(12000);NativeHttp.CURRENT.set(session);boolean ok=false;try{CombinedRemote.validate(value);new CombinedStore(a).save(value);ok=true;}catch(Exception e){}finally{session.stop();NativeHttp.CURRENT.remove();}final boolean connected=ok;a.runOnUiThread(()->{if(connected){dialog.dismiss();Toast.makeText(a,"Connected. Reopen the title to load combined streams.",Toast.LENGTH_LONG).show();}else{status.setText("Could not connect. Check that RoDub Instant still works in this profile, then try again.");dialog.getButton(-1).setEnabled(true);}});},"RoDub-private-setup");t.setDaemon(true);t.start();});
  });
 }
}
