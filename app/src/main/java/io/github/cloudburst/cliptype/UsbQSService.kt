package io.github.cloudburst.cliptype

import android.content.ClipboardManager
import android.content.Intent
import android.os.Build.VERSION
import android.service.quicksettings.TileService
import com.topjohnwu.superuser.ipc.RootService

class UsbQSService : TileService() {

    private var connection: UsbRootService.Connection? = null

    override fun onClick() {
        super.onClick()

        updateDesc(getString(R.string.usb_qs_desc_default))

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val content = clipboard.primaryClip?.getItemAt(0)?.text ?: return

        val intent = Intent(this, UsbRootService::class.java)
        intent.putExtra("text", content)

        createConnection()

        for (i in 1..5) {
            if (connection?.binder == null) {
                Thread.sleep(100)
            } else {
                break
            }
        }

        if (connection?.binder?.hidCapable() == false) {
            updateDesc(getString(R.string.usb_qs_desc_hid_unavailable))
        } else {
            updateDesc(getString(R.string.usb_qs_desc_default))
            connection?.binder?.typeUsb(content.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection?.let {
            RootService.unbind(it)
            connection = null
        }
    }

    private fun createConnection() {
        if (connection != null) return
        connection = UsbRootService.Connection()
        val intent = Intent(this, UsbRootService::class.java)
        RootService.bind(intent, connection!!)
    }

    private fun updateDesc(desc: String) {
        qsTile.contentDescription = desc
        if (VERSION.SDK_INT >= 29)
            qsTile.subtitle = desc
        qsTile.updateTile()
    }
}