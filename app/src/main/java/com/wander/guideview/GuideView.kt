package com.wander.guideview

import android.content.Context
import android.graphics.*
import android.os.IBinder
import android.support.annotation.DrawableRes
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * author wangdou
 * date 2018/7/19
 * 使用说明
 * 焦点的重心默认在引导图的左上角
 * 根据引导图中焦点的重心，焦点在引导图中的重心（也就是那个角）的偏移
 * 即可正确定位
 * 1、在dialog中使用需将dialog设置为全屏--dialog启动会设置window的layoutParams
 */
class GuideView(var mContext: Context) : FrameLayout(mContext) {
    private val tag = javaClass.simpleName
    private var circlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var backGroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mWindowManager: WindowManager
    var guideBackground = Color.parseColor("#b0000000")

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
        set(value) {
            field = dp2px(value).toInt()
        }
    var offsetY: Int = 0
        set(value) {
            field = dp2px(value).toInt()
        }
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
    /**
     * 焦点区域点击
     */
    var mTargetClickListener: OnClickListener? = null
    /**
     * 非焦点区域点击
     */
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
        mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun hide() {
        Log.v(tag, "hide")
        try {
            mWindowManager.removeView(this)
            removeAllViews()
            restoreState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            hide()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun restoreState() {
        isMeasured = false
    }


    fun show() {
        if (targetView == null) {
            throw IllegalArgumentException("targetView is null")
        }
        try {
            targetView?.let {
                Log.d(tag, "show")
                if (hasShown())
                    return
                setBackgroundColor(guideBackground)
                mWindowManager.addView(this, createPopupLayoutParams(it.windowToken))
                isFocusable = true
                isFocusableInTouchMode = true
                if (useShowOneTime) {
                    mContext.getSharedPreferences(tag, Context.MODE_PRIVATE).edit().putBoolean(generateKey(), true).apply()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     *
     * Generate the layout parameters for the popup window.
     *
     * @param token the window token used to bind the popup's window
     *
     * @return the layout parameters to pass to the window manager
     *
     * @hide
     */
    protected fun createPopupLayoutParams(token: IBinder?): WindowManager.LayoutParams {
        val p = WindowManager.LayoutParams()

        // These gravity settings put the view at the top left corner of the
        // screen. The view is then positioned to the appropriate location by
        // setting the x and y offsets to match the anchor's bottom-left
        // corner.
        p.gravity = Gravity.START or Gravity.TOP
        p.flags = computeFlags(p.flags)
        p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
        p.token = token

//        if (mBackground != null) {
//            p.format = mBackground.getOpacity()
//        } else {
        p.format = PixelFormat.TRANSLUCENT
//        }

        // Used for debugging.
        p.title = "GuideView:" + Integer.toHexString(hashCode())

        return p
    }

    private fun computeFlags(curFlags: Int): Int {
        var flags = curFlags and (WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH).inv()
        flags = flags or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
        return flags
    }


    /**
     * 自己画焦点的位置
     */
    private var customDraw = true

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (customDraw) {
            drawBackground(canvas)
        }

    }

    private fun measureTarget() {
        if (isMeasured)
            return
        targetView?.let { targetView ->
            if (targetView.height > 0 && targetView.width > 0) {
                isMeasured = true
            } else {
                return
            }

            // 获取targetView的中心坐标
            // 获取左上角坐标
            targetView.getLocationOnScreen(location)
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
        setBackgroundColor(Color.TRANSPARENT)
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
        val guideViewParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(customWidth, customHeight)
        guideViewParams.setMargins(0, center[1] + radius + 10, 0, 0)

        customGuideView?.let { customGuideView ->

            val left = center[0]
            val right = width - center[0]
            val top = center[1]
            val bottom = height - center[1]

            //焦点view 相对引导图的位置
            when (direction) {
                //焦点重心位于引导图的右上角
//                ---------口
//                |         |
//                |  引导图 |
//                ----------

                Direction.RIGHT_TOP -> {
                    guideViewParams.gravity = Gravity.END
                    guideViewParams.setMargins(0, top - offsetY, right - offsetX, 0)
                }
                //焦点重心位于引导图的右下角
//                ----------
//                |        |
//                |引导图   |
//                ---------口
                Direction.RIGHT_BOTTOM -> {
                    guideViewParams.gravity = Gravity.END or Gravity.BOTTOM
                    guideViewParams.setMargins(0, 0, right - offsetX, bottom - offsetY)

                }
                //焦点重心位于引导图的左上角
//                口---------
//                |        |
//                |  引导图 |
//                ----------
                Direction.LEFT_TOP -> {
                    guideViewParams.gravity = Gravity.START
                    guideViewParams.setMargins(left - offsetX, top - offsetY, 0, 0)

                }
                //焦点重心位于引导图的左下角
//                 ----------
//                |         |
//                |   引导图 |
//                口---------
                Direction.LEFT_BOTTOM -> {
                    guideViewParams.gravity = Gravity.BOTTOM
                    guideViewParams.setMargins(left - offsetX, 0, 0, bottom - offsetY)

                }
            }

            try {
                if (customGuideView.parent != null) {
                    (customGuideView.parent as ViewGroup).removeView(customGuideView)
                }

                addView(customGuideView, guideViewParams)
            } catch (e: Exception) {
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                if (targetRect.contains(event.x.toInt(), event.y.toInt())) {
                    Log.d(tag, "click focus")
                    mTargetClickListener?.onClick(targetView)
                } else {
                    mOnClickListener?.onClick(this)
                }
                hide()
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