package com.toggl.calendar.calendarday.ui.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import com.toggl.calendar.common.domain.CalendarItem
import com.toggl.calendar.common.domain.colorString
import com.toggl.calendar.common.domain.description
import com.toggl.calendar.common.domain.duration
import com.toggl.calendar.common.domain.endTime
import com.toggl.calendar.common.domain.isRunning
import com.toggl.calendar.common.domain.startTime
import com.toggl.common.Constants.ClockMath.minutesInAnHour
import com.toggl.common.extensions.absoluteDurationBetween
import com.toggl.common.feature.models.SelectedCalendarItem
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Suppress("UNUSED_PARAMETER")
class CalendarItemDrawingDelegate(
    private val itemSpacing: Float,
    private val leftMargin: Float,
    private val leftPadding: Float,
    private val rightPadding: Float,
    private val cornerRadius: Float,
    private val calendarEventBottomLineHeight: Float,
    private val normalCalendarIconBitmap: Bitmap,
    private val smallCalendarIconBitMap: Bitmap,
    private val calendarBackgroundColor: Int,
    private val runningTimeEntryThinStripeWidth: Float,
    private val runningTimeEntryStripesSpacing: Float,
    private val runningTimeEntryBorderStrokeWidth: Float,
    private val runningTimeEntryDashedHourTopPadding: Float,
    private val calendarRunningTimeEntryExtraHeight: Float,
    private val calendarIconSize: Float,
    private val calendarIconRightInsetMargin: Float,
    private val shortCalendarItemHeight: Float,
    private val shortCalendarItemFontSize: Float,
    private val regularCalendarItemFontSize: Float,
    private val shortCalendarItemVerticalPadding: Float,
    private val regularCalendarItemVerticalPadding: Float,
    private val shortCalendarItemHorizontalPadding: Float,
    private val regularCalendarItemHorizontalPadding: Float,
    private val primaryTextColor: Int,
    private val editingHandlesHorizontalMargins: Float,
    private val editingHandlesRadius: Float,
    private val editingHandlesStrokeWidth: Float,
    private val defaultCalendarItemColor: Int,
    private val editingHoursLabelsTextSize: Float,
    private val editingHoursLabelsStartMargin: Float,
    private val editingHoursLabelsTextColor: Int
) {
    private val fivePercentIntAlpha: Int = (255 * 0.05).toInt()
    private val tenPercentIntAlpha: Int = 255 / 10
    private val twentyFivePercentIntAlpha = 255 / 4
    private val runningTimeEntryStripesRotationAngle = 45f
    private var calendarItemPaintedBackgroundColor: Int = 0
    private val minimumTextContrast = 1.6f
    private val minutesInAnHourF = minutesInAnHour.toFloat()

    private val textLayouts: MutableMap<String, StaticLayout> = mutableMapOf()
    private val itemPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val calendarIconPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hourLabelsPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        color = editingHoursLabelsTextColor
        textSize = editingHoursLabelsTextSize
    }
    private val hourFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val drawingRect: RectF = RectF()
    private val stripeRect = RectF()
    private val dashEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)

    private var minHourHeight: Float = 0f
    var currentHourHeight: Float = 0f
        set(value) {
            minHourHeight = value / 4
            field = value
        }

    fun onDraw(
        canvas: Canvas,
        calendarBounds: RectF,
        calendarItem: CalendarItem,
        isInEditMode: Boolean = false,
        preCalculatedDrawingRect: RectF
    ) {
        drawingRect.set(preCalculatedDrawingRect)

        if (drawingRect.bottom < calendarBounds.top || drawingRect.top > calendarBounds.bottom)
            return

        when {
            calendarItem.isTimeEntry() && calendarItem.duration != null ->
                canvas.drawStoppedTimeEntryItem(calendarItem)
            calendarItem.isTimeEntry() && calendarItem.duration == null ->
                canvas.drawRunningTimeEntryItem(calendarItem)
            calendarItem.isCalendarEvent() -> canvas.drawCalendarEventItem(calendarItem)
        }
        canvas.drawCalendarItemText(calendarItem)
        if (isInEditMode && calendarItem.isTimeEntry()) {
            canvas.drawEditingHandles(calendarItem)
            canvas.drawHourIndicators(calendarItem)
        }
    }

    fun calculateItemRect(calendarItem: CalendarItem, calendarBounds: RectF, outRect: RectF) {
        val totalItemSpacing = (calendarItem.totalColumns - 1) * itemSpacing
        val availableWidth = calendarBounds.width() - leftMargin
        val eventWidth = (availableWidth - leftPadding - rightPadding - totalItemSpacing) / calendarItem.totalColumns
        val left = leftMargin + leftPadding + eventWidth * calendarItem.columnIndex + calendarItem.columnIndex * itemSpacing
        val startTime = calendarItem.startTime.withOffsetSameInstant(OffsetDateTime.now().offset)
        val totalHours = startTime.hour + startTime.minute.toFloat() / minutesInAnHourF
        val durationHours = duration(calendarItem).toMinutes().toFloat() / minutesInAnHourF
        val durationInHeight = max(minHourHeight, durationHours * currentHourHeight)
        val top = calculateHourHeight(totalHours)
        outRect.set(left, top, left + eventWidth, top + durationInHeight)
    }

    private fun CalendarItem.isTimeEntry() =
        this is CalendarItem.TimeEntry ||
            (this is CalendarItem.SelectedItem && this.selectedCalendarItem is SelectedCalendarItem.SelectedTimeEntry)

    private fun CalendarItem.isCalendarEvent() =
        this is CalendarItem.CalendarEvent ||
            (this is CalendarItem.SelectedItem && this.selectedCalendarItem is SelectedCalendarItem.SelectedCalendarEvent)

    private fun Canvas.drawCalendarItemText(calendarItem: CalendarItem) {
        val textLeftPadding =
            if (calendarItem is CalendarItem.CalendarEvent) calendarIconSize - calendarIconRightInsetMargin else 0f
        val eventHeight = drawingRect.height()
        val eventWidth = drawingRect.width() - textLeftPadding
        val isShort = eventHeight <= shortCalendarItemHeight
        val fontSize = if (isShort) shortCalendarItemFontSize else regularCalendarItemFontSize
        var textVerticalPadding = if (isShort) shortCalendarItemVerticalPadding else regularCalendarItemVerticalPadding
        textVerticalPadding = min(eventHeight - fontSize, textVerticalPadding)
        val textHorizontalPadding = if (isShort) shortCalendarItemHorizontalPadding else regularCalendarItemHorizontalPadding

        val textWidth = eventWidth - textHorizontalPadding * 2
        if (textWidth <= 0) return

        val eventTextLayout = getCalendarItemTextLayout(calendarItem, textWidth, fontSize, calendarItem.isRunning)
        val totalLineHeight = calculateLineHeight(eventHeight, eventTextLayout).toFloat()

        withTranslation(
            x = drawingRect.left + textHorizontalPadding + textLeftPadding,
            y = drawingRect.top + textVerticalPadding
        ) {
            clipRect(0f, 0f, eventWidth - textHorizontalPadding, totalLineHeight)
            eventTextLayout.draw(this)
        }
    }

    private fun calculateLineHeight(eventHeight: Float, eventTextLayout: StaticLayout): Int =
        (0 until eventTextLayout.lineCount)
            .map(eventTextLayout::getLineBottom)
            .filter { it <= eventHeight }
            .maxOrNull() ?: 0

    private fun getCalendarItemTextLayout(
        item: CalendarItem,
        eventWidth: Float,
        fontSize: Float,
        isRunning: Boolean
    ): StaticLayout {
        val eventTextLayout = textLayouts[item.description]
        if (eventTextLayout != null && abs(eventTextLayout.width - eventWidth) <= 0.1 && eventTextLayout.text == item.description)
            return eventTextLayout

        val color = calculateBestContrastingTextColorFor(item, isRunning)

        textPaint.color = color
        textPaint.textSize = fontSize
        @Suppress("DEPRECATION")
        val newEventTextLayout = StaticLayout(
            item.description,
            0,
            item.description.length,
            TextPaint(textPaint),
            eventWidth.toInt(),
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0.0f,
            true,
            TextUtils.TruncateAt.END,
            eventWidth.toInt()
        )

        textLayouts[item.description] = newEventTextLayout
        return newEventTextLayout
    }

    private fun calculateBestContrastingTextColorFor(item: CalendarItem, isRunning: Boolean): Int {
        val basicStrategyColor =
            if (item is CalendarItem.CalendarEvent || isRunning)
                item.colorString.toColorOrDefault()
            else Color.WHITE

        val basicColorContrast =
            ColorUtils.calculateContrast(basicStrategyColor, calendarItemPaintedBackgroundColor)
        if (basicColorContrast >= minimumTextContrast) return basicStrategyColor

        val primaryTextColor = primaryTextColor
        val primaryTextColorContrast =
            ColorUtils.calculateContrast(primaryTextColor, calendarItemPaintedBackgroundColor)
        if (primaryTextColorContrast >= minimumTextContrast) return primaryTextColor

        val whiteContrast =
            ColorUtils.calculateContrast(Color.WHITE, calendarItemPaintedBackgroundColor)
        return if (whiteContrast >= minimumTextContrast) Color.WHITE else Color.BLACK
    }

    private fun Canvas.drawCalendarEventItem(calendarItem: CalendarItem) {
        val originalColor = calendarItem.colorString.toColorOrDefault()
        val fadedColor = ColorUtils.setAlphaComponent(originalColor, twentyFivePercentIntAlpha)
        val fadedColorOnWhite = ColorUtils.compositeColors(fadedColor, Color.WHITE)
        itemPaint.style = Paint.Style.FILL_AND_STROKE

        itemPaint.color = fadedColorOnWhite
        calendarItemPaintedBackgroundColor = fadedColorOnWhite
        drawRoundRect(drawingRect, cornerRadius, cornerRadius, itemPaint)

        itemPaint.color = originalColor
        drawRoundRect(
            drawingRect.left,
            drawingRect.bottom - calendarEventBottomLineHeight,
            drawingRect.right,
            drawingRect.bottom,
            cornerRadius,
            cornerRadius,
            itemPaint
        )

        val calendarBitmap = getProperlySizedCalendarBitmap() ?: return

        calendarIconPaint.colorFilter = PorterDuffColorFilter(originalColor, PorterDuff.Mode.SRC_IN)
        drawBitmap(calendarBitmap, drawingRect.left, drawingRect.top, calendarIconPaint)
    }

    private fun Canvas.drawStoppedTimeEntryItem(calendarItem: CalendarItem) {
        itemPaint.color = calendarItem.colorString.toColorOrDefault()
        itemPaint.style = Paint.Style.FILL_AND_STROKE
        calendarItemPaintedBackgroundColor = itemPaint.color
        drawRoundRect(drawingRect, cornerRadius, cornerRadius, itemPaint)
    }

    private fun Canvas.drawRunningTimeEntryItem(calendarItem: CalendarItem) {
        val itemColor = calendarItem.colorString.toColorOrDefault()
        val calendarFillColor = ColorUtils.setAlphaComponent(itemColor, fivePercentIntAlpha)
        val calendarFillColorOnCalendarBackgroundColor = ColorUtils.compositeColors(calendarFillColor, calendarBackgroundColor)
        val calendarStripeColor = ColorUtils.setAlphaComponent(itemColor, tenPercentIntAlpha)
        calendarItemPaintedBackgroundColor = calendarFillColorOnCalendarBackgroundColor
        drawShapeBaseBackgroundFilling(calendarFillColorOnCalendarBackgroundColor)
        drawShapeBackgroundStripes(calendarStripeColor)
        drawSolidBorder(itemColor)
        drawBottomDashedBorder(calendarFillColorOnCalendarBackgroundColor)
    }

    private fun Canvas.drawShapeBaseBackgroundFilling(color: Int) {
        itemPaint.color = color
        itemPaint.style = Paint.Style.FILL_AND_STROKE
        drawRoundRect(drawingRect, cornerRadius, cornerRadius, itemPaint)
    }

    private fun Canvas.drawShapeBackgroundStripes(color: Int) {
        withClip(drawingRect) {
            rotate(runningTimeEntryStripesRotationAngle, drawingRect.left, drawingRect.top)
            itemPaint.color = color
            val hyp = hypot(drawingRect.height(), drawingRect.width())
            stripeRect.set(
                drawingRect.left,
                drawingRect.top - hyp,
                drawingRect.left + runningTimeEntryThinStripeWidth,
                drawingRect.bottom + hyp
            )
            var stripeStart = 0f
            while (stripeStart < hyp) {
                stripeRect.set(
                    drawingRect.left + stripeStart,
                    stripeRect.top,
                    drawingRect.left + stripeStart + runningTimeEntryThinStripeWidth,
                    stripeRect.bottom
                )
                drawRect(stripeRect, itemPaint)
                stripeStart += runningTimeEntryStripesSpacing
            }
        }
    }

    private fun Canvas.drawSolidBorder(color: Int) {
        itemPaint.style = Paint.Style.STROKE
        itemPaint.color = color
        itemPaint.strokeWidth = runningTimeEntryBorderStrokeWidth
        drawRoundRect(drawingRect, cornerRadius, cornerRadius, itemPaint)
    }

    private fun Canvas.drawBottomDashedBorder(color: Int) {
        itemPaint.color = color
        itemPaint.pathEffect = dashEffect
        val currentHourPx = calculateCurrentHourOffset() - runningTimeEntryDashedHourTopPadding
        val sevenMinutesInPixels = Duration.ofMinutes(7).toHours() / minutesInAnHourF * currentHourHeight
        val bottom = drawingRect.bottom + calendarRunningTimeEntryExtraHeight
        stripeRect.set(
            drawingRect.left - itemPaint.strokeWidth,
            currentHourPx,
            drawingRect.right + itemPaint.strokeWidth,
            bottom + itemPaint.strokeWidth
        )
        withClip(stripeRect) {
            stripeRect.set(
                drawingRect.left,
                stripeRect.top - sevenMinutesInPixels,
                drawingRect.right,
                drawingRect.bottom
            )
            drawRoundRect(stripeRect, cornerRadius, cornerRadius, itemPaint)
        }
        itemPaint.pathEffect = null
    }

    private fun calculateCurrentHourOffset(): Float {
        val now = OffsetDateTime.now()
        return (now.hour + now.minute.toFloat() / minutesInAnHourF) * currentHourHeight
    }

    private fun getProperlySizedCalendarBitmap(): Bitmap? {
        val containerHeight = drawingRect.height()
        if (containerHeight > normalCalendarIconBitmap.height)
            return normalCalendarIconBitmap
        if (containerHeight > smallCalendarIconBitMap.height)
            return smallCalendarIconBitMap
        return null
    }

    private fun Canvas.drawEditingHandles(calendarItem: CalendarItem) {
        val isRunning = calendarItem.isRunning
        itemPaint.color = Color.WHITE
        itemPaint.style = Paint.Style.FILL_AND_STROKE

        drawCircle(drawingRect.right - editingHandlesHorizontalMargins, drawingRect.top, editingHandlesRadius, itemPaint)
        if (!isRunning) {
            drawCircle(drawingRect.left + editingHandlesHorizontalMargins, drawingRect.bottom, editingHandlesRadius, itemPaint)
        }

        itemPaint.style = Paint.Style.STROKE
        itemPaint.strokeWidth = editingHandlesStrokeWidth
        itemPaint.color = calendarItem.colorString.toColorOrDefault()

        drawCircle(drawingRect.right - editingHandlesHorizontalMargins, drawingRect.top, editingHandlesRadius, itemPaint)
        if (!isRunning)
            drawCircle(drawingRect.left + editingHandlesHorizontalMargins, drawingRect.bottom, editingHandlesRadius, itemPaint)
    }

    private fun Canvas.drawHourIndicators(calendarItem: CalendarItem) {
        val startHourLabel = hourFormat.format(calendarItem.startTime)
        val endHourLabel = hourFormat.format(calendarItem.endTime ?: OffsetDateTime.now())
        drawText(startHourLabel, editingHoursLabelsStartMargin, drawingRect.top + hourLabelsPaint.descent(), hourLabelsPaint)
        drawText(endHourLabel, editingHoursLabelsStartMargin, drawingRect.bottom + hourLabelsPaint.descent(), hourLabelsPaint)
    }

    private fun duration(calendarItem: CalendarItem): Duration {
        return when (calendarItem) {
            is CalendarItem.TimeEntry ->
                calendarItem.timeEntry.duration
                    ?: calendarItem.timeEntry.startTime.absoluteDurationBetween(OffsetDateTime.now())
            is CalendarItem.CalendarEvent -> calendarItem.calendarEvent.duration
            is CalendarItem.SelectedItem -> when (val selectedItem = calendarItem.selectedCalendarItem) {
                is SelectedCalendarItem.SelectedTimeEntry -> selectedItem.editableTimeEntry.duration
                    ?: selectedItem.editableTimeEntry.startTime!!.absoluteDurationBetween(OffsetDateTime.now())
                is SelectedCalendarItem.SelectedCalendarEvent -> selectedItem.calendarEvent.duration
            }
        }
    }

    private fun calculateHourHeight(hour: Float) = hour * currentHourHeight

    private fun String?.toColorOrDefault() = this?.run { Color.parseColor(this) } ?: defaultCalendarItemColor
}
