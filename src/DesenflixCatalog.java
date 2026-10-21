import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/** Public DesenFlix title discovery; video resolution remains in ByseClient. */
class DesenflixCatalog {
    static final String SITE="https://desenflix.online";
    static boolean siteUrl(String text){try{URL u=new URL(text);return "https".equals(u.getProtocol())&&"desenflix.online".equals(u.getHost())&&u.getUserInfo()==null&&u.getPort()==-1;}catch(Exception e){return false;}}
    static boolean titleUrl(String text){if(!siteUrl(text))return false;try{return new URL(text).getPath().matches("/[a-z0-9-]+-dublat-in-romana/?")&&new URL(text).getQuery()==null;}catch(Exception e){return false;}}
    String get(String url)throws Exception{return NativeHttp.text(url,Collections.<String,String>emptyMap());}
    static JSONObject metadata(Document page){
        for(Element script:page.select("script[type=application/ld+json]"))try{
            JSONObject value=new JSONObject(script.data());String type=value.optString("@type");
            if(type.equals("Movie")||type.equals("TVSeries"))return value;
        }catch(Exception ignored){}return null;
    }
    static boolean matches(Document page,JSONObject input){
        Element heading=page.select("h1").first();if(heading==null||!RomanianAudio.declaredRomanian(heading.text())||!NativeCatalog.normalize(heading.text()).matches(".*\\bdublat in romana\\b.*"))return false;
        JSONObject meta=metadata(page);if(meta==null)return false;
        boolean series=input.optInt("season")>0&&input.optInt("episode")>0;
        if(!meta.optString("@type").equals(series?"TVSeries":"Movie"))return false;
        int id=input.optInt("tmdbID");if(id<=0)return false;
        String expected="https://www.themoviedb.org/"+(series?"tv/":"movie/")+id;
        JSONArray refs=meta.optJSONArray("sameAs");boolean exact=false;if(refs!=null)for(int i=0;i<refs.length();i++)if(refs.optString(i).replaceFirst("/+$","").equals(expected))exact=true;
        // An explicit TMDB identity binds remakes, translated titles and TV type.
        // A contradictory release year is rejected even when the page ID matches.
        if(!exact)return false;int year=input.optInt("releaseYear");String date=meta.optString("dateCreated");
        return year<=0||(date.matches("(?:19|20)\\d{2}.*")&&date.substring(0,4).equals(String.valueOf(year)));
    }
    static String player(Document page,JSONObject input){
        int season=input.optInt("season"),episode=input.optInt("episode");
        if(season>0&&episode>0){
            Pattern number=Pattern.compile("^sezonul 0*"+season+" episodul 0*"+episode+"(?: |$)");
            for(Element card:page.select(".dfx-episode-card[data-src][data-title]"))if(number.matcher(NativeCatalog.normalize(card.attr("data-title"))).find())return byse(card.attr("data-src"));
            return ""; // Never substitute the default player or next available episode.
        }
        if(!page.select(".dfx-episode-card").isEmpty())return "";
        Element frame=page.select("iframe#dfx-main-player").first();
        return frame==null?"":byse(frame.hasAttr("data-dfx-player-src")?frame.attr("data-dfx-player-src"):frame.attr("src"));
    }
    static String byse(String url){try{URL u=new URL(url);return "https".equals(u.getProtocol())&&u.getUserInfo()==null&&u.getPort()==-1&&NativeCatalog.kind(url).equals("Byse")&&u.getPath().matches("/e/[A-Za-z0-9]+(?:/[^?#]*)?")?u.getProtocol()+"://"+u.getHost()+"/e/"+u.getPath().split("/")[2]:"";}catch(Exception e){return "";}}
    static List<String> searchUrls(Document page){
        Set<String> urls=new LinkedHashSet<>();for(Element a:page.select("main a[href]")){String url=a.absUrl("href");if(titleUrl(url))urls.add(url);}return new ArrayList<>(urls);
    }
    List<NativeCatalog.Candidate> discover(JSONObject input)throws Exception{
        Set<String> names=new LinkedHashSet<>(),visited=new HashSet<>();
        for(String key:Arrays.asList("localTitle","movieName","originalTitle","englishTitle")){String name=input.optString(key).trim();if(!name.isEmpty())names.add(name);}
        int checked=0;
        for(String name:names){String search=SITE+"/?s="+URLEncoder.encode(name,"UTF-8");Document results=Jsoup.parse(get(search),search);
            for(String url:searchUrls(results)){
                if(!visited.add(url))continue;if(++checked>16)return Collections.emptyList();
                Document page=Jsoup.parse(get(url),url);if(!matches(page,input))continue;
                String embed=player(page,input);if(embed.isEmpty())continue;
                return Collections.singletonList(new NativeCatalog.Candidate("Byse",url,"","ROMANA",embed,input));
            }
        }return Collections.emptyList();
    }
}
