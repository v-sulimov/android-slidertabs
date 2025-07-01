package com.vsulimov.slidertabs

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
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
 * A custom Android view that displays two tabs with a sliding indicator. Users can switch between tabs by tapping the opposite side of the current selection. The view features smooth animations for the sliding effect and provides callbacks for tab selection events via [OnTabSelectedListener].
 *
 * This view supports customization through XML attributes (e.g., colors, text, animation duration) or programmatically via public methods. It also handles state persistence across configuration changes.
 *
 * @see OnTabSelectedListener for handling tab selection events.
 * @see SliderState for the possible states of the slider.
 *
 * **Usage Example:**
 * ```kotlin
 * // In an Activity or Fragment
 * val sliderTabs = findViewById<SliderTabs>(R.id.slider_tabs)
 * sliderTabs.setOnTabSelectedListener(object : SliderTabs.OnTabSelectedListener {
 *     override fun onLeftTabSelected() {
 *         Log.d("SliderTabs", "Left tab selected")
 *     }
 *     override fun onRightTabSelected() {
 *         Log.d("SliderTabs", "Right tab selected")
 *     }
 * })
 * // Programmatically select a tab
 * sliderTabs.selectRightTab()
 * ```
 */
class SliderTabs : View {

    /**
     * Paint object used to draw the background rectangle.
     */
    private val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Paint object used to draw the tab text.
     */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Rectangle defining the background area of the view.
     */
    private val backgroundRectF = RectF()

    /**
     * Rectangle defining the sliding indicator's position and size.
     */
    private val sliderRectF = RectF()

    /**
     * Animator controlling the sliding animation of the indicator.
     */
    private val sliderValueAnimator = ValueAnimator()

    /**
     * Background color when the view is not pressed. Defaults to [DEFAULT_BG_COLOR_HEX].
     */
    private var backgroundColor = DEFAULT_BG_COLOR_HEX.toColorInt()

    /**
     * Background color when the view is pressed. Defaults to [DEFAULT_BG_PRESSED_COLOR_HEX].
     */
    private var backgroundColorPressed = DEFAULT_BG_PRESSED_COLOR_HEX.toColorInt()

    /**
     * Color of the sliding indicator. Defaults to [DEFAULT_SLIDER_COLOR_HEX].
     */
    private var sliderColor = DEFAULT_SLIDER_COLOR_HEX.toColorInt()

    /**
     * Text color for the selected tab. Defaults to [DEFAULT_TAB_TEXT_COLOR_HEX].
     */
    private var onTabTextColor = DEFAULT_TAB_TEXT_COLOR_HEX.toColorInt()

    /**
     * Text color for the unselected tab. Defaults to [DEFAULT_TAB_TEXT_COLOR_HEX].
     */
    private var onSurfaceTextColor = DEFAULT_TAB_TEXT_COLOR_HEX.toColorInt()

    /**
     * Text displayed on the left tab. Defaults to a string resource `st_default_left_tab_text`.
     */
    private var leftTabText = context.getString(R.string.st_default_left_tab_text)

    /**
     * Text displayed on the right tab. Defaults to a string resource `st_default_right_tab_text`.
     */
    private var rightTabText = context.getString(R.string.st_default_right_tab_text)

    /**
     * Duration of the sliding animation in milliseconds. Defaults to [DEFAULT_ANIMATION_DURATION_MS].
     */
    private var animationDuration = DEFAULT_ANIMATION_DURATION_MS

    /**
     * Corner radius of the background rectangle in pixels.
     */
    private var backgroundRectRadius = 0.0f

    /**
     * Inset margin for the sliding indicator rectangle in pixels.
     */
    private var sliderRectInset = 0.0f

    /**
     * Corner radius of the sliding indicator rectangle in pixels.
     */
    private var sliderRectRadius = 0.0f

    /**
     * Size of the tab text in pixels.
     */
    private var tabTextSize = 0.0f

    /**
     * Current x-axis offset of the sliding indicator in pixels.
     */
    private var sliderXOffset = 0.0f

