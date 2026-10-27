## 2026-10-27 — latest Netseek v9 source sync; Mini validation pending

Owner reported a new v9 commit. Integrating exactly83a6321966786a561d3dddaa51cf9295dce18946
over completed145/ebe351a9 on integration/netseek-v9-20261027-83a63219. One upstream
commit after edc0cc61: roundabout exit selection/ring tracking, straight-on route
continuation, one-way exit filtering and per-part HUD colour opacity (10–100%).
Nine source/test files. One renderer merge conflict resolved by combining upstream
opacity with local RPM-bar colours; default white/redline pair now compares RGB and
keeps selected alpha. Inactive red segments multiply their original alpha once.
Two new JVM integration cases cover defaults/custom/BLUE at50% opacity.

No version bump, public release/OTA promotion or vehicle command for this source sync.
Installed/public145 and its verified automatic CarPlay preparation remain the last
field-validated release. Phone/route test still pending owner. CP/AA controllers,
OEM binaries, themes, contract/bridge and cluster geometry are unchanged.
Full JVM/Debug verification must run only on authorized Mini from audited immutable
commit; no Android/Kotlin compilation on coordinator. Read-only QA in progress.

## 145 completed — published, installed, automatic reboot preparation verified; vehicle OFF

Final application source: 7b5d79b2aeb13073c13860e32f4439b9bd33236d, private
release/impulse-74145-carplay-readiness branch. Version74145 /1.0.0.74.145-preview.
Signed Mini job haval-impulse-133-7b5d79b2aeb13073-20261027T005221Z-22631 passed
full JVM/Release/R8/lint/production certificate in6m6s;10transport JVM tests and
41portable lifecycle/probe tests pass. No coordinator Android build. Latest Netseek
v9 through edc0cc61 included; themes/OEM AA/CP assets preserved.

Exact signed/installed/public APK:104337315bytes, SHA256
 d96beb6834060643e9568f4060f18e8072dcfe389dba4f591fc153d676adb02a.
Production certificate5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0.
Payload74145-cca41dfe4a54-8f807eaa3c7f. Real preparation and automatic preparation
AFTER full Android reboot both verified:9pinnednativehooks, Javaarmed, bothCLIexit0,
matchingpost-exitnonce samples and currentliveOEMidentity. Newbootbdba7dce-ff0a-
432b-b6e7-8e02776bf33c OEM9785/start4044; policy58d3.../system_server1966 unchanged.
No manual enable/reset after that reboot. Canonical controller enforces3sec proof
freshness; passive audit snapshots are within its30sec refresh interval.

GWM OFFaccepted01:08:29UTC27Oct, freshengine0confirmed01:08:44UTC. Vehicle OFF;
automatic prepTRUE preserved for next boot. No five-minute phone/route capture
was started. Owner iPhone navigation/capture/HUD test remains to be performed.
All prior LIVE ON/pending readiness headings below are historical and superseded.

Public release:
https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.145-preview
release424932275. Publictagpoints048749aebcb792ec163c3f7c601b574d75f0e9ba, notprivate
history. Four assets:APK/SHA256SUMS/build-info/component reproduction source tar.
Whole public APK downloaded and hash/sizeverified BEFORE app-latest/app-preview.json
promotion to74145; public manifest readback matches. Stable channel untouched.
Final publication artifacts: originalworkspace/test-artifacts/publish-74145-final-20261026/.
Oldpublish-74145-20261026 is preliminaryonly and marked DO_NOT_PUBLISH.json.
Private operation/evidence: originalworkspace/test-artifacts/release-74145-preflight-20261026/.
Own temporary HTTPS delta directories removed; immutable Mini artifacts/private traces retained.

Next: owner turns on central WITHOUT iPhone, waits Bridge preparada para esta sessão,
then connects USB iPhone, opens Apple Maps/Waze route and starts five-minute test.
Retrieve local logs later when online. Do not claim physical map/HUD route validated.
No further wake/reboot or scheduled action is needed for this completed release.

## FINAL145 fieldPASS — VEHICLE OFFconfirmed01:08:44UTC; publishexactAPKnext

Source7b5d79b2aeb13073c13860e32f4439b9bd33236d exactMiniAPK
 d96beb6834060643e9568f4060f18e8072dcfe389dba4f591fc153d676adb02a,
104337315bytes, productioncertverified. InstalledactualhashPMsuccess. Preflight
passedbothbeforeandafterfullreboot, boundedstdout17.5.2/stderrempty/exit0 andstable
kernelpolicy/framework. ActualautomaticNEWBOOT READYbdba7dce-ff0a-432b-b6e7-8e02776bf33c,
OEM9785/start4044,9nativepinnedhooks+Javaarmed, bothCLIexit0, matchingnonce2samples
perrole andcurrentliveOEMmapping. NOmanualenable/resetafterreboot. Oldboot625eae..
wasalsoREADY. Controllerenforces3secproofsatREADY;passivesnapshots10/9swithin30srefresh.
Evidencecheck_readiness andfield-outcome oprelease-74145-preflight-20261026.

GWM OFFaccepted01:08:29, freshengine0confirmed01:08:44UTC. CURRENT VEHICLE OFF.
AutomaticprepTRUEleftreadyfornextboot, no phone/captureteststarted. Ownerwilldo5min
CarPlayiPhoneroutetestlater. Do NOTclaimphysicalmap/HUDroutevalidated.

Publicrelease/OTA145STILLPENDING, preview144currently. FINALpublishdir originalworkspace
/test-artifacts/publish-74145-final-20261026 containscorrectd96APK, metadata, SHA256SUMS,
componenttar/notes. Oldpublish-74145-20261026 hasDO_NOT_PUBLISH.json (oldaf43prelim).
Nowcreateprerelease145withtagtargetPUBLIC048749aebcb792ec163c3f7c601b574d75f0e9ba,
explicit4assets, NEVERpushprivatehistory. VerifywholepublicAPKhashfirst, promote
app-latest/app-preview.json LAST aftercheckingcurrent144. Stablechannelunchanged.
Thenupdatefinaldocs/privatepush andreport145installed/autoREADY/off+shortteststeps.

## LIVE145 7b5 preflight/currentREADYpassed, fullrebootrequested, VEHICLE ON

ExactAPK d96beb6834060643e9568f4060f18e8072dcfe389dba4f591fc153d676adb02a installed,
source7b5d79b2, payload74145-cca41dfe4a54-8f807eaa3c7f. Preflightactualpassedstdout17.5.2,
exit0, before/afterenvsame, rootprobeoutputbounded/files400. ActualcurrentREADY9hooks/
Javaarmed/CLIs0, currentboot625eae3b-fea1-4209-8c35-f71c28ea7286 OEM14956/start15301,
systemserver1831/policy58d3same. Normalreceiver enabledtrue refreshed(no retry/reset);
proofages11/10secs atlastsnapshot (<30secmanagerrefreshperiod), sourcecanonicalREADY
itselfenforces3sec. check_readiness receipts after-enable-2 andbefore-reboot-fresh.

Oneguardedfullrebootrequested01:05UTC withfreshP/0, marker
/data/local/tmp/impulse-74145-reviewed-d96beb683406/reboot-once. DO NOTrepeat.
VehicleON fromGWMaccepted00:59:43/fresh1at01:00:23, timer~15min (until01:14:43).
WaitnewbootTCP, field-read.sh capturesprivatepreflight-state/firsterror andprobe
start/stdout/stderr/exit; no losingearlybootevidence now. No manualenableafterboot:
mustvalidateautomaticnewbootREADY viahelper. AlwaysGWM OFFfreshverifiedbeforeend.
Latestop originalworkspace/test-artifacts/release-74145-preflight-20261026/.
Publicpreviewstill144/no145release. Localpub145folderold33fartifactprelimonly; replace
withexactfinalartifactafternewbootREADY. OwneriPhone testlater; no phone interaction.

## LIVE145 preflight — VEHICLE ON, must verify GWM OFF before ending

Source7b5d79b2aeb13073c13860e32f4439b9bd33236d signedMini success6m6s, job
haval-impulse-133-7b5d79b2aeb13073-20261027T005221Z-22631, fullJVM10transportPASS,
41portablePASS. Artifact originalworkspace/artifacts/impulse-74145-preflight-release-mini/;
APK104337315bytes SHAd96beb6834060643e9568f4060f18e8072dcfe389dba4f591fc153d676adb02a,
productioncert5efec0...verified. Exact4MBbyte-delta prepared/HTTPSverified. No public145.

GWM ONaccepted00:59:43UTC27Oct, freshengine1confirmed01:00:23. CURRENT VEHICLE ON.
FreshP/0 andheartbeat/TCP23online01:00:54. Readingbefore-install now; noinstallYET.
Operation originalworkspace/test-artifacts/release-74145-preflight-20261026/.
Oldinstalledaf43 autoFALSE fromlastpause; lastkernelbootfailedpreflightunknownnoCLI
retry, but prioraf43actual9-hook/JavaREADY happenedpre-reboot. Newsource isolated
Rootstdin andseparateddurableprobe stdout/stderr/exit/start; keepsfirstprivatefailure
perboot/rev. Newfirstfailureevidence MUSTread ifblocked; no moreblindhypotheses.
Fullpreparedcurrent/newbootproofs stillneeded beforepublish. Userauthorization/openair
persists. AlwaysOFFverifiedevenifblocked. Localpublish145currentlyold33fONLY preliminary;
replace withfinalactualAPKonlyafterpropernewbootREADY, publictagbase048749ae.

## Current145 preflight IO/evidence correction — VEHICLE OFF confirmed00:41:49UTC

Sourceaf43 actualcurrentbootREADY PASS9hooks/Javaarmed/freshnonces. Authorizedfull
rebootconfirmednewbootc02af8dc.., but startupcreatedEMPTYpreflightmarker and no cold
attempt. Underlyingfirstexceptionlostbecauseallreleaselogsstripped. Bothscheduler
threadsidle(notdeadlocked); kernelCRNGinit0.394s; CLIroot500stable/frameworkstable.
DoNOTclaimautomaticbootREADYoradoptold0b5dREADY. No145publicrelease/previewupdate.
Auto prepFALSEverified, GWM OFFfresh00:41:49UTC. OldLIVEONheadingsbelowSUPERSEDED.

Newsourcewritten/reviewed: Rootcompoundstdin/devnull; typednonzeroRootrawprivate;
preflightrootstart/stdout/stderr/exit receipts, childfilelimit<=16KiB +timeout10
within15rootbudget; stdoutonlyexact17.5.2 +unchangedenv required. Missingtimeout
checkedBEFOREmarker. Private firsterrorperboot/rev retained despite30s retries.
No uncertainmarkerreset/reexecution. No claimstdincausedoldCLIproblem (upstream
versionreturnsbeforeApplicationstdinreading). Production/JVMsameframehelperwith
paddedchildstdinregression. 41portablelifecycle/probe testsPASS; 10JVMawaitMini.
cp_141_deadline_review sourceQAclear and frozeonlytestpath. No otheragentwrites.

Newop originalworkspace/test-artifacts/release-74145-preflight-20261026 prepared;
needsimmutablecommit/audit/signedMini145, validateexactartifact and install, then
actualcurrentREADY/fullreboot/newbootREADY/OFF. Captureprivatepreflightdiagnostics
androotprobefiles ifstillblocked; no blindrepeat. Userauthorizesworkflow/openair
alreadyconfirmed. OwneriPhonetestlater. Publictagbase048749ae, neverprivatehistory.
Localpublish-74145 folder has oldaf43APK33f prelimONLY, no publicrelease; mustreplace
withactualfinalverifiedartifactbeforepublish/publicdownloadverify/previewLAST.

## LIVE145 — actual current-boot READY passed; full reboot requested; VEHICLE ON

FINALsourceaf43c4b5 exactAPK33f03e922d19af69a74f9247ce328c47b02215f73d50e428572d381d87f2693d
installedPMsuccess/actualhash. Payload74145-46a3b080567f-b06609794669. Auto prepTRUE.
Actual READYverifiedboot0b5d2189-f7d0-4159-8113-3d8514b44896, OEM16670/start16296,
9pinnednativehooks/Javaarmed, bothCLIexit0, matchingfreshpost-exitnonceproof2samples
nativeages2520/2270ms andJava1360/1110ms. SamepolicySHA58d3ec6d.../system_server1906,
noAndroidrestart. Checkreadiness receipt after-enable-1-readiness.json in opfolder.
This is REAL successful prep, notfixtureorold137. Userphone teststilllater.

One guardedfullcentralreboot requested with freshP/0. Do NOTrequestagainifTCPslow.
Requestscript marker /data/local/tmp/impulse-74145-reviewed-33f03e922d19/reboot-once.
Waitnewkernelboot, collectfield-read.sh, check_readiness.py, no manualenable/reset
needed: bootreceiver must prepare automatically. CURRENT VEHICLE ON; ALWAYS GWMoff
confirmedbeforeend. Latestop originalworkspace/test-artifacts/
release-74145-atomic-challenge-20261026/. GWMtimerstarted00:28:03~15min, ampletime.
Publicpreviewstill144/no145releaseyet. AfternewbootREADY, OFF thenpublish145 exactAPK,
publicdownloadhashcheckandpreviewmanifestLAST. No privatehistorypublictag.

## LIVE145 atomic challenge — VEHICLE ON, must GWM OFF before ending

Latest sourceaf43c4b538116e98782fa1ade622e63421f36d3b signedMini success5m29s,
jobhaval-impulse-133-af43c4b538116e98-20261027T002152Z-16060. FullJVM8transport PASS,
38portable/OEMreglock PASS. APKvalidatedproductioncert/hash33f03e922d19af69a74f9247ce328c47b02215f73d50e428572d381d87f2693d,
104320931bytes; originalworkspace/artifacts/impulse-74145-atomic-challenge-release-mini/.

GWM ON accepted00:28:03Z, freshengine1confirmed00:28:49Z. CURRENT VEHICLE ON.
Operation originalworkspace/test-artifacts/release-74145-atomic-challenge-20261026/
has authorization/openair, power.jsonl/helper, exact4MBdelta plan and scripts.
CarP/0freshbutlocalengineflaglagged and TCP23notyetup00:28:52; WAITstartup, no install
yet. Oldinstalledac7 auto prepFALSE verifiedpreviouscycle. Newsource exactfix mv-f
for repeated mode400 residency challenge replacesinteractiveOEMprompt. Inertfield
2updatespass0.88s; mustactualcurrentbootREADY+reboot+newbootREADY thenGWMoffverified.

Ready helper check_readiness.py checksnewartifactmeta/actualOEMPIDstart/currentboot,
9nativehooks/Javaarmed/bothlaunch-exit/proofbindings; passivedatacanbe35sold due30s
managerrefresh. Needfreshproofcapture ifneededusingnormal OWN config enabledtrue
withoutretry (preservesliveowners), await~2s thenfieldread. No resetting failure
circuit/injectionoverpublishedowners. FullrebootONLYaftercurrentREADY andfreshP/0.
AlwaysGWMoffevenifprepfailed. No public145/OTA yet; preview144. Publishactualartifact
onlyoncephysicalready; publictagtarget048749aebcb792ec163c3f7c601b574d75f0e9ba,
NEVERprivatehistory. Ownerphone testlater, no needconnectiPhonenow.

## CONFIRMED145 cause/fix — atomic challenge replacement; vehicle OFF; Mini next

Sourceac7 private timeout captured EXACT prompt: mv: overwrite native-residency-
challenge.json (Y/n):. Existing challenge is mode400; plain mv prompts/consumes
remaining script stdin so no final frame. Same function for native+Java. Final
functional fix is ONE LINE mv-f for challenge.next->challenge only; no permission,
identity, nonce, deadline or immutable-receipt weakening. Source-extracted OEM shim
regressions added by cp_141_deadline_review to lifecycle file (onlyagenteditpath),
negative control and two native/Java replacements PASS; 38 tests PASS, sourceQAclear.
Actualheadunit INERT fixture preexisting400 + two forced updates returnedend0 in
0.88sec, never touched realprotocol. Root queue hardening/loaderfix retained.

Currentinstalledsourceac7 NOTREADY; nativeexit0/ARMED, stableframework/policy.
Auto prepFALSEverified, GWM OFF fresh00:20:31UTC27Oct. No fullreboot/public145.
Evidence originalworkspace/test-artifacts/release-74145-reader-20261026/ includes
raw timeout in after-enable-1.txt, probe-forced-twice-response.txt and receipts.
Newop release-74145-atomic-challenge-20261026 prepared. Need commit/audit/Mini signed145,
validateartifact, authorizedON/install/enable/currentbootREADY/reboot/newbootREADY/OFF,
then publishexactAPK/previewlast. Userexplicitlyauthorized/openairconfirmed persists.
OwnerwilltestiPhonelater; no needphoneinteractionnow. Do notclaimreadyfrom137history.

## Current 145 reader correction — source reviewed; vehicle OFF; Mini next

Third private145 source0b108ff8 passed fullMini/JVM(6newtests), exact APK installed.
Again failed Manager978 response: wire1728,received11647,beginSeen true. Nativeexit0/
ARMED, policy/system_server1872 stable. Inert exact source fixture oncar ended0 in
0.726s; extra8kchars are mkshredraw. Echo-off fixture did not help, do NOT ship echo
changes. Auto prepFALSE verified and GWM OFF fresh confirmed00:05:43UTC27Oct. No
full reboot/public145. Evidence originalworkspace/test-artifacts/release-74145-console-20261026/.

Narrow reader fix now reviewed/frozen: Commons background soTimeout0; single
consumer polls available and reads only already-present bytes; same outer deadline,
no retry. Eliminates source-established partial-buffer-loss hazard from background
SocketTimeoutException, not yet proven historical cause. Two new JVM cases (8total)
await Mini. Typed timeout stores bounded raw tail ONLY private filesDir, no raw in
shared failure/publicevents. New operation release-74145-reader-20261026 prepared.
Need immutable audit/signedMini, validateAPK, ON/install/enable, actual currentboot
READY then full reboot/newboot READY then OFF. User authorizes this/openair confirmed.
No iPhone interaction now; owner tests later. Never claim ready from historical137.

## Current 145 transport correction — pending review/Mini, vehicle OFF

Root transport now uses CRC-checked gzip/hex packets with same-session append ACKs
for commands over the single-line bound. Patterns are compiled once and scanned
incrementally. No timeout/retry/gate deadline expansion. Six new JVM tests await
Mini; source cp_141_deadline_review doing read-only review. Source0d649c14's field
result below is unchanged: native ARM/exit0 and stable framework/policy, but later
root response timeout and no READY. Vehicle OFF confirmed23:42:28Z, auto prepFALSE.
No public145. Need reviewed immutable commit and signed Mini build, then authorized
ON/install/enable/current-boot READY/reboot/new-boot READY/OFF before promotion.

## Current 145 field result — vehicle OFF, native loads; root confirmation times out

Second private145 source0d649c14, Mini signed Release PASS5m44s, fullJVM passed.
Actual APK104320931 bytes SHA4b848eb95ee67ee368936edbf15e6677d55cb9a704b62dd4d3100137d0cfacbc,
production certificate verified. Private evidence originalworkspace/test-artifacts/
release-74145-policy-preserved-20261026/. Payload74145-4f7ab2940517-cdf07272facd.
Installed exact APK and enabled through OWN receiver. New injector preflight passed;
native CLI exit0 and fresh ARMED resident response passed. SELinux policy unchanged,
system_server1914/start645 and app14475 stable. No framework restart.

Preparation still FAILED: Local root command timed out at Manager978, resident
response root call, inside observe551 -> recoverAttempt686 -> attempt663. Mapping
fetched read-only from Mini, private mapping.txt. Target16812/start15452 ticks;
response samples162260/162510, failure167084. Exact wire/stdout on timeout not saved.
Native eventually released safely after eligibility revoked; no READY/Java launch.
Do not misattribute historical137READY or claim ready. Root transport/response loop
needs investigation. cp_141_deadline_review is doing read-only source review.

Automatic preparation paused via OWN receiver, false VERIFIED; gate empty. GWM OFF
accepted23:42:13Z and fresh engine0 confirmed23:42:28Z. No full reboot requested in
this cycle because preparation failed first. Vehicle OFF; no public145/OTA yet.
User still authorizes finish145 and ON/install/reboot/validate/OFF, open air confirmed.
Need correct remaining issue offline, immutable Mini build and actual READY/new-boot
proof before publication. Preserve separate patched injector policy fix and deadlines.

## Current 145 correction — source reviewed; vehicle OFF; signed Mini build next

The first private 145 failed during native injector initialization: the SELinux
policy was reloaded just before system_server died. Correlation is established;
causality remains unproven. Quiescence itself passed. Automatic preparation was
paused through the app receiver and GWM OFF was freshly confirmed at 23:07 UTC.
No public 145 or preview update exists.

The new candidate keeps the original Frida resource and derives a separate
bootstrap injector on the Mini. A pinned four-byte initializer tail-call patch
omits the redundant policy reload on this already-permissive device. Admission
requires kernel permissive mode, a one-shot version probe, unchanged policy and
framework identity, and the existing full native/Java resident proofs. It never
changes SELinux mode. Full physical attach and new-boot READY remain unverified.
See docs/architecture/carplay-injector-policy-preservation.md.

Focused portable verification: 56 passed and two Mini-only asset fixtures skipped;
OEM regression lock and diff whitespace checks passed. New JVM tests await Mini.
All source/QA agents are frozen. Build only an audited immutable commit via the
signed Mini queue, version 74145 / 1.0.0.74.145-preview. Verify the exact derived
asset, original resource, production certificate, OEM assets and unchanged themes.

User authorizes GWM ON -> install -> central reboot -> validate -> GWM OFF and
explicitly confirmed open air. Build and transport first. Preparation is currently
disabled: enable only after corrected installation, let current-boot preparation
complete before requesting the full reboot, then validate automatic preparation
on the new boot. Always confirm GWM OFF even if field validation fails. The owner
will connect the iPhone later; do not request an interactive test now.

## LIVE145 UPDATE — VEHICLE NOW OFF; pause verified; investigate loader/policy reload

GWM OFF accepted23:06:55Z, freshengine0confirmed23:07:00Z(updateTime1793056019047).
Candidate145installed exactSHA fb1302e6...; rebootnewd550617b/appinitial3131 then12317.
Quiescence PASSED (retainedDCMvalid,TX45NULL,BTabsent). Nativeinjectchild9802start4250,
target9354start3887 exited4at44780ms: The connection is closed. No cold-state,
ARMED/residency/READY. Full systemrootcause read showsSELinuxpolicyreload42.77s,
then system_serverdead/zygoteSIGKILL43.59s, Androidframeworkrestart(app/systemUI died).
NO evidence OOM (2+GBfree). Relation toattach/policyreload NOTyetproven; don'tblameOEM
ambientlightingcrashstormor changeunrelatedvehiclecontrols. The kernelCMDLINE and
getenforce proveSELinuxalreadyPERMISSIVE beforeloader. PossibleunneededFridaautopolicy
reload; investigatepinnedFrida17.5.2source/artifactoffline, notglobalsetenforcechange.

To avoid nextboot repeatingfailedattach, usedOWN appDUMPreceiver explicitextra_enabled
false and clearedpersist.haval.carplay.experimental_cold_hook; both VERIFIED.
Disabledfeaturepreservesothercarapps/CPnative; no reinjection/reset/retry ofthisboot.
Old heading 'VEHICLE ON' below is SUPERSEDED. VehicleOFF, do notclaimready/complete.
UserstillwantsFINISHready145 thenauthorizedON/install/reboot/OFF whenactualfixready.
Currentopenairconfirmationpersists; no needrepeatapproval unlessconditionschange.
Private145evidencefirst-field-outcome.json/pause-preparation.txt/power.jsonl and
system-before-death.txt/system-root-cause-read.txt preserveallknownfacts. No public145
release/previewupdate yet; installedcandidate145needsfixbeforeuserCarPlaytest.

## LIVE145 — VEHICLE ON; installed/reboot requested; MUST GWM OFF before ending

GWM ON accepted22:55:41Z, fresh hyEngSts1 confirmed22:56:07Z. Current locationOPENAIR
confirmed, owner-authorized GWM ON/install/reboot/OFF. RootMcu freshP/0 confirmed
beforeinstallation and reboot. Signed145 built/validated/installed Success, actual
installedSHA fb1302e6e11328fdf2f60c134893a784daf0d459e3ac830dbddab6167ce3a832,
81666859bytes; UID10052, codePathZmjZohvGtPA19W_dXZj78g==, appPID16161 afterinstall.
One fullcentralreboot requested~22:59:23Z with guardoldboot90869374-54bf-4cb9-a973-3c778ffd6dda,
package145and onetimemarker/data/local/tmp/impulse-74145-reboot-90869374.
Do NOT repeatreboot. Waitnewboot thenread productionq_diagnostics/currentproof.

Mini source4cbd9336fd735b3c864d7bcaf0e6b3b4b180413c, jobhaval-impulse-133-4cbd9336fd735b3c-20261026T224758Z-13419 SUCCESS6m11s finished22:54:26Z.
FullJVM/Release/R8/lint/prodcert allPASS;4newJVM+43portable. Buildcontrolsdigest
f6becec6833767d6934be6f9f08015e32b0d6c8bbd334011d31576cbeeb70b05;
archiveSHA d30fdae826ee3d25a3c9dfd323d529e7125b1ea2d29a1aac6b9cf6a471246276.
Fetched originalworkspace artifacts/impulse-74145-readiness-release-mini/;
validator145PASS, cert5efec0..., payload74145-fa1a127f9224-f04082d4143e,
OEM AA/CPassets andthemesunchanged. Sourcebranchrelease/impulse-74145-carplay-readiness.

ExactAPK144->145delta only4MB compressed transferred via reviewedtemporaryHTTPS,
fulltargethashverified BEFOREPM andinstalledbaseAPKverifiedafter. Private evidence
originalworkspace test-artifacts/release-74145-20261026/ has transport-plan.json,
installation-receipt.json, mini-job.json, source-audit.json, power.jsonl, requests,
parkingreceipts. stage/data/local/tmp/impulse-74145-reviewed-fb1302e6e113.
Currentpublicpreview144; no145publicrelease yet. Readiness NOTconfirmed.

Next NOW: afterreboot use field-read.sh through readonlycompressedhelper, save new
boot/read and privateq_reasons. IfREADY mustmatchnewboot/PID/start/rev/freshnative9hooks
andJavaarmed, notold137READY/142failure. Ifstillblocked, exact145qfields nowavailable;
finish actualprep with evidence, neverbypassunknown state/claimsuccess. Then GWMoff
helper node originalworkspace/test-artifacts/release-74145-20261026/gwm_power.js off
(cwd/opt/gwm-bridge/haval-h6-mqtt), verify freshengine0. ALWAYS OFF evenifprepfailed.
Cleanup onlyownpublictemporarytransportdir afteruse. Publish145actualverifiedAPK +
publicdownloadverify/previewLAST oncefieldoutcomehonest; publictagbase048749ae,
neverpushprivatehistorypublic. OwnerwilltestiPhonelater, nophoneinteractionnow.

## ACTIVE145 — source FROZEN and QA clear; signed Mini build next, then field/OFF

User explicitly requests FINISH/GEnERATE ready-to-test CarPlay145; after completion,
GWM turnON, install, central reboot, validate and GWM turnOFF. CURRENT OPEN AIR
confirmed by user. Do not request these approvals again. No GWM/car action in145yet;
lastconfirmedvehicleOFF. Minimize powered time: build/validate/transport BEFORE ON.

Branchrelease/impulse-74145-carplay-readiness fromf691cad0 includes latestv9edc0cc61.
Source frozen, independentQA clear.145 adds same-sample readDetailed diagnostics
through EXISTING DCM/BT readers, preserves Boolean admission/firmware/binds/TX/
shortcircuits/windows. New private Reading/Reason and bounded numeric/bool factmaps,
no names/MAC/serial/path. Manager publishes samequiet/safety, logs q_ fields with
stable dedup, unknownrecord UI no longer claims activephone. No Frida diagnostic
agent, newcallbacks, recovery, P/speed change, AA/theme/resolution/contractchange.
New Reading included in source hash and Gradle inputs; version74145preview.
43 portable tests(32lifecycle+11visibility), OEMlock/diffcheck PASS;4newJVM diagnostics
tests await Mini. Source review cleared; no knownblockingbug; actualREADY unproven.

