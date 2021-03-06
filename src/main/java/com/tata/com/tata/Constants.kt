package com.tata.com.tata

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment

class Constants {

    companion object {

        private val oneSecond = 1000.toLong()
        private val oneMinute = 60 * oneSecond
        private val oneHour = 60 * oneMinute
        private val oneDay = 24 * oneHour
        private val oneWeek = 7 * oneDay
        private val oneMonth = 30 * oneDay

        val expiryTime = oneWeek
        val tokenPrefix = "Bearer "
        val headerString = "Authorization"
        val signUpUrl = "/users"

        fun getTokenSecret(applicationContext: ApplicationContext): String {
            return applicationContext.environment.getProperty("tokenSecret") ?: ""
        }
    }
}