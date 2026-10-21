# RoDub Combined TV

A separate native Nuvio TV test provider combining RoDubbed v14's five website integrations with a private RoDub Instant hosted connection.

## Install

Add this repository URL in Nuvio TV's plugin repositories:

https://raw.githubusercontent.com/MrDany14/Nuvio-RoDub-Combined-TV/main/repo.json

Install or update **RoDub Combined [TV Test]** to version 3. Existing connections remain stored. For a new connection, keep RoDub Instant installed in the same Nuvio profile, then open a title or press the plugin's Test button. The connection is filled in automatically: just press **OK**, then reopen the title. You can disable the separate RoDub Instant provider after connecting.

The plugin reads only the current profile's saved addon URLs (or the primary profile when that profile explicitly shares its addons). It does not edit Nuvio's settings or choose between multiple private connections. If no unique saved connection is available, manual entry remains possible.

The private connection is encrypted with Android Keystore on that TV. It is never included in this public repository or plugin package. No TorBox/NZBGeek/NNTP keys need to be entered into this plugin. The private hosted addon must support `/tvstream` (v0.5.0 or later).

Select this provider's tab to see the combined order. Disable the separate RoDubbed, TorBox Finder and RoDub Instant providers when you want one list without their duplicate entries. Existing installations and catalogs are not changed by this plugin.

## Results and ordering

Both branches run concurrently:

- Website searches/resolvers run on the TV's own Internet connection: DeseneFaine, DozaAnimata, FilmeDublate, DeseneleDublate and DesenFlix.
- Cached torrents, cached Usenet and direct Usenet come from the private hosted addon. Direct Usenet video is delivered by the existing HTTPS bridge.

Version 3 starts a two-second collection window when the first confirmed source arrives, then cancels unfinished work and sorts the collected sources. Website declarations and positive audio metadata qualify; unconfirmed guesses alone do not shorten the search. If neither branch confirms a source, the original 40-second ceiling remains. This favors fast usable results over waiting for every alternative; a slower or higher-resolution source may be omitted. Backend v0.8.0 uses a similar 1.2-second window, a private persistent metadata/audio cache, and bounded MP4 audio inspection. The original standalone RoDubbed plugin and TMDB/catalog lists are unchanged.

Results are ordered by Romanian confidence first, then resolution (highest first), then release quality when known. Exact duplicate URLs and known duplicate file identities are removed. Different usable hosts/transports remain available as alternatives. No artificial resolution is assigned to force a sort.

The confirmed group contains two explicitly labelled evidence types:

- **RO Dub (audio metadata)**: Romanian audio was identified in the media metadata.
- **RO Dub (website)**: the publisher identifies the source as Romanian dubbed; this is not an independent audio-language measurement.

**Possible RO Dub** is placed after that group. A trusted release group or filename can qualify only when audio metadata is unavailable. Known non-Romanian audio is rejected by the hosted addon.

If Nuvio has a custom global stream sort enabled, use its original/provider order to retain the provider's confidence-first ordering. Filtering can still hide resolutions/codecs according to your own Nuvio preferences.

Hosted torrent playback uses a signed, short-lived HTTPS ticket. Playback refreshes the exact file and media link before redirecting. TorBox file IDs are never interpreted as torrent file indexes. Website playback keeps its original HLS flags, referer and required headers.

## Development

The website Java sources were copied from the existing RoDubbed v14 integration, with the internal callback adapted to collect results before sorting. The original plugin repository is maintained separately.

Build requirements: JDK 21 (Java 8 bytecode), Android API 34 jar, org.json, jsoup and Google's D8/R8. Set `JDK_BIN`, `ANDROID_JAR`, `JSON_JAR`, `JSOUP_JAR` and `R8_JAR` to your installed dependencies. Workspace defaults use the sibling `official-plugin/src` dependency jars and `torbox-tv-lab/platform/android-34-ext12/android.jar`. None of the compile-only Android/Kotlin/Cloudstream stubs are packaged.

Run `node build.cjs`. Tests include Java confidence/quality/deduplication and configuration-policy checks, hosted route/ticket checks and a separate Android TV device probe. Live checks deliberately keep account keys, private installer URLs and signed playback URLs out of saved reports.