Next immutablecommit/sourceaudit/buildcontrols review (only145version+newinput),
exactallowlist and SIGNED RELEASE145 viaMiniONLY. Validate actualproductioncert/
version/hash/payload/unchangedOEMassets. Prefer private temporary HTTPS byte-exact
144->145 transport to install candidate BEFORE final publicOTA. No public145yet;
previewcurrently144. Public release/version promotion only after exactartifactchecks,
and describe fieldreadiness honestly. Use new currentboot facts if stillwaiting;
finish actualprep beforeclaiming ready. Owner will iPhone test later, not now.

Operation evidence originalworkspace test-artifacts/release-74145-20261026/ includes
explicitauthorization/location, GWM on/off helper NOTRUN, validate_145.py. Fresh cloud
powerstatus/correctconfiguredvehicle and freshlocalP/0beforeinstall/reboot. One powerON,
onePackageinstall/reboot ifsuccessful, then ALWAYS GWMoff verification evenifprepfailed.
Telnet100.111.14.79:23; helpers install-74144-20261026/run_authorized_script.py and
143-local-services-20261026/run_readonly_compressed.py. Legacycollectlongline truncates.

Delta builder (noAndroidbuild/repack/sign) originalworkspace
 test-artifacts/install-133-persistent-20261025/build_transport_delta.py creates exact
MiniAPKbytes with full localbytecomparison. Old144APKsource/hash known4807d148..., remote
/data/local/tmp/impulse-74144-reviewed-4807d148affc/app.apk or registeredbaseAPK hashverify.
PreviousHTTPSmetadata transport-https.json samefolder; publicroot/var/lib/loca-driver-downloads.
Use new randomtransportdir, onlycompressedAPKliterals; validate full targetSHA beforePM.
No earlier unsafe diagnostic prototype may be executed. Agentfiles FROZEN, noneediting.

## Current handoff — latest v9 integrated and Mini Debug/JVM PASS

Owner requested latest Netseekv9 commits. Fetched exact branch feature/new-screen-enhancements-v9
through edc0cc610cc5540809e6862b1b3d958ab4bb02ae. Two commits after30e301fb:
13e0341b (three direction-arrow modes/uniform blue) and edc0cc61 (HSV custom colours,
compact part chips, plain power ticks). Conflict-free true merge source
5a8f6b18edd41ee01ce00fd32e3e1f587c4aed6b, private branch
integration/netseek-v9-20261026-edc0cc61, pushed origin. Exact functional delta
stablepatchID b606ab3f72aae1124f2aae313533f6f63658b629 matches upstream delta.
Seven source/test files changed. Local RPMbar/default redline, text-only CP guard,
AA recovery, preparation144, themes, bridge contract and resolutions preserved.

42 portable CarPlay regression tests/OEMlock/diffcheck PASS. Audited immutable tree:
1249 tracked files,1145 content files,8 unchanged buildcontrols digest
ffb51eeec752bacd6ee930f17d21b9ba1115bb8833e0d293b6788f2ef68f4f22.
Mini job haval-impulse-133-5a8f6b18edd41ee0-20261026T223542Z-1850 SUCCESS,
full JVM tests/assembleDebug passed2m36s/49tasks, finished22:38:38Z26Oct.
ArchiveSHA e8b4f16ca3ed471285036577de7e6fdcc8ab425b51b2346a7780f9790914aadc.
Fetched NEW originalworkspace artifacts/v9-edc0cc61-debug-mini-20261026/,
checksums verified. DebugAPK SHA98b7aacb7dfdc5ed57a0707de2bf3d237ced39245db1d7d870db0dd870166fde,
bytes157019757; code74144 unchanged. This is Debug VALIDATION, not a
production release. Android instrumentation and physical HUD not executed.

No version bump/publicAPK/OTA/install/vehicle action. Vehicle remainsOFF; do not
wake for source sync.144 quiescence diagnosis remains unresolved and unchanged.
Next release can build from this integration plus reviewed fixes, with a new
version only when requested. Private receipts originalworkspace
test-artifacts/v9-sync-edc0cc61-20261026/. Sync doc
 docs/UPSTREAM_V9_SYNC_2026-10-26_HUD_COLORS.md. Prior investigation follows.

## Current handoff — offline 144 investigation complete; exact branch not captured

Owner requested investigation after confirmed GWM shutdown. No vehicle connection,
wake, reboot, install or power command during this investigation. Production144
unchanged; no Android/Gradle/Kotlin build, new version or release.

Reviewed current boot events + source + pinned OEM DCM/Bluetooth through REA and
independent read-only QA. Current FALSE has three possible origins: online DCM
record outside the narrow retained signature; nonempty active projection list;
Bluetooth aggregate/profile connected/transitioning. The current log stores only
the merged boolean, so WHICH branch occurred cannot be reconstructed. Unknown
record signature is not physical phone proof. UI text overstates that branch.
Binder/decode failures returnNULL, notFALSE; window does not latch oldFALSE.
ConnDevice field order matches archived OEM; no current evidence BLElights cause.
Historical133/134 retained records are not fresh144 measurements.

Report docs/architecture/carplay-preparation-144-investigation.md records evidence,
limits and exact next fields. Prefer granular private reason telemetry in existing
bound production readers; do not bypass guards or publish a speculative fix.
A separate diagnostic JS prototype was syntax/fixture/bundle checked locally but
review found unload/ServiceConnection lifecycle still unvalidated. Archived PRIVATE
ONLY under originalworkspace test-artifacts/quiescence-investigation-144-20261026/
prototype-not-approved/. NOT APPROVED for execution/staging. No collector in app.
Complete REA records and machine-checked investigation-facts.json in same private
evidence directory. Only documentation tracked. Next physical identification needs
fresh detailed DCM/BT data in a later authorized online session. Vehicle remainsOFF.

## Current handoff — 144 rebooted; preparation WAIT; vehicle OFF confirmed

Latest owner explicitly confirmed parked and authorized central reboot, then asked
for vehicle shutdown after validation and deferred the iPhone test. This supersedes
older reboot/power prohibitions below. ONE guarded Android reboot was issued at
2026-10-26 22:03Z. New kernel boot1c592b4f-d99c-438e-9127-63dd6a079193, appPID3345,
144 payload extracted. Automatic manager now produces precise wait events, without
143 OEM AWK crash: fresh observation, cp_visual=false, aa_visual=false,
quiescent=false, reasonCARPLAY_NOT_KNOWN_INACTIVE (22:03:58Z through22:07Z).
No current attempt/runtime/READY. The persisted142 failure and137 READY are historical;
do not mislabel them as144 failure/success. No iPhone/CarPlay test was performed.
Phone absence versus a stale/overbroad OEM connection signal remains UNRESOLVED.
Next offline work: trace quiescence/DCM/BT evidence; do not bypass unknown projection
ownership or claim a phone is connected from this boolean alone. No additional
reboot, restart, disconnect request or speculative release now.

Owner-authorized GWM TURN_OFF sent ONCE22:07:31Z. API accepted, then fresh cloud
hyEngSts changed1->0 at22:07:41Z (updateTime1793052461171 newer than pre-command
1793052441087), verification attempt3. Local preflightP/0 (speed age9.8s).
Vehicle OFF confirmed; do not turn it on again without a new owner request.
Evidence in originalworkspace test-artifacts/reboot-74144-20261026/ includes
boot/prefs/passive logs/stack reads, authorization/reboot marker, power.jsonl and
validation-and-shutdown-receipt.json. Only credentials-free receipts in Git.

## Current handoff — 144 PUBLISHED + INSTALLED; owner reboot/activation pending

Latest owner explicitly authorized remote INSTALL while moving because passenger
coordinates; owner will reboot central when parked. Installation completed with
PackageManager Success/exit0 using `pm install -r --dont-kill`. Installed registry
is74144/UID10052; actual installed baseAPK size/hash match validated Mini artifact.
App PID3133 preserved, kernel boot2044eaa6-bf1c-4bd2-8fb1-280e2365d80a unchanged.
No full central reboot, manual app restart, callback injection, test, radio/task
change or GWMpower command was issued. Old process/code remains running; do NOT
claim144 active/READY yet. Owner reboot is the next step. No more install needed.

143 current photo was genuine genericlocalservices; live EXACTvisibility helper
failed OEM AWK while basics worked. Replaced parser with POSIX shell/grep and
validated actual helper/guard on car BEFORE build:12cases real/hidden/visibleD0D3/
ambiguous/empty/error/nonzero correct, no segfault. Same visual/quiescence/identity
rules retained. Current143 safetyquiescent=false was separate and not bypassed.

Recovered HISTORICAL142boot43a1bd0a: CLIexit0elapsed39160; valid resident proof was
rejected after caller4s deadline at41086 while prior safety was eligible/quiettrue.
Original nonce/samples had been overwritten later; exact initial timing unknown.
144 uses conservative processbirth+9s proof/+10s release, actual sysconfHZ and cap
by overall attempt; same-shell time checks before fork/nonce/release, fresh identity/
quiescence/first sample and strict postreturn deadlines. No actual gate extension,
extra restart or accepting late proof. Old cold-attach beyond bound fails honestly.
Kernel AT_CLKTCK=100 read from target auxv. Receipt/hash/disposal/9hooks+Java guards
remain. V9 through30e301fb, AA recovery, themes and bridge contracts unchanged.

Independent source QA cleared both fixes.11visibility+31lifecycle Python/OEMlock/
diffcheck PASS. Full JVM including22residentproof tests(sixnew), Release/R8/lint/
production signing PASS on authorized Mini ONLY. Exact source1740a814c5df447836f2c9443504e0efeed41868,
privatebranchrelease/impulse-74144-bootstrap-compat. Jobhaval-impulse-133-1740a814c5df4478-20261026T214752Z-30212 SUCCESS5m42s/83tasks,
finished2026-10-26T21:53:50Z. Buildcontrols8 digestffb51eeec752bacd6ee930f17d21b9ba1115bb8833e0d293b6788f2ef68f4f22;
archiveSHA82b77c177831b6697f0cc616692fdff4d00713c67974f2ce5907fe572415eb86. No coordinator Android/Gradle/Kotlin build.
Fetched NEW artifacts/impulse-74144-bootstrap-compat-release-mini/; queueSHAverified.
Actual code/name/cert/payload/controller/helper/feature markers/Default48 and
unchanged OEMAA assets verified. APK74144/1.0.0.74.144-preview, 81650471bytes,
SHA256 4807d148affc0e08ba610be52edcfd72a0c7dfcb7550ddab689fe69ca5c90326
Production cert5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0 unchanged. Payload74144-c8569e79199f-b54fa7c36655.
Validator originalworkspace test-artifacts/install-133-persistent-20261025/validate_144.py.

PUBLIC release https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.144-preview (release424812302).
APK/SHA256SUMS/build-info.json only; public distribution tag uses048749ae, private
source/history remainprivateorigin only. Whole unauthenticated publicAPK download
and GitHubdigest match MiniAPK. TEN unchanged public theme bytes rechecked; one
transient TLS reset recovered on retry. Default48/Minimalist8/ApexGT16 remain at
artifactcommitb8662d26a4285f15f66d68bcee5ec7c7310ac1f0. OnlyAFTERAPKverify did fixed
previewmanifest143→144 update; exact public bytes verified. Stable channel unchanged.
Private publication evidence originalworkspace test-artifacts/publish-74144-20261026/.

Remote downloaded exact publicAPK to/data/local/tmp/impulse-74144-reviewed-4807d148affc/,
verified bytes/SHA before PM. Installer one-shot directory and rc/log retained;
no retry/duplicate PM, no data clear/uninstall. Current installedAPK:
/data/app/br.com.redesurftank.havalshisuku-OfCK4t6eVrN430kf-tXtjw==/base.apk
Its exact SHA above proves production-signed artifact. Private plan/scripts/rc/
package registry/PID/boot/hash receipts originalworkspace test-artifacts/install-74144-20261026/.
Live diagnostics/packs/auxv evidence test-artifacts/143-local-services-20261026/;
use run_readonly_compressed.py for large reads (legacy octal long-line packs failed).

Next: tell owner144installed; reboot central when parked with phones disconnected,
open Impulse and wait"Bridge preparada para esta sessão" before connecting iPhone
USB/test. After owner reboot obtain NEWboot/currentattempt/ReadyRevision and fresh
native+Java receipts; historical137READY and142failure must not be adopted. No more
install or root restart now. Activation/nativeJNI/CarPlay map firstframe stillPENDING,
not implied by package install or visibility test. If waiting, use precise144state
and records, not another speculative build or repeated P/disconnection loop.

## ACTIVE — 144 compatibility fix frozen; install authorized, owner will reboot later

Latest user explicitly authorized remote APK INSTALL NOW despite movement because
passenger is coordinating; OWNER will reboot central when parked. This supersedes
old blanket handoff "no install while moving" for this installation. Do NOT issue
full central reboot/GWMpower. Install only exact validated signed Mini APK, verify
actual installed version/hash and report activation pending owner reboot. pm help
supports --dont-kill (documented for feature splits; do not promise preservation
on a full update without checking old app PID). No install/reboot done yet.

Actual installed143 confirmed read-only, boot2044eaa6-bf1c-4bd2-8fb1-280e2365d80a,
OEM4163. Current attempt prefs remain historical142 boot43a1bd0a; not143 failure.
143 exact helper crashes OEM awk, preventing observe/attempt and causing generic
local-services UI. Basic awk works. Replaced complex parser with POSIX shell/grep;
exact live helper+guard passed12real/fixture cases on OEM, including actual stacks,
hidden, visibleD0/D3, missing/duplicate flags, errors and unknown. No callbacks,
radio/task/process mutation in those checks. Current safetyquiescent=false remains
an INDEPENDENT blocker; parser fix is not proof of READY or phone absence.

Historical142: successfulCLIexit39160, valid resident sample returned after caller
4s budget in41086, eligible/quiescent true before failure. Old nonce was overwritten
later, so original first-sample timing is unknown. 144 uses conservative target
birth+9s proof /birth+10s release (actual sysconfHZ), caps by overall attempt, checks
same-shell before fork/nonce/release and freshness, preserves strict late rejection.
Old cold-attach beyond bound fails without extra restart. Does not enlarge OEMgate.

Independent QA cleared5runtime/test paths.11visibility+31lifecycle Python/OEMlock/
diffcheck PASS;6newJVM budget tests (22residentproof total) await Mini ONLY. Parser
actual OEM compatibility passed BEFORE build. V9/AA/themes unchanged from143.
Private evidence originalworkspace test-artifacts/143-local-services-20261026/.
Use run_readonly_compressed.py for long scripts; legacy collect single-line octal
transport failed on large packs. No Android/Kotlin build on coordinator.
Next immutableaudit/fullJVM signedRelease144, fetch NEWdir/validatecert/hash/payload,
publicrelease+downloadverify/fixedpreviewLAST, then authorizedremoteinstall and
verify. Owner reboot afterwards, phone/projection quiescence and READY stillrequired.

## Current handoff — 143 published and independently verified (2026-10-26)

User requested latest v9 pull/update and reported142 still waiting for phone
absence/services. Two bounded read-only connection attempts and a VPN ping did not
respond. Current142 boot/quiet/task state and precise field cause remain unknown;
no install, reboot, reset, injection, task action or other vehicle mutation issued.
Optional question about current internet/power is unanswered, not approval.
Private report: originalworkspace test-artifacts/142-prep-waiting-20261026/.

Integrated upstream30e301fb00f30bafa0f540206fca48f3e2fe912e, one commit after0fe2969c.
True merge source8e463602d173e0ac764c683e541edf4cb15f2e61 has parents142a956ad64
and upstream30e301fb. Private branchrelease/impulse-74143-v9. New HUD colour tab/
Blue, power arcs/segmented RPM, vehicle EV percentage fallbackV×I, map behind-car
fade and roundabout routes. Additive preferences, existing values retained. Narrow
local fix applies RPM picks/Blue to BARS too, preserving default white/red. Local
CarPlay text-only guard/AA priority and142 AA recovery/receipt fixes remain.
No theme source/artifact or bridge1.0.2/contractv1.0 change; HUD only geometry,
not cluster resolution. Render instrumentation integrated, NOT run on a device.

Found independent source defect: old visual probe counted any projection package
in amstacklist, including explicit visible=false historical tasks. Archived real141
full dump reproduced the false positive. Shared CarPlayProjectionVisibilityProbe
now ignores only well-formed explicitly hidden tasks; visible/ambiguous/malformed/
truncated/error evidence blocks. Same probe runs in observer and final same-root
idle-host guard. DCM/Bluetooth quiescence, fresh identity/safety, process actions,
4s/15s/330s/10s budgets and P/speed removal unchanged. New helper included in payload
source digest and Gradle inputs. Waiting UI distinguishes unavailable/stale evidence,
visible screens and connection reported by central. Private eventcarplay_preparation_wait
deduplicates reason/quiet/visual/age-bucket changes without device identifiers.
This fixes proven source behavior; it is NOT proof of this owner's latest142 cause.

Independent HUD and CP QA cleared source. Root9visibility+31lifecycle Python tests,
OEM regressionlock/MD5 and diffcheck PASS; independent17visibility cases plus archived
real dump PASS. Authorized Mini jobhaval-impulse-133-8e463602d173e0ac-20261026T200101Z-5015 SUCCESS6m9s/83tasks, finished2026-10-26T20:07:26Z.
Full JVM/testDebugUnitTest, Release/R8/lint/signing passed. No Android/Gradle/Kotlin
build on coordinator. Controls8 digest15ebc36fd58d1901c48bbe34cd6d638a8b7126e10207d3993cad90aa575af9fc;
archiveSHAd0238964ecd1ec5f6a60cb82787638e7ce240d1034d52915c84975620c31ed5d.
Fetched NEW artifacts/impulse-74143-v9-visibility-release-mini/, queue SHA verified.
APK74143/1.0.0.74.143-preview, 81649207bytes, SHA256:
3aecc77b8d20c2df5d0bc086a497fd3c5590d44892af4245bd912244aaae0faf
Production cert5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0 unchanged. Actual code/name/cert,
Default48HTML/metadata, complete payload/controller/helper digest, new visibility/
HUD/retainedAA markers and unchanged OEMAA assets verified. Payload74143-1963436eab95-9ae6ffb10165.
Validator originalworkspace test-artifacts/install-133-persistent-20261025/validate_143.py.

PUBLIC release https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.143-preview
Release424708739, APK/SHA256SUMS/build-info.json only; distribution tag uses existing
PUBLICmaster048749ae. No private source/history pushed public. GitHub digest AND
entire unauthenticated public APK download match MiniAPK. TEN public theme files
rechecked, unchanged artifactcommitb8662d26a4285f15f66d68bcee5ec7c7310ac1f0:
Default1.4.48/Minimalist1.0.8/ApexGT1.0.16. No new theme build/catalog push needed.
Only AFTER public APK verification did fixedapp-latest/app-preview.json advance142→143;
exact public manifest bytes independently verified. Stable channel unchanged.
Private publication/build/queue/OTA/full public download evidence:
originalworkspace test-artifacts/publish-74143-20261026/.

No release work remains. Owner installs when parked via Informações→BuscarAtualizações;
for CarPlay preparation restart central with phones disconnected, wait READY, then
USB iPhone/primary CarPlay/test. Last owner-confirmed installed142, not143. Physical
143 HUD/roundabout/boot and CarPlay JNI/map-first-frame remain pending. If waiting
persists collect current boot/attempt/preFailureSafety and carplay_preparation_wait
before another speculative build. Do not blame BLE/phone from generic UI or ask
repeated P/disconnect/reboot loops. No remote installation/reboot while moving.

## 2026-10-26 — 143 source frozen for signed Mini validation

Merged upstream v9 commit30e301fb over142 and preserved its AA recovery/CP receipts.
HUD QA cleared additive colours/arcs/RPM/EV percent/map changes and narrow RPM bar
colour correction. CarPlay QA cleared shared visibility probe: only explicit,
well-formed hidden projection tasks count as inactive. Unknown or visible tasks
block. Observation and final same-root guard use identical parsing; DCM/Bluetooth,
identity, eligibility, actions and deadlines remain unchanged. Source-only false
positive was reproduced with archived full141 stackdump; current142 cause unknown.
WaitUI now distinguishes missing/recent evidence, visible UI and central-reported
connection. Private wait logs deduplicate enum/boolean/age-bucket combinations,
without device identifiers. Helper included in payload digest and Gradle inputs.

Root9visibility+31lifecycle Python tests/OEMregressionlock/diffcheck PASS. QAalso
checked17independent cases and actual archiveddump. JVM and signedRelease pending
MiniONLY. New HUD render instrumentation is integrated but not executed on a device.
Themes/bridge/contracts unchanged. No vehicle writes or new field validation.

## 2026-10-26 — 143 integrating new v9 and investigating142 preparation wait

Owner requested newv9pull/update, then reported installed142 still waitingforphone
absence/projectionservices (samegenericUI). Telnet andVPNping boundedreads didnot
respond; optional currentnetworkquestion pending. No car mutations/retestrequests.
Do not infer connectedphone or claim actual142fieldcause without currentevidence.
Ownerreport private originalworkspace test-artifacts/142-prep-waiting-20261026/.

Newupstream30e301fb00f30bafa0f540206fca48f3e2fe912e (singlecommitafter0fe2969c)
mergedwithoutconflicts over142a956ad64 onrelease/impulse-74143-v9, notcommittedyet.
IncludesHUDcolors/Blue, circularpowerarcs/segmentedRPM, realEVpercentfallbackV×I,
3Dmapbehindcar/fade/roundaboutroute. Prefs8colorfields additive; no bridge/theme
contract changes, no clusterresolution change. Retains localtextonlyCarPlayguard,
rawAApriority and142runtimefixes. QAclear onintegration; narrowRPMBARScolorfix
added(model/renderer/3JVMcases) tohonornewpicks/Bluewhilepreservingwhite/reddefaults.
ThemesDefault48/Minimalist8/ApexGT16 unchanged, nofrontendbuild/catalogpushneeded.

FoundindependentCPsourcefalsepositive: observe/warmkillguard countALL projection
tasks inamstacklist includingexplicitvisible=false. Archivedreal141AapActivity is
hiddenandstillcounted. Thiscanholdpreparation despiteclosedUI; notprovenaslatest
142fieldcause. Preparing sharedstrict visualtaskprobe: onlywellformedexplicitfalse
may beignored; visibletrue/ambiguous/truncated/error blocks. DCM/BTquiescence,
identity, safetygate, process actions anddeadlines remain. Honestwaitmessages/
boundedprivatewaitreason diagnostics alsoinprogress. No bypass ofrealconnections.
IndependentQA reviewingparser/fixtures; notfrozen/validatedyet.

Next freezeafterreview, source31+newfixtures/OEMregressionlock, immutablecommit
andaudit, fullJVM/signedRelease143 onauthorizedMiniONLY, verifyAPKproductioncert/
source/payload andpublicdownload, thenfixedpreviewmanifestLAST. NoVPSAndroidbuild.
Currentpublicrelease/OTA remains142 while143workinprogress; installationownerlater.
PhysicalHUDnewgeometry/roundabout,143bootstrap andCPmap/JNI validation pending.

## Current handoff — 142 published and independently verified; physical test pending

Owner confirmed141 installed and full AAmap disappearing when leaving primaryUI.
Readonly currentbootad748b68-f097-42b1-99e5-0aa8772113dd captured privately in original
workspace test-artifacts/141-aa-disappears-20261026/. No install/reboot/retry/reset/
injection/power or other vehicle mutations issued. Owner can drive/leave normally.

AA provenfailure12:05:02.622: ClusterFramePump CapacityExceededException (queuefull,
cleanstreamrestartrequired), sessionACTIVE andmapdemandtrue. Controller ignored
FAILED afterclearing surfaceAttached; ownerrouteoff/on restoredLIVE12:05:18.967.
142 adds bounded recovery for knownpostLIVE decoderfailure, preserves rawsession/
route/demand andstrict clientledger authenticatedRELEASEDbefore newoutput. Same
2-attemptbudget/60sbackoff; epoch/demand/surface guards, unknownownership/quarantine
cancel recovery. Does not modify OEMAAAPKs, queuesize, theme/contract or geometry.
Primaryvisibility asqueuepressuretrigger remains ownercorrelation, not proven.

CP141 samebootfailedelapsed36706 waitingforadmittedCLIreceipts while preFailureSafety
36428eligibletrue/quiescenttrue; exactexit0was36310. NativepeerlaterARMED/pinned/no
failure, noJava launch. Source race reproduced in actual141shell: initialpendingflag
ignored validexitarrivingduringattestation.142 classifies validated capturedsnapshots;
missingrequires admitted/attestedstartup, disappearingpairfatal, owner/mode/size/hash/
identity/exit0/noncepostexit stillstrict. Same4s/15s/330s/10sgate, noextrainjection,
no P/speedlockreturned. Fieldmidprobeordering compatiblebutunrecorded, notproven.

IndependentQA cleared both fixes.31Python lifecycle/receiptfixturesPASS(sixnew),
OEMCarPlayregressionlock/MD5PASS. FullJVM including14AAStreamHealth(sixnew) and16
residentproof tests passed on authorizedMiniONLY; Release/R8/lint/signingPASS.
Exact audited sourcec4ca65230392d1d188903f1731a86a2ea37f0e04, privatebranch
release/impulse-74142-runtime-recovery. Jobhaval-impulse-133-c4ca65230392d1d1-20261026T151409Z-12491
SUCCESS5m57s/83tasks finished2026-10-26T15:20:27Z. ControlDigest08acd40f4112d3914ad94bb574fc7cac00744c01958ca7025c33ba17cd011724;
archiveSHA75c0b72f081d876f7b28bbd7b3b16468dd13c3bc771961001e8841b4469ee09c.
FetchedNEW artifacts/impulse-74142-runtime-recovery-release-mini/; queueSHAverified.
APK74142/1.0.0.74.142-preview, 81634091bytes, SHA256:
baa0d1c7f331b857904ecc3f334c4bd3b6317fe7ab3b6b2fc835d86278d33a1a
Productioncert5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0 unchanged. ActualAPKcode/name/cert,
controller/payloadhashes, runtimefixmarkers, Default48HTML/metadata and unchanged
OEMAAassets independentlyverified. Payloadrevision74142-ccf0d703f687-042cab399608.
No coordinator Android/Gradle/Kotlin build. Build log confirms fullJVM executed.

PUBLICreleasehttps://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.142-preview
Release424415773; APK/SHA256SUMS/build-info.json only. Distributiontag uses
existingPUBLICmaster048749ae; private source/history remainedprivateorigin only.
GitHubassetdigest ANDentire unauthenticatedpublicAPKdownload matchMiniAPK. TENpublic
themebytes rechecked againstsource, unchanged141 artifactcommitb8662d26a4285f15f66d68bcee5ec7c7310ac1f0.
Default48/Minimalist8/ApexGT16 unchanged; no themerebuild/catalogpush. OnlyAFTERAPK
verified fixedapp-latest/app-preview.json advanced141→142, exactpublicmanifestbytes
independentlyverified. Stablechannel unchanged. Publicationreceipt/buildinfo/queue/
fullpublicAPK/OTA proofs: originalworkspace test-artifacts/publish-74142-20261026/.
Validator originalworkspace test-artifacts/install-133-persistent-20261025/validate_142.py.

No releasework remains. Ownerinstallwhenparked viaInformações→BuscarAtualizações.
AA checklater: startaroute, leaveprimaryAAUI withoutendingroute; mapshouldrecover
knownqueuefailure automatically, no needrestartnavigation. Actual142fieldbehavior
pending. ForCPtest restartcentralphonesoff, wait"Bridge preparada para esta sessão",
theniPhoneUSB/primaryD0/route/five-minutetest. IfprepFAILS collectactualboot/attempt/
preFailureSafety/times/receipts beforeanotherbuild; do not askrepeatedparkingloops
orblameBLE/phone fromgenericwaiting. Lastconfirmedinstalled141, not142. Do not
remoteinstall/reboot whiledriving. Successful CarPlaymap/JNI firstframeunproven.

## 2026-10-26 — 142 frozen for signed Mini validation

