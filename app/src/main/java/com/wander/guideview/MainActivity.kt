package com.wander.guideview

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    lateinit var guideView: GuideView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        guideView = GuideView(this)
        guideView.targetView = targetShow
        var textView = TextView(this)
        textView.text = "kaishiba"
        textView.textSize = 20f
        textView.setBackgroundColor(Color.YELLOW)
        textView.setTextColor(Color.BLACK)
        guideView.customGuideView = textView
//        guideView.show()
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action") {
                        guideView.show()
                    }.show()
        }

        GuideView(this).apply {
            mTargetClickListener = View.OnClickListener { Toast.makeText(this@MainActivity, "click", Toast.LENGTH_SHORT).show() }
            customImageDrawable = R.drawable.guide_vertical
            customWidth = 250
            customHeight = 400
            targetView = itemBottom
            direction = GuideView.Direction.LEFT_BOTTOM
            offsetX = -10
            offsetY = -70
            useShowOneTime = true
        }.show()


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
