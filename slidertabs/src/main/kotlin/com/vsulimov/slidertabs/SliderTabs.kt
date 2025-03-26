package com.vsulimov.slidertabs

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip

/**
 * A custom view that displays two tabs with a sliding indicator.
 * The user can switch between the tabs by clicking on the opposite side of the current slider position.
 * The slider animates smoothly to the selected tab, providing visual feedback.
 *
 * This view can be customized via XML attributes or programmatically through public methods.
 * It supports state persistence across configuration changes (e.g., screen rotations) and provides
 * a listener interface for reacting to tab selection events.
 *
 * ### Key Features:
 * - Two-tab layout with a sliding indicator.
 * - Touch interaction to switch tabs.
 * - Animated transitions between tabs.
 * - Customizable colors, text, and animation duration.
 * - State saving and restoration.
 *
 * ### Usage:
 * Add to your layout XML:
 * ```xml
 * <com.vsulimov.slidertabs.SliderTabs
 *     android:id="@+id/slider_tabs"
 *     android:layout_width="match_parent"
 *     android:layout_height="40dp"
 *     style="@style/SliderTabs" />
 * ```
 *
 * Programmatically:
 * ```kotlin
 * val sliderTabs = SliderTabs(context)
 * sliderTabs.setOnTabSelectedListener(object : SliderTabs.OnTabSelectedListener {
 *     override fun onLeftTabSelected() { /* Handle left tab selection */ }
 *     override fun onRightTabSelected() { /* Handle right tab selection */ }
 * })
 * sliderTabs.selectRightTab()
 * ```
 *
 * @see OnTabSelectedListener for listening to tab selection events.
 */
class SliderTabs : View {

    /**
     * Paint object used for drawing the background and slider rectangles.
     * Configured with anti-aliasing enabled to ensure smooth edges.
     */
    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Paint object used for drawing the text on the tabs.
     * Configured with anti-aliasing enabled for clear text rendering.
     */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * RectF object defining the bounds of the background rectangle.
     * Spans the entire width and height of the view with rounded corners.
     */
    private val backgroundRectF = RectF()

    /**
     * RectF object defining the bounds of the slider rectangle.
     * Represents the sliding indicator that moves between the left and right tabs.
     */
    private val sliderRectF = RectF()

    /**
     * ValueAnimator responsible for animating the slider's horizontal position.
     * Updates the `sliderXOffset` during animation and triggers view invalidation.
     */
    private val sliderValueAnimator = ValueAnimator()

    /**
     * Color of the background when the view is not pressed.
     * Default value is parsed from `DEFAULT_BG_COLOR_HEX` ("#E0E0E0").
     */
    private var _backgroundColor = DEFAULT_BG_COLOR_HEX.toColorInt()

    /**
     * Color of the background when the view is pressed (e.g., during a touch event).
     * Default value is parsed from `DEFAULT_BG_PRESSED_COLOR_HEX` ("#AEAEAE").
     */
    private var backgroundColorPressed = DEFAULT_BG_PRESSED_COLOR_HEX.toColorInt()

    /**
     * Color of the slider rectangle.
     * Default value is parsed from `DEFAULT_SLIDER_COLOR_HEX` ("#FFFFFF").
     */
    private var sliderColor = DEFAULT_SLIDER_COLOR_HEX.toColorInt()

    /**
     * Color of the text displayed on the tabs.
     * Default value is parsed from `DEFAULT_TAB_TEXT_COLOR_HEX` ("#000000").
     */
    private var onTabTextColor = DEFAULT_TAB_TEXT_COLOR_HEX.toColorInt()

    /**
     * Color of the text displayed on the surface.
     * Default value is parsed from `DEFAULT_TAB_TEXT_COLOR_HEX` ("#000000").
     */
    private var onSurfaceTextColor = DEFAULT_TAB_TEXT_COLOR_HEX.toColorInt()

    /**
     * Text displayed on the left tab.
     * Defaults to a string resource `st_default_left_tab_text` defined in the app.
     */
    private var leftTabText = context.getString(R.string.st_default_left_tab_text)

    /**
     * Text displayed on the right tab.
     * Defaults to a string resource `st_default_right_tab_text` defined in the app.
     */
    private var rightTabText = context.getString(R.string.st_default_right_tab_text)

    /**
     * Duration of the slider animation when switching tabs, in milliseconds.
     * Default value is `DEFAULT_ANIMATION_DURATION_MS` (300ms).
     */
    private var animationDuration = DEFAULT_ANIMATION_DURATION_MS

    /**
     * Corner radius of the background rectangle, in pixels.
     * Calculated based on `BACKGROUND_RECT_RADIUS_DP` and the device's display density.
     */
    private var backgroundRectRadius = 0.0f

    /**
     * Inset (margin) for the slider rectangle from the edges of the background, in pixels.
     * Calculated based on `SLIDER_RECT_INSET_DP` and the device's display density.
     */
    private var sliderRectInset = 0.0f

    /**
     * Corner radius of the slider rectangle, in pixels.
     * Calculated based on `SLIDER_RECT_RADIUS_DP` and the device's display density.
     */
    private var sliderRectRadius = 0.0f

    /**
     * Size of the text on the tabs, in pixels.
     * Calculated based on `TAB_TEXT_SIZE_SP` and the device's display density.
     */
    private var tabTextSize = 0.0f

    /**
     * Horizontal offset of the slider from its leftmost position, in pixels.
     * Ranges from 0 (left tab) to half the view's width (right tab).
     */
    private var sliderXOffset = 0.0f

    /**
     * Flag indicating whether the view is currently being pressed by a touch event.
     * Used to toggle the background color between pressed and unpressed states.
     */
    private var isViewPressed = false

    /**
     * Current state of the slider, represented by the `SliderState` enum.
     * Initial value is `IDLE_LEFT`, indicating the slider is on the left tab.
     */
    private var sliderState = SliderState.IDLE_LEFT

    /**
     * Listener for tab selection events.
     * Can be set to receive callbacks when the left or right tab is selected.
     */
    private var tabSelectedListener: OnTabSelectedListener? = null

    /**
     * Constructor for creating the view programmatically with default values.
     *
     * @param context The context in which the view is running, typically an Activity or Fragment.
     */
    constructor(context: Context) : super(context) {
        performViewInitialization(resources.displayMetrics)
    }

    /**
     * Constructor for creating the view from XML with custom attributes.
     *
     * @param context The context in which the view is running.
     * @param attrs The attribute set defined in XML, containing customization options.
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        attrs?.let { consumeAttributeSet(context, it) }
        performViewInitialization(resources.displayMetrics)
    }

    /**
     * Processes the XML attribute set to customize the view's appearance and behavior.
     * Retrieves values for colors, tab text, and animation duration, falling back to defaults if not specified.
     *
     * @param context The context in which the view is running.
     * @param attrs The attribute set from XML containing customization options.
     */
    private fun consumeAttributeSet(context: Context, attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.SliderTabs, DEFAULT_STYLE_ATTRIBUTE, DEFAULT_STYLE_RESOURCE
        )
        try {
            _backgroundColor = typedArray.getColor(
                R.styleable.SliderTabs_st_backgroundColor, DEFAULT_BG_COLOR_HEX.toColorInt()
            )
            backgroundColorPressed = typedArray.getColor(
                R.styleable.SliderTabs_st_backgroundColorPressed, DEFAULT_BG_PRESSED_COLOR_HEX.toColorInt()
            )
            sliderColor = typedArray.getColor(
                R.styleable.SliderTabs_st_sliderColor, DEFAULT_SLIDER_COLOR_HEX.toColorInt()
            )
            onTabTextColor = typedArray.getColor(
                R.styleable.SliderTabs_st_onTabTextColor, DEFAULT_TAB_TEXT_COLOR_HEX.toColorInt()
            )
            onSurfaceTextColor = typedArray.getColor(
                R.styleable.SliderTabs_st_onSurfaceTextColor, DEFAULT_TAB_TEXT_COLOR_HEX.toColorInt()
            )
            leftTabText = resolveLeftTabText(context, typedArray)
            rightTabText = resolveRightTabText(context, typedArray)
            animationDuration = typedArray.getInt(
                R.styleable.SliderTabs_st_animationDuration, DEFAULT_ANIMATION_DURATION_MS
            )
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * Resolves the text for the left tab from the XML attributes.
     * Falls back to a default string resource if no value is provided.
     *
     * @param context The context in which the view is running.
     * @param typedArray The typed array of attributes from XML.
     * @return The resolved text for the left tab.
     */
    private fun resolveLeftTabText(context: Context, typedArray: TypedArray): String {
        val leftTabText = typedArray.getString(R.styleable.SliderTabs_st_leftTabText)
        return leftTabText ?: context.getString(R.string.st_default_left_tab_text)
    }

    /**
     * Resolves the text for the right tab from the XML attributes.
     * Falls back to a default string resource if no value is provided.
     *
     * @param context The context in which the view is running.
     * @param typedArray The typed array of attributes from XML.
     * @return The resolved text for the right tab.
     */
    private fun resolveRightTabText(context: Context, typedArray: TypedArray): String {
        val rightTabText = typedArray.getString(R.styleable.SliderTabs_st_rightTabText)
        return rightTabText ?: context.getString(R.string.st_default_right_tab_text)
    }

    /**
     * Initializes the view by calculating density-dependent values and configuring paints and the animator.
     * Called by both constructors to ensure consistent setup.
     *
     * @param displayMetrics The display metrics of the device, used for density calculations.
     */
    private fun performViewInitialization(displayMetrics: DisplayMetrics) {
        calculateDensityDependentValues(displayMetrics)
        configureTextPaint()
        configureValueAnimator()
    }

    /**
     * Calculates density-dependent values such as corner radii and text size based on the device's display metrics.
     * Ensures the view scales appropriately across different screen densities.
     *
     * @param displayMetrics The display metrics of the device.
     */
    private fun calculateDensityDependentValues(displayMetrics: DisplayMetrics) {
        backgroundRectRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, BACKGROUND_RECT_RADIUS_DP, displayMetrics
        )
        sliderRectInset = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, SLIDER_RECT_INSET_DP, displayMetrics
        )
        sliderRectRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, SLIDER_RECT_RADIUS_DP, displayMetrics
        )
        tabTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, TAB_TEXT_SIZE_SP, displayMetrics
        )
    }

    /**
     * Configures the text paint with the specified color and size.
     * Ensures the tab text is rendered with the correct appearance.
     */
    private fun configureTextPaint() {
        textPaint.color = onTabTextColor
        textPaint.textSize = tabTextSize
    }

    /**
     * Configures the `ValueAnimator` for animating the slider's position.
     * Sets the duration, interpolator, and listeners to update the slider offset and state.
     */
    private fun configureValueAnimator() {
        sliderValueAnimator.duration = animationDuration.toLong()
        sliderValueAnimator.interpolator = AccelerateDecelerateInterpolator()
        sliderValueAnimator.addUpdateListener { animation ->
            sliderXOffset = animation.animatedValue as Float
            invalidate()
        }
        sliderValueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                sliderState = when (sliderState) {
                    SliderState.MOVING_RIGHT -> SliderState.IDLE_RIGHT
                    SliderState.MOVING_LEFT -> SliderState.IDLE_LEFT
                    else -> throw IllegalStateException("Animation finished while slider in idle state")
                }
            }
        })
    }

    /**
     * Called when the size of the view changes (e.g., during layout or orientation changes).
     * Updates the rectangles and slider offset if the view is visible.
     *
     * @param width The new width of the view in pixels.
     * @param height The new height of the view in pixels.
     * @param oldWidth The previous width of the view in pixels.
     * @param oldHeight The previous height of the view in pixels.
     */
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        if (isViewVisible(width, height)) {
            calculateRectangles()
            recalculateSliderXOffset()
        }
    }

    /**
     * Checks if the view has a visible size (i.e., width and height are greater than zero).
     *
     * @param width The width of the view in pixels.
     * @param height The height of the view in pixels.
     * @return `true` if the view is visible, `false` otherwise.
     */
    private fun isViewVisible(width: Int, height: Int): Boolean {
        return width > 0 && height > 0
    }

    /**
     * Calculates the bounds for the background and slider rectangles.
     * Called when the view size changes to ensure proper layout.
     */
    private fun calculateRectangles() {
        calculateBackgroundRectF()
        calculateSliderRectF()
    }

    /**
     * Sets the bounds for the background rectangle based on the view's dimensions.
     * The rectangle spans the entire view with coordinates (0, 0, width, height).
     */
    private fun calculateBackgroundRectF() {
        backgroundRectF.left = BACKGROUND_RECT_LEFT
        backgroundRectF.top = BACKGROUND_RECT_TOP
        backgroundRectF.right = width.toFloat()
        backgroundRectF.bottom = height.toFloat()
    }

    /**
     * Sets the bounds for the slider rectangle based on the current offset and view dimensions.
     * The slider occupies half the view's width and is inset from the edges.
     */
    private fun calculateSliderRectF() {
        sliderRectF.left = calculateSliderRectLeft()
        sliderRectF.top = calculateSliderRectTop()
        sliderRectF.right = calculateSliderRectRight()
        sliderRectF.bottom = calculateSliderRectBottom()
    }

    /**
     * Calculates the left edge of the slider rectangle.
     *
     * @return The left position in pixels, accounting for inset and offset.
     */
    private fun calculateSliderRectLeft(): Float {
        return SLIDER_RECT_LEFT + sliderRectInset + sliderXOffset
    }

    /**
     * Calculates the top edge of the slider rectangle.
     *
     * @return The top position in pixels, accounting for inset.
     */
    private fun calculateSliderRectTop(): Float {
        return SLIDER_RECT_TOP + sliderRectInset
    }

    /**
     * Calculates the right edge of the slider rectangle.
     *
     * @return The right position in pixels, accounting for inset and offset.
     */
    private fun calculateSliderRectRight(): Float {
        return width / TABS_COUNT - sliderRectInset + sliderXOffset
    }

    /**
     * Calculates the bottom edge of the slider rectangle.
     *
     * @return The bottom position in pixels, accounting for inset.
     */
    private fun calculateSliderRectBottom(): Float {
        return height - sliderRectInset
    }

    /**
     * Recalculates the slider's horizontal offset based on its current state.
     * Sets `sliderXOffset` to 0 for the left tab or half the width for the right tab.
     */
    private fun recalculateSliderXOffset() {
        if (sliderState === SliderState.IDLE_LEFT) {
            sliderXOffset = 0.0f
        } else if (sliderState === SliderState.IDLE_RIGHT) {
            sliderXOffset = width / 2.0f
        }
    }

    /**
     * Measures the view's dimensions based on the provided measure specs.
     * Determines the final width and height considering desired sizes and constraints.
     *
     * @param widthMeasureSpec The width measure specification from the parent layout.
     * @param heightMeasureSpec The height measure specification from the parent layout.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = calculateDesiredWidth()
        val desiredHeight = calculateDesiredHeight()
        val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /**
     * Calculates the desired width of the view.
     * Based on the minimum width plus padding.
     *
     * @return The desired width in pixels.
     */
    private fun calculateDesiredWidth(): Int {
        return suggestedMinimumWidth + paddingLeft + paddingRight
    }

    /**
     * Calculates the desired height of the view.
     * Based on the minimum height plus padding.
     *
     * @return The desired height in pixels.
     */
    private fun calculateDesiredHeight(): Int {
        return suggestedMinimumHeight + paddingTop + paddingBottom
    }

    /**
     * Draws the view's components on the canvas.
     * Renders the background, slider, and tab text in that order.
     *
     * @param canvas The canvas object to draw on.
     */
    override fun onDraw(canvas: Canvas) {
        drawBackground(canvas)
        drawSlider(canvas)
        drawText(canvas)
    }

    /**
     * Draws the background rectangle with the appropriate color based on the pressed state.
     *
     * @param canvas The canvas object to draw on.
     */
    private fun drawBackground(canvas: Canvas) {
        val backgroundColor = resolveBackgroundColor()
        rectPaint.color = backgroundColor
        canvas.drawRoundRect(backgroundRectF, backgroundRectRadius, backgroundRectRadius, rectPaint)
    }

    /**
     * Determines the background color based on whether the view is pressed.
     *
     * @return The resolved background color as an integer.
     */
    private fun resolveBackgroundColor(): Int {
        return if (isViewPressed) {
            backgroundColorPressed
        } else {
            _backgroundColor
        }
    }

    /**
     * Draws the slider rectangle with its specified color and current position.
     *
     * @param canvas The canvas object to draw on.
     */
    private fun drawSlider(canvas: Canvas) {
        rectPaint.color = sliderColor
        recalculateSliderRectSides()
        canvas.drawRoundRect(sliderRectF, sliderRectRadius, sliderRectRadius, rectPaint)
    }

    /**
     * Updates the left and right sides of the slider rectangle based on the current offset.
     * Ensures the slider's position reflects the latest `sliderXOffset` value.
     */
    private fun recalculateSliderRectSides() {
        sliderRectF.left = calculateSliderRectLeft()
        sliderRectF.right = calculateSliderRectRight()
    }

    /**
     * Draws the text for both the left and right tabs.
     *
     * @param canvas The canvas object to draw on.
     */
    private fun drawText(canvas: Canvas) {
        drawLeftTabText(canvas)
        drawRightTabText(canvas)
    }

    /**
     * Draws the text for the left tab at its calculated position.
     *
     * @param canvas The canvas object to draw on.
     */
    private fun drawLeftTabText(canvas: Canvas) {
        val text = leftTabText
        val textWidth = textPaint.measureText(text)
        val x = calculateTabTextX(LEFT_TAB_TEXT_CX, textWidth)
        val y = calculateTabTextY()

        textPaint.color = onSurfaceTextColor

        canvas.drawText(text, x, y, textPaint)
        canvas.withClip(sliderRectF) {
            textPaint.color = onTabTextColor
            drawText(text, x, y, textPaint)
        }
    }

    /**
     * Draws the text for the right tab at its calculated position.
     *
     * @param canvas The canvas object to draw on.
     */
    private fun drawRightTabText(canvas: Canvas) {
        val text = rightTabText
        val textWidth = textPaint.measureText(text)
        val x = calculateTabTextX(RIGHT_TAB_TEXT_CX, textWidth)
        val y = calculateTabTextY()

        textPaint.color = onSurfaceTextColor

        canvas.drawText(text, x, y, textPaint)
        canvas.withClip(sliderRectF) {
            textPaint.color = onTabTextColor
            drawText(text, x, y, textPaint)
        }
    }

    /**
     * Calculates the x-coordinate for the tab text to center it horizontally within its tab area.
     * Uses a center x-ratio (`textCX`) to position the text at a percentage of the view's width.
     *
     * @param textCX The center x-ratio (0.0 to 1.0), e.g., 0.25 for the left tab, 0.75 for the right tab.
     * @param textWidth The width of the text in pixels.
     * @return The x-coordinate for the text in pixels.
     */
    private fun calculateTabTextX(textCX: Float, textWidth: Float): Float {
        return width * textCX - textWidth / 2.0f
    }

    /**
     * Calculates the y-coordinate for the tab text to center it vertically within the view.
     *
     * @return The y-coordinate for the text in pixels.
     */
    private fun calculateTabTextY(): Float {
        return height / 2 - (textPaint.descent() + textPaint.ascent()) / 2
    }

    /**
     * Handles touch events to detect presses and releases, enabling tab switching.
     * - `ACTION_DOWN`: Sets the pressed state if the touch is on the opposite side.
     * - `ACTION_UP`: Releases the pressed state and performs a click to switch tabs.
     * - `ACTION_CANCEL`: Releases the pressed state without switching tabs.
     *
     * @param event The motion event containing touch details.
     * @return `true` if the event was handled, `false` otherwise.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var isEventHandled = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val isOppositeSidePressed = isOppositeSidePressed(event.x)
                if (isOppositeSidePressed) {
                    onViewPressed()
                }
                isEventHandled = isOppositeSidePressed
            }

            MotionEvent.ACTION_UP -> {
                onViewReleased()
                performClick()
                isEventHandled = true
            }

            MotionEvent.ACTION_CANCEL -> {
                onViewReleased()
                isEventHandled = true
            }
        }
        return isEventHandled
    }

    /**
     * Determines if the touch occurred on the opposite side of the current slider position.
     * Used to decide whether to initiate a tab switch.
     *
     * @param x The x-coordinate of the touch in pixels.
     * @return `true` if the opposite side was pressed, `false` otherwise.
     */
    private fun isOppositeSidePressed(x: Float): Boolean {
        return when (sliderState) {
            SliderState.IDLE_LEFT -> x > width / TABS_COUNT
            SliderState.IDLE_RIGHT -> x < width / TABS_COUNT
            SliderState.MOVING_RIGHT -> x < width / TABS_COUNT
            SliderState.MOVING_LEFT -> x > width / TABS_COUNT
        }
    }

    /**
     * Sets the view to the pressed state and requests a redraw.
     * Changes the background color to the pressed color.
     */
    private fun onViewPressed() {
        isViewPressed = true
        invalidate()
    }

    /**
     * Clears the pressed state and requests a redraw.
     * Restores the background color to the unpressed color.
     */
    private fun onViewReleased() {
        isViewPressed = false
        invalidate()
    }

    /**
     * Performs a click action to switch the slider state and animate the transition.
     * Notifies the listener of the tab selection and calls the superclass method for accessibility.
     *
     * @return `true` indicating the click was handled.
     */
    override fun performClick(): Boolean {
        super.performClick()
        val newState = resolveNewSliderState()
        changeStateAnimated(newState)
        return true
    }

    /**
     * Determines the new slider state based on the current state when a click occurs.
     *
     * @return The new `SliderState` to transition to (`MOVING_RIGHT` or `MOVING_LEFT`).
     */
    private fun resolveNewSliderState(): SliderState {
        return when (sliderState) {
            SliderState.IDLE_LEFT -> SliderState.MOVING_RIGHT
            SliderState.IDLE_RIGHT -> SliderState.MOVING_LEFT
            SliderState.MOVING_LEFT -> SliderState.MOVING_RIGHT
            SliderState.MOVING_RIGHT -> SliderState.MOVING_LEFT
        }
    }

    /**
     * Initiates an animated transition to the new slider state.
     * Updates the slider position and notifies the listener of the tab selection.
     *
     * @param newState The new state to transition to (`MOVING_RIGHT` or `MOVING_LEFT`).
     * @throws IllegalArgumentException If the new state is not `MOVING_RIGHT` or `MOVING_LEFT`.
     */
    private fun changeStateAnimated(newState: SliderState) {
        when (newState) {
            SliderState.MOVING_RIGHT -> {
                sliderState = newState
                sliderValueAnimator.setFloatValues(sliderXOffset, width / 2.0f)
                notifyRightTabSelected()
            }

            SliderState.MOVING_LEFT -> {
                sliderState = newState
                sliderValueAnimator.setFloatValues(sliderXOffset, 0.0f)
                notifyLeftTabSelected()
            }

            else -> throw IllegalArgumentException(
                "Cannot perform animated transition to the given state. Expected enum value MOVING_RIGHT or MOVING_LEFT, but received $newState"
            )
        }
        sliderValueAnimator.start()
    }

    /**
     * Notifies the tab selection listener that the right tab was selected, if a listener is set.
     */
    private fun notifyRightTabSelected() {
        tabSelectedListener?.onRightTabSelected()
    }

    /**
     * Notifies the tab selection listener that the left tab was selected, if a listener is set.
     */
    private fun notifyLeftTabSelected() {
        tabSelectedListener?.onLeftTabSelected()
    }

    /**
     * Saves the current state of the view for persistence across configuration changes.
     *
     * @return A `Parcelable` object containing the saved state.
     */
    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.sliderState = resolveSliderStateForSave()
        return savedState
    }

    /**
     * Resolves the slider state to save, converting moving states to their idle equivalents.
     *
     * @return The `SliderState` to save (`IDLE_LEFT` or `IDLE_RIGHT`).
     */
    private fun resolveSliderStateForSave(): SliderState {
        return when (sliderState) {
            SliderState.IDLE_LEFT -> SliderState.IDLE_LEFT
            SliderState.IDLE_RIGHT -> SliderState.IDLE_RIGHT
            SliderState.MOVING_RIGHT -> SliderState.IDLE_RIGHT
            SliderState.MOVING_LEFT -> SliderState.IDLE_LEFT
        }
    }

    /**
     * Restores the view's state from a previously saved state.
     *
     * @param state The saved state to restore from, or null if no state is available.
     */
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        sliderState = state.sliderState
    }

    /**
     * Sets a listener to receive callbacks when tabs are selected.
     *
     * @param tabSelectedListener The listener to set for tab selection events.
     */
    fun setOnTabSelectedListener(tabSelectedListener: OnTabSelectedListener) {
        this.tabSelectedListener = tabSelectedListener
    }

    /**
     * Programmatically selects the left tab without animation.
     * Updates the slider state and position, then requests a redraw.
     */
    fun selectLeftTab() {
        sliderState = SliderState.IDLE_LEFT
        sliderXOffset = 0.0f
        invalidate()
    }

    /**
     * Programmatically selects the right tab without animation.
     * Updates the slider state and position, then requests a redraw.
     */
    fun selectRightTab() {
        sliderState = SliderState.IDLE_RIGHT
        sliderXOffset = width / 2.0f
        invalidate()
    }

    /**
     * Internal class for saving and restoring the slider state across configuration changes.
     * Extends `BaseSavedState` to include the slider's state.
     */
    @Suppress("unused")
    internal class SavedState : BaseSavedState {

        /**
         * The slider state to save and restore.
         * Defaults to `IDLE_LEFT` if not set.
         */
        var sliderState: SliderState = SliderState.IDLE_LEFT

        /**
         * Constructor for creating a `SavedState` instance from a parcel.
         *
         * @param source The parcel containing the saved state data.
         */
        constructor(source: Parcel?) : super(source) {
            sliderState = source?.readSerializable() as SliderState
        }

        /**
         * Constructor for creating a `SavedState` instance with a super state.
         *
         * @param superState The parent state to include in the saved state.
         */
        constructor(superState: Parcelable?) : super(superState)

        /**
         * Writes the slider state to a parcel for saving.
         *
         * @param out The parcel to write the state to.
         * @param flags Flags for parceling (typically 0).
         */
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeSerializable(sliderState)
        }

        /**
         * Companion object providing a `Parcelable.Creator` for `SavedState`.
         */
        companion object {

            /**
             * Creator instance for reconstructing `SavedState` objects from parcels.
             */
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {

                /**
                 * Creates a `SavedState` instance from a parcel.
                 *
                 * @param source The parcel containing the saved state data.
                 * @return A new `SavedState` instance.
                 */
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                /**
                 * Creates an array of `SavedState` objects.
                 *
                 * @param size The size of the array to create.
                 * @return An array of `SavedState` objects, initialized to null.
                 */
                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    /**
     * Enum representing the possible states of the slider.
     * Used to track whether the slider is idle or moving, and in which direction.
     */
    enum class SliderState {

        /**
         * The slider is stationary on the left tab.
         */
        IDLE_LEFT,

        /**
         * The slider is stationary on the right tab.
         */
        IDLE_RIGHT,

        /**
         * The slider is moving toward the right tab.
         */
        MOVING_RIGHT,

        /**
         * The slider is moving toward the left tab.
         */
        MOVING_LEFT
    }

    /**
     * Interface for listening to tab selection events.
     * Implement this interface to receive callbacks when the user selects a tab.
     */
    interface OnTabSelectedListener {

        /**
         * Called when the left tab is selected, either by touch or programmatically.
         */
        fun onLeftTabSelected()

        /**
         * Called when the right tab is selected, either by touch or programmatically.
         */
        fun onRightTabSelected()
    }

    /**
     * Companion object containing constant values used throughout the `SliderTabs` class.
     */
    companion object {

        /**
         * Default style attribute value for XML attribute processing.
         * Set to 0 as a placeholder when no specific style is defined.
         */
        private const val DEFAULT_STYLE_ATTRIBUTE = 0

        /**
         * Default style resource value for XML attribute processing.
         * Set to 0 as a placeholder when no specific style resource is defined.
         */
        private const val DEFAULT_STYLE_RESOURCE = 0

        /**
         * Number of tabs in the view.
         * Fixed at 2 (left and right tabs).
         */
        private const val TABS_COUNT = 2

        /**
         * Default background color in hexadecimal format.
         * Represents a light gray color ("#E0E0E0").
         */
        private const val DEFAULT_BG_COLOR_HEX = "#E0E0E0"

        /**
         * Default background color when pressed in hexadecimal format.
         * Represents a darker gray color ("#AEAEAE").
         */
        private const val DEFAULT_BG_PRESSED_COLOR_HEX = "#AEAEAE"

        /**
         * Default slider color in hexadecimal format.
         * Represents white ("#FFFFFF").
         */
        private const val DEFAULT_SLIDER_COLOR_HEX = "#FFFFFF"

        /**
         * Default tab text color in hexadecimal format.
         * Represents black ("#000000").
         */
        private const val DEFAULT_TAB_TEXT_COLOR_HEX = "#000000"

        /**
         * Left position of the background rectangle in pixels.
         * Fixed at 0.0f to align with the view's left edge.
         */
        private const val BACKGROUND_RECT_LEFT = 0.0f

        /**
         * Top position of the background rectangle in pixels.
         * Fixed at 0.0f to align with the view's top edge.
         */
        private const val BACKGROUND_RECT_TOP = 0.0f

        /**
         * Corner radius of the background rectangle in density-independent pixels (dp).
         * Default is 24dp, converted to pixels during initialization.
         */
        private const val BACKGROUND_RECT_RADIUS_DP = 24.0f

        /**
         * Left position of the slider rectangle in pixels.
         * Fixed at 0.0f relative to its offset position.
         */
        private const val SLIDER_RECT_LEFT = 0.0f

        /**
         * Top position of the slider rectangle in pixels.
         * Fixed at 0.0f relative to its inset position.
         */
        private const val SLIDER_RECT_TOP = 0.0f

        /**
         * Inset for the slider rectangle in density-independent pixels (dp).
         * Default is 2dp, converted to pixels during initialization.
         */
        private const val SLIDER_RECT_INSET_DP = 2.0f

        /**
         * Corner radius of the slider rectangle in density-independent pixels (dp).
         * Default is 18dp, converted to pixels during initialization.
         */
        private const val SLIDER_RECT_RADIUS_DP = 18.0f

        /**
         * Text size for the tabs in scale-independent pixels (sp).
         * Default is 14sp, converted to pixels during initialization.
         */
        private const val TAB_TEXT_SIZE_SP = 14.0f

        /**
         * Center x-ratio for the left tab text (0.0 to 1.0).
         * Positions the text at 25% of the view's width.
         */
        private const val LEFT_TAB_TEXT_CX = 0.25f

        /**
         * Center x-ratio for the right tab text (0.0 to 1.0).
         * Positions the text at 75% of the view's width.
         */
        private const val RIGHT_TAB_TEXT_CX = 0.75f

        /**
         * Default duration of the slider animation in milliseconds.
         * Set to 300ms for a smooth transition.
         */
        private const val DEFAULT_ANIMATION_DURATION_MS = 300
    }
}
