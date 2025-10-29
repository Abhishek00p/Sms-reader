package com.example.smsforwarder

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smsforwarder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("sms_forwarder_prefs", MODE_PRIVATE)

        loadSettings()

        binding.saveSettingsButton.setOnClickListener {
            saveSettings()
        }

        requestSmsPermission()
    }

    private fun loadSettings() {
        binding.apiEndpoint.setText(sharedPreferences.getString("apiEndpoint", ""))
        binding.username.setText(sharedPreferences.getString("username", ""))
        binding.password.setText(sharedPreferences.getString("password", ""))
        binding.workerThreadWaitPeriod.setText(sharedPreferences.getString("workerThreadWaitPeriod", "5"))
        binding.workerThreadIdealAttemptsCount.setText(sharedPreferences.getString("workerThreadIdealAttemptsCount", "33"))
    }

    private fun saveSettings() {
        val editor = sharedPreferences.edit()
        val apiEndpoint = binding.apiEndpoint.text.toString().trim()
        if (apiEndpoint.isEmpty()) {
            Toast.makeText(this, "API Endpoint cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        editor.putString("apiEndpoint", apiEndpoint)
        editor.putString("username", binding.username.text.toString().trim())
        editor.putString("password", binding.password.text.toString())
        editor.putString("workerThreadWaitPeriod", binding.workerThreadWaitPeriod.text.toString())
        editor.putString("workerThreadIdealAttemptsCount", binding.workerThreadIdealAttemptsCount.text.toString())
        editor.apply()
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), SMS_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
