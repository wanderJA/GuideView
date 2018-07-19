package com.wander.guideview

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.support.annotation.DrawableRes
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * author wangdou
 * date 2018/7/19
 *
 */
class GuideView(var mContext: Context) : FrameLayout(mContext) {
    private val tag = javaClass.simpleName
    private var circlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var backGroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var guideBackground = Color.parseColor("#cc222222")
    /**
     * 需要显示提示信息的View
     */
    var targetView: View? = null
    /**
     * targetView是否已测量
     */
    private var isMeasured: Boolean = false
    /**
     * targetView圆心
     */
    private var center = IntArray(2)
    /**
     * targetView左上角坐标
     */
    private var location = IntArray(2)
    /**
     * targetView rect
     */
    private var targetRect = Rect()
    /**
     * targetView 的外切圆半径
     */
    private var radius: Int = 0
    /**
     * 焦点相对于引导图，焦点图位于引导图的方位
     */
    var direction: Direction = Direction.LEFT_TOP

    /**
     * GuideView 偏移量
     */
    var offsetX: Int = 0
    var offsetY: Int = 0
    /**
     * 自定义View 引导图
     */
    var customGuideView: View? = null

    /**
     * 是否使用展示一次
     * 内部处理是否已经展示
     * 老的业务逻辑请勿直接使用
     */
    var useShowOneTime = false
    var preferenceKey: String? = null

    private var backgroundBitmap: Bitmap? = null
    private var backgroundCanvas: Canvas? = null
    var mTargetClickListener: OnClickListener? = null
    var mOnClickListener: OnClickListener? = null
    var customHeight = LayoutParams.WRAP_CONTENT
        set(value) {
            field = dp2px(value).toInt()
        }
    var customWidth = LayoutParams.WRAP_CONTENT
        set(value) {
            field = dp2px(value).toInt()
        }

    var customImageDrawable: Int = 0
        set(@DrawableRes value) {
            field = value
            customGuideView = ImageView(mContext)
            (customGuideView as ImageView).let { imageView ->
                imageView.setImageResource(field)
                imageView.scaleType = ImageView.ScaleType.FIT_XY
            }
        }

