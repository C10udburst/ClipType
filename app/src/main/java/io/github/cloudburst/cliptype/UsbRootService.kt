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

class UsbRootService: RootService() {

    class Interface: IUsbRootService.Stub() {
        override fun hidCapable(): Boolean {
            // find /dev/hidg*
            // return true if found
            val devFolder = File("/dev")
            val hidgFiles = devFolder.listFiles { file -> file.name.startsWith("hidg") } ?: return false
            return hidgFiles.isNotEmpty()
        }

        override fun typeUsb(text: String) {
            Log.d(TAG, "Will type: $text")

            val devFolder = File("/dev/")
            val hidFile = devFolder.listFiles { file -> file.name.startsWith("hidg") }?.last() ?: return

            val kbdMap = UsbKbdMap()
            val hidDev = FileOutputStream(hidFile, true)
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