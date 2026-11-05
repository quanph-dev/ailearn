package com.example.basekmp

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.annotation.IdRes
import kotlin.math.abs
import kotlin.math.max

/**
 * Native ad container that lets a decorative overlay (for example `iv_collapse` on top of
 * `ad_media`) observe a tap without consuming it.
 *
 * The real, unmodified MotionEvent stream is always forwarded through [FrameLayout.dispatchTouchEvent],
 * so the SDK views underneath (`NativeAdView` -> `MediaView`) receive the same gesture they would
 * receive if the overlay did not exist. When the gesture is a tap that started and ended inside the
 * overlay bounds, [onTargetTap] runs afterwards on the next main-loop turn, i.e. after the
 * `performClick` that the view under the finger has already queued.
 */
class CollapsePassThroughFrameLayout(context: Context) : FrameLayout(context) {
    @IdRes
    private var targetId: Int = View.NO_ID
    private var onTargetTap: (() -> Unit)? = null
    private var trackingTap = false
    private var downX = 0f
    private var downY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minTouchTarget = (48 * resources.displayMetrics.density).toInt()
    private val hitRect = Rect()

    fun setPassThroughTap(@IdRes viewId: Int, onTap: (() -> Unit)?) {
        targetId = viewId
        onTargetTap = onTap
        trackingTap = false
    }

    fun clearPassThroughTap() = setPassThroughTap(View.NO_ID, null)

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val callback = onTargetTap
        if (callback != null) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x
                    downY = ev.y
                    val target = findTarget()
                    val startedInside = target?.let {
                        // The SDK may (re)attach a click listener to the overlay; a clickable overlay
                        // would become the touch target and swallow the gesture, so keep it inert.
                        neutralize(it)
                        isInside(it, ev.x, ev.y)
                    } == true
                    trackingTap = startedInside
                    if (startedInside && target != null) {
                        target.drawableHotspotChanged(
                            (ev.x - hitRect.centerX() + target.width / 2f),
                            (ev.y - hitRect.centerY() + target.height / 2f)
                        )
                        target.isPressed = true
                    }
                }
                MotionEvent.ACTION_MOVE -> if (trackingTap) {
                    val target = findTarget()
                    if (abs(ev.x - downX) > touchSlop || abs(ev.y - downY) > touchSlop ||
                        target == null || !isInside(target, ev.x, ev.y)
                    ) {
                        target?.isPressed = false
                        trackingTap = false
                    }
                }
                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> if (trackingTap) {
                    findTarget()?.isPressed = false
                    trackingTap = false
                }
            }
        }

        // Never intercept or alter the event: the ad views below must handle the real gesture.
        val handled = super.dispatchTouchEvent(ev)

        if (callback != null && ev.actionMasked == MotionEvent.ACTION_UP && trackingTap) {
            trackingTap = false
            val target = findTarget()
            target?.isPressed = false
            val endedInside = target?.let { isInside(it, ev.x, ev.y) } == true
            if (endedInside) {
                // Posted after super.dispatchTouchEvent(), so it runs after the MediaView's queued click.
                post { if (onTargetTap === callback) callback() }
            }
        }
        return handled
    }

    private fun findTarget(): View? =
        if (targetId == View.NO_ID) null else findViewById<View?>(targetId)?.takeIf { it.isShown && it.width > 0 && it.height > 0 }

    private fun isInside(target: View, x: Float, y: Float): Boolean {
        target.getDrawingRect(hitRect)
        offsetDescendantRectToMyCoords(target, hitRect)
        val extraX = max(0, minTouchTarget - hitRect.width()) / 2
        val extraY = max(0, minTouchTarget - hitRect.height()) / 2
        hitRect.inset(-extraX, -extraY)
        return hitRect.contains(x.toInt(), y.toInt())
    }

    private fun neutralize(target: View) {
        if (target.isClickable || target.isLongClickable || target.hasOnClickListeners()) {
            target.setOnClickListener(null)
            target.isClickable = false
            target.isLongClickable = false
        }
        target.isFocusable = false
    }
}
