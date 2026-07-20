package com.sisi.expressiontrainer

import android.app.Application
import com.sisi.expressiontrainer.data.local.EmotionLexiconLoader
import com.sisi.expressiontrainer.data.local.SettingsDataStore
import com.sisi.expressiontrainer.lexicon.LexiconAnalyzer

class ExpressionTrainerApplication : Application() {

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var lexiconAnalyzer: LexiconAnalyzer
        private set

    override fun onCreate() {
        super.onCreate()
        settingsDataStore = SettingsDataStore(this)
        val lexicon = EmotionLexiconLoader(this).load()
        lexiconAnalyzer = LexiconAnalyzer(lexicon)
    }
}
