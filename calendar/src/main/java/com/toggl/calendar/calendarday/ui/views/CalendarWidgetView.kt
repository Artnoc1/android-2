package com.toggl.calendar.calendarday.ui.views

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Vibrator
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withTranslation
import com.toggl.calendar.R
import com.toggl.calendar.calendarday.ui.views.CalendarWidgetView.DraggingDirection.Down
import com.toggl.calendar.calendarday.ui.views.CalendarWidgetView.DraggingDirection.Idle
import com.toggl.calendar.calendarday.ui.views.CalendarWidgetView.DraggingDirection.Up
import com.toggl.calendar.common.domain.CalendarItem
import com.toggl.calendar.common.domain.duration
import com.toggl.calendar.common.domain.endTime
import com.toggl.calendar.common.domain.isRunning
import com.toggl.calendar.common.domain.startTime
import com.toggl.common.Constants
import com.toggl.common.Constants.ClockMath.hoursInTheDay
import com.toggl.common.extensions.absoluteDurationBetween
import com.toggl.common.extensions.applyAndRecycle
import com.toggl.common.extensions.performTickEffect
import com.toggl.common.extensions.roundToClosestQuarter
import com.toggl.common.feature.models.SelectedCalendarItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Timer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.math.abs

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalContracts
class CalendarWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var currentHourColor: Int
    private var calendarCurrentHourCircleRadius: Float
    private var calendarEventAutoScrollToFrameExtraDistance: Float
    private var calendarEdgeDistanceToTriggerAutoScroll: Float
    private var calendarMaxHourHeight: Float
    private var calendarBaseHourHeight: Float
    private var calendarCurrentHourIndicatorStrokeSize: Float
    private var calendarBackgroundColor: Int
    private var calendarHourLineStartMargin: Float
    private var calendarVerticalLineDividerStartMargin: Float
    private var calendarHoursStartMargin: Float
    private var calendarBackgroundHourLineColor: Int
    private var calendarHourLinesHeight: Float
    private var calendarHourTextColor: Int
    private var calendarHoursTextSize: Float
    private var calendarEditingHandleTouchExtraMargins: Float
    private var editingHourLabelTextColor: Int
    private var editingHoursLabelPaintTextSize: Float
    private var editingHandlesHorizontalMargins: Float
    private var editingHandlesRadius: Float
    private var editingHandlesStrokeWidth: Float
    private var calendarEventsStartMargin: Float
    private var calendarEventsLeftPadding: Float
    private var calendarEventsRightPadding: Float
    private var calendarEventsItemsSpacing: Float
    private var calendarEventsCornerRadius: Float
    private var shortCalendarItemHeight: Float
    private var regularCalendarItemVerticalPadding: Float
    private var regularCalendarItemHorizontalPadding: Float
    private var shortCalendarItemVerticalPadding: Float
    private var shortCalendarItemHorizontalPadding: Float
    private var regularCalendarItemFontSize: Float
    private var shortCalendarItemFontSize: Float
    private var textEventsPaintTextSize: Float
    private var calendarRunningTimeEntryStripesSpacing: Float
    private var calendarRunningTimeEntryThinStripeWidth: Float
    private var calendarRunningTimeEntryDashedHourTopPadding: Float
    private var calendarRunningTimeEntryBorderStrokeWidth: Float
    private var calendarRunningTimeEntryExtraHeight: Float
    private var calendarEventBottomLineHeight: Float
    private var calendarIconSize: Float
    private var calendarIconRightInsetMargin: Float
    private var calendarIconId: Int
    private var primaryTextColor: Int
    private var defaultCalendarItemColor: Int

    private val hourHeight: MutableStateFlow<Float>
    private val scrollOffset = MutableStateFlow(0)

    private val viewFrame = RectF()
    private val touchRectF = RectF()
    private val itemInEditModeRect = RectF()
    private val dragTopRect = RectF()
    private val dragBottomRect = RectF()
    private val eventStartingRect = RectF()
    private var topAreaTriggerLine: Float = 0f
    private var bottomAreaTriggerLine: Float = 0f

    private var drawingLock = ReentrantLock(true)
    private var drawingData: CalendarWidgetViewDrawingData = CalendarWidgetViewDrawingData()
    private var drawingRectFs: MutableMap<String, RectF> = mutableMapOf()
    private var runningAnimations: MutableMap<String, Pair<RectF, Animator>> = mutableMapOf()
    private val tempDrawingRectFDiff = RectF()
    private var flingWasCalled: Boolean = false
    private var isScrolling: Boolean = false
    private var isDragging: Boolean = false
    private var shouldTryToAutoScrollToEvent = false
    private var editAction: EditAction = EditAction.None
    private var currentTouchY: Float = 0f
    private var draggingDelta: Float = 0f
    private var draggingScrollDelta: Int = 0
    private var dragStartingTouchY: Float = 0f
    private var dragStartingScrollOffset: Int = 0
    private var draggingSpeed: Float = 0f
    private val dragAcceleration = 0.1f
    private val dragMaxSpeed = 15f
    private var draggingDirection = Idle
    private val autoScrollAnimator: ValueAnimator
    private var currentHourTimer: Timer? = null

    private val scroller: OverScroller
    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector
    private var hapticFeedbackProvider: Vibrator? = context.getSystemService()

    private val calendarItemDrawingDelegate: CalendarItemDrawingDelegate
    private val backgroundDrawingDelegate: CalendarBackgroundDrawingDelegate
    private val currentHourDrawingDelegate: CalendarCurrentHourDrawingDelegate

    private val itemTappedChannel = ConflatedBroadcastChannel<CalendarItem>()
    private val emptySpaceLongPressedChannel = ConflatedBroadcastChannel<OffsetDateTime>()

    private var lastSelectedItem: CalendarItem? = null
    private var itemInEditModePreviousEndTime: OffsetDateTime? = null
    private var itemInEditModePreviousStartTime: OffsetDateTime? = null
    private val startTimeChangesChannel = ConflatedBroadcastChannel<OffsetDateTime>()
    private val endTimeChangesChannel = ConflatedBroadcastChannel<OffsetDateTime>()
    private val offsetChangesChannel = ConflatedBroadcastChannel<OffsetDateTime>()

    val itemTappedFlow = itemTappedChannel.asFlow()
    val emptySpaceLongPressedFlow = emptySpaceLongPressedChannel.asFlow()
    val startTimeFlow = startTimeChangesChannel.asFlow()
    val endTimeFlow = endTimeChangesChannel.asFlow()
    val offsetFlow = offsetChangesChannel.asFlow()
    val scrollOffsetFlow: Flow<Int> = scrollOffset
    val hourHeightFlow: Flow<Float>

    init {
        context.resources.run {
            val defaultCalendarBaseHourHeight = getDimension(R.dimen.default_calendar_calendar_base_hour_height)
            val defaultCalendarMaxHourHeight = getDimension(R.dimen.default_calendar_calendar_max_hour_height)
            val defaultCalendarEdgeDistanceToTriggerAutoScroll =
                getDimension(R.dimen.default_calendar_calendar_edge_distance_to_trigger_auto_scroll)
            val defaultCalendarEventAutoScrollToFrameExtraDistance =
                getDimension(R.dimen.default_calendar_calendar_event_auto_scroll_to_frame_extra_distance)
            val defaultCalendarCurrentHourCircleRadius =
                getDimension(R.dimen.default_calendar_calendar_current_hour_circle_radius)
            val defaultCurrentHourColor = ContextCompat.getColor(context, R.color.default_calendar_current_hour_color)
            val defaultCalendarCurrentHourIndicatorStrokeSize =
                getDimension(R.dimen.default_calendar_calendar_current_hour_indicator_stroke_size)
            val defaultCalendarBackgroundColor =
                ContextCompat.getColor(context, R.color.default_calendar_calendar_background_color)
            val defaultCalendarHourLineStartMargin = getDimension(R.dimen.default_calendar_calendar_hour_line_start_margin)
            val defaultCalendarVerticalLineDividerStartMargin =
                getDimension(R.dimen.default_calendar_calendar_vertical_line_divider_start_margin)
            val defaultCalendarHoursStartMargin = getDimension(R.dimen.default_calendar_calendar_hours_start_margin)
            val defaultCalendarHourLineColor =
                ContextCompat.getColor(context, R.color.default_calendar_calendar_hour_line_color)
            val defaultCalendarHourLinesHeight = getDimension(R.dimen.default_calendar_calendar_hour_lines_height)
            val defaultCalendarHourTextColor =
                ContextCompat.getColor(context, R.color.default_calendar_calendar_hour_text_color)
            val defaultCalendarHoursTextSize = getDimension(R.dimen.default_calendar_calendar_hours_text_size)
            val defaultCalendarEditingHandleTouchExtraMargins =
                getDimension(R.dimen.default_calendar_calendar_editing_handle_touch_extra_margins)
            val defaultEditingHourLabelTextColor =
                ContextCompat.getColor(context, R.color.default_calendar_editing_hour_label_text_color)
            val defaultEditingHoursLabelPaintTextSize =
                getDimension(R.dimen.default_calendar_editing_hours_label_paint_text_size)
            val defaultEditingHandlesHorizontalMargins =
                getDimension(R.dimen.default_calendar_editing_handles_horizontal_margins)
            val defaultEditingHandlesRadius = getDimension(R.dimen.default_calendar_editing_handles_radius)
            val defaultEditingHandlesStrokeWidth = getDimension(R.dimen.default_calendar_editing_handles_stroke_width)
            val defaultCalendarEventsStartMargin = getDimension(R.dimen.default_calendar_calendar_events_start_margin)
            val defaultCalendarEventsLeftPadding = getDimension(R.dimen.default_calendar_calendar_events_left_padding)
            val defaultCalendarEventsRightPadding = getDimension(R.dimen.default_calendar_calendar_events_right_padding)
            val defaultCalendarEventsItemsSpacing = getDimension(R.dimen.default_calendar_calendar_events_items_spacing)
            val defaultCalendarEventsCornerRadius = getDimension(R.dimen.default_calendar_calendar_events_corner_radius)
            val defaultShortCalendarItemHeight = getDimension(R.dimen.default_calendar_short_calendar_item_height)
            val defaultRegularCalendarItemVerticalPadding =
                getDimension(R.dimen.default_calendar_regular_calendar_item_vertical_padding)
            val defaultRegularCalendarItemHorizontalPadding =
                getDimension(R.dimen.default_calendar_regular_calendar_item_horizontal_padding)
            val defaultShortCalendarItemVerticalPadding =
                getDimension(R.dimen.default_calendar_short_calendar_item_vertical_padding)
            val defaultShortCalendarItemHorizontalPadding =
                getDimension(R.dimen.default_calendar_short_calendar_item_horizontal_padding)
            val defaultRegularCalendarItemFontSize = getDimension(R.dimen.default_calendar_regular_calendar_item_font_size)
            val defaultShortCalendarItemFontSize = getDimension(R.dimen.default_calendar_short_calendar_item_font_size)
            val defaultTextEventsPaintTextSize = getDimension(R.dimen.default_calendar_text_events_paint_text_size)
            val defaultCalendarRunningTimeEntryStripesSpacing =
                getDimension(R.dimen.default_calendar_calendar_running_time_entry_stripes_spacing)
            val defaultCalendarRunningTimeEntryThinStripeWidth =
                getDimension(R.dimen.default_calendar_calendar_running_time_entry_thin_stripe_width)
            val defaultCalendarRunningTimeEntryDashedHourTopPadding =
                getDimension(R.dimen.default_calendar_calendar_running_time_entry_dashed_hour_top_padding)
            val defaultCalendarRunningTimeEntryBorderStrokeWidth =
                getDimension(R.dimen.default_calendar_calendar_running_time_entry_border_stroke_width)
            val defaultCalendarRunningTimeEntryExtraHeight =
                getDimension(R.dimen.default_calendar_calendar_running_time_entry_extra_height)
            val defaultCalendarEventBottomLineHeight = getDimension(R.dimen.default_calendar_calendar_event_bottom_line_height)
            val defaultCalendarIconSize = getDimension(R.dimen.default_calendar_calendar_icon_size)
            val defaultCalendarIconRightInsetMargin = getDimension(R.dimen.default_calendar_calendarIcon_right_inset_margin)
            val defaultCalendarIconId = R.drawable.ic_calendar
            val defaultPrimaryTextColor = ContextCompat.getColor(context, R.color.default_calendar_primary_text_color)
            val defaultDefaultCalendarItemColor =
                ContextCompat.getColor(context, R.color.default_calendar_default_calendar_item_color)
            context.theme.obtainStyledAttributes(attrs, R.styleable.CalendarWidgetView, 0, 0).applyAndRecycle {
                calendarBaseHourHeight =
                    getDimension(R.styleable.CalendarWidgetView_calendarBaseHourHeight, defaultCalendarBaseHourHeight)
                calendarMaxHourHeight =
                    getDimension(R.styleable.CalendarWidgetView_calendarMaxHourHeight, defaultCalendarMaxHourHeight)
                calendarEdgeDistanceToTriggerAutoScroll = getDimension(
                    R.styleable.CalendarWidgetView_calendarEdgeDistanceToTriggerAutoScroll,
                    defaultCalendarEdgeDistanceToTriggerAutoScroll
                )
                calendarEventAutoScrollToFrameExtraDistance = getDimension(
                    R.styleable.CalendarWidgetView_calendarEventAutoScrollToFrameExtraDistance,
                    defaultCalendarEventAutoScrollToFrameExtraDistance
                )
                calendarCurrentHourCircleRadius = getDimension(
                    R.styleable.CalendarWidgetView_calendarCurrentHourCircleRadius,
                    defaultCalendarCurrentHourCircleRadius
                )
                currentHourColor = getColor(R.styleable.CalendarWidgetView_currentHourColor, defaultCurrentHourColor)
                calendarCurrentHourIndicatorStrokeSize = getDimension(
                    R.styleable.CalendarWidgetView_calendarCurrentHourIndicatorStrokeSize,
                    defaultCalendarCurrentHourIndicatorStrokeSize
                )
                calendarBackgroundColor =
                    getColor(R.styleable.CalendarWidgetView_calendarBackgroundColor, defaultCalendarBackgroundColor)
                calendarHourLineStartMargin = getDimension(
                    R.styleable.CalendarWidgetView_calendarHourLineStartMargin, defaultCalendarHourLineStartMargin
                )
                calendarVerticalLineDividerStartMargin = getDimension(
                    R.styleable.CalendarWidgetView_calendarVerticalLineDividerStartMargin,
                    defaultCalendarVerticalLineDividerStartMargin
                )
                calendarHoursStartMargin =
                    getDimension(R.styleable.CalendarWidgetView_calendarHoursStartMargin, defaultCalendarHoursStartMargin)
                calendarBackgroundHourLineColor =
                    getColor(R.styleable.CalendarWidgetView_calendarHourLineColor, defaultCalendarHourLineColor)
                calendarHourLinesHeight =
                    getDimension(R.styleable.CalendarWidgetView_calendarHourLinesHeight, defaultCalendarHourLinesHeight)
                calendarHourTextColor =
                    getColor(R.styleable.CalendarWidgetView_calendarHourTextColor, defaultCalendarHourTextColor)
                calendarHoursTextSize =
                    getDimension(R.styleable.CalendarWidgetView_calendarHoursTextSize, defaultCalendarHoursTextSize)
                calendarEditingHandleTouchExtraMargins = getDimension(
                    R.styleable.CalendarWidgetView_calendarEditingHandleTouchExtraMargins,
                    defaultCalendarEditingHandleTouchExtraMargins
                )
                editingHourLabelTextColor =
                    getColor(R.styleable.CalendarWidgetView_editingHourLabelTextColor, defaultEditingHourLabelTextColor)
                editingHoursLabelPaintTextSize = getDimension(
                    R.styleable.CalendarWidgetView_editingHoursLabelPaintTextSize, defaultEditingHoursLabelPaintTextSize
                )
                editingHandlesHorizontalMargins =
                    getDimension(
                        R.styleable.CalendarWidgetView_editingHandlesHorizontalMargins, defaultEditingHandlesHorizontalMargins
                    )
                editingHandlesRadius =
                    getDimension(R.styleable.CalendarWidgetView_editingHandlesRadius, defaultEditingHandlesRadius)
                editingHandlesStrokeWidth =
                    getDimension(R.styleable.CalendarWidgetView_editingHandlesStrokeWidth, defaultEditingHandlesStrokeWidth)
                calendarEventsStartMargin =
                    getDimension(R.styleable.CalendarWidgetView_calendarEventsStartMargin, defaultCalendarEventsStartMargin)
                calendarEventsLeftPadding =
                    getDimension(R.styleable.CalendarWidgetView_calendarEventsLeftPadding, defaultCalendarEventsLeftPadding)
                calendarEventsRightPadding =
                    getDimension(R.styleable.CalendarWidgetView_calendarEventsRightPadding, defaultCalendarEventsRightPadding)
                calendarEventsItemsSpacing =
                    getDimension(R.styleable.CalendarWidgetView_calendarEventsItemsSpacing, defaultCalendarEventsItemsSpacing)
                calendarEventsCornerRadius =
                    getDimension(R.styleable.CalendarWidgetView_calendarEventsCornerRadius, defaultCalendarEventsCornerRadius)
                shortCalendarItemHeight =
                    getDimension(R.styleable.CalendarWidgetView_shortCalendarItemHeight, defaultShortCalendarItemHeight)
                regularCalendarItemVerticalPadding = getDimension(
                    R.styleable.CalendarWidgetView_regularCalendarItemVerticalPadding,
                    defaultRegularCalendarItemVerticalPadding
                )
                regularCalendarItemHorizontalPadding = getDimension(
                    R.styleable.CalendarWidgetView_regularCalendarItemHorizontalPadding,
                    defaultRegularCalendarItemHorizontalPadding
                )
                shortCalendarItemVerticalPadding = getDimension(
                    R.styleable.CalendarWidgetView_shortCalendarItemVerticalPadding, defaultShortCalendarItemVerticalPadding
                )
                shortCalendarItemHorizontalPadding = getDimension(
                    R.styleable.CalendarWidgetView_shortCalendarItemHorizontalPadding,
                    defaultShortCalendarItemHorizontalPadding
                )
                regularCalendarItemFontSize = getDimension(
                    R.styleable.CalendarWidgetView_regularCalendarItemFontSize, defaultRegularCalendarItemFontSize
                )
                shortCalendarItemFontSize = getDimension(
                    R.styleable.CalendarWidgetView_shortCalendarItemFontSize, defaultShortCalendarItemFontSize
                )
                textEventsPaintTextSize =
                    getDimension(R.styleable.CalendarWidgetView_textEventsPaintTextSize, defaultTextEventsPaintTextSize)
                calendarRunningTimeEntryStripesSpacing = getDimension(
                    R.styleable.CalendarWidgetView_calendarRunningTimeEntryStripesSpacing,
                    defaultCalendarRunningTimeEntryStripesSpacing
                )
                calendarRunningTimeEntryThinStripeWidth = getDimension(
                    R.styleable.CalendarWidgetView_calendarRunningTimeEntryThinStripeWidth,
                    defaultCalendarRunningTimeEntryThinStripeWidth
                )
                calendarRunningTimeEntryDashedHourTopPadding = getDimension(
                    R.styleable.CalendarWidgetView_calendarRunningTimeEntryDashedHourTopPadding,
                    defaultCalendarRunningTimeEntryDashedHourTopPadding
                )
                calendarRunningTimeEntryBorderStrokeWidth = getDimension(
                    R.styleable.CalendarWidgetView_calendarRunningTimeEntryBorderStrokeWidth,
                    defaultCalendarRunningTimeEntryBorderStrokeWidth
                )
                calendarRunningTimeEntryExtraHeight = getDimension(
                    R.styleable.CalendarWidgetView_calendarRunningTimeEntryExtraHeight,
                    defaultCalendarRunningTimeEntryExtraHeight
                )
                calendarEventBottomLineHeight = getDimension(
                    R.styleable.CalendarWidgetView_calendarEventBottomLineHeight,
                    defaultCalendarEventBottomLineHeight
                )
                calendarIconSize = getDimension(R.styleable.CalendarWidgetView_calendarIconSize, defaultCalendarIconSize)
                calendarIconRightInsetMargin = getDimension(
                    R.styleable.CalendarWidgetView_calendarIconRightInsetMargin,
                    defaultCalendarIconRightInsetMargin
                )
                calendarIconId = getResourceId(R.styleable.CalendarWidgetView_calendarIcon, defaultCalendarIconId)
                primaryTextColor = getColor(R.styleable.CalendarWidgetView_primaryTextColor, defaultPrimaryTextColor)
                defaultCalendarItemColor =
                    getColor(R.styleable.CalendarWidgetView_defaultCalendarItemColor, defaultDefaultCalendarItemColor)
            }
        }

        val normalCalendarIconBitmap =
            ContextCompat.getDrawable(context, calendarIconId)!!.toBitmap(calendarIconSize.toInt(), calendarIconSize.toInt())
        val smallCalendarIconBitmap =
            ContextCompat.getDrawable(context, calendarIconId)!!
                .toBitmap((calendarIconSize / 2f).toInt(), (calendarIconSize / 2).toInt())

        hourHeight = MutableStateFlow(calendarBaseHourHeight)
        hourHeightFlow = hourHeight
        calendarItemDrawingDelegate = CalendarItemDrawingDelegate(
            itemSpacing = calendarEventsItemsSpacing,
            leftMargin = calendarVerticalLineDividerStartMargin,
            leftPadding = calendarEventsLeftPadding,
            rightPadding = calendarEventsRightPadding,
            cornerRadius = calendarEventsCornerRadius,
            calendarEventBottomLineHeight = calendarEventBottomLineHeight,
            normalCalendarIconBitmap = normalCalendarIconBitmap,
            smallCalendarIconBitMap = smallCalendarIconBitmap,
            calendarBackgroundColor = calendarBackgroundColor,
            runningTimeEntryStripesSpacing = calendarRunningTimeEntryStripesSpacing,
            runningTimeEntryThinStripeWidth = calendarRunningTimeEntryThinStripeWidth,
            runningTimeEntryBorderStrokeWidth = calendarRunningTimeEntryBorderStrokeWidth,
            runningTimeEntryDashedHourTopPadding = calendarRunningTimeEntryDashedHourTopPadding,
            calendarRunningTimeEntryExtraHeight = calendarRunningTimeEntryExtraHeight,
            calendarIconRightInsetMargin = calendarIconRightInsetMargin,
            calendarIconSize = calendarIconSize,
            primaryTextColor = primaryTextColor,
            regularCalendarItemFontSize = regularCalendarItemFontSize,
            regularCalendarItemHorizontalPadding = regularCalendarItemHorizontalPadding,
            regularCalendarItemVerticalPadding = regularCalendarItemVerticalPadding,
            shortCalendarItemFontSize = shortCalendarItemFontSize,
            shortCalendarItemHeight = shortCalendarItemHeight,
            shortCalendarItemHorizontalPadding = shortCalendarItemHorizontalPadding,
            shortCalendarItemVerticalPadding = shortCalendarItemVerticalPadding,
            editingHandlesHorizontalMargins = editingHandlesHorizontalMargins,
            editingHandlesRadius = editingHandlesRadius,
            editingHandlesStrokeWidth = editingHandlesStrokeWidth,
            defaultCalendarItemColor = defaultCalendarItemColor,
            editingHoursLabelsStartMargin = calendarHoursStartMargin,
            editingHoursLabelsTextColor = editingHourLabelTextColor,
            editingHoursLabelsTextSize = editingHoursLabelPaintTextSize
        ).apply {
            currentHourHeight = calendarBaseHourHeight
        }

        backgroundDrawingDelegate = CalendarBackgroundDrawingDelegate(
            hourLineColor = calendarBackgroundHourLineColor,
            hourLabelTextColor = calendarHourTextColor,
            hourLabelTextSize = calendarHoursTextSize,
            verticalLineLeftMargin = calendarVerticalLineDividerStartMargin,
            timeSliceStartX = calendarHourLineStartMargin,
            hoursX = calendarHoursStartMargin
        ).also {
            it.currentHourHeight = calendarBaseHourHeight
        }

        currentHourDrawingDelegate = CalendarCurrentHourDrawingDelegate(
            currentHourColor = currentHourColor,
            calendarCurrentHourIndicatorStrokeSize = calendarCurrentHourIndicatorStrokeSize,
            currentHourCircleRadius = calendarCurrentHourCircleRadius,
            timeSliceStartX = calendarHourLineStartMargin
        )

        val gestureListener = GestureListener(
            ::onTouchDown,
            ::onLongPress,
            ::scrollView,
            ::flingView,
            ::onSingleTapUp,
            ::onScale
        )
        gestureDetector = GestureDetector(context, gestureListener)
        scaleGestureDetector = ScaleGestureDetector(context, gestureListener)
        scroller = OverScroller(context)

        autoScrollAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { onAutoScrollAnimationUpdate() }
        }
    }

    fun updateList(newCalendarItems: List<CalendarItem>) {
        drawingLock.withLock {
            drawingData = CalendarWidgetViewDrawingData(newCalendarItems).also {
                if (isSelectingNewCalendarItem(lastSelectedItem, it.selectedCalendarItemToDraw)) {
                    vibrate()
                }
                lastSelectedItem = it.selectedCalendarItemToDraw
                animateChangingSelectedItemRectIfNecessary(it.selectedCalendarItemToDraw)
                animateChangingCalendarItemsIfNecessary(it.nonSelectedCalendarItemsToDraw)
            }
            postInvalidateOnAnimation()
        }
    }

    fun setScrollOffset(newScrollOffset: Int) {
        if (scrollOffset.value == newScrollOffset) return

        scrollOffset.value = newScrollOffset
        invalidate()
    }

    fun setHourHeight(newHourHeight: Float) {
        if (hourHeight.value == newHourHeight) return

        hourHeight.value = newHourHeight
        updateDrawingDelegatesHourHeights()
        cleanAnimationsAndDrawingCache()
        invalidate()
    }

    private fun animateChangingSelectedItemRectIfNecessary(selectedCalendarItemToDraw: CalendarItem?) {
        if (viewFrame.isEmpty || selectedCalendarItemToDraw == null)
            return

        calendarItemDrawingDelegate.calculateItemRect(selectedCalendarItemToDraw, viewFrame, tempDrawingRectFDiff)
        val drawingId = selectedCalendarItemToDraw.drawingId()
        val runningAnimation = runningAnimations[drawingId]
        if (runningAnimation != null) {
            val (runningAnimationEndRectF, runningAnimator) = runningAnimation
            if (runningAnimationEndRectF.left == tempDrawingRectFDiff.left && runningAnimationEndRectF.right == tempDrawingRectFDiff.right)
                return
            runningAnimator.cancel()
            runningAnimator.removeAllListeners()
            runningAnimations.remove(drawingId)
        }
        if (itemInEditModeRect.left != tempDrawingRectFDiff.left || itemInEditModeRect.right != tempDrawingRectFDiff.right) {
            val startingRect = RectF()
            val endingRect = RectF()
            startingRect.set(itemInEditModeRect)
            endingRect.set(tempDrawingRectFDiff)
            // note that this itemInEditModeRect is the same used to handle drags and it will be modified by the animator
            // through the RectFSideEvaluator to create the rect left/right animations for the current selected item
            // note that this same rect has its top/bottom values changed during drags to edit
            val rectEvaluator = RectFSideEvaluator(itemInEditModeRect)
            val newAnimator = ObjectAnimator.ofObject(rectEvaluator, startingRect, endingRect).apply {
                duration = 200
                addUpdateListener { invalidate() }
                start()
            }
            runningAnimations[drawingId] = endingRect to newAnimator
        }
    }

    private fun animateChangingCalendarItemsIfNecessary(nonSelectedCalendarItemsToDraw: List<CalendarItem>) {
        if (viewFrame.isEmpty)
            return

        for (calendarItem in nonSelectedCalendarItemsToDraw) {
            val drawingId = calendarItem.drawingId()
            val oldDrawingRectF = drawingRectFs[drawingId] ?: continue
            calendarItemDrawingDelegate.calculateItemRect(calendarItem, viewFrame, tempDrawingRectFDiff)
            val runningAnimation = runningAnimations[drawingId]
            if (runningAnimation != null) {
                val (runningAnimationEndRectF, runningAnimator) = runningAnimation
                if (runningAnimationEndRectF == tempDrawingRectFDiff)
                    continue
                runningAnimator.cancel()
                runningAnimator.removeAllListeners()
                runningAnimations.remove(drawingId)
            }
            if (oldDrawingRectF != tempDrawingRectFDiff) {
                val startingRect = RectF()
                val endingRect = RectF()
                startingRect.set(oldDrawingRectF)
                endingRect.set(tempDrawingRectFDiff)
                // note that this oldDrawingRectF is the same from the drawingRectFs and it will be modified to create
                // the moving animations from the animator through the RectFEvaluator
                val rectEvaluator = RectFEvaluator(oldDrawingRectF)
                val newAnimator = ObjectAnimator.ofObject(rectEvaluator, startingRect, endingRect).apply {
                    duration = 200
                    addUpdateListener { invalidate() }
                    start()
                }
                runningAnimations[drawingId] = endingRect to newAnimator
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        currentHourTimer = fixedRateTimer(period = Duration.ofMinutes(1).toMillis()) { postInvalidate() }
    }

    override fun onDetachedFromWindow() {
        currentHourTimer?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val currScrollOffset = scrollOffset.value.toFloat()
        viewFrame.set(0f, currScrollOffset, width.toFloat(), currScrollOffset + height)
        topAreaTriggerLine = viewFrame.top + calendarEdgeDistanceToTriggerAutoScroll
        bottomAreaTriggerLine = viewFrame.bottom - calendarEdgeDistanceToTriggerAutoScroll

        canvas.withTranslation(0f, -currScrollOffset) {
            clipRect(viewFrame)
            drawCalendarBackground()
            drawCalendarItems()
            drawCurrentHourIndicator()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (!changed)
            return

        backgroundDrawingDelegate.onLayout()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleResult = scaleGestureDetector.onTouchEvent(event)
        if (scaleGestureDetector.isInProgress)
            return scaleResult || super.onTouchEvent(event)

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                shouldTryToAutoScrollToEvent = false
                gestureDetector.onTouchEvent(event)
                true
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                gestureDetector.onTouchEvent(event)
                if (scrollFrameToDisplayItemInEditModeIfNeeded())
                    return true
                if (flingWasCalled)
                    return true
                if (!isScrolling)
                    return true

                isScrolling = false
                invalidate()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                gestureDetector.onTouchEvent(event)
                if (isDragging) {
                    dragEvent(event)
                }
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                gestureDetector.onTouchEvent(event)
                isScrolling = false
                true
            }
            else -> gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
        }
    }

    private fun Canvas.drawCalendarBackground() {
        backgroundDrawingDelegate.onDraw(this, viewFrame)
    }

    private fun Canvas.drawCalendarItems() {
        drawingData.let { currentDrawingData ->
            val calendarItemsToDraw = currentDrawingData.nonSelectedCalendarItemsToDraw
            val selectedItemToDraw = currentDrawingData.selectedCalendarItemToDraw
            calendarItemsToDraw.forEach { calendarItem ->
                val drawingId = calendarItem.drawingId()
                val drawingRectF = drawingRectFs.computeIfAbsent(drawingId) {
                    RectF().apply { calendarItemDrawingDelegate.calculateItemRect(calendarItem, viewFrame, this) }
                }
                calendarItemDrawingDelegate.onDraw(this, viewFrame, calendarItem, false, drawingRectF)
            }
            selectedItemToDraw?.let {
                if (!isDragging) {
                    calendarItemDrawingDelegate.calculateItemRect(it, viewFrame, itemInEditModeRect)
                }
                calendarItemDrawingDelegate.onDraw(this, viewFrame, it, true, itemInEditModeRect)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun Canvas.drawCurrentHourIndicator() {
        val now = OffsetDateTime.now()
        val currentHourY = (now.hour + now.minute.toFloat() / Constants.ClockMath.minutesInAnHour) * hourHeight.value
        currentHourDrawingDelegate.onDraw(this, viewFrame, currentHourY)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onTouchDown(e: MotionEvent) {
        scroller.forceFinished(true)
        flingWasCalled = false
        handler.removeCallbacks(::continueScroll)
        onTouchDownWhileEditingItem(e)
        invalidate()
    }

    private fun onTouchDownWhileEditingItem(event: MotionEvent) {
        drawingData.selectedCalendarItemToDraw?.let {
            calendarItemDrawingDelegate.calculateItemRect(it, viewFrame, touchRectF)
            calculateTopDragRect(touchRectF, dragTopRect)
            calculateBottomDragRect(touchRectF, dragBottomRect)

            val touchX = event.x
            val touchY = event.y

            val currScrollOffset = scrollOffset.value
            dragStartingTouchY = touchY
            dragStartingScrollOffset = currScrollOffset
            eventStartingRect.set(touchRectF)

            editAction = when {
                dragTopRect.contains(touchX, touchY + currScrollOffset) -> EditAction.ChangeStart
                it.isRunning -> EditAction.None
                dragBottomRect.contains(touchX, touchY + currScrollOffset) -> EditAction.ChangeEnd
                touchRectF.contains(touchX, touchY + currScrollOffset) -> EditAction.ChangeOffset
                else -> EditAction.None
            }

            isDragging = editAction != EditAction.None
        }
    }

    private fun calculateTopDragRect(sourceTouchRect: RectF, targetTouchRect: RectF) {
        targetTouchRect.set(sourceTouchRect)
        targetTouchRect.bottom = targetTouchRect.top + calendarEditingHandleTouchExtraMargins
        targetTouchRect.top -= calendarEditingHandleTouchExtraMargins
        targetTouchRect.left = targetTouchRect.right - calendarEditingHandleTouchExtraMargins
        targetTouchRect.right += calendarEditingHandleTouchExtraMargins
    }

    private fun calculateBottomDragRect(sourceTouchRect: RectF, targetTouchRect: RectF) {
        targetTouchRect.set(sourceTouchRect)
        targetTouchRect.top = targetTouchRect.bottom - calendarEditingHandleTouchExtraMargins
        targetTouchRect.bottom += calendarEditingHandleTouchExtraMargins
        targetTouchRect.right = targetTouchRect.left + calendarEditingHandleTouchExtraMargins
        targetTouchRect.left -= calendarEditingHandleTouchExtraMargins
    }

    private fun dragEvent(event: MotionEvent) {
        drawingData.selectedCalendarItemToDraw?.let {
            val histCount = event.historySize
            var touchYSum = 0f
            for (i in 0 until histCount) {
                touchYSum += event.getHistoricalY(0, i)
            }
            val touchY = if (histCount > 0) touchYSum / histCount else event.y
            currentTouchY = touchY
            draggingDelta = touchY - dragStartingTouchY
            draggingScrollDelta = scrollOffset.value - dragStartingScrollOffset

            when (editAction) {
                EditAction.ChangeStart -> updateItemInEditModeStartTime()
                EditAction.ChangeEnd -> updateItemInEditModeEndTime()
                EditAction.ChangeOffset -> updateItemInEditModeOffset()
                EditAction.None -> Unit
            }

            when {
                shouldAutoScrollUp() -> draggingDirection = Up
                shouldAutoScrollDown() -> draggingDirection = Down
                else -> cancelDraggingAndAutoScroll()
            }

            if (draggingDirection != Idle && !autoScrollAnimator.isRunning) {
                autoScrollAnimator.start()
            }

            invalidate()
        }
    }

    private fun shouldAutoScrollUp(): Boolean {
        val currScrollOffset = scrollOffset.value
        return currentTouchY + currScrollOffset < topAreaTriggerLine &&
            currScrollOffset > 0 &&
            itemInEditModeRect.top > 0
    }

    private fun shouldAutoScrollDown(): Boolean {
        val currScrollOffset = scrollOffset.value
        val maxHeight = calculateMaxHeight()
        return currentTouchY + currScrollOffset > bottomAreaTriggerLine &&
            currScrollOffset < maxHeight - height &&
            itemInEditModeRect.bottom < maxHeight
    }

    private fun onAutoScrollAnimationUpdate() {
        if (!isDragging) {
            cancelDraggingAndAutoScroll()
            return
        }

        val maxScrollOffset = calculateMaxScrollOffset()

        draggingSpeed = (draggingSpeed + dragAcceleration * draggingDirection.value).coerceIn(-dragMaxSpeed, dragMaxSpeed)
        scrollOffset.value = (scrollOffset.value + draggingSpeed).coerceIn(0f, maxScrollOffset.toFloat()).toInt()
        val currentScrollOffset = scrollOffset.value
        draggingScrollDelta = currentScrollOffset - dragStartingScrollOffset

        if (draggingDirection == Up && currentScrollOffset <= 0 || draggingDirection == Down && currentScrollOffset >= maxScrollOffset) {
            cancelDraggingAndAutoScroll()
            return
        }

        when (editAction) {
            EditAction.ChangeStart -> updateItemInEditModeStartTime()
            EditAction.ChangeEnd -> updateItemInEditModeEndTime()
            EditAction.ChangeOffset -> updateItemInEditModeOffset()
            EditAction.None -> Unit
        }

        postInvalidate()
    }

    private fun updateItemInEditModeStartTime() {
        drawingData.let {
            if (it.selectedCalendarItemToDraw == null) return

            val newTop = (eventStartingRect.top + draggingDelta + draggingScrollDelta).coerceIn(0f, itemInEditModeRect.bottom)
            itemInEditModeRect.top = newTop
            val calendarItem = it.selectedCalendarItemToDraw
            val newStartTime = snappingTimeAtYOffset(itemInEditModeRect.top, it.itemsStartAndEndTimes)
            val newDuration = calendarItem.endTime?.absoluteDurationBetween(newStartTime)

            if (newDuration != null && newDuration <= Duration.ZERO || newDuration == null && newStartTime > OffsetDateTime.now())
                return

            if (itemInEditModePreviousStartTime != newStartTime) {
                itemInEditModePreviousStartTime = newStartTime
                startTimeChangesChannel.offer(newStartTime)
                shouldTryToAutoScrollToEvent = true
            }

            if (newTop <= 0) {
                cancelDraggingAndAutoScroll()
            }
        }
    }

    private fun updateItemInEditModeEndTime() {
        drawingData.let {
            if (it.selectedCalendarItemToDraw == null) return
            if (it.selectedCalendarItemToDraw.duration == null) return

            val maxHeight = calculateMaxHeight()
            val newBottom = (eventStartingRect.bottom + draggingDelta + draggingScrollDelta).coerceIn(
                itemInEditModeRect.top,
                maxHeight.toFloat()
            )
            itemInEditModeRect.bottom = newBottom
            val newEndTime = snappingTimeAtYOffset(itemInEditModeRect.bottom, it.itemsStartAndEndTimes)
            val calendarItem = it.selectedCalendarItemToDraw
            val newDuration = newEndTime.absoluteDurationBetween(calendarItem.startTime)
            val nextDay = calendarItem.startTime.plusDays(1).truncatedTo(ChronoUnit.DAYS)

            if (newDuration <= Duration.ZERO || newEndTime >= nextDay)
                return

            if (itemInEditModePreviousEndTime != newEndTime) {
                itemInEditModePreviousEndTime = newEndTime
                endTimeChangesChannel.offer(newEndTime)
                shouldTryToAutoScrollToEvent = true
            }

            if (newBottom >= maxHeight)
                cancelDraggingAndAutoScroll()
        }
    }

    private fun updateItemInEditModeOffset() {
        drawingData.let {
            if (it.selectedCalendarItemToDraw == null) return
            val currentDuration = it.selectedCalendarItemToDraw.duration ?: return

            val newTop = eventStartingRect.top + draggingDelta + draggingScrollDelta
            val newBottom = eventStartingRect.bottom + draggingDelta + draggingScrollDelta

            if (newTop <= 0 || newBottom >= calculateMaxHeight()) {
                cancelDraggingAndAutoScroll()
                return
            }

            itemInEditModeRect.top = newTop
            itemInEditModeRect.bottom = newBottom

            val newStartTime =
                newStartTimeWithStaticDuration(itemInEditModeRect.top, it.itemsStartAndEndTimes, currentDuration)
            val calendarItem = it.selectedCalendarItemToDraw
            val nextDay = calendarItem.startTime.plusDays(1).truncatedTo(ChronoUnit.DAYS)

            if (newStartTime + currentDuration >= nextDay)
                return

            if (itemInEditModePreviousStartTime != newStartTime) {
                offsetChangesChannel.offer(newStartTime)
                shouldTryToAutoScrollToEvent = true
            }
        }
    }

    private fun vibrate() {
        hapticFeedbackProvider?.performTickEffect()
    }

    private fun cancelDraggingAndAutoScroll() {
        if (autoScrollAnimator.isRunning) {
            autoScrollAnimator.cancel()
        }
        draggingSpeed = 0f
        draggingDirection = Idle
        invalidate()
    }

    private fun newStartTimeWithStaticDuration(
        yOffset: Float,
        currentItemsStartAndEndTimes: List<OffsetDateTime>,
        duration: Duration
    ): OffsetDateTime {
        if (currentItemsStartAndEndTimes.isEmpty()) {
            return dateTimeOffsetAtYOffset(yOffset).roundToClosestQuarter()
        }

        val newStartTime = dateTimeOffsetAtYOffset(yOffset)
        val newEndTime = newStartTime + duration

        val startSnappingPointDifference = distanceToClosestSnappingPoint(
            newStartTime, currentItemsStartAndEndTimes.plus(newStartTime.roundToClosestQuarter())
        )

        val endSnappingPointDifference = distanceToClosestSnappingPoint(newEndTime, currentItemsStartAndEndTimes)
        val snappingPointDifference = minOf(startSnappingPointDifference.positive(), endSnappingPointDifference.positive())
        return newStartTime + snappingPointDifference
    }

    private fun snappingTimeAtYOffset(
        yOffset: Float,
        currentItemsStartAndEndTimes: List<OffsetDateTime>
    ): OffsetDateTime {
        if (currentItemsStartAndEndTimes.isEmpty()) {
            return dateTimeOffsetAtYOffset(yOffset).roundToClosestQuarter()
        }

        val newTime = dateTimeOffsetAtYOffset(yOffset)
        val snappingPointDifference = distanceToClosestSnappingPoint(
            newTime,
            currentItemsStartAndEndTimes.plus(newTime.roundToClosestQuarter())
        )

        return newTime + snappingPointDifference
    }

    private fun distanceToClosestSnappingPoint(time: OffsetDateTime, data: List<OffsetDateTime>): Duration =
        data.fold(Duration.ofHours(999)) { min, next ->
            val durationUntilNext = time.absoluteDurationBetween(next)
            if (min.positive() <= durationUntilNext) min
            else durationUntilNext
        }

    @Suppress("UNUSED_PARAMETER")
    private fun onLongPress(e: MotionEvent) {
        val x = e.x
        val y = e.y

        val touchedCalendarItem = findCalendarItemFromPoint(x, y + scrollOffset.value)
        if (touchedCalendarItem == null) {
            emptySpaceLongPressedChannel.offer(dateTimeOffsetAtYOffset(y + scrollOffset.value))
        }
    }

    private fun dateTimeOffsetAtYOffset(y: Float): OffsetDateTime {
        val currentOffset = OffsetDateTime.now().offset
        val currentDate = OffsetDateTime.now().toLocalDate().atStartOfDay()
        val seconds = (y / hourHeight.value) * 60 * 60
        val duration = Duration.ofSeconds(seconds.toLong())
        val nextDay = currentDate.plusDays(1)

        val offset = currentDate + duration

        return when {
            offset < currentDate -> currentDate.atOffset(currentOffset)
            offset > nextDay -> nextDay.atOffset(currentOffset)
            else -> offset.atOffset(currentOffset)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun scrollView(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float) {
        if (isDragging)
            return

        val oldScrollOffset = scrollOffset.value
        val newScrollOffset = (oldScrollOffset + distanceY).toInt()
        scrollOffset.value = newScrollOffset.coerceIn(0, calculateMaxScrollOffset())

        onScrollChanged(0, scrollOffset.value, 0, oldScrollOffset)

        isScrolling = true
        postInvalidate()
    }

    private fun calculateMaxHeight() = (hourHeight.value * hoursInTheDay).toInt()
    private fun calculateMaxScrollOffset() = calculateMaxHeight() - height

    @Suppress("UNUSED_PARAMETER")
    private fun flingView(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float) {
        scroller.forceFinished(true)
        flingWasCalled = true
        isScrolling = true
        scroller.fling(0, scrollOffset.value, 0, (-velocityY / 2f).toInt(), 0, 0, 0, calculateMaxHeight().toInt())
        handler.post(::continueScroll)
    }

    private fun continueScroll() {
        isScrolling = isScrolling && scroller.computeScrollOffset()
        if (!isScrolling) {
            invalidate()
            return
        }

        if (scroller.currY == scrollOffset.value) {
            handler.post(::continueScroll)
            return
        }

        val oldScrollOffset = scrollOffset.value
        scrollOffset.value = scroller.currY.coerceIn(0, calculateMaxScrollOffset())

        onScrollChanged(0, scrollOffset.value, 0, oldScrollOffset)

        handler.post(::continueScroll)
        invalidate()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onSingleTapUp(e: MotionEvent) {
        val x = e.x
        val y = e.y

        val touchedCalendarItem = findCalendarItemFromPoint(x, y)
        if (touchedCalendarItem != null) {
            itemTappedChannel.offer(touchedCalendarItem)
        }
    }

    private fun onScale(scaleDetector: ScaleGestureDetector): Boolean {
        val oldHourHeight = hourHeight.value
        val scaleFactor = scaleDetector.scaleFactor
        val newHourHeight = (oldHourHeight * scaleFactor)
        hourHeight.value = newHourHeight.coerceIn(calendarBaseHourHeight, calendarMaxHourHeight)

        val hourSizeChanged = oldHourHeight != hourHeight.value
        if (!hourSizeChanged)
            return true

        // Since the size of each hour is an integer
        // we first need to calculate the real scale
        // factor applied to the calendar
        val actualScale = (newHourHeight - oldHourHeight) / ((newHourHeight + oldHourHeight) / 2)

        // We need to calculate so the calendar feels
        // like it's zooming in and not sliding below
        // the user's fingers
        val focusPointOffset = scaleDetector.focusY * actualScale
        val currScrollOffset = scrollOffset.value
        val scaledOffset = currScrollOffset * actualScale
        val newScrollOffset = currScrollOffset + scaledOffset + focusPointOffset
        scrollOffset.value = newScrollOffset.toInt().coerceIn(0, calculateMaxScrollOffset())

        updateDrawingDelegatesHourHeights()
        cleanAnimationsAndDrawingCache()
        invalidate()

        return true
    }

    private fun updateDrawingDelegatesHourHeights() {
        hourHeight.value.let {
            backgroundDrawingDelegate.currentHourHeight = it
            calendarItemDrawingDelegate.currentHourHeight = it
        }
    }

    private fun cleanAnimationsAndDrawingCache() {
        runningAnimations.values.forEach { (_, animator) ->
            animator.cancel()
        }
        runningAnimations.clear()
        drawingRectFs.clear()
    }

    private fun findCalendarItemFromPoint(x: Float, y: Float): CalendarItem? = drawingData.let {
        if (it.selectedCalendarItemToDraw != null) {
            calendarItemDrawingDelegate.calculateItemRect(it.selectedCalendarItemToDraw, viewFrame, touchRectF)
            if (touchRectF.contains(x, y + scrollOffset.value))
                return it.selectedCalendarItemToDraw
        }

        it.nonSelectedCalendarItemsToDraw.find { calendarItem ->
            calendarItemDrawingDelegate.calculateItemRect(calendarItem, viewFrame, touchRectF)
            touchRectF.contains(x, y + scrollOffset.value)
        }
    }

    private fun scrollFrameToDisplayItemInEditModeIfNeeded(): Boolean {
        val currentItemInEditMode = drawingData.selectedCalendarItemToDraw
        if (currentItemInEditMode == null || !shouldTryToAutoScrollToEvent) return false

        val eventTop = itemInEditModeRect.top
        val eventBottom = itemInEditModeRect.bottom

        val frameTop = viewFrame.top
        val frameBottom = viewFrame.bottom

        if (eventTop < frameTop) {
            scroller.forceFinished(true)
            isScrolling = false
            scrollVerticallyBy(-(abs(frameTop - eventTop) + calendarEventAutoScrollToFrameExtraDistance).toInt())
            return true
        }

        if (eventBottom > frameBottom) {
            scroller.forceFinished(true)
            isScrolling = false
            scrollVerticallyBy((abs(frameBottom - eventBottom) + calendarEventAutoScrollToFrameExtraDistance).toInt())
            return true
        }

        return false
    }

    private fun scrollVerticallyBy(deltaY: Int) {
        if (isScrolling) return
        scroller.forceFinished(true)
        isScrolling = true
        scroller.startScroll(0, scrollOffset.value, 0, deltaY)
        handler.post(::continueScroll)
    }

    private fun isSelectingNewCalendarItem(
        currentlySelectedCalendarItem: CalendarItem?,
        newSelectedCalendarItemToDraw: CalendarItem?
    ): Boolean =
        currentlySelectedCalendarItem == null && newSelectedCalendarItemToDraw != null ||
            currentlySelectedCalendarItem != null && !currentlySelectedCalendarItem.isEquivalent(newSelectedCalendarItemToDraw)

    private fun CalendarItem.isEquivalent(other: CalendarItem?): Boolean {
        return when {
            this == other -> true
            this is CalendarItem.SelectedItem && other is CalendarItem.SelectedItem ->
                this.selectedCalendarItem.isEquivalent(other.selectedCalendarItem)
            else -> false
        }
    }

    private fun SelectedCalendarItem.isEquivalent(other: SelectedCalendarItem): Boolean {
        return when {
            this is SelectedCalendarItem.SelectedTimeEntry && other is SelectedCalendarItem.SelectedTimeEntry ->
                this.isEquivalent(other)
            this is SelectedCalendarItem.SelectedCalendarEvent && other is SelectedCalendarItem.SelectedCalendarEvent ->
                this.isEquivalent(other)
            else -> false
        }
    }

    private fun SelectedCalendarItem.SelectedTimeEntry.isEquivalent(other: SelectedCalendarItem.SelectedTimeEntry): Boolean =
        this.editableTimeEntry.ids.firstOrNull() == other.editableTimeEntry.ids.firstOrNull()

    private fun SelectedCalendarItem.SelectedCalendarEvent.isEquivalent(other: SelectedCalendarItem.SelectedCalendarEvent): Boolean =
        this.calendarEvent.calendarId == other.calendarEvent.calendarId &&
            this.calendarEvent.id == other.calendarEvent.id

    private fun Duration.positive(): Duration =
        if (this.isNegative) this.negated() else this

    inner class GestureListener(
        val onDownCallback: (MotionEvent) -> Unit,
        val onLongPressCallback: (MotionEvent) -> Unit,
        val onScrollCallback: (MotionEvent, MotionEvent, Float, Float) -> Unit,
        val onFlingCallback: (MotionEvent, MotionEvent, Float, Float) -> Unit,
        val onSingleTapUpCallback: (MotionEvent) -> Unit,
        val onScaleCallback: (ScaleGestureDetector) -> Boolean
    ) : GestureDetector.SimpleOnGestureListener(), ScaleGestureDetector.OnScaleGestureListener {

        override fun onDown(e: MotionEvent): Boolean {
            onDownCallback(e)
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            onFlingCallback(e1, e2, velocityX, velocityY)
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            onScrollCallback(e1, e2, distanceX, distanceY)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onLongPressCallback(e)
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onSingleTapUpCallback(e)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean = onScaleCallback(detector)

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

        override fun onScaleEnd(detector: ScaleGestureDetector) {}
    }

    enum class EditAction {
        None, ChangeStart, ChangeEnd, ChangeOffset
    }

    enum class DraggingDirection(val value: Int) {
        Up(-1),
        Idle(0),
        Down(1)
    }
}

private fun CalendarItem.drawingId(): String = when (this) {
    is CalendarItem.TimeEntry -> timeEntry.id.toString()
    is CalendarItem.CalendarEvent -> "${calendarEvent.calendarId}:${calendarEvent.id}"
    is CalendarItem.SelectedItem -> when (selectedCalendarItem) {
        is SelectedCalendarItem.SelectedTimeEntry -> (selectedCalendarItem.editableTimeEntry.ids.firstOrNull()?.toString()
            ?: "null")
        is SelectedCalendarItem.SelectedCalendarEvent -> "${selectedCalendarItem.calendarEvent.calendarId}:${selectedCalendarItem.calendarEvent.id}"
    }
}
