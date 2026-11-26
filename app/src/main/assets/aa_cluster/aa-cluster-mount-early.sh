#!/system/bin/sh
# Bind both Android Auto APKs before the process opens them.
# No md5: hashing the Service made the last boot finish after the process
# had already started, and the later swap broke the phone certificate.
# "app-only" (Mapa no cluster off): bind the visual App, leave the Service stock.
MODE="${1:-all}"
STAGE=/data/local/tmp/aa_patches
SVC=$STAGE/AndroidAutoService.apk
APP=$STAGE/AndroidAutoApp.apk
VSVC=/vendor/app/AndroidAutoService/AndroidAutoService.apk
VAPP=/vendor/app/AndroidAutoApp/AndroidAutoApp.apk
note() {
    echo "$1 $(cut -d. -f1 /proc/uptime)" >> /data/local/tmp/aa-cluster-early.log
    echo "$1"
}
if [ "$MODE" != "app-only" ] && ! [ -f $SVC ]; then note MOUNT_NO_SOURCE; exit 0; fi
if ! [ -f $APP ]; then note MOUNT_NO_SOURCE; exit 0; fi
# Field 2026-11-26: the race is ~1 s (Impulse up at 15 s, Android Auto at 14-16 s). Lost, the
# whole drive had no cluster map. A process younger than LATE_MAX_AGE (no phone session yet) is
# restarted after the binds, so the new one opens the patched APKs like an early mount; an older
# one is never touched (a swap under a live session broke the phone certificate before).
LATE_MAX_AGE=45
LATE_RESTART=0
proc_age() {
    p=$(pidof "$1" 2>/dev/null | cut -d' ' -f1)
    [ -n "$p" ] || { echo -1; return; }
    st=$(cut -d' ' -f22 /proc/$p/stat 2>/dev/null)
    up=$(cut -d. -f1 /proc/uptime)
    [ -n "$st" ] || { echo 99999; return; }
    echo $(( up - st / 100 ))
}
if pidof com.ts.androidauto >/dev/null 2>&1; then
    age=$(proc_age com.ts.androidauto)
    if [ "$age" -ge 0 ] && [ "$age" -lt $LATE_MAX_AGE ]; then
        LATE_RESTART=1
    else
        note "MOUNT_LATE age=$age"; exit 0
    fi
fi
mkdir -p $STAGE/empty_oat
chmod 755 $STAGE $STAGE/empty_oat
chmod 644 $APP
[ "$MODE" != "app-only" ] && chmod 644 $SVC
if [ "$MODE" != "app-only" ] && ! grep -F $VSVC /proc/mounts >/dev/null 2>&1; then
    touch -r $VSVC $STAGE/stock-mtime-AndroidAutoService.apk.ref
fi
if ! grep -F $VAPP /proc/mounts >/dev/null 2>&1; then
    touch -r $VAPP $STAGE/stock-mtime-AndroidAutoApp.apk.ref
fi
if [ "$MODE" != "app-only" ]; then
    touch -r $STAGE/stock-mtime-AndroidAutoService.apk.ref $SVC || { note MOUNT_NO_SOURCE; exit 0; }
fi
touch -r $STAGE/stock-mtime-AndroidAutoApp.apk.ref $APP || { note MOUNT_NO_SOURCE; exit 0; }
chcon u:object_r:vendor_app_file:s0 $APP
if [ "$MODE" != "app-only" ]; then
    chcon u:object_r:vendor_app_file:s0 $SVC
    if ! grep -F $VSVC /proc/mounts >/dev/null 2>&1; then
        mount --bind $SVC $VSVC || { note MOUNT_NO_SOURCE; exit 0; }
    fi
fi
if ! grep -F $VAPP /proc/mounts >/dev/null 2>&1; then
    mount --bind $APP $VAPP || { note MOUNT_NO_SOURCE; exit 0; }
fi
if [ "$MODE" != "app-only" ] && [ -d /vendor/app/AndroidAutoService/oat ]; then
    mount --bind $STAGE/empty_oat /vendor/app/AndroidAutoService/oat || true
fi
if [ -d /vendor/app/AndroidAutoApp/oat ]; then
    mount --bind $STAGE/empty_oat /vendor/app/AndroidAutoApp/oat || true
fi
rm -f /data/dalvik-cache/arm64/*AndroidAutoService* /data/dalvik-cache/arm64/*AndroidAutoApp*
if [ "$LATE_RESTART" = 1 ]; then
    for n in com.ts.androidauto com.ts.androidauto.app; do
        p=$(pidof $n 2>/dev/null) && [ -n "$p" ] && kill $p
    done
    note "MOUNT_LATE_RESTARTED $MODE"
    exit 0
fi
if pidof com.ts.androidauto >/dev/null 2>&1; then
    # Started while the binds were made: it is seconds old, restart it on the patched APKs.
    for n in com.ts.androidauto com.ts.androidauto.app; do
        p=$(pidof $n 2>/dev/null) && [ -n "$p" ] && kill $p
    done
    note "MOUNT_LATE_RESTARTED $MODE"
    exit 0
fi
note "MOUNT_EARLY $MODE"