    /**
     * Indicates whether the view is currently pressed by the user.
     */
    private var isViewPressed = false

    /**
     * Current state of the slider, determining its position and animation status.
     */
    private var sliderState = SliderState.IDLE_LEFT

    /**
     * Listener for tab selection events. Can be null if no listener is set.
     */
    private var tabSelectedListener: OnTabSelectedListener? = null

    /**
     * Constructs a [SliderTabs] instance programmatically without XML attributes.
     *
     * @param context The [Context] in which the view operates.
     */
    constructor(context: Context) : super(context) {
        performViewInitialization(resources.displayMetrics)
    }

    /**
     * Constructs a [SliderTabs] instance from XML, applying custom attributes if provided.
     *
     * @param context The [Context] in which the view operates.
     * @param attrs Optional [AttributeSet] containing XML attributes for customization.
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        attrs?.let { consumeAttributeSet(context, it) }
        performViewInitialization(resources.displayMetrics)
    }

    /**
     * Parses the XML attribute set to customize view properties such as colors and text.
     *
     * @param context The [Context] used to access resources and themes.
     * @param attrs The [AttributeSet] containing XML attributes.
     */
    private fun consumeAttributeSet(context: Context, attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SliderTabs,
            DEFAULT_STYLE_ATTRIBUTE,
            DEFAULT_STYLE_RESOURCE
        )
        try {
            backgroundColor = typedArray.getColor(
                R.styleable.SliderTabs_st_backgroundColor,
                DEFAULT_BG_COLOR_HEX.toColorInt()
            )
            backgroundColorPressed = typedArray.getColor(
                R.styleable.SliderTabs_st_backgroundColorPressed,
                DEFAULT_BG_PRESSED_COLOR_HEX.toColorInt()
            )
            sliderColor = typedArray.getColor(
                R.styleable.SliderTabs_st_sliderColor,
                DEFAULT_SLIDER_COLOR_HEX.toColorInt()
            )
            onTabTextColor = typedArray.getColor(
                R.styleable.SliderTabs_st_onTabTextColor,
                DEFAULT_TAB_TEXT_COLOR_HEX.toColorInt()
            )
            onSurfaceTextColor = typedArray.getColor(
                R.styleable.SliderTabs_st_onSurfaceTextColor,
                DEFAULT_TAB_TEXT_COLOR_HEX.toColorInt()
            )
            leftTabText = resolveLeftTabText(context, typedArray)
            rightTabText = resolveRightTabText(context, typedArray)
            animationDuration = typedArray.getInt(
                R.styleable.SliderTabs_st_animationDuration,
                DEFAULT_ANIMATION_DURATION_MS
            )
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * Retrieves the left tab text from the attribute set, falling back to a default string resource.
     *
     * @param context The [Context] used to access string resources.
     * @param typedArray The [TypedArray] containing attribute values.
     * @return The text for the left tab.
     */
    private fun resolveLeftTabText(context: Context, typedArray: TypedArray): String {
        val leftTabText = typedArray.getString(R.styleable.SliderTabs_st_leftTabText)
        return leftTabText ?: context.getString(R.string.st_default_left_tab_text)
    }

    /**
     * Retrieves the right tab text from the attribute set, falling back to a default string resource.
     *
     * @param context The [Context] used to access string resources.
     * @param typedArray The [TypedArray] containing attribute values.
     * @return The text for the right tab.
     */
    private fun resolveRightTabText(context: Context, typedArray: TypedArray): String {
        val rightTabText = typedArray.getString(R.styleable.SliderTabs_st_rightTabText)
        return rightTabText ?: context.getString(R.string.st_default_right_tab_text)
    }

    /**
     * Initializes the view by calculating density-dependent values and configuring paints and animators.
     *
     * @param displayMetrics The [DisplayMetrics] of the device for density-aware calculations.
     */
    private fun performViewInitialization(displayMetrics: DisplayMetrics) {
        calculateDensityDependentValues(displayMetrics)
        configureTextPaint()
        configureValueAnimator()
    }

    /**
     * Converts density-independent values (e.g., dp, sp) to pixel values based on device metrics.
     *
     * @param displayMetrics The [DisplayMetrics] of the device.
     */
    private fun calculateDensityDependentValues(displayMetrics: DisplayMetrics) {
        backgroundRectRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            BACKGROUND_RECT_RADIUS_DP,
            displayMetrics
        )
        sliderRectInset = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            SLIDER_RECT_INSET_DP,
            displayMetrics
        )
        sliderRectRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            SLIDER_RECT_RADIUS_DP,
            displayMetrics
        )
        tabTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            TAB_TEXT_SIZE_SP,
            displayMetrics
        )
    }

    /**
     * Sets up the text paint with the initial color and size for drawing tab text.
     */
    private fun configureTextPaint() {
        textPaint.color = onTabTextColor
        textPaint.textSize = tabTextSize
    }

    /**
     * Configures the [sliderValueAnimator] for the sliding animation, including duration and listeners.
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
     * Updates the view's layout when its size changes, recalculating rectangles and slider position.
     *
     * @param width New width of the view in pixels.
     * @param height New height of the view in pixels.
     * @param oldWidth Previous width of the view in pixels.
     * @param oldHeight Previous height of the view in pixels.
     */
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        if (isViewVisible(width, height)) {
            calculateRectangles()
            recalculateSliderXOffset()
        }
    }

    /**
     * Determines if the view is visible based on its dimensions.
     *
     * @param width The current width of the view in pixels.
     * @param height The current height of the view in pixels.
     * @return `true` if the view is visible (width and height > 0), `false` otherwise.
     */
    private fun isViewVisible(width: Int, height: Int): Boolean = width > 0 && height > 0

    /**
     * Recalculates the background and slider rectangles based on the current view size.
     */
    private fun calculateRectangles() {
        calculateBackgroundRectF()
        calculateSliderRectF()
    }

    /**
     * Sets the dimensions of the background rectangle to match the view's size.
     */
    private fun calculateBackgroundRectF() {
        backgroundRectF.left = BACKGROUND_RECT_LEFT
        backgroundRectF.top = BACKGROUND_RECT_TOP
        backgroundRectF.right = width.toFloat()
        backgroundRectF.bottom = height.toFloat()
    }

    /**
     * Calculates the dimensions of the slider rectangle based on its position and inset.
     */
    private fun calculateSliderRectF() {
        sliderRectF.left = calculateSliderRectLeft()
        sliderRectF.top = calculateSliderRectTop()
        sliderRectF.right = calculateSliderRectRight()
        sliderRectF.bottom = calculateSliderRectBottom()
    }

    /**
     * Computes the left edge of the slider rectangle.
     *
     * @return The x-coordinate of the left edge in pixels.
     */
    private fun calculateSliderRectLeft(): Float = SLIDER_RECT_LEFT + sliderRectInset + sliderXOffset

    /**
     * Computes the top edge of the slider rectangle.
     *
     * @return The y-coordinate of the top edge in pixels.
     */
    private fun calculateSliderRectTop(): Float = SLIDER_RECT_TOP + sliderRectInset

    /**
     * Computes the right edge of the slider rectangle.
     *
     * @return The x-coordinate of the right edge in pixels.
     */
    private fun calculateSliderRectRight(): Float = width / TABS_COUNT - sliderRectInset + sliderXOffset

    /**
     * Computes the bottom edge of the slider rectangle.
     *
     * @return The y-coordinate of the bottom edge in pixels.
     */
    private fun calculateSliderRectBottom(): Float = height - sliderRectInset

    /**
     * Adjusts the slider's x-offset based on its current state ([SliderState.IDLE_LEFT] or [SliderState.IDLE_RIGHT]).
     */
    private fun recalculateSliderXOffset() {
        if (sliderState === SliderState.IDLE_LEFT) {
            sliderXOffset = 0.0f
        } else if (sliderState === SliderState.IDLE_RIGHT) {
            sliderXOffset = width / 2.0f
        }
    }

    /**
     * Measures the view to determine its dimensions based on the provided specs.
     *
     * @param widthMeasureSpec The width measurement specification from the parent.
     * @param heightMeasureSpec The height measurement specification from the parent.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = calculateDesiredWidth()
        val desiredHeight = calculateDesiredHeight()
        val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /**
     * Calculates the desired width of the view, including padding.
     *
     * @return The desired width in pixels.
     */
    private fun calculateDesiredWidth(): Int = suggestedMinimumWidth + paddingLeft + paddingRight

    /**
     * Calculates the desired height of the view, including padding.
     *
     * @return The desired height in pixels.
     */
    private fun calculateDesiredHeight(): Int = suggestedMinimumHeight + paddingTop + paddingBottom

    /**
     * Renders the view by drawing the background, slider, and text on the canvas.
     *
     * @param canvas The [Canvas] object to draw on.
     */
    override fun onDraw(canvas: Canvas) {
        drawBackground(canvas)
        drawSlider(canvas)
        drawText(canvas)
    }

    /**
     * Draws the background rectangle with the appropriate color based on press state.
     *
     * @param canvas The [Canvas] to draw on.
     */
    private fun drawBackground(canvas: Canvas) {
        val backgroundColor = resolveBackgroundColor()
        rectPaint.color = backgroundColor
        canvas.drawRoundRect(backgroundRectF, backgroundRectRadius, backgroundRectRadius, rectPaint)
    }

    /**
     * Determines the background color based on whether the view is pressed.
     *
     * @return The color to use for the background.
     */
    private fun resolveBackgroundColor(): Int = if (isViewPressed) {
        backgroundColorPressed
    } else {
        backgroundColor
    }

    /**
     * Draws the sliding indicator rectangle.
     *
     * @param canvas The [Canvas] to draw on.
     */
    private fun drawSlider(canvas: Canvas) {
        rectPaint.color = sliderColor
        recalculateSliderRectSides()
        canvas.drawRoundRect(sliderRectF, sliderRectRadius, sliderRectRadius, rectPaint)
    }

    /**
     * Updates the left and right edges of the slider rectangle before drawing.
     */
    private fun recalculateSliderRectSides() {
        sliderRectF.left = calculateSliderRectLeft()
        sliderRectF.right = calculateSliderRectRight()
    }

    /**
     * Draws the text for both tabs, applying different colors based on selection.
     *
     * @param canvas The [Canvas] to draw on.
     */
    private fun drawText(canvas: Canvas) {
        drawLeftTabText(canvas)
        drawRightTabText(canvas)
    }

    /**
     * Renders the left tab's text, with color changes based on the slider's position.
     *
     * @param canvas The [Canvas] to draw on.
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
     * Renders the right tab's text, with color changes based on the slider's position.
     *
     * @param canvas The [Canvas] to draw on.
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
     * Calculates the x-coordinate to center the tab text horizontally.
     *
     * @param textCX The center x-position as a fraction of the view's width (e.g., 0.25 for left tab).
     * @param textWidth The width of the text in pixels.
     * @return The x-coordinate for text placement in pixels.
     */
    private fun calculateTabTextX(textCX: Float, textWidth: Float): Float = width * textCX - textWidth / 2.0f

    /**
     * Calculates the y-coordinate to center the tab text vertically.
     *
     * @return The y-coordinate for text placement in pixels.
     */
    private fun calculateTabTextY(): Float = height / 2 - (textPaint.descent() + textPaint.ascent()) / 2

    /**
     * Handles touch events to detect tab switches and update the view state.
     *
     * @param event The [MotionEvent] containing touch details.
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
     * Checks if the touch occurred on the opposite side of the current tab, triggering a switch.
     *
     * @param x The x-coordinate of the touch event in pixels.
     * @return `true` if the opposite side was pressed, `false` otherwise.
     */
    private fun isOppositeSidePressed(x: Float): Boolean = when (sliderState) {
        SliderState.IDLE_LEFT -> x > width / TABS_COUNT
        SliderState.IDLE_RIGHT -> x < width / TABS_COUNT
        SliderState.MOVING_RIGHT -> x < width / TABS_COUNT
        SliderState.MOVING_LEFT -> x > width / TABS_COUNT
    }

    /**
     * Updates the view's pressed state and triggers a redraw.
     */
    private fun onViewPressed() {
        isViewPressed = true
        invalidate()
    }

    /**
     * Clears the view's pressed state and triggers a redraw.
     */
    private fun onViewReleased() {
        isViewPressed = false
        invalidate()
    }

    /**
     * Performs a click action by switching the tab and notifying the listener.
     *
     * @return `true` to indicate the click was handled.
     */
    override fun performClick(): Boolean {
        super.performClick()
        val newState = resolveNewSliderState()
        changeStateAnimated(newState)
        return true
    }

    /**
     * Determines the new slider state based on the current state for a tab switch.
     *
     * @return The new [SliderState] to transition to.
     */
    private fun resolveNewSliderState(): SliderState = when (sliderState) {
        SliderState.IDLE_LEFT -> SliderState.MOVING_RIGHT
        SliderState.IDLE_RIGHT -> SliderState.MOVING_LEFT
        SliderState.MOVING_LEFT -> SliderState.MOVING_RIGHT
        SliderState.MOVING_RIGHT -> SliderState.MOVING_LEFT
    }

    /**
     * Animates the slider to a new state, updating its position and notifying listeners.
     *
     * @param newState The [SliderState] to animate to (must be [SliderState.MOVING_RIGHT] or [SliderState.MOVING_LEFT]).
     * @throws IllegalArgumentException If [newState] is not [SliderState.MOVING_RIGHT] or [SliderState.MOVING_LEFT].
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
     * Invokes the [OnTabSelectedListener.onRightTabSelected] callback if a listener is set.
     */
    private fun notifyRightTabSelected() {
        tabSelectedListener?.onRightTabSelected()
    }

    /**
     * Invokes the [OnTabSelectedListener.onLeftTabSelected] callback if a listener is set.
     */
    private fun notifyLeftTabSelected() {
        tabSelectedListener?.onLeftTabSelected()
    }

    /**
     * Saves the view's state for restoration after configuration changes.
     *
     * @return A [Parcelable] containing the saved state.
     */
    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.sliderState = resolveSliderStateForSave()
        return savedState
    }

    /**
     * Maps the current [sliderState] to a stable state for saving.
     *
     * @return The [SliderState] to save ([SliderState.IDLE_LEFT] or [SliderState.IDLE_RIGHT]).
     */
    private fun resolveSliderStateForSave(): SliderState = when (sliderState) {
        SliderState.IDLE_LEFT -> SliderState.IDLE_LEFT
        SliderState.IDLE_RIGHT -> SliderState.IDLE_RIGHT
        SliderState.MOVING_RIGHT -> SliderState.IDLE_RIGHT
        SliderState.MOVING_LEFT -> SliderState.IDLE_LEFT
    }

    /**
     * Restores the view's state from a saved instance.
     *
     * @param state The [Parcelable] containing the saved state, or null.
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
     * Sets a listener to receive tab selection events.
     *
     * @param tabSelectedListener The [OnTabSelectedListener] to set.
     */
    fun setOnTabSelectedListener(tabSelectedListener: OnTabSelectedListener) {
        this.tabSelectedListener = tabSelectedListener
    }

    /**
     * Selects the left tab immediately without animation and redraws the view.
     */
    fun selectLeftTab() {
        sliderState = SliderState.IDLE_LEFT
        sliderXOffset = 0.0f
        invalidate()
    }

    /**
     * Selects the right tab immediately without animation and redraws the view.
     */
    fun selectRightTab() {
        sliderState = SliderState.IDLE_RIGHT
        sliderXOffset = width / 2.0f
        invalidate()
    }

    /**
     * Internal class for saving and restoring the view's state across configuration changes.
     */
    @Suppress("unused")
    internal class SavedState : BaseSavedState {

        /**
         * The saved state of the slider.
         */
        var sliderState: SliderState = SliderState.IDLE_LEFT

        /**
         * Creates a [SavedState] instance from a [Parcel].
         *
         * @param source The [Parcel] containing the saved state data.
         */
        constructor(source: Parcel?) : super(source) {
            sliderState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                source?.readSerializable(SliderState::class.java.classLoader, SliderState::class.java)
                    ?: SliderState.IDLE_LEFT
            } else {
                @Suppress("DEPRECATION") // Suppress for older APIs
                source?.readSerializable() as? SliderState ?: SliderState.IDLE_LEFT
            }
        }

        /**
         * Creates a [SavedState] instance with the superclass state.
         *
         * @param superState The [Parcelable] state from the superclass.
         */
        constructor(superState: Parcelable?) : super(superState)

        /**
         * Writes the state to a [Parcel] for saving.
         *
         * @param out The [Parcel] to write to.
         * @param flags Flags for parceling.
         */
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeSerializable(sliderState)
        }

        companion object {

            /**
             * Creator for restoring [SavedState] instances from a [Parcel].
             */
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {

                override fun createFromParcel(source: Parcel) = SavedState(source)

                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }

    /**
     * Enum representing the possible states of the sliding indicator.
     */
    enum class SliderState {
        /** Slider is stationary on the left tab. */
        IDLE_LEFT,

        /** Slider is stationary on the right tab. */
        IDLE_RIGHT,

        /** Slider is animating towards the right tab. */
        MOVING_RIGHT,

        /** Slider is animating towards the left tab. */
        MOVING_LEFT
    }

    /**
     * Interface for receiving tab selection events.
     */
    interface OnTabSelectedListener {
        /** Called when the left tab is selected. */
        fun onLeftTabSelected()

        /** Called when the right tab is selected. */
        fun onRightTabSelected()
    }

    /**
     * Contains constant values used by [SliderTabs].
     */
    companion object {
        /** Default style attribute for attribute resolution. */
        private const val DEFAULT_STYLE_ATTRIBUTE = 0

        /** Default style resource for attribute resolution. */
        private const val DEFAULT_STYLE_RESOURCE = 0

        /** Number of tabs (fixed at 2). */
        private const val TABS_COUNT = 2

        /** Default background color in hex format (#E0E0E0). */
        private const val DEFAULT_BG_COLOR_HEX = "#E0E0E0"

        /** Default pressed background color in hex format (#AEAEAE). */
        private const val DEFAULT_BG_PRESSED_COLOR_HEX = "#AEAEAE"

        /** Default slider color in hex format (#FFFFFF). */
        private const val DEFAULT_SLIDER_COLOR_HEX = "#FFFFFF"

        /** Default text color for tabs in hex format (#000000). */
        private const val DEFAULT_TAB_TEXT_COLOR_HEX = "#000000"

        /** Left edge of the background rectangle (pixels). */
        private const val BACKGROUND_RECT_LEFT = 0.0f

        /** Top edge of the background rectangle (pixels). */
        private const val BACKGROUND_RECT_TOP = 0.0f

        /** Background rectangle corner radius in dp. */
        private const val BACKGROUND_RECT_RADIUS_DP = 24.0f

        /** Left edge of the slider rectangle (pixels). */
        private const val SLIDER_RECT_LEFT = 0.0f

        /** Top edge of the slider rectangle (pixels). */
        private const val SLIDER_RECT_TOP = 0.0f

        /** Slider rectangle inset in dp. */
        private const val SLIDER_RECT_INSET_DP = 2.0f

        /** Slider rectangle corner radius in dp. */
        private const val SLIDER_RECT_RADIUS_DP = 18.0f

        /** Tab text size in sp. */
        private const val TAB_TEXT_SIZE_SP = 14.0f

        /** Center x-position of the left tab text (fraction of width). */
        private const val LEFT_TAB_TEXT_CX = 0.25f

        /** Center x-position of the right tab text (fraction of width). */
        private const val RIGHT_TAB_TEXT_CX = 0.75f

        /** Default animation duration in milliseconds. */
        private const val DEFAULT_ANIMATION_DURATION_MS = 300
    }
}
