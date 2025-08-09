/*
 * MIT License
 *
 * Copyright (c) 2017 GwonHyeok
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.ghyeok.stickyswitch.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import io.ghyeok.stickyswitch.R

/**
 * Created by ghyeok on 2017. 3. 13..
 *
 * This class implements a beautiful switch widget for android
 *
 * @author GwonHyeok
 *
 * Fixed 29.08.2023
 */
class StickySwitch : View {

    @Suppress("unused", "PrivatePropertyName")
    private val TAG = "StickySwitch"

    enum class AnimationType {
        LINE,
        CURVED
    }

    enum class TextVisibility {
        VISIBLE,
        INVISIBLE,
        GONE
    }

    // left, right icon drawable
    var leftIcon: Drawable? = null
        set(drawable) {
            field = drawable
            invalidate()
        }
    var rightIcon: Drawable? = null
        set(drawable) {
            field = drawable
            invalidate()
        }

    // icon drawable
    var icon: GradientDrawable? = null
        set(drawable) {
            field = drawable
            invalidate()
        }

    // icon variables
    var iconSize = 100
        set(value) {
            field = value
            invalidate()
        }
    var iconPadding = 70
        set(value) {
            field = value
            invalidate()
        }

    // text variables
    var leftText = ""
        set(value) {
            field = value
            invalidate()
        }
    var rightText = ""
        set(value) {
            field = value
            invalidate()
        }

    // colors
    var sliderBackgroundColor = 0XFF181821.toInt()
        set(@ColorInt colorInt) {
            field = colorInt
            invalidate()
        }

    var switchColor = 0xFF2371FA.toInt()
        set(@ColorInt colorInt) {
            field = colorInt
            invalidate()
        }

    var switchCheckedColor: IntArray = intArrayOf(0xFF2371FA.toInt(), 0xFF2371FA.toInt(), 0xFF2371FA.toInt())
        set(colors) {
            if (colors.size == 3) {
                field = colors
                invalidate()
            } else {
                throw IllegalArgumentException("switchCheckedColor array must contain exactly 3 colors")
            }
        }

    var switchUncheckedColor: IntArray = intArrayOf(0xFF2371FA.toInt(), 0xFF2371FA.toInt(), 0xFF2371FA.toInt())
        set(colors) {
            if (colors.size == 3) {
                field = colors
                invalidate()
            } else {
                throw IllegalArgumentException("switchUncheckedColor array must contain exactly 3 colors")
            }
        }

    var textColor = 0xFFFFFFFF.toInt()
        set(@ColorInt colorInt) {
            field = colorInt
            invalidate()
        }

    var selectedTextColor = 0xFFFFFFFF.toInt()
        set(@ColorInt colorInt) {
            field = colorInt
            invalidate()
        }

    // typeface
    var typeFace: Typeface = Typeface.DEFAULT
        set(typeFace) {
            field = typeFace
            leftTextPaint.typeface = typeFace
            rightTextPaint.typeface = typeFace
            invalidate()
        }

    // rounded rect
    private val sliderBackgroundPaint = Paint()
    private val sliderBackgroundRect = RectF()

    // circular switch
    private val switchBackgroundPaint = Paint()

    // left, right text paint and size
    private val leftTextPaint = Paint()
    private val leftTextRect = Rect()
    private val rightTextPaint = Paint()
    private val rightTextRect = Rect()

    private var textWidth = -1

    // left text size
    private var leftTextSize = 50f
        set(value) {
            field = value
            invalidate()
        }

    // right text size
    private var rightTextSize = 50f
        set(value) {
            field = value
            invalidate()
        }

    // text max,min transparency
    private val textAlphaMax = 255
    private val textAlphaMin = 163

    // text color transparency
    private var leftTextAlpha = textAlphaMax
        set(value) {
            field = value
            invalidate()
        }
    private var rightTextAlpha = textAlphaMin
        set(value) {
            field = value
            invalidate()
        }

    // text size
    private var textSize = 50
        set(value) {
            field = value
            invalidate()
        }

    // text padding
    var textPadding = 0
        set(value) {
            field = value
            invalidate()
        }

    // text size when selected status
    private var selectedTextSize = 50
        set(value) {
            field = value
            invalidate()
        }

