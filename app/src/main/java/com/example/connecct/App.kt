package com.example.connecct

import android.app.Application
import com.example.connecct.storage.LoadStorageKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate(){
        super.onCreate()
        try {
            val oldProvider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
            if (oldProvider != null) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            }
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            println("✅ BouncyCastle aktif sejak startup")
        } catch (e: Exception) {
            println("❌ Gagal register BC: ${e.message}")
        }

        CoroutineScope(Dispatchers.IO).launch{
            val loader = LoadStorageKey(applicationContext)
            val keys = loader.loadKeys()
        }
    }
}