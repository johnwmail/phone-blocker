package com.phoneblocker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RuleAdapter(
    private val rules: MutableList<BlockRule>,
    private val onDelete: (BlockRule) -> Unit,
    private val onToggle: (BlockRule, Boolean) -> Unit
) : RecyclerView.Adapter<RuleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val patternText: TextView = view.findViewById(R.id.patternText)
        val actionText: TextView = view.findViewById(R.id.actionText)
        val enableSwitch: Switch = view.findViewById(R.id.enableSwitch)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rule = rules[position]
        
        holder.patternText.text = rule.pattern
        holder.actionText.text = when (rule.action) {
            BlockRule.Action.BLOCK -> "🚫 Block"
            BlockRule.Action.SILENCE -> "🔇 Silence"
            BlockRule.Action.VOICEMAIL -> "📞 Voicemail"
        }
        
        holder.enableSwitch.setOnCheckedChangeListener(null)
        holder.enableSwitch.isChecked = rule.enabled
        holder.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggle(rule, isChecked)
        }
        
        holder.deleteButton.setOnClickListener {
            onDelete(rule)
        }
    }

    override fun getItemCount() = rules.size
    
    fun updateRules(newRules: List<BlockRule>) {
        rules.clear()
        rules.addAll(newRules)
        notifyDataSetChanged()
    }
}
