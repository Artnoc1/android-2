package com.toggl.calendar.datepicker.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import androidx.transition.TransitionManager
import com.toggl.calendar.R
import java.time.OffsetDateTime

class WeekViewHolder(
    private val rootConstraintLayout: ConstraintLayout,
    private val onDayTappedCallback: (OffsetDateTime) -> Unit
) : RecyclerView.ViewHolder(rootConstraintLayout) {
    private val animationDuration = 250L
    private val dayTextViews: List<TextView> = rootConstraintLayout.children.filterIsInstance<TextView>().toList()
    private val currentDayIndicator: View = itemView.findViewById(R.id.currentDayIndicator)
    private lateinit var currentWeek: Week
    private lateinit var currentlySelectedDate: OffsetDateTime

    fun initData(week: Week, selectedDate: OffsetDateTime) {
        currentWeek = week
        currentlySelectedDate = selectedDate
        updateView()
    }

    fun updateCurrentlySelectedDate(selectedDate: OffsetDateTime) {
        currentlySelectedDate = selectedDate
        updateView()
    }

    fun updateDays(week: Week) {
        currentWeek = week
        updateView()
    }

    private fun updateView() {
        val week = currentWeek
        val currentDate = currentlySelectedDate
        var foundCurrentDay = false
        val constraintSet = ConstraintSet()
        val transition = AutoTransition()
        transition.duration = animationDuration
        val textAnimations = mutableListOf<ValueAnimator>()
        constraintSet.clone(rootConstraintLayout)

        week.dates.forEachIndexed { index, visibleDate ->
            dayTextViews[index].run {
                if (visibleDate.isSelectable) {
                    this.setOnClickListener { onDayTappedCallback(visibleDate.date) }
                } else {
                    this.setOnClickListener(null)
                }
                this.text = visibleDate.dateLabel
                val dayType = getCalendarWeekDayType(visibleDate, currentDate)
                val startingTextColor = this.currentTextColor
                val newTextColor = selectTextColorFor(this.context, dayType)

                if (startingTextColor != newTextColor) {
                    val valueAnimator = createTextColorAnimator(startingTextColor, newTextColor, this)
                    textAnimations.add(valueAnimator)
                }

                if (visibleDate.date.dayOfYear == currentDate.dayOfYear) {
                    val dayIndicatorColor =
                        if (dayType === CalendarWeekDayType.TodaySelected) R.color.primary else R.color.primary_dark
                    currentDayIndicator.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(itemView.context, dayIndicatorColor))

                    constraintSet.connect(currentDayIndicator.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP)
                    constraintSet.connect(currentDayIndicator.id, ConstraintSet.RIGHT, this.id, ConstraintSet.RIGHT)
                    constraintSet.connect(currentDayIndicator.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM)
                    constraintSet.connect(currentDayIndicator.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT)
                    foundCurrentDay = true
                }
            }
        }

        constraintSet.setVisibility(currentDayIndicator.id, if (foundCurrentDay) View.VISIBLE else View.GONE)
        transition.addListener(TransitionListener(textAnimations.toList()))
        TransitionManager.beginDelayedTransition(rootConstraintLayout, transition)
        textAnimations.forEach { animator -> animator.start() }

        constraintSet.applyTo(rootConstraintLayout)
    }

    private fun createTextColorAnimator(
        startingTextColor: Int,
        newTextColor: Int,
        textView: TextView
    ): ValueAnimator {
        return ValueAnimator.ofArgb(startingTextColor, newTextColor).apply {
            this.duration = animationDuration
            addUpdateListener {
                textView.setTextColor(it.animatedValue as Int)
            }
            doOnCancel {
                textView.setTextColor(startingTextColor)
                removeAllUpdateListeners()
                removeAllListeners()
            }
            doOnEnd {
                textView.setTextColor(newTextColor)
                removeAllUpdateListeners()
                removeAllListeners()
            }
        }
    }

    private fun selectTextColorFor(context: Context, dayType: CalendarWeekDayType): Int {
        return when (dayType) {
            CalendarWeekDayType.Disabled -> ContextCompat.getColor(context, R.color.week_stripe_disabled_day_color)
            CalendarWeekDayType.Enabled -> ContextCompat.getColor(context, R.color.text_on_background)
            CalendarWeekDayType.Today -> ContextCompat.getColor(context, R.color.primary)
            CalendarWeekDayType.Selected -> Color.WHITE
            CalendarWeekDayType.TodaySelected -> Color.WHITE
        }
    }

    private fun getCalendarWeekDayType(visibleDate: VisibleDate, currentDate: OffsetDateTime): CalendarWeekDayType {
        if (!visibleDate.isSelectable) return CalendarWeekDayType.Disabled

        if (visibleDate.date.dayOfYear == currentDate.dayOfYear) {
            return if (visibleDate.isToday) CalendarWeekDayType.TodaySelected else CalendarWeekDayType.Selected
        }

        return if (visibleDate.isToday) CalendarWeekDayType.Today else CalendarWeekDayType.Enabled
    }

    enum class CalendarWeekDayType {
        Disabled,
        Enabled,
        Today,
        Selected,
        TodaySelected
    }

    class TransitionListener(private val animators: List<ValueAnimator>) : TransitionListenerAdapter() {
        override fun onTransitionEnd(transition: Transition) {
            super.onTransitionEnd(transition)
            animators.forEach { it.end() }
        }

        override fun onTransitionCancel(transition: Transition) {
            super.onTransitionCancel(transition)
            animators.forEach { it.cancel() }
        }
    }
}