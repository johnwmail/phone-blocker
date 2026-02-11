package com.phoneblocker

import android.Manifest
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var statusText: TextView
    private lateinit var setupButton: Button
    private lateinit var callLogButton: Button
    private lateinit var adapter: RuleAdapter
    private var rules = mutableListOf<BlockRule>()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        updateStatus()
    }
    
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        updateStatus()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        recyclerView = findViewById(R.id.rulesRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        statusText = findViewById(R.id.statusText)
        setupButton = findViewById(R.id.setupButton)
        callLogButton = findViewById(R.id.callLogButton)
        
        adapter = RuleAdapter(
            rules,
            onDelete = { rule -> deleteRule(rule) },
            onToggle = { rule, enabled -> toggleRule(rule, enabled) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        findViewById<FloatingActionButton>(R.id.addButton).setOnClickListener {
            showAddRuleDialog()
        }
        
        setupButton.setOnClickListener {
            requestPermissionsAndRole()
        }
        
        callLogButton.setOnClickListener {
            startActivity(Intent(this, CallLogActivity::class.java))
        }
        
        loadRules()
        updateStatus()
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    private fun updateStatus() {
        val hasPermissions = hasRequiredPermissions()
        val isScreeningApp = isDefaultCallScreeningApp()
        
        when {
            !hasPermissions -> {
                statusText.text = "⚠️ Permissions needed"
                statusText.setTextColor(getColor(R.color.warning))
                setupButton.visibility = View.VISIBLE
                setupButton.text = "Grant Permissions"
            }
            !isScreeningApp -> {
                statusText.text = "⚠️ Set as Call Screener"
                statusText.setTextColor(getColor(R.color.warning))
                setupButton.visibility = View.VISIBLE
                setupButton.text = "Set as Call Screener"
            }
            else -> {
                statusText.text = "✅ Active & Ready"
                statusText.setTextColor(getColor(R.color.success))
                setupButton.visibility = View.GONE
            }
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun isDefaultCallScreeningApp(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }
        return true
    }
    
    private fun requestPermissionsAndRole() {
        if (!hasRequiredPermissions()) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.ANSWER_PHONE_CALLS
                )
            )
        } else if (!isDefaultCallScreeningApp()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                requestRoleLauncher.launch(intent)
            }
        }
    }
    
    private fun loadRules() {
        rules = RuleStorage.loadRules(this)
        // Sort: ALLOW rules first, then others
        rules.sortBy { if (it.action == BlockRule.Action.ALLOW) 0 else 1 }
        adapter.updateRules(rules)
        updateEmptyView()
    }
    
    private fun updateEmptyView() {
        if (rules.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun showAddRuleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_rule, null)
        val patternInput = dialogView.findViewById<EditText>(R.id.patternInput)
        val actionSpinner = dialogView.findViewById<Spinner>(R.id.actionSpinner)
        
        val actions = arrayOf("✅ Allow (priority)", "🚫 Block", "🔇 Silence", "📞 Voicemail")
        actionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actions)
        actionSpinner.setSelection(1) // Default to Block
        
        AlertDialog.Builder(this)
            .setTitle("Add Rule")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val pattern = patternInput.text.toString().trim()
                if (isValidPattern(pattern)) {
                    val action = when (actionSpinner.selectedItemPosition) {
                        0 -> BlockRule.Action.ALLOW
                        1 -> BlockRule.Action.BLOCK
                        2 -> BlockRule.Action.SILENCE
                        else -> BlockRule.Action.VOICEMAIL
                    }
                    val rule = BlockRule(pattern = pattern, action = action)
                    RuleStorage.addRule(this, rule)
                    loadRules()
                    Toast.makeText(this, "Rule added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid pattern. Use digits and wildcards (* ?) only.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun isValidPattern(pattern: String): Boolean {
        if (pattern.isEmpty()) return false
        // Allow digits, * (zero or more), ? (exactly one)
        return pattern.all { it.isDigit() || it == '*' || it == '?' }
    }
    
    private fun deleteRule(rule: BlockRule) {
        AlertDialog.Builder(this)
            .setTitle("Delete Rule")
            .setMessage("Delete rule for ${rule.pattern}?")
            .setPositiveButton("Delete") { _, _ ->
                RuleStorage.removeRule(this, rule.id)
                loadRules()
                Toast.makeText(this, "Rule deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun toggleRule(rule: BlockRule, enabled: Boolean) {
        val updatedRule = rule.copy(enabled = enabled)
        RuleStorage.updateRule(this, updatedRule)
        loadRules()
    }
}
