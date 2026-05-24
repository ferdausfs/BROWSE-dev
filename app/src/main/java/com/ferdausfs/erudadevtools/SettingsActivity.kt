package com.ferdausfs.erudadevtools

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private val prefsName = "eruda_prefs"
    private val keyAutoInject = "auto_inject_eruda"
    private val keyDarkMode = "dark_mode_webview"
    private val keyHomepage = "homepage_url"
    private val defaultHomepage = "https://example.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val autoInjectSwitch = findViewById<Switch>(R.id.switchAutoInject)
        val darkModeSwitch = findViewById<Switch>(R.id.switchDarkMode)
        val homepageEdit = findViewById<EditText>(R.id.editHomepage)
        val saveBtn = findViewById<Button>(R.id.btnSave)
        val resetBtn = findViewById<Button>(R.id.btnReset)

        autoInjectSwitch.isChecked = prefs.getBoolean(keyAutoInject, true)
        darkModeSwitch.isChecked = prefs.getBoolean(keyDarkMode, false)
        homepageEdit.setText(prefs.getString(keyHomepage, defaultHomepage))

        autoInjectSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(keyAutoInject, isChecked).apply()
        }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(keyDarkMode, isChecked).apply()
        }

        saveBtn.setOnClickListener {
            val homepage = homepageEdit.text.toString().trim().ifEmpty { defaultHomepage }
            prefs.edit()
                .putBoolean(keyAutoInject, autoInjectSwitch.isChecked)
                .putBoolean(keyDarkMode, darkModeSwitch.isChecked)
                .putString(keyHomepage, homepage)
                .apply()
            Toast.makeText(this, R.string.toast_settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        }

        resetBtn.setOnClickListener {
            autoInjectSwitch.isChecked = true
            darkModeSwitch.isChecked = false
            homepageEdit.setText(defaultHomepage)
            prefs.edit()
                .putBoolean(keyAutoInject, true)
                .putBoolean(keyDarkMode, false)
                .putString(keyHomepage, defaultHomepage)
                .apply()
            Toast.makeText(this, R.string.toast_settings_reset, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
