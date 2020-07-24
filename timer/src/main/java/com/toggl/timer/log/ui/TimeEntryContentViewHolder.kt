package com.toggl.timer.log.ui

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.toggl.timer.R
import com.toggl.common.feature.extensions.formatForDisplaying
import com.toggl.timer.log.domain.TimeEntryContentViewModel
import com.toggl.timer.log.domain.TimeEntryGroupViewModel
import com.toggl.common.feature.domain.formatForDisplay

abstract class TimeEntryContentViewHolder<in T : TimeEntryContentViewModel>(itemView: View) : TimeEntryLogViewHolder(itemView) {
    val addDescriptionLabel: View = itemView.findViewById(R.id.add_description_label)
    val project: TextView = itemView.findViewById(R.id.project_label)
    val description: TextView = itemView.findViewById(R.id.description)
    val continueButton: View = itemView.findViewById(R.id.continue_button)
    val billableIcon: View = itemView.findViewById(R.id.billable_icon)
    val tagsIcon: View = itemView.findViewById(R.id.tags)
    val groupCount: TextView = itemView.findViewById(R.id.group_count)

    open fun bind(item: T) {
        val hasDescription = item.description.isNotBlank()
        addDescriptionLabel.isVisible = !hasDescription
        description.isVisible = hasDescription

        description.text = item.description

        val formattedProjectText = item.project.formatForDisplay()
        project.text = formattedProjectText
        project.isVisible = formattedProjectText.isNotBlank()

        billableIcon.isVisible = item.billable
        tagsIcon.isVisible = item.hasTags

        duration.text = item.duration.formatForDisplaying()

        groupCount.isVisible = item is TimeEntryGroupViewModel
    }
}