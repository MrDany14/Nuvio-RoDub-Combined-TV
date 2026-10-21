import java.io.File;
public final class SavedConnectionDeviceProbe {
 public static void main(String[] args){String value=SavedConnection.find(new File(args[0]));System.out.println("PREFILL_DEVICE_RESULT found="+!value.isEmpty());}
}
