package com.securevault.passwordmanager

import android.app.Application
import com.securevault.passwordmanager.core.security.CryptoManager
import com.securevault.passwordmanager.core.security.SecureClipboardManager
import com.securevault.passwordmanager.core.timeout.IdleTimeoutTracker
import com.securevault.passwordmanager.data.preferences.AppPreferences
import net.zetetic.database.sqlcipher.SQLiteDatabase

class PasswordManagerApp : Application() {

    lateinit var appPreferences: AppPreferences
        private set
    lateinit var cryptoManager: CryptoManager
        private set
    lateinit var idleTimeoutTracker: IdleTimeoutTracker
        private set
    lateinit var secureClipboardManager: SecureClipboardManager
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize SQLCipher native libraries
        SQLiteDatabase.loadLibs(this)

        appPreferences = AppPreferences(this)
        cryptoManager = CryptoManager()
        idleTimeoutTracker = IdleTimeoutTracker()
        secureClipboardManager = SecureClipboardManager(this)

        idleTimeoutTracker.updateTimeoutSetting(appPreferences.idleTimeoutMinutes)
    }
}
