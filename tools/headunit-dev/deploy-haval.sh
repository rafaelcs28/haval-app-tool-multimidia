#!/usr/bin/env bash
# deploy-haval.sh — builda o app Haval assinado com SUA chave e instala no head unit
# via telnet, preservando o UID baixo (<=10999) e suas preferencias.
#
# Uso:
#   ./tools/headunit-dev/deploy-haval.sh <IP_DO_CARRO> [opcoes]
#
# Opcoes:
#   --no-build    nao roda o gradle; usa o APK ja buildado
#   --clean       reinstalacao LIMPA (backup prefs -> uninstall -> hook -> install -> restore).
#                 Use se o UID sair > 10999 num update, ou pra reinstalar do zero.
#
# Pre-requisitos (ja atendidos nesta maquina/carro):
#   - keystore pessoal configurado (keystore.properties + havalkey.jks na raiz do repo)
#   - binarios na central em /data/local/tmp: fridaserver, fridainject, system_server.js
#   - Shizuku instalado no carro
#
# Por que o hook: a PMS da OEM (beantechs) bloqueia `pm install` deste pacote; o
# system_server.js (injetado via fridainject) desativa o bloqueio E faz o app pegar
# UID baixo (necessario pra iniciar o Shizuku automaticamente). Veja a memoria do projeto.

set -uo pipefail

PKG="br.com.redesurftank.havalshisuku"
PORT=23
REMOTE_APK=/data/local/tmp/haval-personal.apk
REMOTE_BKP=/data/local/tmp/haval_prefs_deploy_bkp.tgz
PREFS_REL="data/user_de/0/${PKG}/shared_prefs/haval_prefs.xml"
HTTP_PORT=8771
MAX_UID=10999

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# ---------- args ----------
CAR_IP="${1:-}"
if [ -z "$CAR_IP" ] || [ "$CAR_IP" = "-h" ] || [ "$CAR_IP" = "--help" ]; then
  sed -n '2,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  exit 1
fi
shift
BUILD=1; CLEAN=0
for a in "$@"; do
  case "$a" in
    --no-build) BUILD=0 ;;
    --clean)    CLEAN=1 ;;
    *) echo "Opcao desconhecida: $a"; exit 1 ;;
  esac
done

log(){ echo "[deploy] $*"; }
die(){ echo "[deploy][ERRO] $*" >&2; exit 1; }

# ---------- helper de telnet confiavel (marcadores START/END; contorna o echo race) ----------
HU="$(mktemp /tmp/hu.XXXXXX.py)"
cat > "$HU" <<'PY'
import socket,sys,time,re
host,port,cmd=sys.argv[1],int(sys.argv[2]),sys.argv[3]
timeout=float(sys.argv[4]) if len(sys.argv)>4 else 60.0
payload=('echo "__STA""RT__"; '+cmd+'; echo "__EN""D__"\r\n').encode('utf-8','replace')
try: s=socket.create_connection((host,port),timeout=8)
except OSError as e: sys.stderr.write("CONNECT ERROR: %s\n"%e); sys.exit(2)
s.settimeout(0.5)
t0=time.monotonic()
while time.monotonic()-t0<1.0:
    try:
        if not s.recv(4096): break
    except socket.timeout: break
for i in range(0,len(payload),48):
    s.sendall(payload[i:i+48]); time.sleep(0.03)
out=bytearray(); dl=time.monotonic()+timeout
while time.monotonic()<dl:
    try:
        d=s.recv(8192)
        if not d: break
        out+=d
        if b"__END__" in out: break
    except socket.timeout: continue
try: s.close()
except Exception: pass
raw=re.sub(rb'\xff[\x00-\xff]{2}',b'',bytes(out))
t=raw.decode('utf-8','replace').replace('\r','')
m=re.search(r'__START__\n(.*?)\n?__END__',t,re.S)
sys.stdout.write(m.group(1) if m else t)
PY

HTTP_PID=""
cleanup(){ rm -f "$HU" 2>/dev/null || true; [ -n "$HTTP_PID" ] && kill "$HTTP_PID" 2>/dev/null || true; }
trap cleanup EXIT

hu(){ python3 "$HU" "$CAR_IP" "$PORT" "$1" "${2:-60}"; }

ensure_hook(){
  log "Garantindo hook no system_server (fridaserver + fridainject)..."
  hu 'cd /data/local/tmp || exit 1
pgrep fridaserver >/dev/null 2>&1 || { setsid ./fridaserver >/dev/null 2>&1 </dev/null & sleep 3; }
SP=$(pidof system_server)
setsid ./fridainject -p "$SP" -s system_server.js >/data/local/tmp/inject.log 2>&1 </dev/null &
sleep 5; echo hook_ok' 40 >/dev/null
}

# ---------- 1. conectividade ----------
log "Testando $CAR_IP:$PORT ..."
ping -c1 -t2 "$CAR_IP" >/dev/null 2>&1 || die "Carro nao responde a ping ($CAR_IP). Acorde a tela / confira o IP."
echo "$(hu 'id' 10)" | grep -q 'uid=0' || die "Telnet nao deu shell root em $CAR_IP."
log "Conectado (root)."