Independent QA cleared both runtime fixes after terminal ownership cancellation
and strict missing-snapshot admission edge cases were covered. Root ran31Python
lifecycle/receipt fixtures PASS (sixnew), CarPlay regressionlock/OEMMD5 PASS and
gitdiffcheck PASS. AA StreamHealth now14JVMtests(sixnew), execution pendingMini.
No unchangedtheme rebuild or OEMAPK regeneration needed. Immutable source audit,
fullJVM/signedRelease andartifact checks NEXT. No Android/Kotlin build onVPS.

## 2026-10-26 — 141 field evidence captured; 142 runtime corrections in progress

Owner confirmed141 and later full AA map disappearance when leaving primary AA UI.
Current readonly bootad748b68-f097-42b1-99e5-0aa8772113dd, OEMCarPlay7699/start3032.
Complete 2,093,894-byte daily log prefix SHA1e2799f6... and filtered logcat saved in
originalworkspace test-artifacts/141-aa-disappears-20261026/ with incident-summary.
AA12:04:24LIVE ->12:05:02.622 ClusterFramePump CapacityExceededException (queue full,
clean stream restart required), FAILED while sessionACTIVE/demandtrue. Owner route
restartoff/on ->12:05:18.967LIVE. Failed callback clears surfaceAttached, so old
healthloop cannot recover. Queue trigger by primary visibility is ownerreported,
not independently proven; decoder failure and missing recovery are established.

CarPlay sameboot failselapsed36706: Resident proof deadline expired: Admitted CLI
has not completed its birth/exit receipts. preFailureSafety elapsed36428 remained
eligibletrue/quiescenttrue. Matching nativeROOT400 birth+exit0(elapsed36310) exist;
postfailure resident response42800/43050 ARMED/pinned/no failure; noJava launch.
Native eligibility failure follows cancellation. Source race: initialmissingflag
latched before attestation/read ignores exit arriving during sameprobe. Actual
midprobe ordering is unrecorded; source race is compatible, not fieldproven.

142 branchrelease/impulse-74142-runtime-recovery combines narrow independent fixes:
AA bounded known-postLIVE decoder recovery through existing authenticated release
ledger, demandepoch/cancellation/backoff/budget; no newOEMAPK/geometry/theme/protocol.
CP uses validated captured receipts to decide pending; no weaker receipts/identity,
no newinjection and unchanged4s/15s/330s budgets/Pspeedremoval. QA/tests stillpending.
No car writes, install, reboot, reset, injection or vehiclepower operation issued.
Owner can drive/leave; logs saved. Publication/build pending;141 remains public.
Next freezeafterQA, sourcechecks, immutableaudit, signedMiniONLY fullJVM/Release,
validate cert/payload, then authorized previewpublication142; owner installlater.

## 2026-10-26 — New owner photos: preparation waiting; remote diagnosis unavailable

After141 publication owner supplied paused-after-failure screenshot, then clicked
retry and twice saw "Aguardando telefone desconectado e serviços de projeção livres".
This is NOT READY and does not prove a phone is connected. Owner confirmed installed141 in Informações. Currentboot and exact failure
remain unconfirmed. Bounded
readonly Telnet capture timed out; TCP23/5555 and Tailscale ping gave no response,
although VPN control status advertises peer online. No current failure payload or
preFailureSafety could be read. No installation/reboot/retry/injection/car write,
no speculative code change/build/publication.141 remains published and verified.
Private evidence: originalworkspace test-artifacts/141-photo-waiting-20261026/.
Next restore reachability, obtain actualversion/currentboot/cpBridgeLastFailure+
preFailureSafety and currentattempt evidence; do not blame BLE/phone from generic
UI or repeat parking/reboot loops. Experimentaltest should wait for realREADY.

## Current handoff — 141 PUBLISHED/OTA verified; field validation still pending

Owner's140 screenshot after restart showedinterruptedprep. Readonly live capture
confirmed140/currentbootb753f2ea/OEM8544-start3549. Birth/exit0worked, nativepeerwas
stillresident/fresh but statefailed0/pinnedfalse/eligibilitylost; app reportedmissing
nativeproof. NoJava launch, noPhoneUSBobserved. Fieldcause/order remainsUNKNOWN:
oldfailure code overwrotepriorpacket. Never blamephone/Bluetooth or claim140READY.
No vehicle mutation, retry, injection, install, reboot or power command was issued.
Finalorderingquerytimedout; no morecarcalls. Owner was told hecanleave/testlater.
Privatefieldsummary: originalworkspace test-artifacts/140-photo-interrupted-20261026/.

141 addresses independently verified SOURCE premature-abort paths: ARMEDJS before
C++SOmapping, and missing/unfinished proof treated asfatal after one shortpoll.
TypedPending stays in SAMEticket/attempt/deadline; knownARMED/noSO canwait, startup
receipts pending onlywith just-admitted/attestedsupervisor. Terminalidentity/hash/
receipt/disposal/nativefailure/unknownphase/foreignmapping staysfatal. Noncereuse
prevents two-sample starvation. Bothsamples/BOTHroles revalidatedjointlyfresh after
finalidentity check; noREADYafterdeadline. Existing4s/15s/330s budgets andquiescence
remain; noextralaunch/unload/retry, no P/speed gate restored. Bounded4096parsed/
whitelistedpreFailureSafety captured UNDERlock BEFOREcancel/overwrite, withtimes.
No nativeJS/helper/protocol/lifecycle change. IndependentQA no remainingblockers.

Includes previously validatedNetseekv9 fourcommits through0fe2969c: HUDarrows/range/
2linetrip, themeTBTswitches andpostturnjunctiontail. Keeps140OEMbuttons/ACfocus/
bridge1.0.2/contractv1.0/catalog discipline. Default48/Minimalist8/ApexGT16 compiled
packages alreadypassed Node/46focus/byteparity; all are now published for141.

Exact signedMini SOURCE e6b81fa8215bb2d123a03c932379f4354d15d02b, privatebranchrelease/impulse-74141-v9.
Jobhaval-impulse-133-e6b81fa8215bb2d1-20261026T091527Z-14193: SUCCESS5m44s/83tasks, finished2026-10-26T09:21:26Z.
FullJVM including16residentproofcases, Release/R8/lint/production signing passed.
25Python lifecyclecases andCarPlayOEMregressionlock passed. No Android/Gradle/Kotlin
build onVPS. One SSHstatusquerytimedoutnearR8; queuecontinued andlaterfetchsucceeded.
APK74141/1.0.0.74.141-preview, 81634083bytes, SHA-256:
00b5760d78933e694f2d0a5e0ec4a3c120b94c99966225717d620bd1438ec5e7
Certificate5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0 unchanged productionidentity.
Payloadrevision74141-93d2184c1e33-763540956e51. ExactAPKcode/name/cert/source/payloadhashes,
Default48HTML/manifest bytes andretainedACfocus JSmethods verified.

PUBLICrelease https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.141-preview
Release424048097, APKasset677643701; APK/SHA256SUMS/build-info.json uploaded.
GitHubdigest ANDcomplete unauthenticatedpublicAPKdownload matchMiniAPK. TENpublic
compiledthemefiles independentlyverified; artifactcommitb8662d26a4285f15f66d68bcee5ec7c7310ac1f0
fastforwardedfrompublicfeb9a001 historyONLY, no privatecode/history/sourcepublished.
OnlyTHEN fixed app-latest/app-preview.json advanced140→141 and exactpublicbytes were
independentlychecked. Stablechannel unchanged. Private sourcebranch onlypushed to
PRIVATErafaelcs28/haval-impulse; distributiontagusesexistingPUBLIC048749ae pattern.

Privateevidence under /root/projects/haval-impulse:
- artifacts/impulse-74141-pending-proof-release-mini/: APK/buildlog/metadata/queueSHA/
  validation.json. Newfetchdirverified.
- test-artifacts/publish-74141-20261026/: fullpublicAPK, before/afterOTA, verified
  publicationreceipt, buildinfo/queue, notes, checksums andTENthemeSHA/sizeproofs.
- test-artifacts/theme-catalog-74141-20261026/: cleanpubliccompiled-onlyGitclone.
- test-artifacts/install-133-persistent-20261025/validate_141.py.

Next OWNERinstall141 viaInformações→BuscarAtualizações WHENconvenient. Restartwhole
centralwithphonesdisconnected, wait"Bridge preparada para esta sessão", THENiPhoneUSB/
primaryCarPlay andtest. UpdateinstalledMinimalist fromthemeUIifused; Defaultisbundled.
Do NOT holdowneratcarforbuild oraskrepeatparking/disconnectionquestions. No root
remoteinstall/rebootauthorizedbylatestphoto. Installedlastconfirmed140, not141.
Actual141boot/JNI/map remainPENDING, fieldcausalorderunknown; neverclaimphysicalfix.
IfnewfailurecollectpreFailureSafety+times BEFOREsuggestinganotherbuild. 138+ clients
normallyexit; neveruse137externalPIDownercriteria. No releasework remains for141.

## 2026-10-26 — 141 fix ready for immutable audit and signed Mini validation

Follow-up to owner's140 interrupted-preparation photo. Resident launch/birth and
eternalized peer worked in observedboot; failed140state was preserved without any
vehicle mutation. Exactfieldcause/order unknown because oldcode overwrote safety.
Independently demonstrated source defects are addressed in141: ARMED beforeC++SO
mapping and temporary peer response delays no longer immediatelycancel sameattempt.

TypedPending is limited to verified not-yet-published/unfinished responses and
explicit just-admittedstartup. Valid persistentattempt/adoption and ARMED/noSO stay
within existing4s/15s/330s deadlines/ticket; no new injector/retry/unload. Invalid
identity/hash/receipt/disposal/nativefailure or foreignmap/unknownphase remains
terminal. Pendingnonce kept underboundedlease to avoid two-sample starvation. Both
samples for BOTHroles must remain jointlyfresh after finalidentity check before
READY/clientrefresh, and lateafterdeadline results are rejected. Quiescence/publish
cancellation never relaxed; no P/speed requirement restored.

Prior safety packet is captured bounded4096bytes, parsed/whitelisted, UNDERlock
BEFORE cancellation/writefailure, with failuretimes. SourceQA has no remaining
blockers; six newfocusedJVM regressions plus existing adjustments await MiniONLY.
Finalsource/lifecycle checks and controllerfreeze pending; do not stagewhileagent
stillchecks. Versiondefaults141, latestvalidatedNetseek changes inherited. No UI or
nativeJS helper/protocol change. Afterfreeze onlyone auditedsignedMiniRelease, then
artifact/productioncert/source validation, approvedpublicAPKandcompiledcatalog
publication andindependentchecks; fixedpreviewmanifestLAST. Ownerinstalls later.

## ACTIVE — 140 field failure captured; preparing141, no more vehicle operations

Owner installed140 and restarted, newphoto "O preparo foi interrompido; sessão atual
preservada". Readonly live query confirmed code74140, bootb753f2ea-415f-4028-b6b6-
35547e098a7a, OEM8544/start3549. Captured private evidence originalworkspace
 test-artifacts/140-photo-interrupted-20261026/field-summary.json plus initial/runtime
files. ONLYreadcommands issued; no install/reboot/kill/reset/retry/injection. Final
ordering/logcatquery timedout; no morecarcalls. Owner was told no needwait atcar,
leave experimentaltest off, preserve session. Do not ask anotherpark/disconnect loop.

140 firstpassed139 birth barrier: native-launch andexitROOT400 receipts PRESENT,
child9025/start3726, exactsamebinding, exit0 elapsed38820. Nativepeer remainsresident,
freshnonce2samples disposingfalse in SAMEOEMidentity, but its currentstatefailed0/
pinnedfalse with "Automatic eligibility lost before native preparation"; appfailure
"Native resident proof unavailable during preparation". NOJava launch. CurrentUSB
onlyroot hubs, noPhoneUSB; do NOT blameBluetooth/phonefromicon. Historicalready137
prefs irrelevant. Currentattemptticketfc54f1a2-9779-4980-ac77-7e51451749d0, revision
74140-ed95f3e47037-2d6479ac8ae2. FieldorderingUNKNOWN becausefailAttempt overwrote
prior safety packet. Nativefailure could be consequence of earlycontrollercancellation.

Source defects verified: (a) aftervalidpre-gateARMEDJSproof, C++SO maynotbemappedyet;
observe onlyasksresidentnativeproof whenmap==bundlebridge, recoverimmediatelyfails
null. (b) one1.2speerpolltimeout collapsesnull→failAttempt→gatefalse→nativeeligibility
failure. No proof either caused THISfieldattempt specifically. Do not claimfixphysical.

Preparing141 under ongoing projectfix/publication authorization; ownerinstalls later.
Branchrelease/impulse-74141-v9 fromvalidatedlatestNetseekintegration8557890d. Root
bumpedgradledefaults74141/1.0.0.74.141-preview. Controller_pid_fix owns manager/proof
andfocusedJVMtests: typedPending onlyexplicittemporarilymissing/unfinishedresponse,
samedeadline/ticket/noextrainjection, terminalidentity/receipt/dispose/nativefailed;
retainpendingnonce,handleARMEDnoSO,guardforeignmap/unknownphase, preservepreFailure
safetypacket. QA persistent_bridge_qa reviewing; NOTfrozen/committed/built yet. Avoid
blindtimeoutincrease or weakenedquiescence/READY. Pendingadoptionroute andjointfresh
native+Javaproof areQA requirements. NOAndroid/Kotlinbuild onVPS; allMiniONLY.

Root alreadyprepared:
- originalworkspace test-artifacts/install-133-persistent-20261025/validate_141.py
  checks productioncert,code141,payloadsource,Default48andbothACfocusmethods.
- test-artifacts/publish-74141-20261026/release-notes.txt,theme-artifacts.json.
- test-artifacts/theme-catalog-74141-20261026/ iscleanclonePUBLICcompiled-onlybranch,
  localartifactcommitb8662d2 (parentfeb9a001...), TENapprovedfiles(Default48/Minimalist8/
  ApexGT16). NOTpushedyet. No privatecode/history public.
Currentpublicrelease/OTA/catalog remain140 whilefixisbeingworked. LatestNetseek4
commits are alreadyintegrated andvalidatedDebug inbase;141willinclude them.

Remaining: freezefocusedfixafterQA/tests prepared, updateprovenance, immutablecommit+
/tmp/haval133-approve-build.py audit/allowlist, signedMini141 viawrapperONLY, fetchNEWdir,
validateAPK, publishversionedprereleaseassets toPUBLICapp-toolrepo usingexistingpublic
commit048749ae fordistributiontag. Publishcompiledcatalogbyfastforwardonly; independently
verifyTENpublicassetbytes+entirepublicAPKsha, THENfixedpreviewmanifestLAST. Private
sourcebranchONLYtoPRIVATEhaval-impulse. No carinstall/reboot. Finalmustexplain140failed,
141fixsourcevalidatedbutboot/mapphysicalpending; usercaninstallwhenconvenient.

## Current handoff — Netseek v9 pulled, merged and validated (2026-10-26)

Request "Teve update na v9 do netseek. Veja e puxa" COMPLETED as source integration.
Branch integration/netseek-v9-20261026, based on private published140 tip8bf470b3.
True merge commit 4f7741531caa16005daf28474cc1efd5c4dc9434 has parents8bf470b3 and exact upstream
0fe2969c94faf3ee485222a8a619613abd1f53b8; all four newer upstream commits are ancestors of HEAD.
Local refs/remotes/netseek/v9 now points to0fe2969c. Keep release140/publicAPK/OTA/
themes-preview as published; no new production version was requested or published.
No vehicle query, install, reboot or power command was issued.

Integrated four updates since5ccd91e2:
- fbae25f1 andf9a2c857: HUD next-turn default threshold1km (existing saved preference
  honored), larger two-line trip display, Sempre arrow follows the mapped road and
  uses the measured angle for soft curves.
- 34eb92c6: global/theme switches for the turn-by-turn card, enabled by default;
  inactive theme directions only, keeping rawHUD/radar data. Scoped Default and
  Minimalist option leaves independent radar alerts visible.
- 0fe2969c: post-turn route tail ends at the next real junction, excluding driveways,
  minimum5m and maximum120m, instead of guessing the route beyond that junction.

Preserved140 OEM steering backup/short-default policy, globalACfocus bridge1.0.2,
private source/public-artifact boundary and current catalogue URL. No CarPlaybootstrap,
experimentalnative/controller, ServiceManager, BasicSettings or workflow changes.
CarPlay uses the shared HUD visual changes; its local text-only/unknown-maneuver
AR guard/test and AndroidAuto precedence remain intact. Contractv1.0 and oldtheme
API/keys remain compatible. Six merge conflicts resolved through source regeneration
and retaining BOTH local text-only and new upstream soft-turn tests. Default48 and
Minimalist8 packages prepared; ApexGT16 unchanged. Never overwrite published47/7
packages with these new bytes under the same versions.

Found and fixed an actual upstream bug during integration: republishing the same
raw directions was suppressed before the theme visibility filter. Shared pure
ThemeNavigationDirectionsPolicy and pre-dedup effective comparison now deliver
on/off/on immediately while preserving raw bus/HUD/radar and otherkeydedup. Five
new JVM regressions passed in the full suite, including the real HUD arbiter and
alias/non-navigation preservation. No new lifecycle/listener behavior.

Validation COMPLETE on authorized Mac Mini ONLY:
Jobhaval-impulse-133-4f7741531caa1600-20261026T083243Z-6913, exact audited merge source above; SUCCESS2m34s/49tasks,
finished2026-10-26T08:35:35Z. FulltestDebugUnitTest and assembleDebug passed.
DebugAPK fetched into a NEW directory, queue SHA256SUMS verified; signature validates,
actual bundledDefault1.4.48/bridge1.0.2 and rawHTML bytes match source; retainedACfocus
JS methods and newvisibility policy are present. Debug artifact only, not for OTA.
APK SHA-256 ab1d6d1dc87e3dfdff53039b53b7fe63c2e81eabdc93f50965138e3c23000512, 156872297 bytes.
NormalNode builds forDefault/Minimalist and aggregate npmcheck passed, including46
ACfocus scenarios; independentQA checked all6 source/dist/APK/catalog parity groups.
CarPlay regression lock/OEM hashes and gitdiffcheck passed. No coordinator Android,
Gradle or Kotlin builds. No physical HUD/newtoggle/map validation claimed.

Private evidence relative to /root/projects/haval-impulse:
- artifacts/netseek-v9-20261026-debug-mini/: APK/build.log/SHA256SUMS/validation.json.
- test-artifacts/netseek-v9-20261026/integration-receipt.json: exact commits, queue
  archive/control digests, checks and artifact hashes.
- test-artifacts/build-default-v9-20261026.log, build-minimalist-v9-20261026.log,
  check-themes-v9-20261026.log.

Next only if requested: choose a NEW release number, signedRelease on Mini, validate
and publish matchingAPK plus compiled Default48/Minimalist8 catalogue assets using
existingartifact-only branch discipline. Never publish private source/history.
The current owner request requires no further build or publication work.

## Current handoff — Netseek v9 source integration, Android Debug validation next

Latest request2026-10-26: "Teve update na v9 do netseek. Veja e puxa". Integrate
source on integration/netseek-v9-20261026, based on published140/private8bf470b3.
No new version number or release APK requested. Keep published140 APK/OTA and its
matching publicthemes-preview catalogue frozen. No car commands or installation.

Fetched and true-Git-merged Netseek v9 exact0fe2969c94faf3ee485222a8a619613abd1f53b8,
old incorporatedancestor5ccd91e29bd2eafe651d4c072e13e59567801f49. Four commits:
fbae25f1/f9a2c857: HUD nextturn default1km, larger2line trip, Sempre chevron follows
road/softturnangles. Existing stored thresholds retained; HUDmodel also serves
CarPlay but localtextOnly/unknownmaneuver guard retained.
34eb92c6: optional global themeTBTcard control and scopedDefault/Minimalist option,
both defaultenabled; onlytheme-facing directions become inactive, rawHUD/radar
unchanged. 0fe2969c: afterturn route ends atnext realjunction (min5m, max120m,
driveways ignored) rather than inventing path through an unknownnextturn.

Resolved six conflicts by normal source regeneration and keeping BOTH localtextOnly
and upstreamsoftturn tests. Preserve140PR165 OEMbuttons, PR166focus/bridge1.0.2,
private-source/publiccompiledcatalog boundary and allCarPlaybootstrap/workflows.
Defaultadvanced1.4.47→1.4.48, Minimalist1.0.7→1.0.8; ApexGT1.0.16 unchanged.
NormalNodebuilds of both themes and aggregate npmcheck PASS, including46focuscases.
IndependentQA confirmed6source/dist/APK/catalog byte-parity groups.

Mapper found/fixed a real upstream integration bug: republishingidenticalrawJSON
was dropped by InstrumentProjector2dedup BEFOREThemeBridge masking. Newpure
ThemeNavigationDirectionsPolicy centralizes themevalue; dedup compares that while
forwarding originalraw payload toexistingbridge/HUD/radar. FiveJVM regressions cover
samepayload on/off/on, duplicates, hiddenupdates/latestrestore, initialread, realHUD
arbiter andunrelatedkeys. No newlisteners/lifecycle. Backcompatreview+CarPlayguard
PASS; fullJVM awaits authorizedMacMiniDebug ONLY, not local Kotlin/Gradle.

Next: commit audited merge, Mini Debug/fullJVM, inspectresult/artifacts, updatefinal
handoff andpush integrationbranch toPRIVATEorigin rafaelcs28/haval-impulse. Do not
publish a newAPK or update publicthemes/OTA as part of sourcepull. PhysicalHUD/map/
newtoggle integration remains untested. Published140 remains last verified release.

## Current handoff — 140 PUBLISHED: PR165/166, matching themes and OTA verified

Latest request: "Da pra puxar e gerar140?" COMPLETED. Owner installs himself;
no car queries, remote installation/reboot, service or vehicle power operation.
Told him 140 is available in Informações → Buscar Atualizações. If using Minimalist
or ApexGT, also update the installed theme from the theme screen. Default is bundled.

Exact reviewed public PRs ported into our private v9 source, preserving local UI:
- PR165 head688b99571a327f5cf33d00db64c25beda24fc75f: OEM steering selection preserved
  with default short/custom double/long; durable backup before capture, rereads and
  write confirmation, overlap warnings. OEM double/long overlap and missing-backup
  limitations remain documented; no dispatch/stealth/timing changes.
- PR166 head1228f84c2e134a22a284545bfd3e9b7824c6ffeb: additive bridge1.0.2 global AC
  focus read/save using existing lastClusterAcConfig. Restore before render; off/
  unknown telemetry cannot reset target; restore/save do not issue HVAC commands.
  Older bridge1.0.0/1.0.1 themes remain supported; contractv1.0/layout unchanged.
No upstream AI docs or GitHub Ubuntu Android build workflow imported. CarPlay code
and all its strict139 residence proofs are unchanged. Neither PR fixes CarPlay boot.

Built immutable private source 3dcbc3b1fd2d080dac20a8da0442f79224b5d182, branchrelease/impulse-74140-v9.
Authorized signed Mac Mini job haval-impulse-133-3dcbc3b1fd2d080d-20261025T225103Z-4717:
SUCCESS5m47s,83tasks, finished2026-10-25T22:57:05Z. Full JVM task, signedRelease/R8/
lint passed. Queue source scanned1139 text files; exact control/archive digests in
private publication build-queue.json. No Android/Gradle/Kotlin build on coordinator.
APK74140 /1.0.0.74.140-preview, 81616399 bytes, SHA-256:
5588b006d48a0192e0bcf37483daa41a3d476662c0e560595a71e8f6488e7dea
Production certificate unchanged: 5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0.
Controller/payload digests and actual signed APK Default HTML/manifest bytes,
version1.4.47/minBridge1.0.2, plus both new JS interface method names verified.
Payload revision 74140-ed95f3e47037-2d6479ac8ae2.

Three theme source builds passed. Aggregate npmruncheck, six focus helper tests,
46 integrated focus scenarios, fullApex checks and independent source/dist/APK/
catalog byte-parity passed. First pre-build aggregate run saw expected oldmanifest
parity; regenerated by normal builds and rerunPASS. Updated versions preserve our
newer local base: Default1.4.47, Minimalist1.0.7, ApexGT1.0.16, minBridge1.0.2.
No pin/hash or source change to legacySport. Independent QA completed without blockers.

PUBLIC prerelease https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.140-preview
Release423713746; APKasset676354297. APK, SHA256SUMS and build-info.json published.
GitHub assetdigest AND complete unauthenticated HTTP download match MiniAPK. Only
then fixed app-latest/app-preview.json advanced139→140; exact public manifest bytes
independently verified. Stable channel unchanged. Private source branch pushed only
to rafaelcs28/haval-impulse, never to public distribution repository.

Dynamic-theme delivery is complete, not just private compiled files:
ThemeManager now uses rafaelcs28/haval-app-tool-multimidia:themes-preview,
cluster-widgets/Themes/v1.0. Published a NEW clean orphan Git repository containing
exactly TEN approved compiled package files; no private source/history or workflow.
Artifact commit feb9a001e19cd5f6ffeb76f14efc30bbdcbac239. All ten public raw file bytes independently
verified against their approved SHA/size. Legacy Sport catalogue remains upstream
preview, separately pinned. Existing139 still uses its old catalogue; new packages
require bridge1.0.2. A hidden customThemeRepoUrlProd could override downloads, but no
evidence owner has one; do not preemptively refactor unrelated preference handling.

Private receipts under /root/projects/haval-impulse:
- artifacts/impulse-74140-pr165-pr166-release-mini/: signedAPK, build.log,
  release metadata, checksums and validation.json.
- test-artifacts/publish-74140-20261025/: build-info/queue, release notes,
  SHA256SUMS, public-download-74140.apk, before/after fixedOTA and
  published-release.json; theme-artifacts.json contains all ten SHA/size proofs.
- test-artifacts/theme-catalog-74140-20261025/: clean artifact-only publicGit tree.
- test-artifacts/install-133-persistent-20261025/validate_140.py.
- test-artifacts/build-default-140.log, build-minimalist-140.log,
  check-themes-140.log. Artifact initial-root diff reports normal existing generated
  HTML final blank lines; bytes deliberately retained identical to build outputs.

Physical140 steering/AC integration and139/140 CarPlayboot/JNI/map remain untested
on vehicle. Last installed version independently confirmed was owner138; do not
claim140 installed or currentcarREADY. Owner can update at convenience. For later
CarPlay validation, wholeheadunit restart afterupdate with phones disconnected,
wait Bridge preparada para esta sessão, then iPhoneUSB/primaryCarPlay/route/5mintest.
Use current boot identity/proofs, not old137 external-owner criteria or historical
failed preferences. No further build or release work remains for140.

## Current handoff — 140 integrates upstream PR165/166; signed Mini build next

Owner explicitly requested "puxar e gerar140". Branch release/impulse-74140-v9,
based on private139 plus its publication/review docs. Ported exact public PR165
688b99571a327f5cf33d00db64c25beda24fc75f and PR166
1228f84c2e134a22a284545bfd3e9b7824c6ffeb (common base2d4e32e6). Imported production
logic/tests/docs only, preserving local UI and v9 changes, omitting upstream AI docs
and external Android CI workflow. No car/installation/reboot operation authorized
or performed; deliver a signed preview for owner to install himself.

165: preserve live OEM choice when shortDEFAULT; synchronous durable backup before
99 capture, reread race guards, write confirmation, two per-button overlap warnings.
Eight bounded files ported and reviewed; source contracts preserve stealth/dispatch.
166: additive bridge1.0.2 get/save global AC focus using existinglastClusterAcConfig;
restores before render, keeps selected target through off/unknown power telemetry.
Existing methods/keys/layout/contractv1.0 unchanged, legacySport untouched. No HVAC
commands during restoration/save. The preference still uses apply (acceptance, not
synchronous disk proof). All source/package versions advanced from our newer base:
Default1.4.47, Minimalist1.0.7, ApexGT1.0.16, allminBridge1.0.2.