    // switch Status
    // false : left status
    // true  : right status
    private var isSwitchOn = false
        set(value) {
            field = value
            invalidate()
        }

    // percent of switch animation
    // animatePercent will be 0.0 ~ 1.0
    private var animatePercent: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    // circular switch bounce rate
    // animateBounceRate will be 1.1 ~ 0.0
    private var animateBounceRate: Double = 1.0
        set(value) {
            field = value
            invalidate()
        }

    // type of transition animation between states
    var animationType = AnimationType.LINE
        set(value) {
            field = value
            invalidate()
        }

    // listener
    var onSelectedChangeListener: OnSelectedChangeListener? = null

    // AnimatorSet, Animation Options
    var animatorSet: AnimatorSet? = null
    var animationDuration: Long = 600

    // state of text visibility
    var textVisibility = TextVisibility.VISIBLE
        set(value) {
            field = value
            invalidate()
        }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs, defStyleAttr)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs, defStyleAttr, defStyleRes)
    }

    init {
        isClickable = true
    }

    fun setA(onSelectedChangeListener: OnSelectedChangeListener) {
        this.onSelectedChangeListener = onSelectedChangeListener
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int = 0, defStyleRes: Int = 0) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.StickySwitch,
            defStyleAttr,
            defStyleRes
        )

        // left switch icon
        leftIcon = typedArray.getDrawable(R.styleable.StickySwitch_ss_leftIcon)
        leftText = typedArray.getString(R.styleable.StickySwitch_ss_leftText) ?: leftText

        // right switch icon
        rightIcon = typedArray.getDrawable(R.styleable.StickySwitch_ss_rightIcon)
        rightText = typedArray.getString(R.styleable.StickySwitch_ss_rightText) ?: rightText

        // icon drawable
        icon = typedArray.getDrawable(R.styleable.StickySwitch_ss_icon) as? GradientDrawable

        // icon size
        iconSize = typedArray.getDimensionPixelSize(R.styleable.StickySwitch_ss_iconSize, iconSize)
        iconPadding = typedArray.getDimensionPixelSize(R.styleable.StickySwitch_ss_iconPadding, iconPadding)

        // saved text size
        textSize = typedArray.getDimensionPixelSize(R.styleable.StickySwitch_ss_textSize, textSize)

        // text padding
        textPadding = typedArray.getDimensionPixelSize(
            R.styleable.StickySwitch_ss_textPadding,
            textPadding
        )

        selectedTextSize = typedArray.getDimensionPixelSize(
            R.styleable.StickySwitch_ss_selectedTextSize,
            selectedTextSize
        )

        // current text size
        leftTextSize = selectedTextSize.toFloat()
        rightTextSize = textSize.toFloat()

        // slider background color
        sliderBackgroundColor = typedArray.getColor(
            R.styleable.StickySwitch_ss_sliderBackgroundColor,
            sliderBackgroundColor
        )

        // switch color
        switchColor = typedArray.getColor(R.styleable.StickySwitch_ss_switchColor, switchColor)

        // switch 3 checked colors
        val checkedColorArrayResId = typedArray.getResourceId(R.styleable.StickySwitch_ss_switchCheckedColor, 0)
        if (checkedColorArrayResId != 0) {
            switchCheckedColor = context.resources.getIntArray(checkedColorArrayResId)
        }

        // switch 3 unchecked colors
        val uncheckedColorArrayResId = typedArray.getResourceId(R.styleable.StickySwitch_ss_switchUncheckedColor, 0)
        if (uncheckedColorArrayResId != 0) {
            switchUncheckedColor = context.resources.getIntArray(uncheckedColorArrayResId)
        }

        // text color
        textColor = typedArray.getColor(R.styleable.StickySwitch_ss_textColor, textColor)

        // selected text color
        selectedTextColor = typedArray.getColor(R.styleable.StickySwitch_ss_selectedTextColor, selectedTextColor)

        // animation duration
        animationDuration = typedArray.getInt(
            R.styleable.StickySwitch_ss_animationDuration,
            animationDuration.toInt()
        ).toLong()

        //animation type
        animationType = AnimationType.values()[typedArray.getInt(
            R.styleable.StickySwitch_ss_animationType,
            AnimationType.LINE.ordinal
        )]

        // text visibility
        textVisibility = TextVisibility.values()[typedArray.getInt(
            R.styleable.StickySwitch_ss_textVisibility,
            TextVisibility.VISIBLE.ordinal
        )]

        typedArray.recycle()

    }

    //used to draw connection between two circle
    private val connectionPath = Path()
    val xParam = 1 / 2f //Math.sin(Math.PI / 6).toFloat()
    val yParam = 0.86602540378f //Math.cos(Math.PI / 6).toFloat()

    /**
     * Draw Sticky Switch View
     *
     * Animation
     *
     * 0% ~ 50%
     * radius : circle radius -> circle radius / 2
     * x      : x -> x + widthSpace
     * y      : y
     *
     * 50% ~ 100%
     * radius : circle radius / 2 -> circle radius
     * x      : x + widthSpace -> x + widthSpace
     * y      : y
     *
     * @param canvas the canvas on which the background will be drawn
     */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // icon margin
        val iconMarginLeft = iconPadding
        val iconMarginBottom = iconPadding
        val iconMarginRight = iconPadding
        val iconMarginTop = iconPadding

        // icon width, height
        val iconWidth = iconSize
        val iconHeight = iconSize

        // circle Radius
        val sliderRadius = iconMarginTop + iconHeight / 2f
        val circleRadius = iconMarginTop + iconHeight / 2f

        // draw circular rect
        sliderBackgroundPaint.color = sliderBackgroundColor
        sliderBackgroundRect.set(
            textPadding.toFloat(),
            0f,
            measuredWidth.toFloat() - textPadding.toFloat(),
            (iconMarginTop + iconHeight + iconMarginBottom).toFloat()
        )

        canvas?.drawRoundRect(
            sliderBackgroundRect,
            sliderRadius,
            sliderRadius,
            sliderBackgroundPaint
        )

        // switch background
        switchBackgroundPaint.color = switchColor

        canvas?.save()

        // if animation is before half
        val isBeforeHalf = animatePercent in 0.0..0.5

        // width at which objects move in animation
        val widthSpace = measuredWidth - circleRadius * 2 - textPadding * 2

        // original circular switch x, y, radius
        val ocX = (circleRadius + widthSpace * Math.min(1.0, animatePercent * 2)) + textPadding
        val ocY = circleRadius
        val ocRadius = circleRadius * (if (isBeforeHalf) 1.0 - animatePercent else animatePercent)

        // copied circular switch x, y, radius
        val ccX = (circleRadius + widthSpace * (if (isBeforeHalf) 0.0 else Math.abs(0.5 - animatePercent) * 2)) + textPadding
        val ccY = circleRadius
        val ccRadius = circleRadius * (if (isBeforeHalf) 1.0 - animatePercent else animatePercent)

        // circular rectangle
        val rectL = ccX
        val rectR = ocX

        if (icon == null) {

            canvas?.drawCircle(
                ocX.toFloat(),
                ocY,
                evaluateBounceRate(ocRadius).toFloat(),
                switchBackgroundPaint
            )

            canvas?.drawCircle(
                ccX.toFloat(),
                ccY,
                evaluateBounceRate(ccRadius).toFloat(),
                switchBackgroundPaint
            )

            if (animationType == AnimationType.LINE) {

                val rectT = circleRadius - circleRadius / 2
                val rectB = circleRadius + circleRadius / 2

                canvas?.drawCircle(
                    ccX.toFloat(),
                    ccY,
                    evaluateBounceRate(ccRadius).toFloat(),
                    switchBackgroundPaint
                )

                canvas?.drawRect(
                    rectL.toFloat(),
                    rectT,
                    rectR.toFloat(),
                    rectB,
                    switchBackgroundPaint
                )

            } else if (animationType == AnimationType.CURVED) {

                // curved connection between two circles
                if (animatePercent > 0 && animatePercent < 1) {

                    connectionPath.rewind()

                    //puts points of rectangle on circle, on point  π/6 rad
                    val rectLCurve = rectL.toFloat() + ccRadius.toFloat() * xParam
                    val rectRCurve = rectR.toFloat() - ccRadius.toFloat() * xParam

                    val rectTCurve = circleRadius - ccRadius.toFloat() * yParam
                    val rectBCurve = circleRadius + ccRadius.toFloat() * yParam

                    //middle points through which goes cubic interpolation
                    val middlePointX = (rectRCurve + rectLCurve) / 2
                    val middlePointY = (rectTCurve + rectBCurve) / 2

                    // draws 'rectangle', but in the way that top line is concave, and bottom is convex
                    connectionPath.moveTo(rectLCurve, rectTCurve)

                    connectionPath.cubicTo(
                        rectLCurve,
                        rectTCurve,
                        middlePointX,
                        middlePointY,
                        rectRCurve,
                        rectTCurve
                    )

                    connectionPath.lineTo(
                        rectRCurve,
                        rectBCurve
                    )

                    connectionPath.cubicTo(
                        rectRCurve,
                        rectBCurve,
                        middlePointX,
                        middlePointY,
                        rectLCurve,
                        rectBCurve
                    )
                    connectionPath.close()

                    canvas?.drawPath(connectionPath, switchBackgroundPaint)
                }
            }

        } else {

            icon?.run {
                canvas?.let {
                    it.save()

                    val _ocRadius = evaluateBounceRate(ocRadius).toFloat()
                    val _ocLeft = (ocX.toFloat() - _ocRadius).toInt()
                    val _ocTop = (ocY - _ocRadius).toInt()

                    setBounds(
                        _ocLeft,
                        _ocTop,
                        _ocLeft + (_ocRadius * 2).toInt(),
                        _ocTop + (_ocRadius * 2).toInt()
                    )

                    val _ccRadius = evaluateBounceRate(ccRadius).toFloat()
                    val _ccLeft = (ccX.toFloat() - _ccRadius).toInt()
                    val _ccTop = (ccY - _ccRadius).toInt()

                    setBounds(
                        _ccLeft,
                        _ccTop,
                        _ccLeft + (_ccRadius * 2).toInt(),
                        _ccTop + (_ccRadius * 2).toInt()
                    )

                    colors = evaluateColorRate()

                    draw(it)
                    it.restore()
                }
            }

        }

        canvas?.restore()

        // draw left icon
        leftIcon?.run {
            canvas?.let {
                it.save()
                setBounds(
                    iconMarginLeft + textPadding,
                    iconMarginTop,
                    iconMarginLeft + textPadding + iconWidth,
                    iconMarginTop + iconHeight
                )
                alpha = if (isSwitchOn) 153 else 255
                draw(it)
                it.restore()
            }
        }

        // draw right icon
        rightIcon?.run {
            canvas?.let {
                it.save()
                setBounds(
                    measuredWidth - iconWidth - iconMarginRight - textPadding,
                    iconMarginTop,
                    measuredWidth - iconMarginRight - textPadding,
                    iconMarginTop + iconHeight
                )
                alpha = if (!isSwitchOn) 153 else 255
                draw(it)
                it.restore()
            }
        }

        // bottom space
        val bottomSpaceHeight = measuredHeight - (circleRadius * 2) - textPadding

        // set text paint
        leftTextPaint.color = textColor
        leftTextPaint.alpha = leftTextAlpha
        rightTextPaint.color = if (isSwitchOn) selectedTextColor else textColor
        rightTextPaint.alpha = rightTextAlpha

        // set text antialias paint
        leftTextPaint.isAntiAlias = true
        rightTextPaint.isAntiAlias = true

        // set text size
        leftTextPaint.textSize = leftTextSize
        rightTextPaint.textSize = rightTextSize

        // draw text when isShowText is true
        if (textVisibility == TextVisibility.VISIBLE) {

            // measure text size
            measureText()

            // left text position
            val leftTextX = (circleRadius * 2 + textPadding * 2 - leftTextRect.width()) * 0.5
            val leftTextY = (circleRadius * 2) + (bottomSpaceHeight * 0.5) + textPadding

            // draw left text
            canvas?.save()
            canvas?.drawText(leftText, leftTextX.toFloat(), leftTextY.toFloat(), leftTextPaint)
            canvas?.restore()

            // right text position
            val rightTextX = ((circleRadius * 2 + textPadding * 2 - rightTextRect.width()) * 0.5) + (measuredWidth - (circleRadius * 2) - (textPadding * 2))
            val rightTextY = (circleRadius * 2) + (bottomSpaceHeight * 0.5) + textPadding

            // draw right text
            canvas?.save()
            canvas?.drawText(rightText, rightTextX.toFloat(), rightTextY.toFloat(), rightTextPaint)
            canvas?.restore()
        }

    }

    private fun evaluateBounceRate(value: Double): Double = value * animateBounceRate

    private fun evaluateColorRate(): IntArray {
        val evaluator = ArgbEvaluator()
        val interpolatedColors = IntArray(switchUncheckedColor.size)
        for (i in switchUncheckedColor.indices) {
            interpolatedColors[i] = evaluator.evaluate(animatePercent.toFloat(), switchUncheckedColor[i], switchCheckedColor[i]) as Int
        }
        return interpolatedColors
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isEnabled.not() || isClickable.not()) return false

        when (event?.action) {
            ACTION_UP -> {
                isSwitchOn = isSwitchOn.not()
                animateCheckState(isSwitchOn)
                notifySelectedChange()
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // measure text size
        measureText()

        val diameter = (iconPadding + iconSize / 2) * 2

        if (textWidth == -1) {
            textWidth = leftTextRect.width() + rightTextRect.width()
        }

        val maxTextHeight = Math.max(selectedTextSize, textSize)
        val measuredTextHeight = if (textVisibility == TextVisibility.GONE) 0 else maxTextHeight * 2 + textPadding

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightSize = 0

        when (heightMode) {
            MeasureSpec.UNSPECIFIED -> heightSize = heightMeasureSpec
            MeasureSpec.AT_MOST -> heightSize = diameter + measuredTextHeight
            MeasureSpec.EXACTLY -> heightSize = MeasureSpec.getSize(heightMeasureSpec)
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var widthSize = 0

        when (widthMode) {
            MeasureSpec.UNSPECIFIED -> widthSize = widthMeasureSpec
            MeasureSpec.AT_MOST -> widthSize = diameter * 2 + textWidth + textPadding * 2
            MeasureSpec.EXACTLY -> widthSize = MeasureSpec.getSize(widthMeasureSpec)
        }

        setMeasuredDimension(widthSize, heightSize)
    }

    @JvmOverloads
    fun setDirection(
        direction: Direction,
        isAnimate: Boolean = true,
        shouldTriggerSelected: Boolean = true
    ) {
        val newSwitchState = when (direction) {
            Direction.LEFT -> false
            Direction.RIGHT -> true
        }

        if (newSwitchState != isSwitchOn) {

            isSwitchOn = newSwitchState

            // cancel animation when if animate is running
            animatorSet?.cancel()

            // when isAnimate is false not showing liquid animation
            if (isAnimate) animateCheckState(isSwitchOn) else changeCheckState(isSwitchOn)
            if (shouldTriggerSelected) {
                notifySelectedChange()
            }

        }
    }

    fun getDirection(): Direction = when (isSwitchOn) {
        true -> Direction.RIGHT
        false -> Direction.LEFT
    }

    @JvmOverloads
    fun getText(direction: Direction = getDirection()): String = when (direction) {
        Direction.LEFT -> leftText
        Direction.RIGHT -> rightText
    }

    fun setLeftIcon(@DrawableRes resourceId: Int) {
        this.leftIcon = this.getDrawable(resourceId)

    }

    fun setRightIcon(@DrawableRes resourceId: Int) {
        this.rightIcon = this.getDrawable(resourceId)
    }

    private fun getDrawable(@DrawableRes resourceId: Int) =
        AppCompatResources.getDrawable(context, resourceId)

    private fun notifySelectedChange() {
        onSelectedChangeListener?.onSelectedChange(
            if (isSwitchOn) Direction.RIGHT else Direction.LEFT,
            getText()
        )
    }

    private fun measureText() {
        leftTextPaint.getTextBounds(leftText, 0, leftText.length, leftTextRect)
        rightTextPaint.getTextBounds(rightText, 0, rightText.length, rightTextRect)
    }

    private fun animateCheckState(newCheckedState: Boolean) {
        this.animatorSet = AnimatorSet()
        if (animatorSet != null) {
            animatorSet?.playTogether(
                getLiquidAnimator(newCheckedState),
                leftTextSizeAnimator(newCheckedState),
                rightTextSizeAnimator(newCheckedState),
                leftTextAlphaAnimator(newCheckedState),
                rightTextAlphaAnimator(newCheckedState),
                getBounceAnimator()
            )
            animatorSet?.start()
        }
    }

    private fun changeCheckState(newCheckedState: Boolean) {
        // Change TextAlpha Without Animation
        leftTextAlpha = if (newCheckedState) textAlphaMin else textAlphaMax
        rightTextAlpha = if (newCheckedState) textAlphaMax else textAlphaMin

        // Change TextSize without animation
        leftTextSize = if (newCheckedState) textSize.toFloat() else selectedTextSize.toFloat()
        rightTextSize = if (newCheckedState) selectedTextSize.toFloat() else textSize.toFloat()

        // Change Animate Percent(LiquidAnimation) without animation
        animatePercent = if (newCheckedState) 1.0 else 0.0
        animateBounceRate = 1.0
    }

    private fun leftTextAlphaAnimator(newCheckedState: Boolean): Animator {
        val toAlpha = if (newCheckedState) textAlphaMin else textAlphaMax
        val animator = ValueAnimator.ofInt(leftTextAlpha, toAlpha)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.startDelay = animationDuration / 3
        animator.duration = animationDuration - (animationDuration / 3)
        animator.addUpdateListener { leftTextAlpha = (it.animatedValue as Int) }
        return animator
    }

    private fun rightTextAlphaAnimator(newCheckedState: Boolean): Animator {
        val toAlpha = if (newCheckedState) textAlphaMax else textAlphaMin
        val animator = ValueAnimator.ofInt(rightTextAlpha, toAlpha)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.startDelay = animationDuration / 3
        animator.duration = animationDuration - (animationDuration / 3)
        animator.addUpdateListener { rightTextAlpha = (it.animatedValue as Int) }
        return animator
    }

    private fun leftTextSizeAnimator(newCheckedState: Boolean): Animator {
        val toTextSize = if (newCheckedState) textSize else selectedTextSize
        val textSizeAnimator = ValueAnimator.ofFloat(leftTextSize, toTextSize.toFloat())
        textSizeAnimator.interpolator = AccelerateDecelerateInterpolator()
        textSizeAnimator.startDelay = animationDuration / 3
        textSizeAnimator.duration = animationDuration - (animationDuration / 3)
        textSizeAnimator.addUpdateListener { leftTextSize = (it.animatedValue as Float) }
        return textSizeAnimator
    }

    private fun rightTextSizeAnimator(newCheckedState: Boolean): Animator {
        val toTextSize = if (newCheckedState) selectedTextSize else textSize
        val textSizeAnimator = ValueAnimator.ofFloat(rightTextSize, toTextSize.toFloat())
        textSizeAnimator.interpolator = AccelerateDecelerateInterpolator()
        textSizeAnimator.startDelay = animationDuration / 3
        textSizeAnimator.duration = animationDuration - (animationDuration / 3)
        textSizeAnimator.addUpdateListener { rightTextSize = (it.animatedValue as Float) }
        return textSizeAnimator
    }

    private fun getLiquidAnimator(newCheckedState: Boolean): Animator {
        val liquidAnimator = ValueAnimator.ofFloat(animatePercent.toFloat(), if (newCheckedState) 1f else 0f)
        liquidAnimator.duration = animationDuration
        liquidAnimator.interpolator = AccelerateInterpolator()
        liquidAnimator.addUpdateListener { animatePercent = (it.animatedValue as Float).toDouble() }
        return liquidAnimator
    }

    private fun getBounceAnimator(): Animator {
        val animator = ValueAnimator.ofFloat(1f, 0.9f, 1f)
        animator.duration = (animationDuration * 0.41).toLong()
        animator.startDelay = animationDuration
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animateBounceRate = (it.animatedValue as Float).toDouble() }
        return animator
    }

    enum class Direction {
        LEFT, RIGHT
    }

    interface OnSelectedChangeListener {
        fun onSelectedChange(direction: Direction, text: String)
    }
}