# ---------- 2. build ----------
if [ "$BUILD" = 1 ]; then
  log "Buildando release assinado com a sua chave..."
  ( cd "$ROOT_DIR" && ./gradlew :app:assembleRelease --console=plain ) || die "Build falhou."
fi
APK="$(ls -t "$ROOT_DIR"/app/build/outputs/apk/release/*.apk 2>/dev/null | head -1)"
[ -f "$APK" ] || die "APK nao encontrado. Rode sem --no-build."
APK_SIZE="$(wc -c < "$APK" | tr -dc '0-9')"
log "APK: $APK ($APK_SIZE bytes)"

# ---------- 3. envio do APK via HTTP ----------
MAC_IP="$(route get "$CAR_IP" 2>/dev/null | awk '/interface:/{print $2}' | head -1 | xargs -I{} ipconfig getifaddr {} 2>/dev/null)"
[ -z "$MAC_IP" ] && MAC_IP="$(ipconfig getifaddr en0 2>/dev/null)"
[ -z "$MAC_IP" ] && die "Nao consegui descobrir o IP local do Mac."
python3 -m http.server "$HTTP_PORT" --bind 0.0.0.0 --directory "$(dirname "$APK")" >/tmp/deploy_httpd.log 2>&1 &
HTTP_PID=$!; sleep 1
kill -0 "$HTTP_PID" 2>/dev/null || die "Servidor HTTP local nao subiu (porta $HTTP_PORT ocupada?)."
log "Enviando APK ($MAC_IP:$HTTP_PORT)..."
hu "rm -f $REMOTE_APK; curl -fsSL http://$MAC_IP:$HTTP_PORT/$(basename "$APK") -o $REMOTE_APK; echo rc=\$?" 180 >/dev/null
REMOTE_SIZE="$(hu "wc -c < $REMOTE_APK 2>/dev/null" 20 | tr -dc '0-9')"
kill "$HTTP_PID" 2>/dev/null || true; HTTP_PID=""
[ "$REMOTE_SIZE" = "$APK_SIZE" ] || die "Falha no envio (remoto=$REMOTE_SIZE, esperado=$APK_SIZE)."
log "APK no carro OK ($REMOTE_SIZE bytes)."

# ---------- 4. instalar ----------
if [ "$CLEAN" = 1 ]; then
  log "[--clean] Backup das prefs..."
  hu "cd / && tar czf $REMOTE_BKP $PREFS_REL 2>/dev/null; ls -la $REMOTE_BKP" 30 | grep -q haval_prefs && log "backup ok" || log "aviso: sem prefs pra backup (instalacao nova?)"
  ensure_hook
  log "[--clean] Uninstall + install limpo..."
  hu "pm uninstall $PKG" 60 >/dev/null
  OUT="$(hu "pm install -r -d $REMOTE_APK 2>&1" 180)"
  echo "$OUT" | grep -qi Success || die "Install (clean) falhou: $OUT"
  NEWUID="$(hu "stat -c %u /data/user_de/0/$PKG" 15 | tr -dc '0-9')"
  log "Novo UID=$NEWUID"
  log "[--clean] Restaurando prefs..."
  hu "am force-stop $PKG
cd / && mkdir -p $(dirname "$PREFS_REL") && tar xzf $REMOTE_BKP $PREFS_REL 2>/dev/null
chown -R ${NEWUID}:${NEWUID} /data/user_de/0/$PKG/shared_prefs
restorecon -R /data/user_de/0/$PKG
echo restore_ok" 30 >/dev/null
else
  log "Instalando (update no lugar: mantem UID + prefs)..."
  OUT="$(hu "pm install -r -d $REMOTE_APK 2>&1" 180)"
  if echo "$OUT" | grep -qiE "beantechs disallow|INSTALL_FAILED"; then
    log "Bloqueio da OEM ativo — injetando hook e tentando de novo..."
    ensure_hook
    OUT="$(hu "pm install -r -d $REMOTE_APK 2>&1" 180)"
  fi
  echo "$OUT" | grep -qi Success || die "Install falhou: $OUT"
fi
log "Instalado."

# ---------- 5. verificar + reiniciar ----------
UID_NOW="$(hu "stat -c %u /data/user_de/0/$PKG 2>/dev/null" 15 | tr -dc '0-9')"
KEYS="$(hu "grep -cE 'name=' /data/user_de/0/$PKG/shared_prefs/haval_prefs.xml 2>/dev/null" 15 | tr -dc '0-9')"
hu "am force-stop $PKG; am start -n $PKG/.SplashActivity >/dev/null 2>&1; echo started" 20 >/dev/null
log "UID=$UID_NOW  prefs=${KEYS:-0} chaves"
if [ -n "$UID_NOW" ] && [ "$UID_NOW" -le "$MAX_UID" ]; then
  log "✅ Deploy concluido. App reiniciado, UID baixo, prefs preservadas."
else
  log "⚠️ UID=$UID_NOW > $MAX_UID — o Shizuku automatico NAO vai ligar."
  log "   Rode de novo com --clean pra reinstalar limpo e pegar UID baixo:"
  log "   $0 $CAR_IP --no-build --clean"
  exit 2
fi
