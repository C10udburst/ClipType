package io.github.cloudburst.cliptype

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.topjohnwu.superuser.ipc.RootService
import java.io.File
import java.io.FileOutputStream

val TAG = "UsbRootService"
const val HID_KEYBOARD_GADGET = "/config/usb_gadget/keyboard"

class UsbRootService: RootService() {

    class Interface: IUsbRootService.Stub() {
        override fun devPath(): String? {
            return File("/dev/").listFiles { _, s -> s.startsWith("hidg") }?.lastOrNull()?.path
        }

        override fun canWrite(): Boolean {
            try {
                val hidPath = devPath() ?: return false
                val hidDev = FileOutputStream(hidPath, true)
                hidDev.write(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
                hidDev.flush()
                hidDev.close()
                return true
            } catch (e: Exception) {
                return false
            }
        }

        override fun gadgetExists(): Boolean {
            val hidGadget = File(HID_KEYBOARD_GADGET)
            return hidGadget.exists() && hidGadget.listFiles()?.isNotEmpty() == true
        }

        override fun enabledGadgets(): MutableList<String> {
            val gadgets = File("/config/usb_gadget").listFiles {
                file -> file.isDirectory
            } ?: return mutableListOf()
            return gadgets.filter { file ->
                try {
                    val udc = file.listFiles { f -> f.name == "UDC" }?.firstOrNull()
                        ?: return@filter false
                    if (!udc.isFile || !udc.canRead() || udc.length() == 0L) return@filter false
                    val text = udc.readText()
                    if (text.trim().isBlank()) return@filter false
                    if (text.trim() == "not set") return@filter false
                    return@filter true
                } catch (e: Exception) {
                    return@filter false
                }
            }.map { file -> file.path }.toMutableList()
        }

        override fun typeUsb(text: String) {
            Log.d(TAG, "Will type: $text")

            val hidPath = devPath()
            if (hidPath == null) {
                Log.e(TAG, "HID device not found")
                return
            }

            val kbdMap = UsbKbdMap()
            val hidDev = FileOutputStream(hidPath, true)
            for (c in text) {
                hidDev.write(kbdMap.getScancode(c))
                Thread.sleep(10)
                hidDev.write(kbdMap.getScancode(null))
                hidDev.flush()

            }
            hidDev.close()
        }

    }

    class Connection: ServiceConnection {

        var onConnected: ((Connection) -> Unit)? = null

        var binder: IUsbRootService? = null
            private set

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            binder = IUsbRootService.Stub.asInterface(p1)
            onConnected?.invoke(this)
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            binder = null
        }

    }

    override fun onBind(intent: Intent): IBinder {
        return Interface()
    }
}