Theme npmci (ignore-scripts), three normal theme builds, aggregate npmruncheck,
ApexGTfullchecks and six helpertests PASS. Aggregate first ran before generatedfiles
and failed expected oldmanifest parity; after builds rerunPASS. Fullsource/dist/
APK/catalog HTMLandmanifest parity independently reviewed. QA: additive bridge
backwardscompatible, existingUIretained, CarPlayandworkflowdiff139EMPTY. Full JVM
and AndroidsignedRelease must run ONLYauthorizedMini; not oncoordinator.

Delivery: actual prior catalog pointed NETSEEKv9, so private artifacts alone would
not reach owner. ThemeManager fixedcatalog nowpoints PUBLICdistributionrepo
rafaelcs28/haval-app-tool-multimidia branchthemes-preview/cluster-widgets/Themes/v1.0.
Only TEN compiledpackagefiles will be published in a clean artifact-only Gitbranch:
Default{index.html,theme.xml,thumbnail.png},minimalist{app.html,theme.xml,thumbnail.png,
car-bg.png},ApexGT{app.html,theme.xml,thumbnail.png}. NEVERpushprivatebranch/history
public. LegacySportcatalog/hashallowlist unchanged; hostcatalogtest adapted. After
signedAPKverification publishcompiledcatalog, versionedAPK release, independent
publicchecks, thenfixedpreviewmanifestLAST. Sourcebranch goesPRIVATEhaval-impulse.
No new issue/PRcomment to others. Owner normalthemeupdate needed forinstalleddynamic
packages; Defaultisembedded. Existinghidden customThemeRepoUrlProd mayoverride
packagedownload (noevidenceownerhasit); do not expand into unrelatedrefactor.

Next: finaldiff/sourceaudit immutablecommit, submit74140/1.0.0.74.140-preview signed
Mini, fetchNEWdirectory, validatecert/version/controllerandpayload/themeassetdigests.
PhysicalCarPlayboot/JNI/map remains pending from139;140doesnotclaimtofixthatpath.

## 2026-10-25 — Read-only review of upstream PRs 165 and 166

Owner asked to look at bobaoapae/haval-app-tool-multimidia PRs 165 and 166. Reviewed
metadata, changed-file patches, discussions and exact-head fork CI via GitHub
connector. Both OPEN/DRAFT, individually mergeable against upstream preview
2d4e32e6be039d6e741c5f764bf72ab34b92ede4. No discussion comments returned.

- 165 head 688b99571a327f5cf33d00db64c25beda24fc75f preserves live OEM button choices
  when short is DEFAULT, even with custom double/long. Requires successful synchronous
  backup commit before capture, rereads before writes, confirms writes. Limitation:
  custom double/long can still trigger OEM actions; a missing original backup cannot
  reconstruct a choice already overwritten by 99. Settings still has no atomic CAS.
- 166 head 1228f84c2e134a22a284545bfd3e9b7824c6ffeb adds optional bridge 1.0.2 methods
  for the existing global AC focus preference. Restores before component setup;
  telemetry/power changes no longer reset focus. Adds compatible Default, Minimalist,
  ApexGT packages; restore/save does not itself issue HVAC commands. apply() acceptance
  is not a synchronous durable-write guarantee. Dynamic theme packages also need update.
- Exact-head fork CI VERIFIED success, both theme regressions and Android debug build/
  JVM test job steps: runs 40232996153 and 40233024618. No local tests or Android builds
  were run for this review, and historical hardware claims were not independently tested.
- Neither correction is present in 139 (host bridge remains 1.0.1; current button policy
  still captures for any configured gesture). Neither addresses CarPlay preparation.
- Read-only git apply --check of selected production patches found real integration
  differences: PR165 BasicSettingsScreen; PR166 Default/Minimalist manifests. Our
  Default is already 1.4.46 (PR1.4.44), Minimalist1.0.6 (PR1.0.5). Port logic while
  retaining local changes and advance theme versions, never replace with older packages.
  Checks covered 4 production files for165 and16 for166, not missing generated HTML.
- No merge, source changes, new release, installation or car command authorized or made
  by this review. Recommended candidates for a future integrated version. On integration,
  preserve local AI docs, build policy and CarPlay source; review external CI workflow
  separately rather than importing the GitHub Ubuntu Android build path implicitly.

139 remains published and independently verified, with physical boot/JNI/map validation
pending as detailed below. Source tree was clean before documentation-only review entry.

## Current handoff — 139 published and independently verified; owner free to leave

Owner is leaving for home. Told him to use normal CarPlay and leave the experimental
test off. No remote install, reboot, service kill or vehicle power command was
issued during this follow-up. He will install when convenient; do not ask him to
wait at the car or repeat another roadside preparation attempt.

Release 1.0.0.74.139-preview is PUBLIC and the fixed preview OTA now points to 139:
https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.139-preview
Release ID 423702178; APK asset 676301027. GitHub SHA-256 and a complete independent
unauthenticated HTTP download match the signed Mini artifact. The fixed
app-latest/app-preview.json was updated LAST and its public bytes match the local
checked manifest. Stable channel unchanged. APK/checksum/build metadata only were
published to the public distribution repository; source remains PRIVATE.

Exact built source b663935a906929f65456e5fced70ddde2122f278, private source branch
release/impulse-74139-v9. Signed Mac Mini job:
haval-impulse-133-b663935a906929f6-20261025T222402Z-10596
Full JVM suite (including ten residency proof tests), Release/R8/lint and production
signing passed in 6m9s, 83 tasks. APK code 74139 / 1.0.0.74.139-preview,
81,615,171 bytes, SHA-256:
8be913d17a86dae010ab277366bfa6e76f006d90421d99344c56c2257a267716
Production certificate unchanged: 5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0.
Payload revision 74139-57570b56ed98-2d6479ac8ae2. Exact controller and payload hashes
checked against reviewed source. Five real Linux supervisor fixtures passed,
including immediate child exit and delayed parent capture. Shared peer unchanged;
previous real Frida callback/trampoline/dispose checks still apply, not Android JNI.

139 replaces the missing launch-receipt race with a pre-exec birth barrier. It
records actual child PID/start and a root-owned atomic receipt before releasing the
exact command to exec, retaining PID/start. Both receipts require declared command
evidence and the exact generated supervisor SHA. Existing fresh post-exit resident
proofs and dispose rejection remain mandatory; P/speed are still absent from prep.

Private artifacts/evidence, relative to /root/projects/haval-impulse:
- artifacts/impulse-74139-birth-release-mini-fetch2/ (APK, validation.json, build log,
  release metadata and verified queue checksums). First fetch destination was empty;
  retry into a new directory completed successfully. Never reuse an existing fetch dir.
- test-artifacts/publish-74139-20261025/ (full public APK download, before/after OTA,
  published-release.json recording both independent checks, checksums and build info).
- test-artifacts/install-133-persistent-20261025/validate_139.py.

Car still last confirmed on owner-installed 138. Actual 139 boot, Android JNI and
secondary map/frame remain PENDING. The 138 current waiting guard cause is still
unidentified; USB showed only root hubs, no phone USB. Historical missing receipt
belongs to boot a83e95b3, not the later waiting boot 24f1efa0. Do not infer a connected
phone from UI alone or call an old persisted failure a new attempt.

Next field session: owner installs 139, restarts the whole headunit with phones
disconnected, opens Impulse with automatic prep on and waits for "Bridge preparada
para esta sessão". Only then connect iPhone USB, confirm normal primary display 0
CarPlay, choose a route and run the five-minute experimental test. If not ready,
collect current boot/target/receipt evidence before proposing another build. External
injectors normally exit in 138/139; old 137 PID-owner criteria are invalid. Read app
challenges passively; never overwrite them during manager work. No map success claimed.

Commands: mac-mini-build fetch JOB NEW_DIR; validate_139.py APK --source-commit SHA
--release; gh release create/edit/upload using notes file; independent curl downloads;
Python SHA/size/certificate/source and manifest comparisons. No coordinator Android build.

## Current handoff — 139 fixes missing birth receipt; owner leaving, no car wait

Ownerinstalled138 HIMSELF and manuallyrebooted. Firstphotoafterrestart showedfailed;
hepressedretry, secondphotoWAITINGphone/projectionfree. Latest "Vamos rápido ...
preciso ir embora pra casa". Told himGOHOMEusingnormalCarPlay, experimentaltestOFF;
wecollectedneededlogs andcontinueOFFLINE. Do NOT askanotherstop orremoteinstall/reboot.
No furthercarops planned; no criticalservice/HUpower mutations issued thisepisode.

Confirmedinstalled74138 bypackageXML. Currentkernel24f1efa0-6751-43b4-9b40-7a7016ec8806,
OEM4203, noexternalclients(expected138), currentquiescentFALSE/WAITING andresetcleared
AttemptPhase/AttemptBoot/circuit. USBONLYroot hubs: noPhoneUSB. Thus donotblameUSB
orclaimBluetoothlightsprovedconnected; remainingprojectionguardcauseunidentified.
PersistentLastFailure belongs to PREVIOUSboota83e95b3-4645-47ca-bebe-70b5917cb1ef,
target18139/start20735. Exact138runtime native-launch.json ABSENT; native-exit.json
root400 haschild18332/start0/exit0/exitElapsed209300, ARMED0/pinned nativefile andlog.
This provesreceiptadmissionfailed, notthatCLI-e orJNIfailed. Neveradoptzero-start.
CARreadonlyprocFILE/stdin/pipeSHA matched89c4..., disprovingstat0checksumhypothesis.
Private138-first-owner-boot/usb-and-package/launch-failure/proc-hash*.txt retained
originalworkspace test-artifacts/install-133-persistent-20261025/.

139 replacesliveargvscanwithpreEXECbirthbarrier: capturechildPID/startwhilewaiting,
atomicROOT400launchreceipt, thenROOT400uniqueinstancereleasefile, THENexec exacttyped
CLIcommand (samePID/start), wait/exitatomicreceipt. commandEvidence=declared-before-
exec-v1 plus actualSupervisorSHA mustmatchexpectedgeneratedsource BOTHreceipts;
roleScriptSHAcheckedbeforefork. Heavy52MiBCLIhash isALREADYverifiedinstallPayload
BEFOREcoldwindow, notduplicatedinside. Allpostexitnonce/dispose/native9/Javaarmed
proofs unchanged. P/speedremainremoved; phone/projectionguardkept. No newerbridge
script/helper/nativeAPIchange. Nativeclientmissing isNORMALafter-e, noterror.

Mapper/controller/QA reviewcomplete; decisive5hostshellfixturespassed including
immediate/bin/true andparent200msdelaybeforestat(oldpatternlosesPID), missingroleSHA
andfailedbirthwriteneverreleaseexec. TenJVMtests prepared forMiniONLY. SharedJSpeer
unchangedfrozenfc5f06aa... andrealFrida17.5.2/nativecallback/trampoline/send/dispose4
cases from138 remainvalid. NoVPSAndroidbuild. Sourceworktree133,branchrelease/
impulse-74139-v9; defaults74139/1.0.0.74.139-preview. Needfinalimmutableaudit+single
signedMini139build, validate/cert/source digest. Currentpublicpreview138stillpublished
andmatchingchecksum; ownerhasstanding"Pode publicar que instalo". Once139validated,
publishversionedpublicprereleaseAPK/checksum/info toPUBLICapp-toolrepo, independently
verifypublicAPK, thenupdatefixedpreviewmanifest LAST. KeepCODEinPRIVATEhaval-impulse;
no privatecodepushpublic. Ownerinstalls/rebootslater; do notholdhimoncarforbuild.

## Current handoff — 138 PUBLISHED/OTA VERIFIED; owner downloading on car

Latest owner: "já tô baixando aqui pelo carro ... abrirPulse, esperarpreparar,
conectariPhone, depoisiniciarteste?" AnswerYESWITHmandatoryfirststep: AFTERinstall138
restartwholeAndroidcentralwithphonesdisconnected, openImpulse/automaticoptionON,
wait"Bridgepreparadaparaestasessão", THENiPhoneUSB andprincipalCarPlayDISPLAY0,
AppleMapsroute, IniciartesteCarPlay, returntoprincipalCarPlay;5minautostop.
Ownerinstalls/reboots HIMSELF now. Do NOT install/reboot remotely or sendGWMpower.
No furtherpermission question needed. No physical138success/firstframeclaimed.

FINALsignedMiniAPKsourceef26864ff3f0f1d17c0dd2ae59cd79fe15603ddc SUCCESS5m33s/83tasks
jobhaval-impulse-133-ef26864ff3f0f1d1-20261025T215416Z-15985. FULLJVMpassedincluding9
newtests, Release/R8/lint/signingallPASS. APKcode74138/name1.0.0.74.138-preview,
size81615171, SHAe702d2d788ca0b5ae1aba846f1ef9c11f782c8c7c5cd35c5e6ce1986a6e9db9e,
certificate5efeunchanged. SourcecontrollerdigestANDallpayloadhashesverified;
revision74138-cdd0c0a4f94c-fa15c1bedf3c;residencyProtocol1/injectorModeeternalize.
Artifactoriginalworkspaceartifacts/impulse-74138-resident-release-mini/validation.json.
Preliminary668APKsuperseded, NOTpublished. No newbuildrequired.

PublicprereleaseANDfixedpreviewmanifest PUBLISHEDandindependentlyVERIFIED:
https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.138-preview
APKassetimpulse-74138.apk, release423689881, asset676247235. GitHubdigestANDcomplete
unauthenticatedHTTPdownloadsha matchsignedMiniAPK. Onlyafterthatfixedapp-latest/
app-preview.json updated131to138; downloadedmanifestmatchesexactlocalbytes.
AssetsalsoSHA256SUMS+build-info.json. SourcePRIVATEhaval-impulsebranchpushedprivately;
no privatecodepushedpublic. Publictagusesexistingpublicmastercommit048749ae,
followingprior131distributionpattern. No extraGitHubAndroidbuildtriggered.
Publicationreceipt: originalworkspace test-artifacts/publish-74138-20261025/
published-release.json; fullqueue/validationreceiptinstall-133-persistent-20261025/
build-138-queue.json. No rootinstallation or on-carcommands thisturn.

138 IMPORTANT: externalcpbridgeclients intentionally EXIT(-e); no PIDowner is
requiredaftertransfer. DO NOT use oldinspect_137.py todeclare138failed because
pidofcpbridgeisempty. Newreadyrequires2freshpost-exitchallengesamples natively
installed9/pinned/noFailure ANDJavaarmed. Bothnativearmed0andinstalled9 areaccepted
beforegateack tosupportnaturalcoldattach; actualREADYstillrequiresinstalled9.
Script.pinprotectedJavaBEFOREcallbackpublication andnativepinremain;dispose revokes
residenceproofwithoutunpin/revert. Preserveanyunknown/legacymappedcallbacks.

Forfielddiagnosis READcurrentboot/OEMpid+start+installedAPKversionfirst. Readper-role
launch/exitrootreceipts, app-ownedchallenge+responsefiles PASSIVELY (workerrefreshes
readiness~30s); neveroverwriteappchallengefilesduringpreparation/observation.
Lastpeerresponsescanbeolderthan3sbetweenchecks; stalefilealoneNOTproofscriptdied.
Old137LastFailurepref maypersistafter138success; currentidentity/revision/phase and
residentproofsdeterminehealth. 137oldboot28c59failurewasafterOWNERmanualreboot.
Test actual138boot + AndroidReceiver REVOKE/REQUEST/REVOKE and principalD0CarPlay
before claimingnewJNI/binding/mapvalidation. Linuxrealtests are NOTAndroidJNI:
4cases passfrozenhelperSHAfc5f06aa... inclactualNativeCallback/replaceFasttrampoline
and send() AFTERclientexit, plusordinarydetach+pinneddispose rejection.53Python,
27Node+actualpinnedJavaDEXsignature and4supervisorshellfixtures alsoPASS.

## Publication authorized — owner will install138 himself

Latest owner: "Pode publicar que instalo. Vou no carro agora". Publish138 once
FINALef26864ff3f0f1d17c0dd2ae59cd79fe15603ddc MiniAPK passes all verification.
Do NOT install/reboot remotely now: owner explicitly takes installation himself.
Finaljob haval-impulse-133-ef26864ff3f0f1d1-20261025T215416Z-15985 RUNNING; JVM
already passed, Release stillbuilding. Controldigest
 a493fad51b5d92fda269ecb3f7095f8fbd32d334a4374142ef728ca7bfd5c760
archiveSHA200f7fcae3b6a1e2f02678888b3b6f821289f4fab68ba2eb8b85f3cd27152452.
Preliminary6687f240 job was superseded by ef26864f: accept proven alreadyinstalled9
aswellasarmed0 beforegate acknowledgement, preservingvalidcoldattach timing.
Do NOT publish preliminary668 APK even though tests pass. Fetchfinal into NEW
artifacts/impulse-74138-resident-release-mini. Validator138 private prepared.

Publication destination is PUBLICrafaelcs28/haval-app-tool-multimidia, existing
pattern versionedprerelease1.0.0.74.138-preview assetimpulse-74138.apk thenLASTupdate
fixedapp-latest/app-preview.json. Sourcecode repo rafaelcs28/haval-impulse isPRIVATE;
DO NOT pushprivatecode intoPUBLICfork. Prior131publicrelease targetmaster; use
publicexistingcommit048749aebcb792ec163c3f7c601b574d75f0e9ba forpublictag, attach
onlyverifiedAPK/checksum/buildmetadata. Publicforkworkflow triggersONLYpushmaster/
preview orworkflow_dispatch; versionedrelease doesnottriggeranotherAndroidbuild.
Existingmanifest downloadedprivatepublish-74138-20261025/app-preview.json:131.
Releasebodypreparedrelease-notes.txt. GHCLIusedbecauseGitHubconnectordoesnotoffer
release/assetpublication. PublicAPKdownloadSHA mustmatch BEFORE updatingfixedOTA;
thenindependentlydownloadfixedmanifestwithcachebusterandverifyexactbytes/version.
No extra approval requested; owneralreadyauthorizedpublication. Stablechannelunchanged.
Afterinstall ownermustrestartcentralwithphonesdisconnected (current137mapping
cannotbehotreplaced), wait138ready thenconnectiPhone/principalD0. Physical138notyet
validated. Do not promise firstframe/map/type111. No vehiclepowercommand.

## Current handoff — 138 resident agents implemented; Mini build pending, car offline

Owner CONFIRMED manually restarted central after137. This is automatic bootstrap
failure after deliberate reboot, not evidence of spontaneous whole-HU reset.
Current installed137/boot28c59 remains failed/mapped; no on-car changes this turn.
Last telemetry stale15+minutes, gear/speedunknown; Telnet timeout. Do not reboot,
reset/reinject/kill existing mapped session. P/speed remain removed from PREPARATION.

138 removes external injector lifetime dependency with SAME pinnedFrida17.5.2
raw ELF (bdfa97cdd84ca678c5e5eea9eb6625c9528acff32b4f9413f1e5c58a3d15222e).
Exact binary has17.5.2/-e help strings. Native+Java launch-e under detached one-shot
supervisor, recordactualargvSHA/PID/start/exit atomically, thenrequire fresh challenge
AFTERclientexit with2advancingsamples fromresidentOEMagents. NativeARMED proof
BEFOREcoldgaterelease; READY requiresnative9/pin/nofailure plusJavaarmedproof.
New helper impulse_residency_peer.js and pureKotlin CarPlayResidentProof.kt. Peer
validatesactualprocidentity, compiledpaths/scriptSHA; dispose terminallyinvalidates
proof evenifordinaryunloadwaitsforpin. Java wrapper now pins BEFOREautoarm/callbacks;
no dispose/failure unpin. Existing pins/mappings, DCMquiescence, tickets, attempt
circuit and AndroidAuto isolation remain. Neveradopt old137statesasresident138.

READ-ONLY architecture/code audits completed bymapper/controller/QA beforechanges.
53Python checks pass(28asset/wrapper/launcher+25legacy lifecycle); Nodepeer15,
endpoint12 and realpinnedJava7.0.13 DEXsignature tests pass. Nine newJVMtests ready,
NOT run locally (Mini ONLY). RealFrida17.5.2 tests on OWN disposableLinuxprocesses
passed4cases with exacthelperSHAfc5f06aa122270ade0e8bc927bd66be121407399de0c66736b30eef1844290b4:
APIeternalize/detach/clientEXIT thenfreshnonces2xrole and actualNativeCallback+
replaceFast/originaltrampoline stillwork; ordinarydetach+pinneddispose rejectsnew
nonce. 'java' role here is protocolrole, NOTAndroidJava/JNI. Initial libcabsfixture
was too short tointercept; correctedtosacrificialRXfunction; finalallcasesPASS.
Actualgeneratedsupervisortemplate4Linuxfixtures passed(success0/error17/TERM143/
KILL137); noAndroidcompile. Private receipts under originalworkspace
 test-artifacts/residency-138/. No Android/Gradle/Kotlin/nativeAndroid build onVPS.

Next: finish exactsource review/audit immutableGitcommit, submit138ONLYauthorized
MacMini signedqueue, fetchintoNEWdirectory, verifyAPKversion/cert/controllerdigest.
Prepare exactdelta/guarded137to138install OFFLINE. Carstill137, not138. Physical
-e CLI/coldboot, native9+JavaJNIcallbacks and map/frame/type111 remain pending.
No PR/OTA/tag/GWMpower or new vehicle commands. Allsource inworktree133,
branchrelease/impulse-74138-v9; no sourcecommityet.

## Previous handoff — NEW BOOT 28c59: 137 NOT READY; owner retry screenshot

Owner sent screenshot "A sessão atual deve terminar antes de repetir o preparo".
Do NOT interpret this as current137ready or old UI text alone. Current kernel
28c59c97-f0b7-4338-9b02-17dcebf7c278 differs from validated boot22c3. Current OEM
7707/start3143 still matches persisted target. Automatic137 remains enabled,
AttemptPhasefailed and circuit137open. No cpbridge-inject owner in pidof OR whole
ps PID/PPID/NAME/ARGS search; no Java endpoint. Mapped native137 SO with cold-state
ARMED/replacements0/pinnedtrue/failurenull. Do not replace/unload this unknown
mapped session or reset the failure circuit manually. Old22c3 ready records and
actual command preflight remain HISTORICAL, not proof for this new boot.

Exact137 R8 mapping retrieved read-only from immutable Mini job; map_id matches
2937e763e2af8c21e6aecce54b27581b6d304bc227876d428fbcd17e246456ac. Exception y10.o473
retraces to Manager.kt521 check(state.id == identity && state.nativeOwner > 0),
called from attempt491/recoverAttempt. Current absence supports owner loss; exact
historical predicate/termination cause still unknown. No P/speed gate returned.
Retry UI text is generic reset catch; not evidence that clearing it is safe.

Collected read-only private evidence under original workspace
 test-artifacts/install-133-persistent-20261025/:137-photo-retry-*.txt,
137-new-boot-*.txt, mapping-137.txt, 137-kernel-private-*.txt (90680bytes),
137-boot-lifecycle-private-*.txt (102482bytes),137-current-crash-private-*.txt
(1410bytes). No retained kernel cpbridge/LMK/OOM match; targeted logcat and crash
buffers no cpbridge/frida hit. This DOES NOT prove no external termination;
early boot logs may have expired. No cause guessed as a software fix.

Read-only launch/source audit: already nohup+setsid and redirected3stdio; default
Frida CLI does not watch stdin without-i. No app cleanup targeting cpbridge found.
Official Shizuku RemoteProcessHolder only destroys short-shell Process; no tree
kill shown. Shipped server implementation not yet verified. External TERM/KILL,
LMK or injector native crash remain unproved. Do not add eternalize/reinjection
or permanent OOM protection based on speculation. Source remains e93a7846/778647fc,
installed137 unchanged; no new build, install, reboot, process kill or vehicle power
command this turn. Last telemetry freshP0 (earlierD0); no current road test started.

PENDING OWNER QUESTION via async: "Depois da instalação da137, você reiniciou a
central ou ela reiniciou sozinha?" Need distinguish intentional reboot/autoboot
failure from unexpected whole-headunit restart. Owner has not yet answered.
Do not tell owner to start five-minute test or connect for experimental validation
while this current boot is unprepared. Preserve existing native mapping/pins.

## Previous handoff — 137 INSTALLED / bridge READY / real command preflight PASSED

Owner explicitly removed P and km/h lock FROM PREPARATION ("Tira do preparo").
Done in all four bootstrap gates, including initial policy, publication, coherent
idle-host restart check and client refresh. No gear/speed reads or packet fields.
Projection absence/freshness, current process identity, attempt circuit, ticket
revocation and preservation of callback owners remain. Waiting text updated.

Installed74137 / 1.0.0.74.137-preview, source778647fc906a749d61549b8a3c1a7a856af59e77,
branch release/impulse-74137-v9. Mini job
haval-impulse-133-778647fc906a749d-20261025T205147Z-31853 SUCCESS5m33s /83tasks,
full JVM/Release/R8/lint and signing passed. Nineteen endpoint/assets +25 lifecycle
Python checks passed. Actual installed base.apk SHA
 e851f4993aab3f9885e3b1c2324ebe763f572f9a5f599bc268a568f58b1be9d6
size81582383, production cert unchanged. Packaged source digest verified against
reviewed controller; revision74137-237aeef69c42-77685850fb91.

NEW kernel22c3cdf4-507f-4b3a-ba39-5baba9c400b7 observed during build; ROOT DID NOT
issue a reboot. FreshP0/noProjection/noPhoneUSB verified before installation.
Exact APK reconstructed via4,009,406-byte gzip /12chunks; temporary public blob
AND script archive removed. PM reported Success and package registry74137 plus
actual installed APK hash/size independently verified. Installer wrapper timed
out waiting for absent install.rc AFTER success; DO NOT relaunch install. This
wrapper receipt failure is superseded by actual package/hash/runtime evidence.
Private run delta-install-20261025T205856Z-9c1ff5e16736; build-137-queue.json.

Automatic prep137 TRUE and READY. Live OEM15640/start14946, native15779, Java17517.
Current runtime uses kernel+PID+start above: native installed-pinned9 /pinnedtrue /
nofailure; Javaarmed, both owner starttime/argv exact and verified AFTER preflight.
Real Android REVOKE/REQUEST/REVOKE dispatched with no phone while parked; SAME OEM
identity remained alive and native9pinned. New safety packet has NO gear/speed.
This validates BroadcastReceiver dispatch that crashed135; no ServiceConnection
Binder offer, map firstframe or type111 success claimed (phone absent).

Next: owner connects ONLY iPhone USB, verifies primary CarPlay normal on DISPLAY0.
Then controlled combined five-minute test with AppleMaps route. Preserve all live
pins; no hot replacement or additional reboot needed for137. Test does not require
P/zero; initial on-car map validation still pending. Do not move principal toD3.
No new PR/OTA/tag/public release or GWM/vehicle-power action. Artifact:
/root/projects/haval-impulse/artifacts/impulse-74137-preparation-release-mini/
app_build_outputs_apk_release_app-release.apk, validation.json adjacent.
Private diagnostics/inspect/preflight receipt under original workspace
 test-artifacts/install-133-persistent-20261025/137-*.json and build-137-queue.json.

## Previous handoff — 137 preparation change; owner explicitly removes P/speed dependency

Owner: "Tira essa trava de P e km/h", clarified "Tira do preparo". Preparing137.
All four vehicle-state gates removed: bootstrap policy, publisher, coherent shell
check before idle-host restart, postbootstrap clients. Manager no longer reads
ServiceManager gear/speed or serializes them. Policy freshness renamed observation
age because it dates projection quiescence, not vehicle telemetry. Preserve fresh
phone/projection absence, firmware hashes, current boot/PID/start, attempt budget,
terminal ticket cancellation and all live published owners. Native interceptor
still validates eligible/identity/ticket/freshness; no change needed there.
UI waiting text no longer asks for P/zero. Five-minute test already had no gear gate.
Version74137 / 1.0.0.74.137-preview; build/physical validation pending. No car
mutation, reset, reboot, power command or installation performed in this change.
Current mapped failed136 remains untouched; see exact evidence below. Its precise
rejected field is unknown: removal of vehicle prerequisites is requested behavior,
not evidence that gear or speed caused that failure. Do not repeat136install.

## Previous handoff — 136 preparation FAILED; photo received, do not start map test

