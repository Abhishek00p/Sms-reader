package com.example.smsforwarder

import android.Manifest
import android.content.Context
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)

        loadSettings()

        binding.saveSettings.setOnClickListener {
            saveSettings()
        }

        requestSmsPermission()
    }

    private fun loadSettings() {
        binding.apiEndpoint.setText(sharedPreferences.getString("api_endpoint", ""))
        binding.username.setText(sharedPreferences.getString("username", ""))
        binding.password.setText(sharedPreferences.getString("password", ""))
        binding.workerThreadWaitPeriod.setText(sharedPreferences.getInt("worker_period", 5).toString())
        binding.workerThreadIdealAttemptsCount.setText(sharedPreferences.getInt("idle_attempts", 33).toString())
    }

    private fun saveSettings() {
        val editor = sharedPreferences.edit()
        editor.putString("api_endpoint", binding.apiEndpoint.text.toString())
        editor.putString("username", binding.username.text.toString())
        editor.putString("password", binding.password.text.toString())
        editor.putInt("worker_period", binding.workerThreadWaitPeriod.text.toString().toIntOrNull() ?: 5)
        editor.putInt("idle_attempts", binding.workerThreadIdealAttemptsCount.text.toString().toIntOrNull() ?: 33)
        editor.apply()

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 101)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