    /**
     * dp 2 px
     *
     * @param dpVal
     */
    private fun dp2px(dpVal: Int): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal.toFloat(), mContext.resources.displayMetrics)
    }

    init {
        circlePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
    }

    fun hide() {
        Log.v(tag, "hide")
        ((mContext as Activity).window.decorView as FrameLayout).removeView(this)
        restoreState()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            hide()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun restoreState() {
        Log.v(tag, "restoreState")
        isMeasured = false
    }


    fun show() {
        Log.d(tag, "show")
        if (hasShown())
            return
        setBackgroundColor(Color.TRANSPARENT)
        ((mContext as Activity).window.decorView as FrameLayout).addView(this)
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        var edit = mContext.getSharedPreferences(tag, Context.MODE_PRIVATE).edit()
        edit.putBoolean(generateKey(), true).apply()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)

    }

    private fun measureTarget() {
        if (isMeasured)
            return
        targetView?.let { targetView ->
            if (targetView.height > 0 && targetView.width > 0) {
                isMeasured = true
            }

            // 获取targetView的中心坐标
            // 获取右上角坐标
            targetView.getLocationInWindow(location)
            // 获取中心坐标
            center[0] = location[0] + targetView.width / 2
            center[1] = location[1] + targetView.height / 2
            Log.d(tag, "X:${center[0]}\tY:${center[1]}")
            // 获取targetView外切圆半径
            if (radius == 0) {
                radius = getTargetViewRadius()
            }
            targetRect.left = location[0]
            targetRect.top = location[1]
            targetRect.right = location[0] + targetView.width
            targetRect.bottom = location[1] + targetView.height
            // 添加GuideView
            createGuideView()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        Log.d(tag, "onWindowFocusChanged")
        if (hasWindowFocus) {
            measureTarget()
        }
    }

    private fun drawBackground(canvas: Canvas) {
        // 先绘制bitmap，再将bitmap绘制到屏幕
        backgroundBitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
        backgroundCanvas = Canvas(backgroundBitmap)

        backgroundBitmap?.let { bgBitmap ->
            Log.d(tag, "draw")
            // 背景画笔
            backGroundPaint.color = guideBackground

            // 绘制屏幕背景
            backgroundCanvas?.drawRect(0f, 0f, bgBitmap.width.toFloat(), bgBitmap.height.toFloat(), backGroundPaint)

            // targetView 的透明圆形画笔
            backgroundCanvas?.drawCircle((center[0]).toFloat(), (center[1]).toFloat(), radius.toFloat(), circlePaint)

            // 绘制到屏幕
            canvas.drawBitmap(bgBitmap, 0f, 0f, backGroundPaint)
            bgBitmap.recycle()
        }
    }

    /**
     * 定义targetView相对于GuideView的方位，共八种。
     */
    enum class Direction {
        LEFT_TOP, LEFT_BOTTOM,
        RIGHT_TOP, RIGHT_BOTTOM
    }


    /**
     * 添加提示文字，位置在targetView的下边
     * 在屏幕窗口，添加蒙层，蒙层绘制总背景和透明圆形，圆形下边绘制说明文字
     */
    private fun createGuideView() {
        Log.v(tag, "createGuideView")

        // Tips布局参数
        var guideViewParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(customWidth, customHeight)
        guideViewParams.setMargins(0, center[1] + radius + 10, 0, 0)

        customGuideView?.let { customGuideView ->

            val left = targetRect.left
            val right = targetRect.right
            val top = targetRect.top
            val bottom = targetRect.bottom
            //焦点view 相对引导图的位置
            when (direction) {
            //焦点位于引导图的右上方
//                ----------
//                |       口|
//                |  引导图 |
//                ----------

                Direction.RIGHT_TOP -> {
                    guideViewParams.gravity = Gravity.END
                    guideViewParams.setMargins(0, offsetY + top, width - right + offsetX, 0)
                }
            //焦点位于引导图的右下方
//                ----------
//                |        |
//                |引导图 口|
//                ----------
                Direction.RIGHT_BOTTOM -> {
                    guideViewParams.gravity = Gravity.END or Gravity.BOTTOM
                    guideViewParams.setMargins(0, 0, width - right + offsetX, height - bottom + offsetY)

                }
            //焦点位于引导图的左上方
//                ----------
//                |口       |
//                |  引导图 |
//                ----------
                Direction.LEFT_TOP -> {
                    guideViewParams.gravity = Gravity.START
                    guideViewParams.setMargins(left + offsetX, offsetY + top, 0, 0)

                }
            //焦点位于引导图的左下方
//                 ----------
//                |         |
//                |口 引导图 |
//                ----------
                Direction.LEFT_BOTTOM -> {
                    guideViewParams.gravity = Gravity.START or Gravity.BOTTOM
                    guideViewParams.setMargins(left + offsetX, 0, 0, height - bottom + offsetY)

                }
            }


            addView(customGuideView, guideViewParams)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                if (targetRect.contains(event.x.toInt(), event.y.toInt())) {
                    Log.d(tag, "click focus")
                    mTargetClickListener?.onClick(targetView)
                }
                mOnClickListener?.onClick(this)
            }
        }
        return true
    }


    /**
     * 获得targetView 的宽高，如果未测量，返回｛-1， -1｝
     *
     * @return
     */
    private fun getTargetViewSize(): IntArray {
        val location = intArrayOf(-1, -1)
        if (isMeasured) {
            location[0] = targetView?.width ?: 0
            location[1] = targetView?.height ?: 0
        }
        return location
    }

    /**
     * 获得targetView 的半径
     *
     * @return
     */
    private fun getTargetViewRadius(): Int {
        if (isMeasured) {
            val size = getTargetViewSize()
            val x = size[0]
            val y = size[1]

            return (Math.sqrt((x * x + y * y).toDouble()) / 2).toInt()
        }
        return -1
    }

    private fun hasShown(): Boolean {
        if (!useShowOneTime) {
            return false
        }
        return mContext.getSharedPreferences(tag, Context.MODE_PRIVATE).getBoolean(generateKey(), false)
    }

    private fun generateKey(): String {
        preferenceKey?.let {
            if (!TextUtils.isEmpty(it)) {
                return it
            }
        }
        targetView?.let {
            return generateUniqueId(it)
        }
        throw IllegalArgumentException("缺少必要参数")
    }

    private fun generateUniqueId(v: View): String {
        return "guideView" + v.id
    }
}