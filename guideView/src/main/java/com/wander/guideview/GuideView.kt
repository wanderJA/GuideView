package com.wander.guideview

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.RelativeLayout

/**
 * author wangdou
 * date 2018/7/15
 *
 */
class GuideView(var mContext: Context) : RelativeLayout(mContext) {
    private val TAG = javaClass.simpleName
    var circlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var backGroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
     * targetView 的外切圆半径
     */
    private var radius: Int = 0
    /**
     * 相对于targetView的位置.在target的那个方向
     */
    private var direction: Direction = Direction.BOTTOM

    /**
     * GuideView 偏移量
     */
    private var offsetX: Int = 0
    private var offsetY: Int = 0
    /**
     * 自定义View
     */
    private var customGuideView: View? = null

    var backgroundBitmap: Bitmap? = null
    var backgroundCanvas: Canvas? = null


    init {
        circlePaint.color = Color.RED
        circlePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
    }

    fun hide() {
        Log.v(TAG, "hide")
        this.removeAllViews()
        ((mContext as Activity).window.decorView as FrameLayout).removeView(this)
        restoreState()
    }

    fun restoreState() {
        Log.v(TAG, "restoreState")
        isMeasured = false

    }


    fun show() {
        Log.d(TAG, "show")
//        if (hasShown())
//            return

        ((mContext as Activity).window.decorView as FrameLayout).addView(this)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(TAG, "onDraw")
        drawBackground(canvas)
//        canvas.drawRect(0F, 0f, width.toFloat(), height.toFloat(), backGroundPaint)
//        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), 100F, circlePaint)

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        Log.d(TAG, "onLayout\twidth:${width}height:$height")

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
            // 获取targetView外切圆半径
            if (radius == 0) {
                radius = getTargetViewRadius()
            }
            // 添加GuideView
            createGuideView()

        }
    }

    private fun drawBackground(canvas: Canvas) {
        Log.d(TAG, "drawBackgroud")
        // 先绘制bitmap，再将bitmap绘制到屏幕
        backgroundBitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
        backgroundCanvas = Canvas(backgroundBitmap)

        backgroundBitmap?.let { bgBitmap ->
            Log.d(TAG, "draw")
            // 背景画笔
            backGroundPaint.color = guideBackground

            // 绘制屏幕背景
            backgroundCanvas?.drawRect(0f, 0f, bgBitmap.width.toFloat(), bgBitmap.height.toFloat(), backGroundPaint)

            // targetView 的透明圆形画笔
            backgroundCanvas?.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), 100F, circlePaint)

            // 绘制到屏幕
            canvas.drawBitmap(bgBitmap, 0f, 0f, backGroundPaint)
            bgBitmap.recycle()
        }

    }

    /**
     * 定义GuideView相对于targetView的方位，共八种。不设置则默认在targetView下方
     */
    internal enum class Direction {
        LEFT, TOP, RIGHT, BOTTOM,
        LEFT_TOP, LEFT_BOTTOM,
        RIGHT_TOP, RIGHT_BOTTOM
    }


    /**
     * 添加提示文字，位置在targetView的下边
     * 在屏幕窗口，添加蒙层，蒙层绘制总背景和透明圆形，圆形下边绘制说明文字
     */
    private fun createGuideView() {
        Log.v(TAG, "createGuideView")

        // Tips布局参数
        var guideViewParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        guideViewParams.setMargins(0, center[1] + radius + 10, 0, 0)

        if (customGuideView != null) {

            val width = this.width
            val height = this.height

            val left = center[0] - radius
            val right = center[0] + radius
            val top = center[1] - radius
            val bottom = center[1] + radius
            when (direction) {
                Direction.TOP -> {
                    this.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    guideViewParams.setMargins(offsetX, offsetY - height + top, -offsetX, height - top - offsetY)
                }
                Direction.LEFT -> {
                    this.gravity = Gravity.RIGHT
                    guideViewParams.setMargins(offsetX - width + left, top + offsetY, width - left - offsetX, -top - offsetY)
                }
                Direction.BOTTOM -> {
                    this.gravity = Gravity.CENTER_HORIZONTAL
                    guideViewParams.setMargins(offsetX, bottom + offsetY, -offsetX, 0)
                }
                Direction.RIGHT -> guideViewParams.setMargins(right + offsetX, top + offsetY, -right - offsetX, -top - offsetY)
                Direction.LEFT_TOP -> {
                    this.gravity = Gravity.RIGHT or Gravity.BOTTOM
                    guideViewParams.setMargins(offsetX - width + left, offsetY - height + top, width - left - offsetX, height - top - offsetY)
                }
                Direction.LEFT_BOTTOM -> {
                    this.gravity = Gravity.RIGHT
                    guideViewParams.setMargins(offsetX - width + left, bottom + offsetY, width - left - offsetX, -bottom - offsetY)
                }
                Direction.RIGHT_TOP -> {
                    this.gravity = Gravity.BOTTOM
                    guideViewParams.setMargins(right + offsetX, offsetY - height + top, -right - offsetX, height - top - offsetY)
                }
                Direction.RIGHT_BOTTOM -> guideViewParams.setMargins(right + offsetX, bottom + offsetY, -right - offsetX, -top - offsetY)
            }


            addView(customGuideView, guideViewParams)
        }
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
        return if (targetView == null) true else mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE).getBoolean(generateUniqueId(targetView!!), false)
    }

    private fun generateUniqueId(v: View): String {
        return "guideView" + v.id
    }
}