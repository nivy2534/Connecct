package com.example.connecct

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class App : Application() {
    override fun onCreate(){
        super.onCreate()

        val existingProvider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        if(existingProvider == null || existingProvider.javaClass != BouncyCastleProvider::class.java){
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.addProvider(BouncyCastleProvider())
        }

    }
}