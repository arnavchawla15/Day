package com.example.day.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class QuoteManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("day_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_DAILY_QUOTE_ENABLED = "is_daily_quote_enabled"
        private const val KEY_PERSONAL_QUOTE = "personal_quote"
        private const val KEY_LAST_DATE_ROTATED = "last_date_rotated"
        private const val KEY_CURRENT_ROTATED_QUOTE = "current_rotated_quote"

        private val ROTATING_QUOTES = listOf(
            "The best way to predict the future is to create it. - Peter Drucker",
            "Focus on being productive instead of busy. - Tim Ferriss",
            "One day at a time.",
            "Simplify, then add lightness. - Colin Chapman",
            "Make today your masterpiece. - John Wooden",
            "Action is the foundational key to all success. - Pablo Picasso",
            "Done is better than perfect. - Sheryl Sandberg",
            "You change your life by changing your day.",
            "Small daily improvements over time lead to stunning results. - Robin Sharma",
            "Your focus determines your reality. - Qui-Gon Jinn",
            "The secret of getting ahead is getting started. - Mark Twain",
            "Be not afraid of going slowly, be afraid only of standing still.",
            "Energy flows where attention goes.",
            "What you do today can improve all your tomorrows. - Ralph Marston",
            "Only the disciplined in life are free. - Eliud Kipchoge",
            "Simplicity is the ultimate sophistication. - Leonardo da Vinci",
            "We are what we repeatedly do. - Aristotle",
            "Your daily routine is the secret to your success.",
            "Great things are done by a series of small things brought together. - Vincent Van Gogh",
            "Every day is a fresh start."
        )
    }

    fun isDailyQuoteEnabled(): Boolean {
        return prefs.getBoolean(KEY_IS_DAILY_QUOTE_ENABLED, true)
    }

    fun setDailyQuoteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DAILY_QUOTE_ENABLED, enabled).apply()
    }

    fun getPersonalQuote(): String {
        return prefs.getString(KEY_PERSONAL_QUOTE, "Make today count.") ?: "Make today count."
    }

    fun setPersonalQuote(quote: String) {
        prefs.edit().putString(KEY_PERSONAL_QUOTE, quote).apply()
    }

    fun getCurrentQuote(): String {
        if (isDailyQuoteEnabled()) {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            val lastRotatedDate = prefs.getString(KEY_LAST_DATE_ROTATED, "")
            
            if (lastRotatedDate != currentDate) {
                // Determine a quote based on date hash to keep it consistent throughout the day
                val dateHash = abs(currentDate.hashCode())
                val quoteIndex = dateHash % ROTATING_QUOTES.size
                val newQuote = ROTATING_QUOTES[quoteIndex]
                
                prefs.edit()
                    .putString(KEY_LAST_DATE_ROTATED, currentDate)
                    .putString(KEY_CURRENT_ROTATED_QUOTE, newQuote)
                    .apply()
                return newQuote
            }
            
            return prefs.getString(KEY_CURRENT_ROTATED_QUOTE, ROTATING_QUOTES[0]) ?: ROTATING_QUOTES[0]
        } else {
            return getPersonalQuote()
        }
    }

    fun isDateInitialized(date: String): Boolean {
        return prefs.getBoolean("init_date_$date", false)
    }

    fun setDateInitialized(date: String) {
        prefs.edit().putBoolean("init_date_$date", true).apply()
    }
}
