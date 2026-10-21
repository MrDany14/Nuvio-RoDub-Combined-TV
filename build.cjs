const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process');
const root=__dirname,src=path.join(root,'src'),out=path.join(root,'build');
const jdk=process.env.JDK_BIN||'C:/Program Files/Android/Android Studio/jbr/bin';
const android=process.env.ANDROID_JAR||path.resolve(root,'../torbox-tv-lab/platform/android-34-ext12/android.jar');
const deps=path.resolve(root,'../official-plugin/src');
const json=process.env.JSON_JAR||deps+'/json.jar',jsoup=process.env.JSOUP_JAR||deps+'/jsoup.jar',r8=process.env.R8_JAR||deps+'/r8.jar';
require('./src/make-compile-stubs.cjs');fs.mkdirSync(out+'/classes',{recursive:true});fs.mkdirSync(out+'/dex',{recursive:true});
const sources=fs.readdirSync(src).filter(f=>f.endsWith('.java')).map(f=>path.join(src,f));
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(f=>f.isDirectory()?walk(path.join(dir,f.name)):[path.join(dir,f.name)]);}
const run=(exe,args)=>cp.execFileSync(path.join(jdk,exe+'.exe'),args,{stdio:'inherit'});
run('javac',['--release','8','-encoding','UTF-8','-cp',[json,jsoup,android].join(';'),'-d',out+'/classes',...sources,...walk(src+'/stubs').filter(x=>x.endsWith('.java'))]);
// Include production classes only; Android, Kotlin, Cloudstream and JSON are supplied by Nuvio.
const names=sources.map(f=>path.basename(f,'.java'));
const classes=fs.readdirSync(out+'/classes').filter(f=>f.endsWith('.class')&&names.includes(f.split('$')[0].replace(/\.class$/,''))).map(f=>out+'/classes/'+f);
run('java',['-cp',r8,'com.android.tools.r8.D8','--min-api','24','--lib',android,'--classpath',out+'/classes','--output',out+'/dex',...classes]);
run('jar',['--create','--file',root+'/RoDubCombinedTV.cs3','-C',out+'/dex','classes.dex','-C',root,'manifest.json']);
console.log('Built',fs.statSync(root+'/RoDubCombinedTV.cs3').size,'bytes');
const list=JSON.parse(fs.readFileSync(root+'/plugins.json','utf8'));list[0].fileSize=fs.statSync(root+'/RoDubCombinedTV.cs3').size;fs.writeFileSync(root+'/plugins.json',JSON.stringify(list,null,2)+'\n');
