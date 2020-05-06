package org.nunocky.kakip2

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory(application) }

    private var statusbarHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surfaceView = MySurfaceView(this)
        setContentView(surfaceView)
        surfaceView.holder.addCallback(viewModel)

        surfaceView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    //Log.d(TAG, "DOWN")
                    viewModel.onTouch(event.x, event.y)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    //Log.d(TAG, "MOVE")
                    viewModel.onMove(event.x, event.y)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    //Log.d(TAG, "UP")
                    viewModel.onUp()
                    true
                }
                else -> {
                    //Log.d(TAG, "")
                    true
                }
            }


        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        statusbarHeight = rect.top
    }

//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        if (event is MotionEvent) {
//            viewModel.onTouch(event.x, event.y - statusbarHeight)
//        }
//        return true
//    }


}