Owner sent photo showing "Preparo pausado após uma falha; sessão atual preservada".
Read-only collection at device 2026-10-25 17:48 -03 confirmed current OEM31530,
start458316, kernel b6ab050a-4e1c-4360-a947-78eb32c06319. Current136 runtime
cold-state.json: phase failed, replacements0, pinnedfalse, failure
"Automatic eligibility lost before native preparation". Native injector31707
is still alive with exact136 launcher argv; bridge SO mapped, noJavaendpoint.
cpBridgeLastFailure says Native preparation failed; circuit136 open and phasefailed.
Do not reset/reinject this mapped session or treat old135ready record as current.
All phone USB descriptors absent at observation (only root hubs). No new install,
reboot, broadcast, retry, process kill or vehicle power action was issued.

Precise rejected eligibility field NOT recorded, so do not claim owner shifted
gear or reconnected a phone. Last safety packet P0/quiescentnull/failed was stale
(17:45:53 vs read17:48); publisher intentionally skips blocked/failed terminal state.
Do not use it as fresh parking proof for remote reboot. Recovery instructions:
parkP, disconnect phone projections/dataUSB, full Android headunit restart,
then explicit UI retry if paused (circuit survivesboot), wait136ready and notify
before test. Never confuse screenoff with Androidrestart. Physical136 callback
REVOKE/REQUEST/REVOKE preflight still REQUIRED after recovery; no map proven.
Private evidence: original workspace test-artifacts/install-133-persistent-20261025/
photo136-{failure,native,fresh}-*.txt. Source and installed136 unchanged.

## Previous handoff — 136 INSTALLED; owner defers physical steps until later

Latest owner: "Não vou ao carro agora. Me fala passo a passo que faço daqui a pouco".
Give concise instructions for later; no further on-car changes now. 136 actualAPK
verified installed SHA ddf7fb8c1407ef66b4cc02f3bdd23935f7ddf06171cd1babe9b819f1ee8fce1f.
Do NOT repeat install/build. Automatic option136 TRUE (DUMP enableack received).
Current preparation WAITING, P0 confirmed, quiescent=false, no136attempt/owners yet.

Blocker identified DIRECTLY: USB2-1 vendor04e8/product6860 manufacturerSAMSUNG,
productSAMSUNG_Android still attached after iPhone removal. AndroidAutoAapActivity
was initially visible, then disappeared. BLElights are not the identified blocker.
Owner should later disconnect Samsung/Android data USB and all phone projections
from central, keeping lights BLE as is; leave PrepararCarPlayaoligar ON and wait
Bridgepreparadaparuestasessão. If Samsung providesinternet, linkmaydrop but local
bootstrap136doesnotneedVPS. Notify root whenready BEFOREtest: actual136 broadcast
callback preflight still REQUIRED; unit/DEX tests passed, physicaldispatchpending.

Transport firsttwoattempts failed BEFOREapply/install (mkdir thenrawTelnetupload
posthash). Their artifacts were NOTinstalled. Firstpublicfolder e286... removed.
Secondattempt recovered SAFELY through HTTPS tar of exact3scripts using existing
foldera9b...; reconstruction+PMsuccess+actualbase.apkSHAmatch verified. All second
publicfiles removed after verification. Private run delta-install-20261025T195122Z-a9b31f83a752,
resume-https.jsonl; launcher local
/tmp/resume-haval136-https.py. It also rechecked freshP0/noProjection/current135
before PMinstall. No136reboot/injection/preflightperformedyet.

Kernel lastknown b6ab050a-4e1c-4360-a947-78eb32c06319, OEM28661 uninstrumented.
Ownapp136PID7483. Old135sessionOEM11951/native12108/Java14297 DEAD; oldruntimeJSON9
pinned STALE. Preserve any newlydetected actualpins; never kill recycledPIDs.
Old135autoprep was DISABLED beforeupgrade; new136enabledafterverifiedinstall.

Remaining sequence when owner returns: freshP0+nophone+noproject; inspect_136.py
(requireNative9/pinned +Javaarmed +both exactowners). If cannotprep, inspectactual
current device evidence; do NOT ask repeated genericdisconnection or newbuild.
Once136ready, REVOKE/REQUEST/REVOKE real Android receiver preflight while app map
experimentdisabled, preservingOEMidentity/owners. Withphoneabsent epoch0 retries;
withphoneCP0 epoch>0 canvalidateoffer beforeMapgrant. Allow>5s offerTTLcleanup.
Onlythenask owner tostartmaptestparked; principalCarPlay staysD0 and AppleMapsroute.
Mapfirstframe/type111 NOTvalidated. Sourceacc346af/release/impulse-74136-v9;
all signedMini/JVM/Node12/Python11checks PASS, no newcodebuildrequired.

## Current handoff — 134 READY TO INSTALL; owner will turn on car

Owner requested notification when ready, then will turn on the car for installation
and test. The new APK is now ready and validated. Do not ask them to wait through
another build. Source worktree: /root/projects/haval-impulse-133, branch
release/impulse-74134-v9, functional commit 563123d984c3e7284a4501cbaf29c21f971bc73f.

Version 74134 / 1.0.0.74.134-preview. Authorized Mini job
haval-impulse-133-563123d984c3e728-20261025T184740Z-20158 SUCCESS, 5m53s, 83 tasks.
Full JVM task passed, including seven new safety race tests; Release/R8/lint vital
passed. Eleven local asset/endpoint Python tests passed. APK signature, version,
OEM patch hashes, packaged payload hashes and local controller digest verified.
APK: /root/projects/haval-impulse/artifacts/impulse-74134-safety-release-mini/
app_build_outputs_apk_release_app-release.apk (81,582,375 bytes).
SHA-256: 3c762663b1da384c17c2e0d9f7bc1d959a12029ec0658d39d0c969e30bad5f5b.
Production certificate unchanged. Payload revision 74134-4585aae86fb8-5715e2d41ca3.
No OTA/tag/PR publication. Car STILL has 133/e531; 134 NOT INSTALLED OR ROAD TESTED.

Prepared private delta-134-safety: 4,009,451 gzip bytes, 12 chunks, exact full-APK
reconstruction verified. Guarded install_delta_134.py accepts only validated signed
134 and current installed 133; --validate-only PASSED with no vehicle/public access.
Both scripts are under test-artifacts/install-133-persistent-20261025 in the original
/root/projects/haval-impulse directory. Cached remote baseline remains pinned at
/data/local/tmp/impulse-74133-reviewed/app.apk (86426a2d...); local corresponding APK
is artifacts/impulse-74133-persistent-release-mini/app_build_outputs_apk_release_app-release.apk.
No public blob served yet. Installer handles filtered stack reads to avoid long
Telnet transfer and rechecks P/0 freshness/no projection before replacement.

Next: owner turns on car; confirm fresh P/0 and no phones, install 134 via prepared
delta, verify ACTUAL installed base.apk SHA above. Determine current boot/OEM
identity; preserve any mapped/pinned callbacks. A clean HU boot may be needed if
previous failed mapping survives sleep. HU reboot has prior authorization while
parked; do not act while moving. Require native installed-pinned/9, Java armed,
both exact owners before asking for iPhone connection. Confirm factory D0 CarPlay
normal before the experimental map test. Do not promise bridge runtime fixed:
the two source races are proven; observed native eligibility-loss cause remains
unproven. User can drive AFTER preparation is confirmed, rather than during changes.

## Current work — version 134 offline preparation (2026-10-25)

Owner now wants notification ONLY AFTER the new APK is ready, then will turn on
the car for installation followed by a test. Do not wait on or assume a parked
car now. Prepare and validate offline; no vehicle modification in this phase.

Version 134 adds cancelled-ticket publication protection, a coherent single
warm-restart safety snapshot, and a separate persistent failure diagnostic.
Functional manager/helper/tests commit: 23b4e3d74576e938486f5da2f1f2e545b4600961.
Root also includes local controller source in the bundle revision digest and
Gradle asset inputs, so controller repairs receive a new failure-circuit revision.
Version defaults/build target: 74134 / 1.0.0.74.134-preview. No OTA/tag/PR update.
Eleven asset/endpoint Python tests passed. Seven new JVM tests await the authorized
Mini release job. Do not claim runtime cause identified or physical bridge fixed.
Installed car APK is STILL 133/e531; experimental bridge remains unready.

## Current handoff — 2026-10-25, pharmacy window ended; bridge not ready

The owner's five-minute window started around 18:32:53 UTC and has ended. Tell the
owner plainly that experimental preparation did not complete; do not keep them
parked or imply an experimental CarPlay/map test is ready. No new retry, install,
HU reboot, injector action or vehicle power command was sent during this window.
Only bounded read-only diagnostics were collected. Do not resume physical changes
on the assumption that the owner is still parked after the window.

Installed APK remains version 74133, source 04ee94e27f50df9c185315172b97ab38cae08c00,
SHA-256 e531c2a1bc4255ef0b704827903ba8c238533299a94327d54823547fdd984811.
Current boot is 9055ddb9-85ac-4648-8105-6e6c1f43a377; OEM PID 21258/start 130473;
native owner 21405 remains alive with the exact bundled bridge mapped. Native
cold-state is failed, replacements=0, pinned=false, failure "Automatic eligibility
lost before native preparation". Java endpoint is not armed. Do not confuse this
with the previous boot's successfully pinned nine replacements.

Timeline: launcher 18:30:42 UTC, native failure 18:31:10, final safety packet
18:32:27 (all before the pharmacy window). That packet has P/0 but unknown
quiescence and empty attempt identity; it is frozen and cannot diagnose the
conditions at native failure. Root did not establish which eligibility predicate
failed. Do not weaken guards or claim a software cause without evidence. The
mapped failed session prevents the current built-in reset; cleanup/restart needs
explicit proof and current safe conditions, never blanket process killing.

Evidence: private test-artifacts/install-133-persistent-20261025/pharmacy-window-
native-status.txt, pharmacy-final-readonly.txt and pharmacy-final-timeline.txt in
/root/projects/haval-impulse. No Android/native build or additional map validation
was performed during this window. Latest Netseek v9 remains incorporated; the
unresolved item is runtime bridge preparation and subsequent physical map test.

## ACTIVE — clean boot9055 detected; owner moving, experimental validation deferred

Latestownerrequest: continue/reboot/activatewhileMOVINGwithpassenger; ownerfrustrated
by5hworkflow. Acknowledgeactualexcessiveanalysis/iterativebuilddelay. Do NOTreboot,
installoractivateexperimentalbridgewhilemoving; onlylightreadonly/offlinework. Do not
keeprequestingbriefroadsidestops; userneedsstablePwindowuntilreadiness, ordestination.

CURRENTBOOT9055ddb9-85ac-4648-8105-6e6c1f43a377 (uptime12minat18:21UTC). ThusOLD
boot79/OEM20459/native20605/Java22512AREPREVIOUSBOOTandNOTcurrentowners. No Root
rebootcommandeverexecuted. LatestAPK133/source04ee/e531 remainsinstalled+verified.
CleanbootcurrentOEM4112/start1644, NOBridgeDLL/native-state/owners, Javaunarmed.
Automaticfirstattemptatbootelapsed~27sfailedBEFOREhostsignal, oldOEM4112unchanged.
Newfailurestackretrace: Managerattempt429 (guardedwarmrestartrootcommand), notJava.
MountedTsServiceMD5b61correct, SELinuxPermissive. Budgetcircuitclosedthisrevision.

OwnerconfirmedPtwice; machinefirstP0freshthenlaterD34.7, secondinitialD4.4age12s,
localwaitobservedP0butFULLamstackreadtimedoutBEFOREretrybroadcast, laterD41.7fresh.
NOprotectedretrybroadcastwassentthisturnandNOphysicalchanges. Lastmovement41.7D.
Normalreadonlymetadataworks; fullamstackoutputcan timeout. AtNEXTstableparkedP,
usebounded/filteredamstackread: captureoutputinremoteshell, returnknownStackmarker
andprojectionmatchesonly; don'ttransferentireoutput. FreshP0before/aftercheck. Then
CONFIGURE_CARPLAY_BRIDGE extra_enabled=true extra_retry=true alreadyauthorized,
onlyifnoBridge/owners (resetmethodenforcesthis). Currentbootisalreadyclean: another
fullHUrebootnotnecessarilyneeded. Wait15–20swithownerholdingP, readlatestrootstatus
+native/Javafiles; require9pinned/Javaarmed/bothownersbeforeaskingconnectiPhone.
Ifnewfailure, readpersistentSafetyfailureStack, retraceMini04ee mapping beforeactions.
NeveroverrideP0/phones/projection/identityguards. NoGWMpowercommands.

Codealreadycomplete/committed/pushed; Sourceworktree /root/projects/haval-impulse-133.
Functional04ee94e27f50df9c185315172b97ab38cae08c00; installedAPKsha
 e531c2a1bc4255ef0b704827903ba8c238533299a94327d54823547fdd984811.
Privateevidence /root/projects/haval-impulse/test-artifacts/install-133-persistent-
20261025/clean-boot-9055-* includesprefsfailure, noNativepinscurrentboot. Physicalmap
orlatestJavaARMstillNOTvalidated. Nextteststaysdeferreduntilparked, noactivation
whileD evenifpassengercoordinates. Userasksnotificationwhenconnect; giveonlyafter
actualendpointreadyandparked. ExistingfactoryCarPlayseparatefromexperimentaltest.

## ACTIVE 2026-10-25 — owner DRIVING: read-only until explicit parked P

Latestownersteering: alreadyINMOTIONwithpassengercoordinating; maponotherphone.
UseraskswhenCarPlaytestcanstart. Answer: experimental133bridge/maptestneedsclean
headunitbootFIRST, thenconnectiPhoneafterreadiness. Thiswaitsforownerexplicitly
parkedinP+freshP0; NOrestart/install/experimentalactivationwhilemoving. Prior
rebootapproval("Pode reiniciar agora")persistsforlaterPstop, but NEVERexecuteinD
orontheassumptionpassengermakessafetydisplaysunnecessary. Do notaskpermissionagain
onceownerconfirmsP/freshmachineP0;performalreadyapprovedHUrebootthenvalidate.

IMPORTANT: NOREBOOTCOMMANDWASSENT. AttemptabortedatFIRSTfreshP0assertbecausegear/
speedunknown(-1),telemetryage56s; nextreadTelnetoffline. Ownerthenconfirmedmoving,
latertelemetryfreshD/speed0. WiFilossmatchesdepartinghouse. Onlylightread-only
queriesaftermovement; noFridaprobes/injection/callbackorvehiclepowerchanges.
LatestAPK04ee94e2 signedsource E531SHAinstalled+verifiedactualbase.apk; autooption
savedtrue; allfilesbundledlocalfornextboot. Upstreamv9still5ccd91e2(latestchecked).
NativeoldsessionlastconfirmedOEM20459/start1131261, nativeowner20605installed-
pinned9/pinnedtrue/failureNULL, Java22512oldentryfailedimplicitprocfsread0. Preserve
bothclients/OEM; do notunloadpinnedcallbacks. LatestAPKfixreadTextboundedinstalled
butnewcleanbootpending. Fullboot/Javaarmed/mapfirstframeNOTvalidatedyet. No5mintest.

FunctionalSource04ee94e27f50df9c185315172b97ab38cae08c00, personalbranch
release/impulse-74133-v9. Actualinstalledsha
e531c2a1bc4255ef0b704827903ba8c238533299a94327d54823547fdd984811,81582379bytes,
productioncert5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0.
MiniRelease04eeSUCCESS5m17/83tasks;905JVM0fail1skip(sameAndroidsourceas9df),
25lifecyclePythonPASS,19endpoint/assetsPythonPASS,103NodeinterceptormodelPASS.
Corefixes: native/ESMassetsbundled;localbootmanager;firmwarepinnedstrictDCM/OEMBT
quiescence;parentROOT/versions/runtimeRoot0711;exactownerargvwithoptionalNUL;
procfsexplicitreadlength;failurestack;guardedexplicitretryDUMP;noGlobalpkill.
NoVPSAndroidbuilds. GitHubbranchcontainsfunctionalchanges;noPR15/OTA/tag/publicrelease.

Nextwhenparked: freshP0+noprojrootguard, approvedAndroidHUrebootONLY(noGWMpower).
Reconnectread-onlywithbackoff, requireNEWbootuuid, latestAPKe531, localselfprep
Nativeinstalled-pinned9+Javaarmed+bothowners; thensafeApprestartifneeded/appropriate.
TellownerWHENto connectiPhone; confirmoriginalD0normalparkedbeforeexperimentaltest.
Map/driving5mintestremainownerinitiated. Beforepark, onlyreviewandlightdatareads.
Privateevidence/helpers remain /root/projects/haval-impulse/test-artifacts/
install-133-persistent-20261025. Sourceworktree /root/projects/haval-impulse-133.

## ACTIVE 2026-10-25 18:05 UTC — FINAL133 installed; clean boot approval pending

CURRENT FUNCTIONAL SOURCE04ee94e27f50df9c185315172b97ab38cae08c00, branch
release/impulse-74133-v9. Latest signedMiniRelease
haval-impulse-133-04ee94e27f50df9c-20261025T175610Z-19343 SUCCESS5m17/83tasks.
FETCHED+VALIDATED+INSTALLED sameversion74133/1.0.0.74.133-preview. Actualinstalled
base.apk SHA e531c2a1bc4255ef0b704827903ba8c238533299a94327d54823547fdd984811,
81582379bytes, productioncert5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0.
Artifactdir /root/projects/haval-impulse/artifacts/impulse-74133-procfs-release-mini.
Installationreceipt delta-install-20261025T180255Z... under private133directory;
helperstdoutconfirmed actualinstalledbase.apkSHA +Success. Publictempblobremoved.
AllAndroid/nativebuildsMiniONLY; noVPSfallback. Boot/mapphysicalvalidationpending.

Latestfunctionalfixboundedprocfsread: REALFridaFile.readText()withoutn returnsEMPTY
on/proc/self/stat andboot_id despiteexplicitread4096return288/37bytes. Primary
file-staged-result.txt usesdisposablesleep9866/root(noOEMtouch), stagecomplete.
Thusautomaticendpointfailedbeforepublishingloaded: invalidkernelbootidentity.
REGEXHYPOTHESISWASFALSE: actualAPKregexsingle92bytecorrect. eab1192d testsprovedit;
04ee94e2realfixusesreadText(max+1),boot128/stat4096,oversize/typeguards+close.
19Pythonendpoint/assets testsPASS. Testonlyeab+dacf25;905JVM0fail1skiplastMini9df,
latest04eeMiniReleasepassesDebugJVMagain. FullCarPlayregressionguardlastPASS.

CRITICAL CURRENT LIVE SESSION (old9dfpayload): kernelboot79f7b4b4-483e-4199-aba2-
122e8346027f, OEM20459/start1131261, Nativeowner20605 INSTALLED-PINNED9/pinnedtrue/
failureNULL. Nativecode/callbacksVALID; preserve20605andOEM. Javaowner22512alive
butoldprocess-prefixfailedbeforeloaded/receiver dueimplicitprocfsreadEMPTY; preserve
ittoo pendingcleanboot (nonewduplicateJavaarchive/unload). NoSurfacemaplease or5min
started. OldsessionnowpreviouspayloadrevisionafterlatestAPK; managerpreservesmapping
andawaitsnewboot. DO NOTexplicitretry/killOEM/killNativeowner whilepinned.
CurrentautoPrefTRUEpersists; controllerwaitsafteroldsession. FreshP0throughinstalls,
userconfirmedNOphones. RealOEMBTprobeall5supportedprofilesstate0SUCCESS+aggregate
empty, noactiveprojection. Firmwarefingerprintguardsinstalled. Parent0711DACfix
andowneroptionalNULfixalreadyinstalled/proven; oldfailed22453wasguardedTERMonly
prepublication, confirmedexitedbeforelaterrealNative9success. ItisNOTliveanymore.

ROOT HAS ASKED USER via async tool at18:05: "Posso reiniciar o Android da central
agora ... telas e áudio ..." choicesPode reiniciar agora/Prefiro reiniciar depois.
APPROVALISREQUIREDANDPENDING; elapsedtimeisNOTapproval. NeednewfreshP0+noprojection
immediatelybeforeanyauthorizedHUreboot. NoGWMvehiclepowercommands. Userdeferredmap
anddrivingtest. Ifapproved, rebootHUonly, reconnectboundedread-only afterboot, wait
localselfbootstrap, inspect_133.py LATESTvalidation withnewlabel. RequireNEWbootid,
Nativeinstalled-pinned9+Javaarmed+bothliveowners. Preserveanypublishedcallbacks.
Ifownerdefers, finishdocs/sourcepush+finalhonestlyallinstalled/autoconfigsaved but
fullboot/Javaarm/mapvalidationpending; don'tclaimbridgecompletelyreadycurrentboot.

CurrentGitfunctional04eecommitted,4AIcontextfilesmodified. Pushfinalbranchtoorigin
personal rafaelcs28/haval-impulse (previouspushonlya294); noPR15/OTA/tag/publicrelease.
UpdateoriginalnonGit/rootprojectAIcontext withfinalcurrentstate forfuturehandoff.
Privateevidence/helpers /root/projects/haval-impulse/test-artifacts/
install-133-persistent-20261025. Keyfinal-native receipt final-bootstrap-receipt.json
showsNative9+bothownersbutJavafalse (OLD9dfsession). Don'tmistakeitforfinalbootpass.

## ACTIVE 2026-10-25 17:22 UTC — final DAC/owner fix building, car 133 installed

CURRENT SOURCE9df7dd247f6a759557f309f1260ac34ca9aeb277, release/impulse-74133-v9.
Mini signedRelease job haval-impulse-133-9df7dd247f6a7595-20261025T172142Z-16514
RUNNING. Control digest37b9d1476d3e99e59973924f3f27a4d709b551d6464adcfa3c0fd35f6719d5a7.
NoVPSAndroidbuilds. LastsuccessfulMiniRelease21d5e55d(5m34/83tasks), installed
sameversion133 SHA b94ee548f201f43031c5c59097b24bddb0d78ab4b38763873fc01a4cdedc8b4c.
Signedcert5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0.
Priorbaseline133APK SHA86426a2d416c08130f52c56538821a5e285316d7c84f3743b9e5737b84d2aa26
is STILL remote /data/local/tmp/impulse-74133-reviewed/app.apk; useasdeltaBASE.

PhysicalBTproblem SOLVED in21d: DCMtx45NULL=emptyonlyexactfirmware, EMPTYlist=pending;
retainedOFFLINEAndroidBTREADYrecord +noactivecaps admittedONLYafterOEMBTrawBinder
provesall5realprofileproxiesSUCCESS/state0, stablecachedaddressset andaggregateempty.
REALownAppreadonlyprobe confirmsall5state0/SUCCESS1,aggregate(-1)empty SUCCESS1,
adapterON12/code0quirk. UserexplicitlyconfirmsNOphonesconnected. ExactDCM/BTfirmware
hashespinned. ConnDevice FQCNkeptforOEMBundle. SourceData/Javawireunchanged.

FirstREALautoprep21d DID passP0/BTquiet and safewarmrestartedOEM6137->22287,
start952661, sameboot79f7b4b4-483e-4199-aba2-122e8346027f. Thenfailed BEFOREnative
publication. ROOTCAUSEparentversions/runtime0700ROOT underumask077: targetUID1000
CapEff0 cannotreadbridgeorwritestatus. NoBridgeDLLmapped, no cold-state; emptylog,
ownrootCLI22453/start952785 alive argsoldrevision74133-b311fc614550-07956bb33df3.
SourcecoldarmfileBytes BEFOREModule.load/anypin/hook proven byNodeEACCESfixture.
CurrentparentsareNOTyetfixedoncar. Rootguardedcleanup ofONLY22453 runninglocal
execsession58463 via private stop-unmodified-client.sh; checkresultbeforeproceed.
NEVERkillifbridge/pinnedstate appears. TargetOEM22287 mustremainuntoucheduntilnew
managerexplicitguardedretry. NoSurface/maplease, noGWMpower orfullcentralreboot.

9df fixesall3parents ROOT/versions/runtime ROOT:ROOT0711 BEFOREexisting-version
branch; childSystem permissionsunchanged. Alsoownerloop exactargs normalizesoptional
finalNUL (real/proc22453cmdlinewasunterminated; oldtrailing-spaceprefixwouldmissowner).
Persistentfailuremessage/stack nowinsafetyfile; DUMPconfigreceiver optionalboolean
extra_retry=true callsSAMEguardedUIreset(no mappings/no owners; freshworkerP0checks).
25lifecyclePythonPASS inclREALUID1000DACreproductionandowner4valid/24rejectcases.
NewBTpureJUnit11passedpreviousMini; needfinalMiniRelease/JVM.

NEXT: fetch9dfMiniRelease NEWdest, validate_apk.py --release exactsourcecommit.
Build transportdelta using PRIVATE build_transport_delta.py baselineold86426APK
->newsignedAPK ->newdir. install_delta_133.py baselineLocal newLocal deltadir
validatesprod/hashes/reconstructsMINIbytes, freshP0/noprojection/install-rsameversion,
verifiesactualinstalledAPKhash; transfersONLY~4MBgzip viaexistingtemporaryHTTPS.
First21d update successfullyusedthishelper(rootreviewed,18offlinechecks). DO NOT
retransfer69MBunlessnecessary. CurrenthelperBASEfixed86426andremotebaseunchanged.
Afternewinstall, rootbroadcast CONFIGURE_CARPLAY_BRIDGE --ez extra_enabled true
--ez extra_retry true -p package; thisexplicitlyclearsoldfailedbudgetONLYifno
mappedbridge/owner. Wait20sec, inspect_133.py NEWvalidation --label final-ready;
requireinstalled-pinned9,pinnedtrue,NULLfailure,Javaarmed,bothliveownersidentity.
VerifyApprestart ONLYP0/noproj: ownImpulseforce-stop+start, preserveOEM/injectors.
Fullcentralboot/maptestdeferredtolaterowner. Neverclaimmapfirstframevalidated.

Private helpers/evidence /root/projects/haval-impulse/test-artifacts/
install-133-persistent-20261025. Helpers inspect_133, validate_apk, delta builder,
delta installer allready. Ifnewattemptfails, newSafetyJSONpersistentfailurestack.
RepoAIcontextmodifieduncommitted; code9dfcommitted. Personaloriginbranchpushedonly
a29444d2 earlier; pushfinalcode+docsatcompletion. Upstreamv9tip5ccd91e2all6today
alreadymergedd6e2d216 andverifiedlatest. NoPR15/OTA/tag/publicrelease scope.

## ACTIVE 2026-10-25 16:04 UTC — final Release retry, central still 132

Current Android/build source a29444d257af49eac0ce2bc7448f49d5b02b291a.
Mini Debug6124febc SUCCESS2m27s/49tasks; fullJVM894 total,893pass/1skip,
including15bootstrappolicy+12quiescencewindow. DebugAPK verified assets/ELF/ESM,
156767461bytes, SHA2c620e2835c5c57d8f04fcd66bad8e01fd469afea1229ea4e61531a93e9edd81,
notproduction-signed and NOTinstalled. Private fetchedDebugdir
artifacts/impulse-74133-persistent-debug-mini.

FirstRelease6124febc failedafterR8 at generateReleaseLintVitalReportModel:
implicit generated-assets dependency. a29444d2 explicitly links ALL variantlint
consumers to asset generator; no Android/runtime source changes from Debugpass.
Audited control digest37b9d1476d3e99e59973924f3f27a4d709b551d6464adcfa3c0fd35f6719d5a7.
Current signedRelease74133 job
haval-impulse-133-a29444d257af49ea-20261025T160032Z-30377 RUNNING on Mini.
No VPS Android builds. No133 install or bridge enabled yet.

Owner confirms central online waiting. Readonly freshP0; boot79f7b4b4,
OEM6137/start2272, no projectiontasks, mountedserviceMD5b61 exact.
Currentvehicle132. Do NOT repeat oldmanualinjection. Await signedRelease,
fetchNEWdir, verify using private install-133-persistent-20261025/validate_apk.py
(--release --source-commit exactsha), prepare_https.py temporary artifact then
install_133.py APK transport-https.json (freshP0/no projection installguards).
Enable via DUMP-protected CONFIGURE_CARPLAY_BRIDGE with extra_enabled true;
inspect_133.py validation.json verifies nativeinstalled-pinned9/Javaarmed/owners.
Only aftersuccessfulready, safeP0/no projection, verifyImpulseapprestart survives
withouttouchingOEMowners. Fullcentralboot/map/driving validation deferred.
Do not GWMpoweron/off. NoOTA, tag, publicrelease or PR15 update this scope.

