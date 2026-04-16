package com.sonyairplay.receiver

import android.content.Context
import android.content.SharedPreferences
import kotlin.random.Random

object PairingManager {
    private const val PREFS = "airplay_prefs"
    private const val KEY_PIN = "pair_pin"
    private var prefs: SharedPreferences? = null
    private var pinExpiry: Long = 0

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun generatePin(): String {
        val pin = (1000 + Random.nextInt(9000)).toString()
        prefs?.edit()?.putString(KEY_PIN, pin)?.apply()
        pinExpiry = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes
        return pin
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs?.getString(KEY_PIN, "") ?: ""
        val valid = stored == pin && System.currentTimeMillis() <= pinExpiry
        if (valid) {
            prefs?.edit()?.putBoolean("paired", true)?.apply()
        }
        return valid
    }

    fun isPaired(): Boolean = prefs?.getBoolean("paired", false) ?: false
}
