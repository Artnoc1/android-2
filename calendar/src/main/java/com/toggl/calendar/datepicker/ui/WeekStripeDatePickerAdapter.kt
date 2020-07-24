package com.toggl.calendar.datepicker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter
import com.toggl.calendar.R
import java.time.OffsetDateTime

class WeekStripeDatePickerAdapter(private val onDayTappedCallback: (OffsetDateTime) -> Unit) : PagerAdapter() {
    private val pages: MutableMap<Int, WeekViewHolder> = mutableMapOf()
    private var items: List<Week> = emptyList()
    private var currentlySelectedDate: OffsetDateTime = OffsetDateTime.now()

    override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj

    override fun getCount(): Int = items.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val weekStripe = LayoutInflater.from(container.context)
            .inflate(R.layout.fragment_calendardatepicker_week_stripe_page, container, false) as ConstraintLayout

        val week = items[position]
        val vh = WeekViewHolder(weekStripe, onDayTappedCallback)
        vh.initData(week, currentlySelectedDate)
        pages[position] = vh

        weekStripe.tag = position
        container.addView(weekStripe)
        return weekStripe
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        val view = obj as View
        view.tag = null
        container.removeView(view)
    }

    override fun getItemPosition(obj: Any): Int {
        val tag = (obj as View).tag ?: return POSITION_NONE
        val positionFromTag = (tag as Int)
        return if (positionFromTag == count) POSITION_NONE
        else positionFromTag
    }

    fun updateWeekDays(newItems: List<Week>) {
        items = newItems
        notifyDataSetChanged()
        pages.forEach {
            it.value.updateDays(newItems[it.key])
        }
    }

    fun updateSelectedDate(selectedDate: OffsetDateTime) {
        currentlySelectedDate = selectedDate
        pages.forEach {
            it.value.updateCurrentlySelectedDate(currentlySelectedDate)
        }
    }
}