package com.phoneblocker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class CallLogActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: CallLogAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_log)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CallLogAdapter()
        recyclerView.adapter = adapter
        
        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_clear) {
                CallLogStorage.clearLogs(this)
                loadLogs()
                true
            } else {
                false
            }
        }
        
        loadLogs()
    }
    
    override fun onResume() {
        super.onResume()
        loadLogs()
    }
    
    private fun loadLogs() {
        val logs = CallLogStorage.getLogs(this)
        adapter.setLogs(logs)
        
        if (logs.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    inner class CallLogAdapter : RecyclerView.Adapter<CallLogAdapter.ViewHolder>() {
        private var logs: List<CallLogEntry> = emptyList()
        
        fun setLogs(newLogs: List<CallLogEntry>) {
            logs = newLogs
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_call_log, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(logs[position])
        }
        
        override fun getItemCount() = logs.size
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val numberText: TextView = view.findViewById(R.id.numberText)
            private val timeText: TextView = view.findViewById(R.id.timeText)
            private val statusText: TextView = view.findViewById(R.id.statusText)
            private val patternText: TextView = view.findViewById(R.id.patternText)
            
            fun bind(entry: CallLogEntry) {
                numberText.text = entry.rawNumber
                timeText.text = entry.getFormattedTime()
                
                when {
                    entry.wasBlocked -> {
                        // Blocked: BLOCK, SILENCE, VOICEMAIL
                        val icon = when (entry.action) {
                            "BLOCK" -> "🚫"
                            "SILENCE" -> "🔇"
                            "VOICEMAIL" -> "📞"
                            else -> "🚫"
                        }
                        statusText.text = "$icon ${entry.action}"
                        statusText.setTextColor(0xFFE53935.toInt())
                        patternText.text = "Pattern: ${entry.matchedPattern}"
                        patternText.visibility = View.VISIBLE
                    }
                    entry.matchedAllowRule -> {
                        // Matched ALLOW rule
                        statusText.text = "✅ ALLOW"
                        statusText.setTextColor(0xFF43A047.toInt())
                        patternText.text = "Pattern: ${entry.matchedPattern}"
                        patternText.visibility = View.VISIBLE
                    }
                    else -> {
                        // No rule matched
                        statusText.text = "⚪ NOT MATCH"
                        statusText.setTextColor(0xFF757575.toInt())
                        patternText.visibility = View.GONE
                    }
                }
            }
        }
    }
}