## ACTIVE 2026-10-25 15:51 UTC — final candidate 133 on Mini; central online

Current source 6124febc, release/impulse-74133-v9. User confirms central online
waiting for installation. Read-only telemetry fresh P/0. Still production74132;
no133 installed yet. No map/driving test or central reboot authorized this turn.

Mini attempts: 5bcb9fc9 rejected SourceSet Provider (fixed concrete file path);
3b218741 rejected private Shizuku.newProcess (fixed public IShizukuService Binder,
allpipeFDs closed); a73bc98c compiled+packaged native+assets+APK and ran893JVMtests,
1HUDtest failed/1skipped. Root cause real v9 integration regression: unconditional
AR straight-arrow fallback fabricated a maneuver for text-only CarPlay. 6124febc
restores text-only handling, preserves new AA cases, adds boundary test. NewMini
Debug pending, signedRelease after validation. Do not install earlierfailedsource.

Latest offline checks103NodePASS,42scopedPythonPASS,17lifecyclePythonPASS,
fullregressionlockPASS; generic59Python50pass9fixture-dependent skips. Audited
build-control digest07e4947a8985e28516cf19d853769e95231465703425a46d431e188b0a59af01.
No Android, Gradle, Kotlin or native compilation on VPS.

Pending: successful finalMiniDebug/JVM and productionRelease74133; verify APK
signature/hashes/version/OEMassets/embeddedbundle; freshP0/no projection install;
DUMP-protected CONFIGURE_CARPLAY_BRIDGE boolean extra_enabled=true; inspect local
bootstrapstate/owners withoutstartingmaplease. Physicalcoldboot/drivingtestlater.
Private install/evidence staging /root/projects/haval-impulse/test-artifacts/
install-133-persistent-20261025. Preserve all productionsettings/Minimalist/noOTA.

## ACTIVE 2026-10-25 — version 133 persistent local bootstrap + latest v9

User deferred driving tests and explicitly requested a new installed version that
prepares the bridge locally after central restarts, including Netseek changes today.
Active Git worktree `/root/projects/haval-impulse-133`, branch
`release/impulse-74133-v9`; upstream merge d6e2d216 includes all six v9 commits
through 5ccd91e29bd2eafe651d4c072e13e59567801f49. Personal changes preserved.

Implementation in progress: APK-bundled versioned native/Java assets, local root
bootstrap, boot/app/package receivers, strict DCM quiescence + fresh P/0 checks,
PID/starttime/boot identity, detached dedicated injectors, circuit breaker and UI.
Never persist map-test lease or Surface across boots. Native ELF built only on
Mini. Global legacy `pkill -f fridainjector` removed to preserve pinned callbacks.
Source review frozen for first Mini validation; no 133 install yet. Android/Gradle/Kotlin build ONLY authorized Mini queue,
immutable audited commit and exact build-control allowlist. No VPS fallback.

Read-only central recovery at 15:31 UTC: boot
79f7b4b4-483e-4199-aba2-122e8346027f, uptime~4148sec, stock OEM6137/start2272,
version74132, empty coldgate, no bridge or injector. Thus old PID5629 / injectors
5909/9338 below belong to a PREVIOUS BOOT and are no longer live. Boot reason
reported shutdown; cause of owner-reported restart remains unproven. Private
receipt `test-artifacts/133-current-readonly-20261025T153154Z/state.txt`.

Offline checks: 103 Node interceptor cases, 42 scoped Python packaging/controller
tests, 14 lifecycle guards and full CarPlay regression lock passed. Full generic
Python discovery: 59 cases, 50 pass/9 fixture-dependent skips. Root shell framing
was verified read-only on the central with >2300-byte command, case preserved.
Additional review: root single-flight boot marker, monotonic recovery deadline,
immediate opt-out eligibility revocation, strict unreadable maps/tasks failure,
30-second ready polling and one guarded OEM client refresh after own safe reload.

Pending: Mini Debug/JVM + signed Release133;
verify APK and stage/install once fresh safety allows; enable persisted auto option;
verify local readiness and document real boot/map validation limits. User will
perform the driving test later. Do not start map Surface lease, vehicle power,
central reboot, or repeat old manual injection. Earlier GWM OFF request fulfilled.

## COMPLETE 2026-10-25 11:11 — bridge ready; owner confirms current principalD0

Latestresultsupersedesallactivefailure/investigationnotesbelow. UserCorrija
completedsource+fieldbootstrap. OEM5629/starttime476590 isLIVE installed-pinned,
9entries,pinnedtrue,failureNULL. Nativeinjector5909 MUSTremainalive. NewESMJava
agent injector9338 reportsloaded+armed; receiverregistered. OwnerreconnectediPhone AFTER
thisactivationandconfirmedprincipalCarPlaynormal. No5minteststartedthisturn,
noBinderSurface/type111/firstD3frameyet; doNOTclaimcleanmapworking.

Sourcee55014b1 pushedoriginrelease/impulse-74132-v9, withmainfix7e1f249d,
loaderflush ecff75e5, reviewedC++/MFIclosure3338b812, kerneltrampfix2763e827,
ESMpackaginge55014b1. Native/AndroidappinputsUNCHANGEDfromMini7eDebugsuccess
2m21s/47tasksincludingARM64JNIandunitstask; downloadedAPK/log/checksumsverified,
notinstalled(Debugcert). CurrentproductionAPKremains74132fromc591; bridge.so
stillMini2f793449d9, separatelystaged. No neednewAPK/versionforJSfieldfix.
54NodemodelPASS;49Pythontests=48pass1optionalconverterskip;fullregressionguardPASS.

ExactC++capture7,812,664bytesSHAd25fdec8..., REAiostreamroot/helper/imbueaudit,
pluspriorPlatformMediaidleworkerreview admitted9extraOEMdeps withnewlinkerprofile
digest5501d81df03cb9e7baf5dbc37d594ce973ae708e13390a4330d3dde5be977ae8.
ActualGumtrampprobed: ProcessrangeNULL,Cloaktrue,Memory.queryProtectionrwx.
KernelpermissionscheckedAFTERflush, null/self/duplicate/alignmentguardspreserved.
Priorfailedattemptsrollbackzero/unpinned verifiedbeforeidlemanualcoldrestarts;
nofailedpinnedkill/retry/unload. LastsuccessfulnativeprocessNOTrestarted.

EndpointrootcausewasCJSexportsbeforeJavaimport. BarearchiveisolatedReferenceError
reproduced; ESMtype=moduleisolatedarchiveexecuted. Newagentartifact
/root/projects/haval-impulse/artifacts/carplay-endpoint-agent-esm-132/clean-map-agent.js
SHA bfe37072bd70faa7399451d247508b077830208d1a688776079050df7c4d0cb9,
540424bytes; manifestpinsmetadata/moduleMode+entry+lock. Builder/readerrejectCJS.
Old1a429e6dendpointartifactINVALIDforruntime—nowrejectedasstale. Itsinjector7437
remainsidlewithuninitializedscript; firstCJSentrycannotreachJava/receiver and
receiverabsencewasverifiedbeforeloadingnewagent. DoNOTkillnative5909ornewESM
agent. No needalteroldidleclientduringphysicaltest; allfreedwithOEMexit.

Private evidence bridge-132-trampoline-fixed-20261025 includesendpoint-esm-result,
safetyproof, finalbridge-ready, commonJS/ESMisolatedprobes. Earlierattempts dirs
bridge-132-fixed-20261025T132412Z, bridge-132-fixed-cxx-20261025. Currentartifact
carplay-loader-fix-7e1f249d/validation.json hasfinalruntime +Mini receipts. Manifest
androidRuntimeValidatedtrueSCOPEmodule/JavaarmONLY; Binder/mapstagesnotvalidated.

Nextuserstep: AppleMapsroute, ImpulseRecursos->TesteCarPlaycaptura+HUD->Iniciarteste
CarPlay(5min), then pullprivatecaptures/cluster/endpointlogswhilecentralonline.
PrincipalD0normalalreadyconfirmedcurrentPID. CarPlay/AAisolation, safety/native
panels and leaseguardsremain. Nevermove/resize/restartprincipal orenableAA.
Ifcaroff/OEMdies, gateempty meansstockboot; newattendedP0/disconnectedcoldprep
needed. Userstartedcarthissession; earlierGWMshutdownrequestalreadyfulfilled.
NOvehiclepowercommands/APKinstalls/displaymoves/AAchangesinthiscorrection.

## ACTIVE 2026-10-25 10:52 — FIRST REAL installed-pinned; preserve live injectors

CRITICAL: OEM5629 now installed-pinned,replacements9,pinnedtrue,failureNULL.
Nativeinjector5909 MUST stayalive forcallbacks. Endpointinjector7437 alsoalive,
unknowninitialization; DO NOTkill/restart/unload either orOEM whileinvestigating.
Controllerlocalexec24546 finished1 ONLYbecauseendpoint8s timeout; finallycleared
coldgate/releasefile. Nativehooksremainvalid, notrolledback. No secondarySurface/
mapteststarted. CarPlayphysicalprincipal stillneedsconfirmationforcurrentPID;
useransweredearlierattemptprincipalnormal, lastaskedCanIdisconnect; toldYES.
FreshP/noCPtasksobservedbeforethisactualattempt. Vehiclepowerownercontrolled.

CurrentcodeHEAD2763e827 after3338b812(c++MFIallowlist),ecff75e5(flush),7e1f249d
(loader/PIDfix). 2763notyetpushed;3338pushed. Native/Androidappinputsunchangedfrom
Mini7e success2m21/47tasks. 54Node+48Python47pass1skipaftertrampfix.
ActualisolatedreplaceFastprobe provedProcess.findRange(original)==null,
Cloaktrue,kernel/rwx, Memory.queryProtection(original and+3)=='rwx'.
Sourcefixcollects9distinct/nonself/alignedoriginals, flushes, verifies actualkernel
protection+Cloakownershipbeforebootstrap. NoCloakunhide/noXbypass. Actualsuccess
nowconfirmedcurrentcold-stateJSONunderstagedroot.

Privateattemptdir bridge-132-trampoline-fixed-20261025 hascontrollerlog,
activation-failure(endpointtimeout), installed-pinned-endpoint-check. Rootcurrently
runsbarefrida-compile18archiveprobein35sdisposablesleep: execsession... seeactive
sessions. Probeisolatesarchiveformat (noJava) fromstaticJavaimporthang. Newendpoint
archive540920bytesSHA1a429e6d compiledwithfrida-compile18/javaBridge7.0.13; stdout
log0 andnoendpointstatefile >60s. NOproofagentboot orJavaarmed. Plaincoldscript
works. Needpreserveunknownendpointscript; no duplicate receiver/rearm blindly.
Read-onlyagentcontroller_pid_fix investigatesdurablebootprogress/importJava/
performNow; noedits/caraccess. Rootisolatedarchiveprobewritesprivatearchive-probe
result; DO NOT touchliveOEMforformatprobe. Ifarchiveloaderworks, usepreludemodule
beforeJavaimport tolocalizeJavaRuntimeconstructorhang.

Remaining: Java control readiness thenfreshprincipalD0confirmation, explicit5min
capture andfirstD3frame onlyafteractualbridgeendpointarmed. Do notclaimcleanmap.
Userstillparked/connectedconditioncanchange; keepfreshP0beforeanyliveactions.
NoAPKinstalledthissession; production132stillactive, nativeexcludedfromAPK.

## ACTIVE 2026-10-25 10:34 — loader crash avoided; reviewing missing C++ dependency

Corrected live attempt24265 DID NOTcrash. It safely failed before nativepins/entries:
phasefailed,replacements0,pinnedfalse, failureUnreviewedcold-loaddependency
/vendor/app/TsCarPlayService/lib/arm64/libc++_shared.so. Controllerexited1, finally
clearedgate/releasefile. PID24265alive; injector24480attached butno OEMentryhooks,
scriptunpinned, nativebootstrapnevercalled onthisfailurepath. Noendpointstarted.
OwnerconnectedCarPlayandconfirmedprincipalD0normal, thenexplicitlyTireiCarPlay
(disconnectedagain). NeedcurrentP0+noCP/AA beforeanothercoldattempt afteraudit.

Read-onlyreviewagent bridge_crash_review nowfetches exactlibc++7,812,664bytes,
SHA d25fdec80cb2ab3b4ca02b189d909bc90a6378411bbf3d3a36a09512f2798638,
private bridge-132-libcxx-review-20261025. Partialfetchresumed afterchunk timeout,
now4.78MB; do NOTconcurrentlargeTelnetcaptures. ELFpreliminary1initentry0x91824,
needs libc+libdl only. AgentwillREAconstructorafterfullhashverified. Addonlyafter
review; also admitpreviouslyreviewedinitfree libmfiinterface.so actualstockmaps.
Currentallowlist7depomittedbothC++andMFI, doNOTwaive unknownlibs.

SourceHEADecff75e5 (JSflush) after7e1f249d (mainfix); ecffnotyetpushed.
Mini7eSUCCESS2m21s/47tasksJNI+unit; exactAndroid/native inputs unchangedinecff.
45NodePASS and48Python47pass1optionalskip; fullguardPASS. Finalisolatedprobe
PASS: directloader/trapsall/cooperative sawapprovedinternalenter/leave andnested
NOLOADsuccess underreviewedcompleteboundary. Firsttwo globaldlopenprobeshadno
hookevents, wereNOT successes. All3probesdisposable35ssleepprocessesnowexited.
Evidencefixedsession bridge-132-fixed-20261025T132412Z (includesactivation-failure).
Needupdateprofiledigest+NodefixturesincludeC++/MFI whenauditcomplete, commit,
restageupdatedlauncher. Oldmappedbridge/injector mayremain fromknownunmodified
failed attempt; wrapperpreflightmustdistinguishverifiedfailed0/unpinned vsunknown
orfailedpinned. Noautomatickill/retry/unload forfailedpinned/livecallbacks.

## ACTIVE 2026-10-25 10:30 — corrected cold attempt armed, awaiting iPhone

Owner explicitly says Corrija and confirmed stillparkedP/disconnected (Sim).
Sourcefix7e1f249d pushed; MiniDebug jobhaval-impulse-132-7e1f249d000595e5-20261025T132305Z-29383
SUCCESS2m21s/47tasks, JNIARM64 andtestDebugUnitTest. NativeCpp unchanged. Fetched
APK/log/checksums verified artifacts/carplay-loader-fix-7e1f249d/mini-debug; doNOT
installDebug APKoverproduction132. Additional runtimeJSflush commitecff75e5
ensuresloader listener activebeforestartuprelease; testmodelrequiresflush too.
Android/native sources identical to Mini-tested7e; no needcompileJavaforJSflush.

Three disposable35s sleep-process probes; firsttwo globaldlopen calls didn'thit
boundary (not successes), finaldirectpublicloader/trapsall/cooperative probePASS:
linkerfound,3bodyhashesexact, targetPatchedtrue, callerapproved, rootNOLOADsuccess,
nestedNOLOADfrominternalonLeave success. Evidence private bridge-132-fixed-20261025T132412Z.
NoOEMrestart inprobes; disposableprocessesautoexit35s, no retainedsessionhooks.
Thenoneactualcorrectedcoldattempt via privateactivate.py using exacthashreuse
forunchangednative793449d9 andendpoint1a429e6d; NEWlauncherfromecffsource.
FreshP/0 andCP/AAtasks0,noexistingOEMinstrumentation. OldOEM24700->24265,
absentmoduleschecked, boundPID/starttimecontroller armed. Startup released.
Running execsession82413observesup to5min, newlivePIDchecks preventstalestate.
Asked ownerconnectiPhone andconfirmCarPlayD0 normal; answerpending. KeepP and
no5mintest untilinstalled-pinned+endpointarmed+principalconfirmed. NoAPKinstall,
noAA/display/resolution/powerchange. Nativecode/production132remainunchanged.
Mustfinishobserving/pullstatus; neverkill/unload a livepinnedprocess onfailure.
Controllerfinallyclearsgate+releasefile. Native/endpointinjectorsmustremainalive
on success. Do notdeclaremapworkingwithoutsecondary111/firstD3frameevidence.

## 2026-10-25 — loader abort corrected in source; device validation pending

User explicitly requested correcting the failed attended bridge attempt. The
native bridge and installed132 APK are unchanged. The cold JS interceptor now
records module additions and pins/installs only on the attested internal
Android9 do_dlopen outermost successful return, before its public caller unlocks
the recursive loader mutex. It rejects incomplete/nested-failed groups, callers
outside the two verified wrapper sites, untracked threads and unknown new images.
New testdata/android9-linker-profile.json pins the real central linker and seven
reviewed OEM dependencies; existing module identity includes path/base/name/size.
New dependencies require exact file and executable hashes; baseline system libs
are reused, new unknown ones reject. PlatformMedia initializer has an idle worker,
not no worker: REA confirms empty queue, wake semaphore0, wait-before-dispatch.
No asynchronous installation/timing workaround; native module ownership and all
nine entries remain unchanged. Endpoint bundle remains1a429e6d and native remains
793449d9; updated launcher is required (old staged launcher must NOT be reused).

Controller status now bound to original live PID + stat starttime + cmdline and
bracketed reads. Old armed/installed/endpoint files after process death cannot
acknowledge a replacement/reused process. No additional cold-window roundtrips;
cleanup and no automatic retry/kill/unload remain. Code review addressed baseline
identity, cloned boundary manifest, explicit dependency errors and nestedNOLOAD.
Source tests:45Node interception scenarios;48Python tests(47pass,1optional skip),
including22controller and5launcher. Finalfullguard/Mini result follow below.

Read-only linker capture1741640bytes SHA63556e8d8a90ccfbff156c3330a75a2fd0e5955d87000ea77fad1e77c0def06b
is private under test-artifacts/linker-fix-20261025T130532Z. REA genuine dossiers:
loader wrapper ev_457e61e48dc02e84f00b991721b778a32db560ed07c40f409b7adbcf1a0ff6c1;
extwrapper ev_01b06092793110d59a04d344a1dc2a7e4d1340d2ffd94ee4f811d238660b4b66;
internaldo_dlopen ev_89e3ff5462e8e11b6ffa62e3514f4ab7bf716128e77db0f74c3a079559238077.
PlatformMedia4REA dossiers and closure manifest in private
bridge-132-platformmedia-review-20261025. Existing REA CLI6.7.0 used invocation-only
existing Ghidra12.1.4/Java21; no installation or client configuration edits.
No further live restart/injection/installation/power action after failed attempt.
Asked whether owner still parkedP/disconnected; current availability answer
pending. Do not retry merely from online telemetry; new source/build then fresh
coordinated P/0/no projection preflight required. Native map success still unproven.

## 2026-10-25 10:00 - attended cold bridge attempt FAILED; OEM recovered, no retry

Owner said Pronto, initially telemetryN/0; requestedP. Owner confirmedP and two
freshP/0 snapshots plus repeated final gate allowed one reviewed cold attempt.
Staged exact793449d9 native +1a429e6d bundledendpoint +969d2494 launcher reverified.
Private wrapper reused hash-verifiedstage; first pre-mutation wrapper check falsely
classified Telnet greeting as instrumentation (NO mutation); corrected marker-based
guard then one actualrestart6125->23426. Cold absentmodules confirmed, interceptor
armed and startupreleased. Asked owner to connect iPhone, keepP, nofive-minute yet.

OEM23426 then SIGABRT while loading modules. Injector.log actual abort:
linker_phdr.cpp170 Load CHECK did_read_ false; stack __loader_dlopen ->
NativeModulePins+656 (ELF0x127964, sourceinitial_entry_bootstrap.cpp66),
prepareInitialEntries -> namespacepreparation. Synchronous lastonAdded enters
install()/modulepinning while linkerloadunfinished. Nativeprepare precedesnine
replaceFast calls, so no completedentryinstallation/installed-pinned/endpoint.
Existing cold-state.json STALE armed/replacements0/pinnedtrue reflects Script.pin,
NOT healthy bridge. Do not trust status file without matchinglivePID.

Androidauto-recreatedOEM24700; app3260unchanged, no injector/bridge mappings in
newOEM. LocallySIGINTobserver after detectingPIDchange; controllerfinallycleared
optinpropertyandremovedreleasefile. No retry/kill/unload ofpinnedlivecallbacks,
noAPK/config/display/AA/powerchanges. Vehicleownerstartedthissession; previous
GWMshutdownrequestalreadyfulfilled. No remoteoffcommandissued.
Private evidence /root/projects/haval-impulse/test-artifacts/bridge-132-live-20261025T125436Z:
controllerlog, exactinjectorcrash/crashbuffer, systemevents, state/telemetry,
field-result.json andevidencehashes. No nativecodefix/build performed thissession.
Read-onlyreviewagent confirmedprobablereentrantlinkermechanismfromAOSP9; arbitrary
asyncsetTimeoutlosescoldbefore-first-use guarantee, NOT acceptablequickfix.
Next offline: designprovensafeloadercompletionboundary/moduleownership, tests for
incomplete-linkerstate, failPID-boundcontrollerobserver; Mini-onlynativebuild.
MapcleanNOTreleased. Physicalprincipalrendering stillawaitingownerresponse.

## 2026-10-25 09:47 - inert bridge assets staged and hash verified; no activation

Owner answered parkedP/disconnected, then said proceed despite movement.
Fresh rechecks contradicted parked status: D31.2, D34.7, briefP/0, thenD55.4,
D43.8km/h. Did NOT restart/inject/set gate or touch display/power.
Completed authorized stage-only to prepare inert files, preceded by noinjector/
nomappedbridge check and saved old status/log tails. Exact mounted/staged service
MD5b61f0e4d verified, CP/AA visualtasks0, OEM6125. Controller.stage succeeded:
native7798416bytes SHA793449d908e1755c6821362049da39e0efda41f3647ed55951b5929550cddbdf,
launcher template969d2494814b84090b64b84d6fc75498b5fd0a8150ec8af0688ce781e1fdae0d,
full endpoint archive1a429e6d1118e429628ffde3d3172290dd339b39b1b5388cb36558d1797ee895.
Private /root/projects/haval-impulse/test-artifacts/bridge-132-stage-20261025T124529Z
contains preflight, saved prior logs, stage receipt, post-state/telemetry. Stock
processes preserved. Actual activation pending stablefreshP/0 and parked session;
do not treat transient0 asparked or override absentmodules/visualtask guards.
Owner confirmation alreadyexists but fresh telemetry MUST also agree before
cold restart. Prepared assets alone are not installed-pinned/endpointarmed or
proof of principalD0/secondaryD3frame. No Android build or source edits.

## 2026-10-25 09:43 - bridge preparation deferred: vehicle moving

Owner reported car on; readiness read-only confirmed installed132, process3260,
foreground/accessibility services, Minimalist, no Impulse crash-buffer mentions.
Owner then explicitly authorized bridge preparation ("Faca"). Fresh telemetry
09:42:43 showed engine1, gearD,44.9km/h, age1.2s. No staging/restart/injection or
power command issued. Asked owner to report parkedP with CP/AA disconnected.
Required answer pending; do not proceed merely because speed temporarily reaches0.
OEM service mounted/staged MD5b61f0e4d63535a2ef447be559211697b; injector available,
no visual CP/AA tasks or bridge/Frida mapping observed in OEM6125. Local exact
Mini native793449d9 and bundled agent1a429e6d plus manifest verified offline.
Private evidence: /root/projects/haval-impulse/test-artifacts/readiness-132-20261025T124112Z.
When owner confirms parkedP, recheck freshP/0 + visual projection/task absence,
then reviewed controller with explicit native+endpoint-bundle paths. Require
installed-pinned and endpointarmed; keep pinned injectors alive, no hot bypass.
Vehicle was started by owner; previous remote-session shutdown was already
fulfilled. No power commands in this session.

# Handoff

## COMPLETE — corrected132 installed; GWM shutdown explicitly confirmed

OwneraskedGWMstart/review/install132 andOFFwhenfinished. Workcomplete:
actualinstalled74132/1.0.0.74.132-preview fromc591cb53a80d89a92eebc79ec7dbbeb9e6e0d8b7.
Mini jobhaval-impulse-132-c591cb53a80d89a9-20261025T000314Z-1733 success4m48s,
81tasks,94JVMsuites/864tests=863pass/1skip/0failures/errors,all12newpolicycases.
GuardfullPASS,6Nodefrontendmodels,9Pythonpatchtests. Actualsignaturematches131,
manifest/ZIP/queuehashes/OEMassets/embeddedHTMLverified. CorrectedAPK76105795bytes
SHA6de3d0f410c46a2482dcecbbe9e76fcb92ec3574a197febff72b7999ce731198,
/root/projects/haval-impulse/artifacts/impulse-74132-overlays-release/impulse-74132.apk.

Devicehashofinstalledbase.apk EXACTmatch; cmdpackageinstall-rSuccess, actualpackage
132,MainActivitylaunchStatusok,process7836healthy,noappFatalExceptionobserved.
Minimalistselectionpreserved. Evidence/receipts/screenshots inprivate
/root/projects/haval-impulse/test-artifacts/gwm-132-session-20261024 andartifact
validation.json. Before-installcapture131satisfiedbaselinecontract; oldb266132
candidatearchivedneverinstalled. NewDefaultHTML1974322f...,CSS/XML/geometrysame.

GWMoffexplicitlysent2026-10-25T00:17:20.569Z,acceptedtrue; actualhyEngSts0with
freshupdateconfirmed00:17:30.756Z; cachedbridgeengine0/P/0kmh. CARLEFT OFF.
Do notsendanyadditionalpower-on. Installationauthorizationfulfilled; future
physicalphone/steering/secondarymapteststillrequiresanattendedparkedsession.
NoOTA/publicrelease,coldOEMrestart,Fridainjection,CPdisplaymove/resizeorAAwork.

Transportnotes: firstcarrootcurlcouldnotreachprivateTailnetHTTPport. Switchedto
temporaryunlistedHTTPSfileunderexistingCaddy/downloadsroute,NOserviceconfigchange.
Slowtransferhit120slimit; partialsaved. First15minGWMsessionautostopped. Owner
saidContinue; renewedcanonicalGWMstart00:14:01,engine1confirmed00:14:11, then
HTTPRange-resumeddownloadcompleted,hashverifiedbeforeinstall. Temporarystatic
file+directoryremovedimmediatelyafterdownload; HTTPserverstopped; remoteAPKs/gzip
removedafterinstall. Onlylogs/rcfilesremainintask-specificremotetmpdirectory.

NoGWMbridgeconfigurationedits. EnvVINlowercaseworksforreadsbecausegetCarInfo
uppercasesbutmustuseaccount-listcanonicalVINforcommands. Initialtwo551017
rejectionswereourdirecthelperparameterissue,notanaccountbindingproblem.
Posthealthinitialassertusedwrongprefkey(currentVirtualClusterTheme); corrected
readonlychecktoactualvirtualClusterTheme=Minimalist. Appwashealthythroughout.
Documentation-onlycommitafterc591recordsfinalresult,app/buildsourceunchanged.

## Latest — GWM on; baseline captured; overlay correction ready for Mini/install/off

Owner authorizedGWMstart,review/install132 andexplicitlyOFFwhenfinished; answered
remote startonlyelectric. GWMcarUtil.engine succeeded with canonicalVINreturnedby
getCarList. TwoinitiallowercaseconfiguredVINrequestsfailed551017 (notbound);
readqueriesuppercasedVINhadworked. No account/credential/serviceconfigchanges.
Canonicalstart23:57:09UTC succeeded000000;commandresult0,hyEngSts1confirmed23:57:39.
CurrentbridgeAPKfreshgearP/speed0;centralonline. Remoteoperationtime15min, doNOT
forgetexplicitGWMengineOFF +verify atworkend (canonicalVIN!), evenonfailure.

Private /root/projects/haval-impulse/test-artifacts/gwm-132-session-20261024 holds
powerreceiptandbaselinebeforefunctionalchanges: actual131,activities,display,
windows,preferences,processes,logcat,D0/D3screenshots. InstalledthemeMinimalist,
noactiveprojection; D3screenshotblackremotemode is notproofCarPlayfailure.
No coldrestart/hookinjection/CarPlaymove/resize orAAoperationthissession.

