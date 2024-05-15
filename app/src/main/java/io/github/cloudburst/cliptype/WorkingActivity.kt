package io.github.cloudburst.cliptype

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class WorkingActivity : Activity() {
    private var connection: UsbRootService.Connection? = null

    private var clipboardContents: CharSequence = ""

    private var enabledGadgets: List<String> = listOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_working)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) finish();
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardContents = clipboard.primaryClip?.getItemAt(0)?.text ?: return finish()

        connection = UsbRootService.Connection()
        connection!!.onConnected = ::onConnected
        val intent = Intent(this, UsbRootService::class.java)
        RootService.bind(intent, connection!!)
    }


    private fun onConnected(connection: UsbRootService.Connection) {
        val binder = connection.binder ?: return finish()
        CoroutineScope(Dispatchers.Default).launch {
            if (!ensureDevice(binder)) {
                runOnUiThread {
                    findViewById<TextView>(R.id.statusTextView).text = getString(R.string.usb_qs_desc_hid_unavailable)
                }
                revertDevice(binder)
            } else {
                if (waitForWrite(binder)) {
                    binder.typeUsb(clipboardContents.toString())
                }
                revertDevice(binder)
                runOnUiThread {
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection?.let {
            RootService.unbind(it)
        }
    }

    private suspend fun ensureDevice(binder: IUsbRootService): Boolean {
        if (binder.devPath() != null) {
            Log.d(TAG, "Device already exists")
            return true
        }

        if (!Shell.getShell().isRoot) {
            Log.e(TAG, "Root access required")
            return false
        }

        enabledGadgets = binder.enabledGadgets()

        Log.d(TAG, "Enabled gadgets: $enabledGadgets")

        if (!binder.gadgetExists()) {
            Log.d(TAG, "Creating gadget")
            Shell.cmd(resources.openRawResource(R.raw.create_gadget)).exec().let {
                Log.d(TAG, "Creating gadget: ${it.out}, ${it.err}")
                if (!it.isSuccess) return false
            }
            try {
                withTimeout(1000) {
                    while (!binder.gadgetExists()) {
                        delay(100)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gadget creation timed out")
                return false
            }
        }

        // deactivate all
        Shell.cmd("find /config/usb_gadget/ -name UDC -type f -exec sh -c 'echo \"\" >  \"$@\"' _ {} \\;\n").exec().let {
            Log.d(TAG, "Deactivate all: ${it.out}, ${it.err}")
            if (!it.isSuccess) return false
        }

        // activate keyboard
        Shell.cmd("getprop sys.usb.controller > ${HID_KEYBOARD_GADGET}/UDC\n").exec().let {
            Log.d(TAG, "Activate keyboard: ${it.out}, ${it.err}")
            if (!it.isSuccess) return false
        }

        try {
            withTimeout(3000) {
                while (binder.devPath() == null) {
                    delay(100)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Device creation timed out")
            return false
        }


        return true
    }

    private fun revertDevice(binder: IUsbRootService) {
        // deactivate all
        Shell.cmd("find /config/usb_gadget/  -name UDC -type f -exec sh -c 'echo \"\" >  \"$@\"' _ {} \\;\n").exec().let {
            Log.d(TAG, "Deactivate all: ${it.out}, ${it.err}")
        }

        // activate previous
        enabledGadgets.forEach {
            Shell.cmd("getprop sys.usb.controller > $it/UDC\n").exec().let {
                Log.d(TAG, "Activate $it: ${it.out}, ${it.err}")
            }
        }
    }

    private suspend fun waitForWrite(binder: IUsbRootService): Boolean {
        return try {
            withTimeout(3000) {
                while (!binder.canWrite()) {
                    delay(100)
                }
            }
            true
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Device write timed out")
            false
        }
    }
}
