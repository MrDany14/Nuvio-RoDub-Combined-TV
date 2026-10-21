import org.json.JSONObject;
final class DurationEvidence {
 static boolean matches(double actual,JSONObject expected){
  if(!Double.isFinite(actual)||actual<90)return false;
  for(String field:new String[]{"durationSeconds","alternateDurationSeconds"}){double duration=expected.optDouble(field,0);if(duration>=90&&Math.abs(actual-duration)<=Math.max(60,duration*.10))return true;}
  return false;
 }
}
