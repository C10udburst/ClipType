package io.github.cloudburst.cliptype

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.topjohnwu.superuser.ipc.RootService

class WorkingActivity : Activity() {
    private var connection: UsbRootService.Connection? = null

    private var clipboardContents: CharSequence = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_working)

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardContents = clipboard.primaryClip?.getItemAt(0)?.text ?: return finish()

        connection = UsbRootService.Connection()
        connection!!.onConnected = ::onConnected
        val intent = Intent(this, UsbRootService::class.java)
        RootService.bind(intent, connection!!)
    }

    private fun onConnected(connection: UsbRootService.Connection) {
        val binder = connection.binder ?: return finish()
        if (!binder.hidCapable()) {
            findViewById<TextView>(R.id.statusTextView).text = getString(R.string.usb_qs_desc_hid_unavailable)
        } else {
            binder.typeUsb(clipboardContents.toString())
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection?.let {
            RootService.unbind(it)
        }
    }
}
