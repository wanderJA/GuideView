package com.wander.guideview

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.icu.util.Measure
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.RelativeLayout

/**
 * author wangdou
 * date 2018/7/15
 *
 */
class GuideView(var mContext: Context) : RelativeLayout(mContext), ViewTreeObserver.OnGlobalLayoutListener {
    private val TAG = javaClass.simpleName
    var circlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var backGroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var guideBackground = Color.parseColor("#cc222222")
    /**
     * 需要显示提示信息的View
     */
    var targetView: View? = null

    var backgroundBitmap: Bitmap? = null
    var backgroundCanvas:Canvas?= null



    init {
        backGroundPaint.color = Color.GREEN
        backGroundPaint.alpha = 120
        circlePaint.color = Color.RED
        circlePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
    }


    fun show() {
        Log.v(TAG, "show")
        if (hasShown())
            return
        targetView?.viewTreeObserver?.addOnGlobalLayoutListener(this)

        ((mContext as Activity).window.decorView as FrameLayout).addView(this)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)
        canvas.drawRect(0F, 0f, width.toFloat(), height.toFloat(), backGroundPaint)
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), 100F, circlePaint)

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        Log.d(TAG,"onLayout\twidth:${width}height:$height")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(TAG,"onMeasure$widthMeasureSpec")
    }

    private fun drawBackground(canvas: Canvas) {
        // 先绘制bitmap，再将bitmap绘制到屏幕
        backgroundBitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
        backgroundCanvas = Canvas(backgroundBitmap)

        backgroundBitmap?.let { bgBitmap->
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

    override fun onGlobalLayout() {

    }


    private fun hasShown(): Boolean {
        return if (targetView == null) true else mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE).getBoolean(generateUniqId(targetView!!), false)
    }

    private fun generateUniqId(v: View): String {
        return "guideView" + v.id
    }
}