package com.example.ex0710

import android.app.Application
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.LoggerFactory
import java.io.File
import com.naver.maps.map.NaverMapSdk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NaverCloudPlatformClient("3scfi57rri")

        configureLogback()
    }

    private fun configureLogback() {
        try {
            val context = LoggerFactory.getILoggerFactory() as LoggerContext
            val configurator = JoranConfigurator()
            configurator.context = context
            context.reset()

            // logback.xml 파일의 경로를 설정
            val logbackFile = File(filesDir, "logback.xml")
            assets.open("logback.xml").use { input ->
                logbackFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            configurator.doConfigure(logbackFile)
            StatusPrinter.printInCaseOfErrorsOrWarnings(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
