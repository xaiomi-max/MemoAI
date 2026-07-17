package com.memoai.app

import android.app.Application
import android.content.Context
import com.memoai.app.voice.SparkChainAsr

class MemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SparkChainAsr.initialize(this)
    }
}
