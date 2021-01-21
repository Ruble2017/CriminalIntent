package com.tomsk.android.mycriminalintent

import android.app.Application
import com.tomsk.android.mycriminalintent.database.CrimeRepository

class CriminalIntentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}