Functionaloverlayfixprotectsrule26: Sessionphysicalinputscope,bootstrapfalse,
neutralcardretention,identity/disconnect/resetclears; permissionbeforecanonical
cardincludingfullbatchsync. Defaultbothcallerssamepolicy,nonprojectionunchanged.
Restoredpolicy+12JVMcases,6Nodeactualhelper/callermodels,9Pythonpatchtests PASS.
Fullregressionguard NOWPASS: actualsessioncalls/policychecked, activethemepath
migrated,riskysourcecoverageextended, OEMhashesunaltered. No token-onlyfakefix.
RebuiltDefaultHTML/catalogidentical1840505bytesSHA1974322f5aec2ebf57abc5228fc5ea435822300855af57f8114177b1b509632f.
CSS/XML/geometry unchanged. Native/tools/Gradle unchangedfromb266132source.
Nextauditimmutablecommit, Mini signedrelease132, fetch/verifynewAPK, install-r
onlywhileparked/online, actualversion/healthcheck, thenGWMoffandverify.
Older132candidatekeptarchived, notinstalled. ManualCPwheelcasesremainunproven.

## Latest — offline overlay review; owner confirmed vehicle still off

Owner asked reviewing pendingitems and installing132, including a request to
"liga o carro". We explained no validated remotepower workflow and asked only
current parked/P/operator/central condition. Owner answered the car is stilloff
and "faremos depois". Live session deferred; no car connection/power/staging/
restart/injection/installation attempted. Installation authorization exists for
the requested132 workflow once the parked session is coordinated; do not infer
current safe conditions merely from future online presence.

Offline review completed, no functional/source behavior changed. Three stale
projector sentinels reflect one real missingphysicalinput/sessiongate; missing
policy deletedind3b5367a, oldimplementation/tests available691c5f4d. Current
projector uses card-backedmenu directly, physicalkeyevent only logs. Actualtheme
source moved but stillrequires cardId!=0 atbothrightmenurender sites, so neutral
card hidesalreadyauthorizedmenu. Existingboolean+menuscreengate andselected
display preservation remain. SignedAPKcontains exactrawHTML atres/xD.html SHA
3cabb47814a12aec575e25e62457506f48ea8a51238b0eb755e671824a41157b,1840492bytes.
These are staticfindings, notobservedphysicalfailures.

Contract Disciplina deMudanca item1 requires baseline/logs/screenshots before
functionaladjustment whenaccessible. Thus no blindrestoreofoldprojector or
token/pathchange tofakegreen. Fivecasebaselineplan savedin
docs/UPSTREAM_V9_SYNC_2026-10-24.md: bootstrapnokey, realkeymenu/AC, neutralcard
afterauthorization, teardown/newsession, camera/HVACKD0. Need currentthemeactual
andinstalledversion/stacks evidence. Then narrowport/test/rebuildMini ifneeded,
beforeinstallingchangedAPK. Existing132artifact/hash/signature unchanged.

## 2026-10-24 — 74132 integration candidate; installation deferred

BUILD COMPLETED: sourceb266cfe6a3af04be50ca1afd2e5eaaee6cf84875, Mini job
haval-impulse-132-b266cfe6a3af04be-20261024T232851Z-21744 success4m52s/81tasks.
93JVMsuites/852tests:851passed,1skip,0failure/error. ARM64/Kotlin/Java/lint and
signedrelease completed. Actualmanifest74132/1.0.0.74.132-preview and production
certificate5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0
verified onMini. Queuechecksums and localAPKZIP pass. APK76105763bytes SHA
c529fdc15ec66b1e82f9de29f15f059d6133455136f9b14067c34e55c682040c.
Artifact /root/projects/haval-impulse/artifacts/impulse-74132-release/impulse-74132.apk,
validation.json/build.log/checksums alongside. OEMassets unchanged, bridge absent.
Final documentation commit afterb266 changes no build/app/native sources. Branch
pushed toorigin rafaelcs28/haval-impulse. No PR15 edits, tag/release/OTA publication
or car access. Five legacyguard failures remain; installation still deferred.

Owner requested all new Netseek v9 commits and version132. Release worktree
`/root/projects/haval-impulse-132`, branch `release/impulse-74132-v9`, starts at
complete personal131 a427ddde. Cherry-picked namespace2f6e6961 and Java568522b8;
capture ordering6a6277bb was already identical (empty cherry-pick skipped).
Merged v9 through165359a3, retaining personal features, AA recovery and strict
roundabout angle retention. New upstream radar passed-point decision adopted;
two personal tests updated, rather than retaining contradictory assertions.
Roundabout null reset retained with new upstream progress. Source-native remains
identical2f6e6961; Frida tools identical568522b8. App defaults74132/1.0.0.74.132-preview.
Assembly depends on JVM tests; OEM patch rebuild stays manual, bridge excluded.

Offline:32/33 Python native-analysis tests pass,1 optional converter skipped;
8 patch/guard tests pass;9 endpoint and30 cold JS scenarios plus observer models
pass. An initial broad system-Python invocation lackedcapstone/elftools; rerun
passed with existing /tmp/carplay-audit-controller venv and private captured
OEM binaries. Initial direct Node commands omitted fixture arguments; corrected
runs above passed. Java bundle reproducible and matches prior1a429e6d hash.

Legacy overlay checker still has5 preexisting failures, including real missing
physical-input authorization semantics and movedtheme. Not waived/removed.
Corrected only falsepositive zero-prefix match in0.25f with4 regressiontests.
Review docs/UPSTREAM_V9_SYNC_2026-10-24.md. Audited immutable source build through
Mini only; receipt/job/APK validation to follow. No car access or deployment.
Do NOT reconnect/stage/restart/inject/install merely because the car is online.
Owner deferred installation until a fresh coordinated parked session.
Before that deployment reconcile overlay contract discrepancy. Independent map
and firstD3frame remain unproven. Historical notes below are superseded.

## Cold passthrough controller prepared — no vehicle run yet

Added `scripts/carplay-native-analysis/cold_passthrough_controller.py` and four
tests. It stages verified inputs over Telnet, validates the exact mounted/staged
cold-gate patch, refuses visual CP/AA tasks, terminates only the idle OEM host,
specializes the launcher for the fresh PID and requires `armed` then
`installed-pinned`. Property/nonce cleanup runs on every exit; failed-pinned is
never killed/retried automatically. No AltScreen, Surface, decoder, D3, SystemUI,
resolution or handoff action exists in this controller.

Authorized Mini job for source `6d7254627a97c9843c182709e5360976ccba3c3b`
was already successful. Its exact ARM64 bridge was copied read-only from the job
work directory into the coordinator artifact store; SHA-256
`015beb71138ce812fb25f3c2fd9afaa304a064e7859b534154c6c9981a2f0eac`, expected
bootstrap/data/display exports present. This file is not tracked or published.

Validation: four controller tests passed; complete Python suite in a fresh isolated
venv passed 24 with one optional converter skip; all 26 cold-interceptor Node models
passed; `git diff --check` passed. Car read-only preflight timed out because Tailscale
was offline, so no device state changed and OEM passthrough remains unvalidated.

Next: commit/push this source, then with the head unit online run `--stage-only` and
the explicit cold passthrough. Validate principal CarPlay D0 startup/disconnect/
reconnect before implementing endpoint delivery and the independent D3 Surface.

## 74130 — recuperação restrita ao mapa AA do cluster

Durante uma viagem na 74129, o usuário observou o mapa limpo do Android Auto congelado
somente no cluster. A tela principal do AA, o HUD nativo do para-brisa e o TBT sobre o tema
Minimalist continuaram atualizando. Coleta read-only confirmou os processos vivos, a Activity
principal em D0 apresentando frames e a Presentation de D3 montada. Isso isola a ocorrência no
canal de vídeo CLUSTER/TextureView; não prova qual guarda interna encerrou a recuperação.

Commit de runtime `7a588eed7bbb2e19aa2454844c7814d3eabcc9d1` corrige três falhas objetivas:
o orçamento deixa de ser vitalício e rearma após 60 s de frames saudáveis; recuperação em voo
expira em 15 s e faz uma segunda reconciliação limitada; ao sair de LIVE, o quadro antigo some
sem desmontar o consumidor. Persistência agora registra estados, aceite, pedido/ACK de release,
timeout e rearme. O ciclo continua restrito ao output CLUSTER e não reinicia AA, HUD ou TBT.

Debug passou no Mac Mini em 2m11s/49 tarefas. Release passou em 4m44s/83 tarefas com unit tests,
R8 e lint vital. Versão `74130` / `1.0.0.74.130-preview`, APK SHA-256
`9a9a91f9526ff383c53c1dda7f3362643f28d92acc57e820e90615762b9b7237`, 76.072.923 bytes,
certificado de produção `5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0`, igual à 74129
e à identidade fixada desde 74117. Release pública e `app-latest/app-preview.json` foram
publicados; ambos foram baixados e conferidos. Próximo passo é instalar manualmente a 74130 e
repetir uma navegação AA após sono longo. Se houver nova ocorrência, recuperar o logger
persistente antes de alternar a projeção.

## 74129 — teste combinado de um botão publicado

Release concluída a partir do commit imutável `13fb570e6dc29d295fa9acd53f2bd0ab2e7d4840`.
Debug Mac Mini passou em 2m14s/49 tarefas; Release passou em 4m50s/83 tarefas com
unit tests, R8 e lint vital. Versão `74129` / `1.0.0.74.129-preview`, APK SHA-256
`635a3b1b3ac55fe0c7a4a97ea61e863ebbaaf4861ba06e2049b91de3766ba932`, 76.072.927 bytes,
certificado de produção `5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0`.
Release pública e manifesto OTA fixo foram publicados; download CDN com cache-buster foi
baixado e conferido byte a byte pelo hash. O usuário fará a instalação manual mais tarde.

Recursos agora oferece `Iniciar teste CarPlay`; um toque inicia captura Binder privada,
HUD textual experimental e logger correlacionado do cluster por cinco minutos. Durante a
sessão o botão vira `Encerrar teste`. Timeout, parada manual e falha passam pela mesma limpeza,
liberando somente sua lease process-local do logger. O UUID aparece nos delimitadores do cluster.

Arquivos alterados: CarPlayCaptureScreen.kt, CarPlayNavigationCapture.kt,
CarPlayExperimentalHud.kt, CarPlayHudPolicy.kt, CarPlayHudPolicyTest.kt,
ClusterPersistentEventLogger.kt, ClusterPersistentEventLoggerTest.kt,
FeaturesHubScreen.kt e docs/architecture/carplay-combined-five-minute-test.md.

Risco restante: isto captura o estado real do cluster e testa o HUD no mesmo intervalo, mas
não ativa nem finge a Surface de vídeo secundária. O bridge nativo segue excluído da release e
o host de Surface segue Debug-only/sem caller. Não chamar ausência de mapa de regressão da UI.
Próximo passo: instalar manualmente a 74129, tocar uma vez em Recursos > Teste CarPlay,
conectar CarPlay, percorrer até cinco minutos e encerrar no mesmo botão. Depois recuperar os
JSONL privados e o log do cluster delimitado pelo UUID. Não rodar Gradle na VPS.

## Real Android preflight + isolated ARM64 smoke PASSED — 2026-10-23

See docs/architecture/carplay-android-validation-2026-10-23.md. Car user says off,
charging, not inside. Fresh telemetry before/after P/0/engine0, age1s. No power,
charging, OEM restart or APK-install command. Do not turn the car off on our behalf:
we did not start it. Resident CarPlay service is not proof a phone is connected.

Android9/API28/ARM64, installed74127, OEM PID6495/app6170. Existing Frida17.5.2.
Real bounded /proc/PID/mem reads verified all 3 complete RX segments +13 bodies
against v3; same process start throughout. No Frida injection into OEM. Cold
installer rightly requires an earlier startup window, unavailable in loaded process.

Audited 9ceda01b Mini job haval-74126-work-9ceda01bac59c0b9-20261023T124407Z-5277
passed in2m14s/47 tasks, scanner1063. JVM837 total/0 failures/errors/1 radar seed skip.
Native ELF5477688B SHA e7c12e3b520a704aa3dc81ae9cc2e35965896b1d7c9890407b690af80db66223.
Debug APK unchanged byte-for-byte 8fd85d...; native remains excluded. Internal only.

Source32a030d8 adds guarded Android smoke JS. Ran actual new ELF in disposable
sleep PID1874 only: ABI1, invalid/missing-OEM bootstrap rejection, existing-module
observer, real ARM64 PC-relative literal replacement/original trampoline/revert all
passed. No active native bootstrap/OEM module in probe. Child exited naturally;
temporary files removed. OEM start/version unchanged and no experimental mapping.

Transport lesson: this Telnet retains IUCLC, corrupting Base64 even after stty
attempts. Lowercasing local Base64 reproduced exact wrong hashes. Gzip/lowercase
hex over noninteractive awk/xxd receiver passed size/SHA. Failed copies removed.
HTTP temporary port timed out, ADB unauthorized; no security/key/firewall changes.
Private receipts test-artifacts/carplay-android-preflight/20261023T123756Z, build
artifacts/carplay-android-9ceda01b. Local reusable driver /tmp/run-isolated-android-hex.py.

Latest user asks next step and authorizes a new version if useful for CarPlay.
No release yet: no app activation caller, packaging excluded/Debug-only consumers.
Next real gate is controlled cold OEM passthrough, then authenticated app/Surface/
exclusive cluster and secondary-video integration. Do not promise clean-map APK
from successful isolated smoke. Future useful combined release: same .117 key,
next free version, no automatic car power/install while user absent/charging.

## Cold initial interception and loaded-image attestation — source only

Added cold_oem_interceptor.js, cold_profile.py and initial_entry_bootstrap.{h,cpp}.
See native/COLD_INTERCEPTION.md for implementation/trust boundaries and seven
private REA initializer Evidence IDs. Default inert; explicit expected PID and
com.ts.carplay cmdline required. All OEM modules must be absent before observer
registration; existing/warm/raced modules are refused. The last new onAdded event
attests whole files, build notes, full RX segments and thirteen function bodies,
pins the three OEM modules and bridge with native RTLD_NOLOAD, reattests, then
uses replaceFast to obtain nine originals and creates/pins the passthrough bridge.

Manifest v3 canonical digest 31b878ba35c1a7fec07d7e854264837ef3702962a1e059fa5d020afa0d7c4d0f.
Earlier v1/v2 manifests unchanged. Bridge ELF executable bytes also checked against
trusted build file/hash; original CF helpers checked against pinned dlsym symbols.
Successful script/native ownership remains pinned. Rollback verifies pristine code
before releasing preparation; uncertain publication/rollback keeps failed-pinned.
No successful uninstall, warm replacement, force-stop/restart or car command.

Final checks: 26 Node Frida-model cases, 15 native portable modes with ASan/UBSan
and TSan (30 runs), Python 17 total/16 pass/1 converter skip. Binaries:
/tmp/carplay-cold-final-{asan,tsan}. Native success on actual Android/ARM64 and
Frida trampoline relocation/module-observer timing remain untested. Captured stack
initializers traced through containers and CarPlayMsg mutex/cond setup; bounded
static evidence, not live startup proof. No Android build/version/signing/OTA.

Next is authorized Mini compilation/bridge ELF review when requested, then a
controlled early cold-device passthrough test. Still need secure app/UID/token/
socket provisioning, real disconnect/teardown source mapping and AA/safety feeds.
Native library remains excluded and no production entry activates this script.

## OEM C entry bridge, live capture and Unix permit transport — source only

Added live_session_capture, hook_permit, oem_entry_bridge, unix_hook_endpoint and
native/OEM_ENTRY_BINDING.md. Nine noexcept C entries now feed CoordinatedDispatch;
its observation hooks retain UUID before original Start, capture only successful
principal Start nested in observed same-thread StartSession, and invalidate on
stop/finalize including a pending Start. Session replacement/old return races use
separate nonwrapping session epoch/capture revisions. Explicit teardown entry is
admitted through the same gate. No adoption of an old unobserved stream/log pointer.

HookPermit checks exact OS UID, 32-byte token, request ID, deadline and capture
revision, single-use under mutex. The authorized PreparedHooks wrapper rechecks
the authorized generation and deadline through publication, not just initial auth.
UnixHookEndpoint owns a duplicated connected local packet FD, validates kernel
SCM_CREDENTIALS against SO_PEERCRED, rejects extra/oversized payloads/rights and
closes received FD copies. No listener or Android client/socket provisioner exists.

Offline entry_profile.py verifies nine actual target offsets plus four CoreUtils
APIs on the exact captured ARM64 files. Canonical digest 2cd70fbc84b06ef7b69379f83f8629e0baea55a93704cfc852162dbea7c33940.
Original seven-function observer digest remains unchanged. REA retained CFRetain
and CFRelease wrappers verified; Evidence IDs are in OEM_ENTRY_BINDING.md.

Final portable validation: 14 modes passed ASan/UBSan and TSan (28 executions);
Python 16 tests, 15 passed/1 preexisting converter skip. Source uses fake OEM/CF
APIs/module owners but real Linux socket credentials in unix-peer. No Android
build, new version, signing, OTA, car access or production activation. Binary paths
/tmp/carplay-oem-complete-{asan,tsan}. Prior Mini receipt covers older source only.

Still missing: actual initial inline interceptor, attestation of loaded OEM module
bytes/trampolines, trusted UID/signature/token/socket bootstrap, Android app client,
real disconnect/teardown source mapping and proof all old calls retired. Native
packaging remains excluded. Do not claim this source has installed hooks in OEM.

## Concrete dispatch admission/publication backend — source only

Added coordinated_dispatch.{h,cpp} and COORDINATED_DISPATCH.md. Shared entry count
covers the seven video entries plus session/getter routed through this object;
tryFreeze rejects active calls instead of waiting/draining unknown OEM threads.
New arrivals wait while frozen. Complete table changes once under the gate;
scoped thaw restores admission on cancellation/exceptions and commit. Ordinary
nested callbacks supported; freeze-owner reentry rejected instead of self-deadlock.
DispatchPreparedHooks plugs this real dispatch logic into HookInstaller and pins
native ownership. No original Initialize during synthetic warm installation.

Added dispatch and dispatch-install modes. All eleven modes passed final g++
C++17/Wall/Wextra/Werror with ASan/UBSan and TSan (22 final executions). No reports.
See README command; binaries /tmp/carplay-dispatch-final-{asan,tsan}. Portable
host tests only. No Android build/version/signing/OTA/car commands.

Important: no actual OEM initial-entry interceptor, authenticated Binder bootstrap,
loaded-module attestation or real live capture provider exists. Gate cannot drain
calls predating interception or cover teardown events outside it. Full platform
backend remains incomplete. Source native CMake updated; Mini not rerun for this
source. Keep packaging exclusion/default off and validate overhead on hardware.

## Hook installation coordinator implemented — source only

Added native/hook_installation.{h,cpp} and HOOK_INSTALLATION.md. Default off,
no preparation backend/call site/export/JNI activation. Explicit trusted prepare
capability must authenticate and attest live targets; its transaction must drain
all OEM ingress and freeze principal epoch, then publish all hooks atomically.
Coordinator captures/validates/adopts principal without Initialize, checks epoch
again, pins module/UUID ownership in adapter before process registration, then
publishes under the quiescent transaction. A competing process owner rejects.
Publication failure consumes the process slot, closes secondary and pins resources;
no unload/reset/retry. Backend must restore all originals before returning false.

Portable fake-backend tests added to oem_runtime_adapter_test: install,
install-fail, install-contested, plus existing six modes. These are not a real
platform hook engine/authentication/vehicle proof. Native CMake includes source;
no Android build or new version/OTA. Real loaded-module authentication, hook
backend and app/AA/safety feeds remain implementation gates before deployment.
Validation: g++ C++17 -Wall -Wextra -Werror, all nine process modes passed both
ASan/UBSan and TSan on final source. Commands are in native-analysis README;
outputs /tmp/carplay-hook-final-{asan,tsan}. No sanitizer reports.

## Static OEM getter caller contract traced — 2026-10-23

See docs/architecture/carplay-getter-ownership.md for exact hashes, ELF/Ghidra
coordinates and six retained REA Evidence IDs. New read-only REA xrefs/function
queries traced getter -> stack displays delegate -> platform delegate -> server
info dictionary. Getter-owned replacement/release-once is supported for the exact
captured artifacts. Outer delegate adds CFRetain; do not duplicate or compensate
it in the getter hook. Entire OEM lifetime/refcount balance remains unproven.
No runtime source change/build/OTA/car operation. Existing Mini validation remains
the tested baseline. Next: authenticated quiescent installation with attested
live modules/principal identity, then real exclusive display ownership feeds.

## Combined Mini validation PASSED — source 1f1a1f7d

Private immutable source 1f1a1f7d9d6bb063377da7a170da7ac55ae5f51e pushed to
release/impulse-74125. Audited control digest
2a5246d117daef016b4bf08d06883b7b0f80de7cdceeb4f58491a654b5f55819.
Job haval-74126-work-1f1a1f7d9d6bb063-20261023T101355Z-5837 succeeded in 2m19s,
47 tasks including Java/Kotlin, ARM64 JNI/CMake/link, testDebugUnitTest and Debug.
XML totals: 93 suites, 837 tests, 0 failures, 0 errors, 1 skipped (RADAR_SEED_OUT
unset for RadarSeedGenerator.generate). Seven experimental Debug JUnit methods
actually executed and passed: policy 3, retirement 4. AA telemetry 12, roundabout
tracker 6 and radar engine 19 also passed. This is not GPU/Binder/vehicle testing.

Fetched SHA256SUMS verified. APK 148581473 bytes, SHA256
8fd85d22552d39a757aa936d3063b500994e378e5d03b4be5633e4e4f825a125.
Four ABIs retained, bridge/shared C++ runtime absent; four experimental classes
present in Debug DEX. Manifest unchanged (a2565b90f5bb8bda888b424c18759c3a73a2e9db596ac84f65a24cdce1360e30).
Actual linked AArch64 ELF SHA ee71ae6f66b23c6dc69c310724cc2efefee8da2f540ad0a54b6b5d9a06c0ddc3;
both JNI exports and all runtime C entries verified on Mini. Native .so excluded.

Private receipt/artifact: coordinator artifacts/carplay-combined-1f1a1f7d/VALIDACAO.md.
Internal Debug only, NOT production-signed, do not install over vehicle release.
No version bump, OTA, production signing, car commands or local Android builds.
Next gates remain real getter ownership/module/live-principal proof, authenticated
quiescent OEM loader, actual app/AA/safety provider and parked hardware negotiation.

## Combined Mac Mini validation preparation (historical, completed above)

User approved proceeding with compilation/JNI and unit validation, without version
change or OTA. Preparing an audited immutable private commit containing the Netseek
guarded port and experimental CarPlay source. assembleDebug now depends on
testDebugUnitTest so the existing authorized Debug queue runs the JVM suite,
including seven previously unexecuted src/testDebug methods. No release signing
or install is requested. Native library stays excluded and no app entry enables it.

## OEM C adapter/getter and explicit warm adoption implemented, no build

Added oem_runtime_adapter.{h,cpp,test.cpp}, noexcept C callbacks/session/getter/close
and diagnostic entry points. C++ faults retire secondary only; not a native signal
or bad-pointer recovery mechanism. Enabled getter requires owned original-return
contract and nonthrowing release. Successful replacement releases original once;
failure preserves original pointer/status. Null/error original results pass through.

Getter-specific private path permits repeated/concurrent immutable advertisements
on one generation without new decoder. Ordinary advertisement API stays single-use.
Commit is the ownership linearization point: a pre-close committed response can
return after close; secondary remains revoked. No promise of withdrawing that array.
One atomic registration reservation covers legacy ingress and runtime adapter,
preventing stacking/replacement. Adapters/modules stay pinned for process lifetime.

Found warm-start requirement: host requires CarPlay already on D0, so enabling a
bridge without the existing principal record would reject that stream's frames.
Added explicit adoptPrincipal through core/ingress/runtime/C API. It validates known
principal type+UUID, binds an existing stream and NEVER invokes OEM Initialize.
Actual loader must prove live principal/epoch, retain UUID and quiesce callbacks
before hook switch; no first-stream guesses, stale pointers or automatic adoption.

Six adapter process modes passed ASan/UBSan and TSan: off/enabled, legacy-first,
registration race, warm adoption and close-getter. Includes concurrent repeated
queries, exception containment, release accounting, original error/status preservation
and primary continuity. Prior ingress/session/runtime regressions recompiled/passed.
No actual OEM getter, Surface/codec/phone or Android execution; all fixtures synthetic.

Source C adapter is not a loader: no hook installation/injection, live attestation,
app entry or enabled Android bootstrap. No build/version/commit/push/signing/OTA/car.
Seven Debug JUnit tests remain unexecuted; pending combined Mini validation and
parked capture/ownership/lifecycle/negotiation tests. Native library stays excluded.

## Native runtime integration implemented and host-tested, no build/version

Added secondary_runtime.{h,cpp,test.cpp}: ties ingress, advertisement and decoder
factory to one exclusive session generation, default-off exact OEM passthrough.
Enabled runtime validates matching bridge/display identities, readiness, APIs and
owned module/UUID lifetime token (not binary attestation). Secondary session/factory
creation requires committed advertisement. Geometry wrapper rejects a different
video size. Close revokes captured generation only; old runtime cannot stop a new
generation or principal. SecondarySession.advertised/bindRuntime support the gate.

Android runtimeForSurface now checks actual native window dimensions, consumes
the explicit clock-backed factory, retains session/generation and constructs the
runtime; failure revokes captured generation. Adapter/NDK not compiled this batch.
No C/JNI enabled bootstrap or OEM hook installs it; existing C bootstrap is still
passthrough-only. Future loader must bind exception-contained C trampolines,
verified originals and retained CFL/module lifetimes, with quiescent teardown.

Six portable integration scenarios passed ASan/UBSan (20 repeated runs) and TSan:
default passthrough; ad before secondary create/configure/NAL conversion/frame;
principal unaffected by close; duplicate/mismatched runtime; stale-generation
close; geometry mismatch; primary delivery during blocked secondary submit/close.
Callbacks/arrays/decoder are synthetic, NOT actual OEM, Surface or decoded video.
Session regression recompiled and passed with current generation binding code.

Still no new Java/Kotlin/Android build, commit/push/version/signing/OTA/car action.
Seven Debug JUnit methods remain unexecuted. Worktree includes Netseek fix and all
experimental source. Next gates: authenticated actual loader/getter ownership,
authoritative display/AA/safety provider and app entry, clock/physical measurements,
combined Mini validation, then parked hardware negotiation/lifecycle tests.

## Experimental cluster host connected to consumer in source, no build

Added Debug-only CarPlaySecondaryClusterHost and CarPlayClusterHostPolicy, plus
three src/testDebug policy tests (not compiled/executed). Host owns only its child
in an explicit empty FrameLayout on D3. It creates no Presentation or app entry
point and changes no production layout, manifest, resolution or native handoff.

Requires fresh <=1000ms elapsedRealtime snapshot affirming opt-in, exclusive slot,
known AA inactivity, native CarPlay on D0, safety overlays/other apps clear. Direct
read-only checks also deny AA session/request/Surface or non-D0 CarPlay state.
Clock includes deep sleep so cached authorizations expire over vehicle sleep.
Provider must be fast/nonblocking and authoritative, not initial STOPPED defaults.
Checks on start, pre-draw, grant ACK, onInputsChanged and active-only 250ms poll.

Close detaches only its own consumer and unsubscribes callbacks; onHidden precedes
onRetired, which waits for GPU queue retirement. Consumer.isRetired handles start
failure before listener registration. Quarantine may retain view/context until
recovery; single-consumer gate bounds it. No automatic resume or AA mutations.
This is not a global atomic display arbiter: real owner/safety/AA event integration
still required, as are authenticated OEM bootstrap, clock and real negotiation.

Native session ASan/UBSan/TSan and advertisement/lease regressions passed again;
these do not validate new Java. No Java/Kotlin/Android build, version, commit/push,
signing, OTA or car command. All source remains in /tmp/haval-74126-work, including
the Netseek patch. Combined Mini compilation/unit tests and parked testing pending.

