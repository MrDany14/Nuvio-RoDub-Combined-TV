// Private installer goes only through child stdin, never arguments, logs or files.
import cp from 'node:child_process';import fs from 'node:fs';import path from 'node:path';import {validMedia} from '../../torbox-instant-addon/src/audio.mjs';
const root=path.resolve(import.meta.dirname,'../..'),env={...process.env,ANDROID_USER_HOME:root+'/android-tv-lab/user',ADB_VENDOR_KEYS:root+'/android-tv-lab/emulator-home/adbkey'};for(const k of Object.keys(env))if(k.toLowerCase()==='psmodulepath')delete env[k];
const adb=root+'/android-tv-lab/sdk/platform-tools/adb.exe',adbArgs=['-P','5039','-s','127.0.0.1:5581'];
const report={version:'0.5.0',backend:[],device:[]};
try{
 const file=(root+'/.credentials/instant-addon-access.dpapi').replaceAll("'","''");
 const token=cp.execFileSync('powershell.exe',['-NoProfile','-Command',`$s=(Get-Content -LiteralPath '${file}' -Raw).Trim()|ConvertTo-SecureString;$p=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($s);try{[Console]::Out.Write([Runtime.InteropServices.Marshal]::PtrToStringBSTR($p))}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p)}`],{env,encoding:'utf8',windowsHide:true,stdio:['ignore','pipe','ignore']}).trim();
 const base='https://rodub-instant-test.vercel.app/'+token+'/';
 for(const route of process.argv.includes('--movie-only')?[]:['stream/series/tt7678620:1:1','tvstream/series/tt7678620:1:1']){
  const start=Date.now(),r=await fetch(base+route+'.json',{signal:AbortSignal.timeout(35000)}),data=await r.json();
  const row={endpoint:route.split('/')[0],status:r.status,count:data.streams?.length||0,elapsedMs:Date.now()-start};
  if(row.endpoint==='tvstream'){
   row.urlsOnly=data.streams?.every(s=>!!s.url&&!s.clientResolve&&!s.tvPlayback);let t=data.streams?.find(s=>s.url.includes('/torrentplay/'));
   if(t){const redirect=await fetch(t.url,{redirect:'manual',signal:AbortSignal.timeout(30000)});row.ticketStatus=redirect.status;
    if(redirect.status===302){const media=await fetch(redirect.headers.get('location'),{headers:{Range:'bytes=0-511'},signal:AbortSignal.timeout(10000)});const reader=media.body.getReader();let value=await reader.read();await reader.cancel();row.mediaStatus=media.status;row.validMedia=validMedia(Buffer.from(value.value||[]));}
    row.tamperedStatus=(await fetch(t.url+'x',{signal:AbortSignal.timeout(10000)})).status;
   }
  }report.backend.push(row);
 }
 const command=args=>cp.execFileSync(adb,[...adbArgs,...args],{env,encoding:'utf8',timeout:65000,windowsHide:true,stdio:['ignore','pipe','pipe']});
 const apk=command(['shell','pm','path','com.nuvio.tv']).trim().replace('package:','');if(!apk.startsWith('/data/app/'))throw Error();
 command(['push',root+'/combined-tv-plugin/RoDubCombinedTV.cs3','/data/local/tmp/combined-tv.cs3']);command(['push',root+'/combined-tv-plugin/build/probe.jar','/data/local/tmp/combined-probe.jar']);
 const cases=process.argv.includes('--movie-only')?[['Despicable Me 4',{tmdbID:519182,imdbID:'tt7510222',movieName:'Despicable Me 4',originalTitle:'Despicable Me 4',localTitle:'Sunt un mic ticălos 4',releaseYear:2024,durationSeconds:5640}]]:[['Bluey S1E1',null],['Turning Red',{tmdbID:508947,imdbID:'tt8097030',movieName:'Turning Red',originalTitle:'Turning Red',localTitle:'Roșu aprins',releaseYear:2022,durationSeconds:6000}]];
 for(const [name,data]of cases){
  const shell=`CLASSPATH=/data/local/tmp/combined-probe.jar:/data/local/tmp/combined-tv.cs3:${apk} timeout -k 2 48 app_process /system/bin CombinedDeviceProbe`+(data?' '+"'"+JSON.stringify(data).replaceAll("'","'\\''")+"'":'');
  const output=await new Promise((resolve,reject)=>{const child=cp.spawn(adb,[...adbArgs,'shell','-T',shell],{env,windowsHide:true,stdio:['pipe','pipe','pipe']});let out='';child.stdout.on('data',b=>out+=b);child.stderr.on('data',()=>{});child.stdin.on('error',()=>{});child.stdin.end(base+'manifest.json\n');const timer=setTimeout(()=>{child.kill();reject(Error());},55000);child.on('close',()=>{clearTimeout(timer);resolve(out);});child.on('error',reject);});
  const line=output.split(/\r?\n/).find(s=>s.startsWith('COMBINED_DEVICE_RESULT '));if(!line)throw Error('Device lookup did not complete');report.device.push({title:name,...JSON.parse(line.slice('COMBINED_DEVICE_RESULT '.length))});
 }
 const all=report.device.flatMap(r=>r.streams);report.status=report.backend.every(r=>r.count>0)&&(process.argv.includes('--movie-only')||report.backend[1].validMedia===true)&&report.device.every(r=>r.success&&r.sorted&&r.uniqueUrls)&&all.some(s=>s.kind==='website')&&all.some(s=>s.kind!=='website')?'verified':'needs-attention';
}catch{report.status='needs-attention';report.issue='Live check failed; private URLs and credentials were not logged.';}
fs.writeFileSync(root+'/combined-tv-plugin/build/'+(process.argv.includes('--movie-only')?'mixed-verification.json':'live-verification.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report));
