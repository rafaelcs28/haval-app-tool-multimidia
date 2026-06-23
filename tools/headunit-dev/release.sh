#!/usr/bin/env bash
# release.sh — builda o app assinado com a SUA chave e publica como release no seu fork,
# pra o botao "Buscar Atualizacoes" do app instalar OTA (sem telnet).
#
# Uso:
#   ./tools/headunit-dev/release.sh <versao> [--notes "texto"] [--no-publish]
# Exemplos:
#   ./tools/headunit-dev/release.sh 1.0.0.67.1
#   ./tools/headunit-dev/release.sh 1.0.0.68 --notes "Merge do dev .68"
#
# Mantem tudo no canal PREVIEW (sufixo -preview + pre-release) pra NUNCA disparar o
# reset de dados do app (preview->preview nao reseta; veja InformacoesScreen.kt).
# versionCode = X*100+Y a partir dos dois ultimos campos (1.0.0.67.1 -> 6701; 1.0.0.68 -> 6800).
#
# Pre-req: keystore pessoal configurado (keystore.properties + havalkey.jks na raiz);
#          gh autenticado em rafaelcs28 com escopo 'repo'.

set -uo pipefail
REPO="rafaelcs28/haval-app-tool-multimidia"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
INSTALLED_VCODE_HINT=167   # versionCode da build atual no carro (so pra avisar)

RAW="${1:-}"
if [ -z "$RAW" ] || [ "$RAW" = "-h" ] || [ "$RAW" = "--help" ]; then
  sed -n '2,17p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 1
fi
shift
PUBLISH=1; NOTES=""
while [ $# -gt 0 ]; do
  case "$1" in
    --notes) NOTES="${2:-}"; shift 2 ;;
    --no-publish) PUBLISH=0; shift ;;
    *) echo "Opcao desconhecida: $1"; exit 1 ;;
  esac
done

# numero limpo (sem 'v' nem '-preview')
NUM="$(echo "$RAW" | sed -E 's/^v//; s/-preview$//')"
echo "$NUM" | grep -Eq '^[0-9]+(\.[0-9]+)+$' || { echo "Versao invalida: use numeros e pontos (ex 1.0.0.67.1)"; exit 1; }

VNAME="${NUM}-preview"        # canal preview
TAG="${VNAME}"                # SEM 'v': a tag tem que casar com o versionName, senao o OTA compara errado

# versionCode = X*100 + Y (dois ultimos campos; 4 campos => Y=0)
NF="$(echo "$NUM" | awk -F. '{print NF}')"
if [ "$NF" -ge 5 ]; then
  X="$(echo "$NUM" | awk -F. '{print $(NF-1)}')"; Y="$(echo "$NUM" | awk -F. '{print $NF}')"
else
  X="$(echo "$NUM" | awk -F. '{print $NF}')"; Y=0
fi
VCODE=$(( 10#$X * 100 + 10#$Y ))
echo "[release] versionName=$VNAME  versionCode=$VCODE  tag=$TAG"
[ "$VCODE" -gt "$INSTALLED_VCODE_HINT" ] || echo "[release] AVISO: versionCode $VCODE <= $INSTALLED_VCODE_HINT (instalado) — o update pode ser barrado."

# build
( cd "$ROOT_DIR" && ./gradlew :app:assembleRelease --console=plain \
    -PappVersionName="$VNAME" -PappVersionCode="$VCODE" ) || { echo "Build falhou"; exit 1; }
SRC="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
[ -f "$SRC" ] || { echo "APK nao gerado"; exit 1; }
OUT="$ROOT_DIR/app/build/outputs/apk/release/haval-impulse-${NUM}.apk"
cp -f "$SRC" "$OUT"
echo "[release] APK: $OUT ($(wc -c < "$OUT" | tr -d ' ') bytes)"
APKSIGNER="$(ls "$HOME"/Library/Android/sdk/build-tools/*/apksigner 2>/dev/null | sort | tail -1)"
[ -n "$APKSIGNER" ] && "$APKSIGNER" verify --print-certs "$OUT" 2>/dev/null | grep -i "SHA-256 digest" | head -1

if [ "$PUBLISH" = 0 ]; then echo "[release] --no-publish: APK pronto em $OUT"; exit 0; fi

[ -z "$NOTES" ] && NOTES="Build pessoal ${VNAME} (assinada com chave local; canal preview)."
echo "[release] Publicando $TAG em $REPO (pre-release)..."
if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
  gh release upload "$TAG" "$OUT" --repo "$REPO" --clobber
else
  gh release create "$TAG" "$OUT" --repo "$REPO" --prerelease --title "$VNAME" --notes "$NOTES"
fi
echo "[release] OK -> https://github.com/$REPO/releases/tag/$TAG"
echo "[release] No carro: abra o app -> Informacoes -> Buscar Atualizacoes."