## Fresh display-3 consumer implemented in source, no build

Added internalDebug Java CarPlaySecondarySurfaceConsumer + CarPlayConsumerRetirement
and four src/testDebug JUnit methods. View starts hidden; explicit start requires
main thread, attached hardware-accelerated display 3, authenticated endpoint/token
and generation, and no existing texture. External SurfaceTextures are rejected.
It creates Surface from its own new TextureView callback, never the main output.
No call site, manifest entry, Presentation or display-policy change added.

Serial worker handles grant/revoke. onReady is grant acknowledgement only, not
video/maps. Size change closes the one-shot consumer. Framework texture destruction
returns application release ownership, retained until revoke ACK/peer death and
detachment. Unconfirmed revoke quarantines one queue and blocks new consumers;
resource releases execute outside the state monitor with a single drain owner.
explicit retry/peer death can finish. Host must close AND detach/remove the view.
Hung Binder can hold that worker/gate; GPU/codec teardown requires parked testing.

New Java/JUnit NOT compiled or executed per source-only/no-version instruction.
Native session ASan/UBSan + TSan, advertisement and lease host regressions rerun
successfully; these do not test new Android code. No Gradle/NDK build, commit/push,
version, signing, OTA, installation or vehicle command. Netseek port preserved.
Remaining: real host/AA visibility ownership, authenticated OEM loader/bootstrap,
clock validation, linking onReady to coordinated negotiation, actual iPhone/video
acceptance, and combined Mini validation. Library stays excluded; no working map claim.

## Generation coordinator implemented, no build/version

Added secondary_session.{h,cpp,test.cpp}: explicit default-off session, exclusive
lease binding, monotonic generation, one successful publication, and serialized
publish/revoke. Final owned-array handoff holds session and lease guards, including
against direct decoder retirement. Preparation remains outside the session lock;
stale/closed preparations cannot publish. Duplicate close waits for resource stop.
Publisher must be noexcept, prompt, non-reentrant and only transfer array ownership.
Factories/backends cannot reenter the coordinator under lease locks. Callers must
pin coordinator/config/borrowed array/UUID/module lifetimes until they finish.

Surface registry now retains session/generation after explicit factory creation;
JNI revoke removes the registry entry then closes the session. surfaceRouteReady
means active before/after publication, while session.ready means not yet advertised.
New CMake source and Android changes remain uncompiled per user's no-build request.
Host ASan/UBSan and TSan session tests passed, including blocked prepare/publication,
direct revoke, stale generation/rearm, duplicate close, backend failure, concurrent
single publication and another session progressing. No actual OEM publish tested.

At that earlier stage no loader/consumer/bootstrap/phone negotiation or map rendering
was added; the newer consumer source is described above. Close does
not withdraw an advertisement already handed off; callback quiescence still needed
before stream-address reuse/unload. Netseek port remains in working tree for later
combined version. No commit/push, APK/version, signing, OTA or car operation.

## Netseek imported and CarPlay continued — no version/build requested

User authorized importing 7c4f9fc6 and continuing CarPlay, explicitly postponing
a new version until a single combined release. Functional upstream port applied
manually to HudOverlayController and AndroidAutoNavigationTelemetry, preserving
our RoundaboutTracker/radar guards. Retention requires zero/zero EXIT, same
direction, complete prior pair. Four new Kotlin test methods cover road rename,
code 8, ENTER/direction change, partial/malformed data and reset/route boundaries.
Kotlin/JVM tests NOT executed; no Android/Gradle/NDK build submitted or version bump.

CarPlay: new secondary_advertisement.{h,cpp} prepares an owned copy of the display
array and appends the captured AltScreen API only with explicit geometry/identity
and route readiness. Default-off returns untouched borrowed original. RAII/move
ownership and rollback covered with fake arrays under ASan/UBSan. No runtime hook.
Added DecoderLease.active and surfaceRouteReady(handle); lease host ASan/UBSan
and TSan passed; existing bridge/ingress host and observer Node mocks passed.
New Android sources/native additions require future authorized Mini validation.

Two readiness checks alone are not atomic with revoke; the source coordinator
above now guards commit/generation/retirement. Independent consumer, authenticated
OEM loader/bootstrap, clock validation and actual iPhone negotiation remain gates.
Changes currently in the working tree /tmp/haval-74126-work; no commit/push or
build performed for this batch. No OTA, signing, installation or car command.

## Independent Surface transport compiled on Mac Mini

Added Debug/leanDebug-only CarPlaySurfaceEndpoint Binder class (no manifest,
registration, call sites or implicit library load), exact UID/32-byte session
token, single generation/grant, remote owner death and explicit revoke. Native
bounded handle registry imports its own ANativeWindow reference; a one-shot
surfaceDecoderFactory requires explicit clock conversion. Portable DecoderLease
serializes revoke/submission and retires only its own backend. CMake includes
both new native sources, still excludes .so from APK. Release Java unchanged.

Host lease ASan/UBSan and TSan passed; existing Python 14, observer Node mocks,
bridge/ingress host regressions passed. Actual Binder/Surface/JNI not exercised.
Source 7e707bccc120d0eab4319a1d02ea1cc2e1bbfbc5 pushed privately. Mac Mini job
haval-74126-work-7e707bccc120d0ea-20261023T083027Z-20507 passed Java compilation,
ARM64 CMake/link and assembleDebug in 1m55s (40 tasks). Native ELF64 AArch64 SHA
cf576150f93cb2f6d0ba088b2aa4cfa6bd95d574a97a926d6b44c12ea610841c;
both JNI Surface exports and surfaceDecoderFactory verified. Fetched checksums
passed; APK SHA bc136b0c1b375bc27a7d0462e9945dc18b01866542efa7ae1083f251ab7ec01f,
148548705 bytes. Debug class present in DEX; manifest unchanged from prior build,
four ABIs preserved, native bridge/shared C++ runtime absent. Internal Debug only,
not production signed or installable OTA; no JVM test suite in this job.
Receipt: artifacts/carplay-surface-7e707bc/VALIDACAO.md on coordinator.
New consumer/cluster Surface,
authenticated loader/bootstrap, clock validation and advertisement remain gates.
Surface provenance must prove independent BufferQueue; pointer inequality cannot.

Static REA confirmed captured AltScreen API passes type 111 to shared dictionary
builder (accepts 110/111), with DWARF argument widths. Evidence retained privately;
see architecture document. No iPhone acceptance proven, no advertisement enabled,
no car access, OTA or installation. Production remains unchanged.

## OEM ingress adapter and bounded observer validated on Mac Mini

Added native/oem_ingress.{h,cpp}, C ABI exception containment, pre-Initialize
thread-local identity, captured 32-bit CFL string API and one-shot process pin.
C bootstrap is passthrough only; no OEM hooks installed or APK inclusion enabled.
Original targets must be unmodified/trampolines and stay loaded; no unload API.
Added hash-pinned offline profile generator, inert explicit-start Frida observer
and Node mock tests. Hooks instrument code and need parked validation; no pixels
or raw UUIDs captured. Finalize event denotes entry, not resource-release proof.

Validation: Python 14/14 with private binaries and converter; Node syntax/mock
tests passed; ingress passthrough/enabled tests passed ASan/UBSan and TSan.
Source 8e2f20468bbee92bfacb7cc208c82bcae92c297c pushed privately. Mac Mini job
haval-74126-work-8e2f20468bbee92b-20261023T013650Z-32539 succeeded in 2m;
40 tasks executed, including configure/build/link ARM64 and assembleDebug.
Actual ELF64 AArch64 exports all 11 impulse_carplay C symbols, SHA256
807edb5c8e7ffedb3ecca6bf4976ac189a7c72312477ba0f7b65ffd89cd7c1de.
Fetched SHA256SUMS verified. APK excludes bridge/shared C++ runtime and keeps four
existing ABIs. APK byte-identical to previous internal validation artifact:
befc0fc48f44901f2dedf39fb85d267de6cbcccc22ea15a9b36082da96342fab,
148548705 bytes. Expected: all new native work remains excluded from packaging.
Not production signed; do not install. No JVM unit suite in this Debug job.
Private receipt: artifacts/carplay-oem-ingress-8e2f204/VALIDACAO.md on coordinator.
No local Android build, OTA, install or vehicle command. Production 127 unchanged.
Static REA decompilation confirms captured UpTicks uses monotonic nanoseconds;
frame path also supports synchronized NTP conversion. Runtime calibration pending.

Remaining gates: actual loader/interception, independent Surface IPC, second-video
advertisement/negotiation and parked lifecycle/priority tests. Do not claim clean
CarPlay map works. Current sources keep Android runtime and OEM packages untouched.

## Native bridge validation completed on Mac Mini

Built source 07567766ab6746a5fcc10ca2dfa64a06463e9915, pushed privately to
release/impulse-74125 after explicit authorization. Reviewed control digest
cc9c51fc77190f0f698437021ee3a7ba59b4c56536681efdbd087c958ceb8f15
allowlisted. Job haval-74126-work-07567766ab6746a5-20261023T011011Z-24713
succeeded: ARM64 CMake configure/build/link and assembleDebug, 2m9s. No new
Android JVM unit suite was run by this Debug job.

Fetched artifact and checksums verified; ZIP confirms bridge .so and shared C++
runtime absent, existing four ABIs retained. APK is internal Debug validation,
NOT production-signed OTA and must not be installed over the car release.
SHA256 befc0fc48f44901f2dedf39fb85d267de6cbcccc22ea15a9b36082da96342fab,
148548705 bytes. Private receipts at coordinator artifacts/carplay-native-bridge-0756776.

Host bridge also passed ThreadSanitizer in addition to ASan/UBSan. AVC parser
10,000 seeded cases; NAL converter 14 fixtures + 10,000 cases; Python 12 passed.
Runtime remains entirely unwired/excluded. No loader, Surface transfer, clock
calibration, secondary advertisement, user toggle or Android runtime test yet.
No OTA publication or vehicle operation; do not imply clean-map functionality.

Mac job native output also inspected directly (read-only): ELF64 AArch64 shared
object SHA256 839d58ce5a8e2759e7d2fbce19e47e1c1c7942103facd63fb193cfa1cd54e174.
This confirms an actual linked native binary, intentionally excluded from APK.

## Experimental native bridge implementation and authorized validation

User authorized commit/push to private repository and Mac Mini compilation,
explicitly without OTA publication/vehicle installation for this stage.

New VideoBridge core defaults to OEM passthrough; enabled mode requires identity
before Initialize, independent secondary factory, bounded copies and no OEM
fallback for retired secondary streams. Includes AVC configuration parser and
AndroidSecondaryDecoder (own codec/window/worker, explicit tick converter).
Host ASan/UBSan bridge fixtures and AVC parser tests passed; backend requires Mac
Mini ARM64 compilation. Android build config pins installed NDK27.1/CMake3.22.1,
builds only this native ARM64 target with static C++ runtime and excludes its .so
from APK packaging. No runtime loader/JNI/IPC/advertisement or UI added.

Remaining integration gates are explicit in README/architecture: pre-creation
OEM interception, independent Surface transfer, clock calibration, ABI-boundary
exception handling and callback quiescence on pointer reuse/destruction. Host
tests use synthetic decoder/factory; they do not prove AMediaCodec behavior.
Do not install a build or remove the native packaging exclusion as a shortcut.

## Native component implemented; initialization-order gate identified

Added native/packet_conversion.{h,cpp}, observed_video_abi.h, host CLI and C++
fixtures under scripts/carplay-native-analysis. No Android linkage or hook.
ASan/UBSan: 14 fixtures + 10,000 seeded malformed-input cases passed. Actual
C++ conversion of a ten-frame H.264 fixture preserved decoded hashes. Twelve
Python tests passed with CARPLAY_NATIVE_DIR and CARPLAY_PACKET_CONVERTER supplied.

Ghidra 12.1.4 now installed privately at /root/.local/share/haval-analysis, official
archive checksum verified; existing JDK 21 used with command-local environment.
No persistent REA/global Java changes. REA CLI full Evidence/snapshots privately
beside native capture; corrected Ghidra base is 0x100000, not ELF zero base.
Stack DWARF confirms callback ABI and 96-byte ScreenStreamPrivate. Header is an
observed layout, not permission to cast/hook arbitrary builds.

Critical next design constraint: StartSession calls Create/Initialize BEFORE
SetDelegateContext/Start(UUID). A seven-slot hook that classifies only at Start
would already have touched the singleton decoder. Need identity above creation,
verified lifecycle/reentrancy, then isolated AMediaCodec receiver and reversible
startup integration before secondary-display negotiation. No APK/OTA or car access.

## Continued native investigation and real host decoder experiment

Completed a further offline pass rather than requesting another continuation.
CoreUtils ScreenStreamCreate explicitly dlopens libcarplay_proxy_ts.so and dlsyms
ScreenStream callbacks. AirPlay maintains per-string stream contexts; the string
and integer propagate through StartSession/Start to the stack, whose observed Start
body discards their meaning and retains the shared platform context. Numeric type
semantics and replacement/namespace safety remain unproven. On the inspected
ProcessFrame branch, completion/context are null; do not generalize to every caller.

audit.py now preserves adjacent printable ADRP/ADD literals; added an exact-library
loader regression and test_dual_decoder.py. Eleven tests passed (5 portable,
5 captured-artifact, 1 real FFmpeg H.264 decoder isolation experiment). Secondary
stop/restart with changed dimensions did not stop the principal Linux process.
This is NOT OEM/Android decoder validation or map-clean support. No Android build,
APK, commit/push or car command. Private lifecycle/frame/CoreUtils disassembly and
loader-audit JSON are beside the native capture on coordinator.

Remaining engineering gate: real native receiver/loader integration preserving
the principal OEM video, and verified advertisement/negotiation. Host decoding is
not a reason to ship a flag-only APK. See the architecture report for exact limits.

## Autonomous clean-map investigation follow-up

User requested continuing useful work without repeated follow-up commands. No
vehicle operations are needed for the current offline stage. Captured native
libraries are private on coordinator in test-artifacts/carplay-apks/20261022-205614-telnet;
car was shut down with fresh P/0 km/h telemetry after collection.

Added scripts/carplay-native-analysis/{audit.py,test_audit.py,requirements.txt,README.md}
and docs/architecture/carplay-secondary-video-investigation.md. Read-only hash-pinned
AArch64 audit validates PLT→GOT and resolves the seven gVideoCallback relocations.
Nine tests passed, including four using exact captured binaries; git diff --check
passed. No Android build, source commit/push, OTA or runtime modifications.

New finding: proxy CarPlayVideoInit stores a callback table; ScreenStream wrappers
forward to seven stack callbacks. Candidate integration boundary only: indirect
linker resolution, ABI, lifetimes, completion and stream identity remain gates.
Existing global media codec/window/platform context prevents assuming a flag-only
second stream. Next: ABI/ownership analysis, then actual offline dual-decoder proof
and reversible native integration design. No generic synthetic policy test can
prove actual independent video or clean-map support from the iPhone.

## Current task

Offline APK analysis completed after the 74127 drive; no new APK built. Private technical
report and 61 REA/JADX Evidence records are on coordinator under test-artifacts/carplay-apks/
analysis-20261022. NavigationProxy confirms raw 1 left, 2 right, 4 driving-side-dependent
U-turn, 12 destination flag, 47 sharp-left. Raw 28..46 encode roundabout exit raw-27;
compact image table has 35 entries with index raw-19 for 47..53. Four central methods
match the located stock app reference; nine selected glyphs have identical decoded pixels.
Installed APKs match the fork's bundled carplay_patches assets. Original service binary
03cc69d1... not found; don't claim a full original-service diff or factory authenticity.
Time units remain unconfirmed. Cache is composed from partial updates; stale text/angles
can survive state changes. SourceName/description upstream are dropped before the public
11-field Binder, so do not extend the Parcel reads. METER/HUD RemoteViews path exists
statically but no secondary video/map or active cluster integration is proven. Next proposed
release would add only verified HUD glyphs, with unknowns blank and no ETA, preserving AA.

## Previous delivery status

Current follow-up: user requested experimental CarPlay guidance. Built source
adds CarPlayHudPolicy, CarPlayExperimentalHud and tests, routes only the two AA native-HUD
calls through a priority selector, and adds a session-only switch in CarPlayCaptureScreen.
Capture lifecycle supplies generation-scoped inputs. This phase is native HUD text only;
no arrows/codes/ETA are inferred, no theme/bridge/telemetry or AA map change. Full design,
cleanup limits and test procedure: docs/architecture/carplay-experimental-hud.md.
Source diff validation and full Mac Mini unit/release/R8/lint vital validation passed.
User approved commit/push and signed 74127 generation, without automatic installation.
Audit the immutable commit and use the Mac Mini release queue; verify tests, production
certificate and downloaded OTA before reporting readiness. No live validation claimed.

IMPORTANT scope clarification from TelasScreen: existing native HUD targets physical
display 4096 (windshield), not cluster display 3. Current implementation does NOT add
guidance to the cluster. User was informed and explicitly chose to receive 74127 for
the windshield-HUD test; publication may proceed after artifact validation. Cluster
integration is deferred separately. Unit results: 826 tests, zero failures/errors, one skipped; all 14
new policy tests passed. Build source: 3fc8ce4f203655f7e0fb0128199793a6aed6b558;
Mac Mini job haval-74126-work-3fc8ce4f203655f7-20261022T223038Z-14173.

74127 published; downloaded APK and fixed OTA manifest independently verified.
APK SHA-256 ccb606475373170680327dc96f93088955630afb0eb1f4229ef20ff1ea8bf0e3,
76072475 bytes; certificate matches pinned 74117 identity. User explicitly authorized
remote start, parked installation/version verification and shutdown afterwards.
User later took over manual installation. Coordinator automation was terminated at
download_started BEFORE any package install command. Do not claim 74127 installed.
First start failed at Telnet and shutdown was confirmed after an off retry; second
engine-on did not produce a started state and ended with fresh engine_state 0. Final
download-only attempt used the already-online head unit in P/0 km/h with engine_state 0.
Do not send further vehicle commands; user resumed control and is going to the car.
Receipt/details: coordinator artifacts/ota-74127/install-receipt.json and
manual-install-status.txt. Feature remains off by default. OTA 74127 is ready for manual
installation while parked in P. No navigation/visual test of 74127 has been performed.

Latest 74126 vehicle tests: Waze-only 6 inactive callbacks; mixed Waze/Maps 31 callbacks
with 14 state-1 (source app unidentified); Maps-only 109 with 98 state-1 and two state-2
arrival payloads that retained populated road/maneuver fields. No errors/drops; all three
manually stopped. Private raw source: coordinator test-artifacts/carplay-navigation/
20261022-126-three-tests/capture-0.jsonl (479 cumulative records, 212 new).
SHA-256 31bc64b6ed2a086ce2a7a50c0a50698089c50c6629f20e8696542d5c67db922a.

## Previous 74126 task

User authorized commit/push and signed OTA 74126 based on private 74125 commit
`0fcb11d03671eb89f3689bb55386bb1ea9963386`. Compared with 74124: two commits add the HEV
long-press overlay crash fix and readability UI changes; retain all 24 changed files.
Only CarPlay diagnostic screen/collector, a pure summary classifier and tests are modified.
No map-clean activation, AA, OEM APK, display/Surface/focus or bridge change is planned.
Added exclusive diagnostic payload counts, session duration and route start/end markers.
Built immutable source `68acf612a2dbcf9d744aaf8c7305eed4435b25a3` in Mac Mini job
`haval-74126-work-68acf612a2dbcf9d-20261022T215909Z-9946`: release/R8/lint vital passed
in 4m58s; 90 JUnit reports, 812 tests, zero failures/errors, one skipped (11 new summary tests passed).
Subsequent documentation changes do not alter this built source.

OTA 74126 published and independently downloaded/verified; fixed app-latest manifest
verified byte-for-byte. APK size 76056095; SHA-256
`4d2ed8b0ebe3a744f588c8e7f24565a4bee05f0e9bce732e462240151de51abc`.
Production certificate matches 74117:
`5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0`.
Release: https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.126-preview.
Coordinator artifacts: /root/projects/haval-impulse/artifacts/release-74126-68acf612
and artifacts/ota-74126. No 74126 install or road validation claimed.
Combined private report for download: /mnt/data/Relatorio-CarPlay-Haval-Waze-AppleMaps-2026-10-22.md.
The collected cumulative JSONL contains 267 records (63 Waze plus 204 Apple Maps); its
SHA-256 is da1eb760b58aa3b206be0c97d713cad3122230149f4b473f189ba323147ccdab.

Real capture on 2026-10-22 was Waze (user confirmed): two inactive/default callbacks during
546.715 seconds, zero errors/drops, clean manual stop. No useful active-route data or video
stream proven in that Waze session. Follow-up Apple Maps on installed 74124 delivered
177 callbacks over 197.647s, including 158 state-1 payloads with roads/distances, zero
errors/drops and a clean stop. Active maneuver IDs 1/2/29/47 remain raw, not mapped to AA
arrows. No video stream proven. Raw records and combined downloadable report stay private
on VPS. Compare Waze using the same start/connect ordering; no Apple Maps repeat is needed
just to establish basic delivery. 74126 is diagnostic only, not runtime TBT integration.
Installation requires a fresh parked-in-P check. This trip was user-driven; do not start
or stop the car. For future agent-initiated maintenance startup, user requires shutdown
on completion unless explicitly requested otherwise, after confirming stationary in P.

## Previous task (historical)

Prepare signed OTA `1.0.0.74.124-preview` from the private signed `74123` base with the remaining v9 HUD updates and two protective fixes.

User requested all reviewed v9 changes with protections, explicitly including `97f8ef1c`.
The car is offline. Do not install 74124 automatically; publish/verify OTA and report readiness.
74123 was installed on 2026-10-21 21:32:20 -03 after the user confirmed parked in P.
User did not drive and capture was not started. A fresh manual capture is still needed at departure.

## 74124 changes and pending validation

- Imported `97f8ef1cbfbc45545a7581a950c9eb77a8448c0a` and `414a20fb2a99cbceaacad753d522289945f420b8` as clean cherry-picks.
- AR navigation map stays visible beside a radar card; roundabout progress retains approach history through exit-road renaming.
- Ordinary/non-roundabout steps reset the tracker before any new entry detection.
- Radar warning thresholds/output have a clamped 20-metre lead, but geometry/minima and passed-camera timing retain original coordinates.
- Added regression tests for reset boundaries, early warnings, real-position retention and late acquisition for cameras with/without road geometry.
- No changes to CarPlay capture, cluster recovery, OEM assets, theme sources or bridge contract.
- `git diff --check` and full Mac Mini unit/release validation required before publication.
- Signing identity remains the pinned 74117 certificate. Release code 74124 must exceed the current OTA 74123.

## Diagnostic extension

- Netseek PR 14 is imported at `15ce75fa07d4cbe4bedd00f308a9ce2e298a6cf0`.
- `navigation/CarPlayGuidanceReader.kt`, `CarPlayGuidanceParcel.kt`, `CarPlayCaptureGate.kt`,
  `CarPlayCaptureFiles.kt`, `CarPlayNavigationCapture.kt`, `CarPlayCaptureScreen.kt` implement capture.
- `FeaturesHubScreen.kt` adds its manual entry. Existing AA/HUD/CarPlay visual code is unchanged.
- The reader/queue/files have JVM tests; the actual Android Parcel adapter self-tests before subscription.
- `docs/architecture/carplay-navigation-capture.md` describes lifecycle, privacy, retrieval and vehicle steps.
- Records are in device-protected `noBackupFilesDir/carplay-navigation`, three files of at most 1 MiB each.
- Capture is off by default, lasts at most sixty minutes, continues after leaving its screen and ends on process death.
- Source rules and transaction layout derive from PR 14 static evidence, not a live car confirmation.
- Binding uses flags zero and transactions 7/8 only. No service auto-start, focus, Surface or video commands.
- Check actual callback counts before the drive; oneway subscription success alone is insufficient.

## Delivery and remaining vehicle validation

- Built commit: `8918d3a771acb0ab21f717a01e0583840a53f02a`.
- Mac Mini job: `haval-v9-sync-20261021-8918d3a771acb0ab-20261022T002004Z-18745`.
- JUnit XML: 88 reports, 791 tests, zero failures/errors and one skipped. Release, R8 and lint vital passed.
- The first run exposed accepting directories as rotating log files; regular-file guards fixed the regression before publication.
- Certificate SHA-256 matches 74117–74122: `5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0`.
- Published release: `https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/tag/1.0.0.74.123-preview`.
- APK SHA-256: `49835cf40f4a307be98b5e81d831a0c32746d0a881b938d6800964fad0d4b526`.
- Downloaded APK matched the hash before updating app-latest. Downloaded fixed OTA manifest matches the local 74123 manifest.
- Source is saved in private branch `rafaelcs28/haval-impulse:release/impulse-74123-carplay-capture`; the post-build handoff commit changes documentation only.
- Coordinator artifacts and detailed signing handoff remain under `/root/projects/haval-impulse`.
- 74123 installation was confirmed via Package Manager; the diagnostic remains off until manually started.
- User will message at arrival; collect all private JSONL files via authorized Telnet/Tailscale.
- Do not claim live monitoring, successful Binder delivery, or parser self-test execution until the car is online.

## Previous release context

## Changes

- Applied upstream PR commit `749e34e500bdf85616abe473b50ace6eedcd1df9` as a clean cherry-pick.
- Added source-specific OSM, ANTT and road refresh state and partial-failure retries.
- Added safe cache replacement and validation fallback to APK-bundled radar data.
- Added focused unit tests and updated radar architecture documentation.

## Risks

- The first run may check ANTT and make road data eligible sooner because legacy installs do not have all new per-source state.
- Manual refresh forces camera sources; road association remains governed by its seven-/thirty-day policy.
- Vehicle behavior still requires post-install observation; no production cache should be deleted to test failure handling.

## Validation

- `git cherry-pick` completed without conflicts.
- `git diff --check` passed.
- Full Android validation must run only through the authorized Mac Mini release queue.

## Previous release steps

1. Submit the immutable final commit as release `74122` / `1.0.0.74.122-preview`.
2. Verify queue tests, release APK, SHA-256 and production certificate fingerprint.
3. Publish the versioned GitHub prerelease and then atomically update the fixed `app-latest/app-preview.json` OTA manifest.
# Cold-start controller source staged — 2026-10-23

Implemented a deterministic, default-off gate immediately after
`CarPlayService.onCreate()` calls `super` and before `init()`. REA of the exact
embedded service APK found `carplay_jni` is loaded later from the
`CarPlayNative` constructor. Evidence and limitations are in
`docs/architecture/carplay-cold-start-controller.md`.

New patch helper waits at most 10s for `release.<pid>.<32hex nonce>`, cancels if
the property changes and otherwise continues stock. It does not load native
code. New launcher generator accepts only canonical v3 profile digest31b878...,
fixed private bridge path/hash/size, explicit PID and bounded principal UUID.
Six Python tests and all26 existing Node cold models pass locally.

Gradle Debug now also builds a staged OEM service patch into
`build/outputs/carplay-patches/`; it does not replace the bundled asset. Apktool
3.0.2 was copied on the authorized Mini into `$HOME/BuildQueue/tools` from an
existing trusted copy after SHA-256 eee4669... verification. Next: commit, audit
build-control digest, queue Debug, inspect staged output. No APK/OTA/car change.

First Mini job for f5c86cc6 failed safely before APK output: Apktool tried to
relink the decoded manifest of this resource-less OEM APK and aapt2 could not
resolve android:name. Source follow-up uses Apktool `--copy-original`, preserving
the original binary manifest while rebuilding only DEX; it requires a new commit
and queue run. Nothing was installed or copied into the bundled asset.

The first `--copy-original` retry still relinked because resources had already
been decoded. Next source follow-up adds decode `--no-res`, the documented pair
that keeps `resources.arsc`/binary manifest intact. Second failed job also made
no artifact or vehicle change.

The `--no-res` job rebuilt and signed SHA60e1fd8e... but failed closed in final
verification: comments/sentinel are intentionally absent after smali round-trip,
although both the helper class and service class were present. Follow-up verifies
the exact normalized helper plus the one call ordered strictly between Service
super.onCreate and mContext assignment. The rejected artifact was not embedded.
