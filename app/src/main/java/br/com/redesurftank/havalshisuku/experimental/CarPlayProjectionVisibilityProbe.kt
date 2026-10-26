package br.com.redesurftank.havalshisuku.experimental

/** Read-only OEM task visibility, shared by observation and the final idle-host guard. */
internal object CarPlayProjectionVisibilityProbe {
    /**
     * Only an explicitly hidden, completely framed task is negative visual evidence.
     * This says nothing about phone/session absence; DCM and Bluetooth checks remain separate.
     * The output is bounded to two fields; unfamiliar/truncated input stays unknown.
     */
    fun capture(): String = """
        cpb_stacks=§(am stack list 2>/dev/null) || exit 1
        cpb_projection_visuals=§(printf '%s\n' "§cpb_stacks" | awk '
          BEGIN { cp=0; aa=0; stacks=0; framed=0; bad=0; bytes=0 }
          {
            bytes += length(§0)+1
            if (NR > 4096 || bytes > 262144) { bad=1; exit }
            if (§0 ~ /^[[:space:]]*§/) next
            if (§0 ~ /^[[:space:]]*(Error(:|[[:space:]]|§)|Exception|SecurityException|java[.])/) { bad=1; next }
            if (§0 ~ /^Stack id=/) {
              framed=§0 ~ /^Stack id=[0-9]+[[:space:]]/ && §0 ~ / displayId=[0-9]+([[:space:]]|§)/
              displayCount=0
              for (i=1; i<=NF; i++) if (index(§i,"displayId=") == 1) displayCount++
              if (displayCount != 1) framed=0
              if (!framed) bad=1
              stacks++; next
            }
            iscp=index(§0,"com.ts.carplay.app") > 0
            isaa=index(§0,"com.ts.androidauto") > 0 || index(§0,"com.google.android.projection.gearhead") > 0
            if (!iscp && !isaa) next
            visible=2; count=0
            for (i=1; i<=NF; i++) {
              if (index(§i,"visible=") == 1) {
                count++
                if (§i == "visible=false") visible=0
                else if (§i == "visible=true") visible=1
                else visible=2
              }
            }
            if (!framed || count != 1 ||
                §0 !~ /^[[:space:]]+taskId=[0-9]+: [A-Za-z0-9_]+([.][A-Za-z0-9_]+)+\/[A-Za-z0-9_.§]+ bounds=\[-?[0-9]+,-?[0-9]+\]\[-?[0-9]+,-?[0-9]+\] userId=[0-9]+([[:space:]]|§)/ ||
                §0 !~ / topActivity=(ComponentInfo\{[A-Za-z0-9_]+([.][A-Za-z0-9_]+)+\/[A-Za-z0-9_.§]+\}|null)§/) visible=2
            if (iscp && visible > cp) cp=visible
            if (isaa && visible > aa) aa=visible
          }
          END {
            if (!stacks || bad) { print "cp_visual=unknown"; print "aa_visual=unknown" }
            else {
              print "cp_visual=" (cp == 2 ? "unknown" : cp)
              print "aa_visual=" (aa == 2 ? "unknown" : aa)
            }
          }
        ') || exit 1
    """.trimIndent().replace('§', '$') + "\n"

    fun requireClear(): String = capture() + """
        test "§cpb_projection_visuals" = "§(printf 'cp_visual=0\naa_visual=0')" || exit 1
    """.trimIndent().replace('§', '$') + "\n"
}
