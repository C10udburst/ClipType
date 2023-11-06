package io.github.cloudburst.cliptype

import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build.VERSION
import android.service.quicksettings.TileService
import com.topjohnwu.superuser.ipc.RootService

class UsbQSService : TileService() {

    override fun onClick() {
        super.onClick()

        // start Working Activity
        if (VERSION.SDK_INT >= 34) {
            val intent = PendingIntent.getActivity(this, 0, Intent(this, WorkingActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(intent)
        } else {
            val intent = Intent(this, WorkingActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivityAndCollapse(intent)
        }
    }

}