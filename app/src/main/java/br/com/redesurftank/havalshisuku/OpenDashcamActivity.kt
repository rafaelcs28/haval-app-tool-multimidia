package br.com.redesurftank.havalshisuku

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import br.com.redesurftank.havalshisuku.managers.StealthModeManager

/**
 * Narrow, exported entry for other apps (the Haval 3D dashcam card): opens Impulse directly on the
 * Dashcam tab and nothing else. MainActivity itself stays unexported. During the Modo
 * Concessionária the app must look absent, so the request is ignored.
 */
class OpenDashcamActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!StealthModeManager.isActive()) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_SCREEN, "Dashcam")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        finish()
    }
}
