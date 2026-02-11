package com.phoneblocker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RuleAdapter(
    private var rules: MutableList<BlockRule>,
    private val onDelete: (BlockRule) -> Unit,
    private val onToggle: (BlockRule, Boolean) -> Unit
) : RecyclerView.Adapter<RuleAdapter.ViewHolder>() {

    fun updateRules(newRules: MutableList<BlockRule>) {
        rules = newRules
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rules[position])
    }

    override fun getItemCount() = rules.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val patternText: TextView = view.findViewById(R.id.patternText)
        private val actionText: TextView = view.findViewById(R.id.actionText)
        private val enableSwitch: Switch = view.findViewById(R.id.enableSwitch)
        private val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)

        fun bind(rule: BlockRule) {
            patternText.text = rule.pattern
            
            val (icon, label, color) = when (rule.action) {
                BlockRule.Action.ALLOW -> Triple("✅", "ALLOW", 0xFF43A047.toInt())
                BlockRule.Action.BLOCK -> Triple("🚫", "BLOCK", 0xFFE53935.toInt())
                BlockRule.Action.SILENCE -> Triple("🔇", "SILENCE", 0xFFFF9800.toInt())
                BlockRule.Action.VOICEMAIL -> Triple("📞", "VOICEMAIL", 0xFF1976D2.toInt())
            }
            
            actionText.text = "$icon $label"
            actionText.setTextColor(color)
            
            enableSwitch.setOnCheckedChangeListener(null)
            enableSwitch.isChecked = rule.enabled
            enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                onToggle(rule, isChecked)
            }

            deleteButton.setOnClickListener {
                onDelete(rule)
            }
        }
    }
}
