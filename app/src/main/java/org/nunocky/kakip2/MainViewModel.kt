package org.nunocky.kakip2

import android.app.Application
import android.graphics.*
import android.view.SurfaceHolder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainViewModel(application: Application) : AndroidViewModel(application),
    SurfaceHolder.Callback {
    companion object {
        const val TAG = "MainViewModel"
    }

    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(application) as T
        }
    }

    private var surfaceHolder: SurfaceHolder? = null

    private var job: Job? = null

    private lateinit var world: KakiPWorld

    private fun loadKakiPBitmaps(): List<Bitmap> {
        val bitmaps = ArrayList<Bitmap>()
        val options = BitmapFactory.Options()
        options.inScaled = false

        val application = getApplication<Application>()

        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip0, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip1, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip2, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip3, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip4, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip5, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip6, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip7, options))

        return bitmaps
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        //Log.d(TAG, "surfaceChanged $width x $height")
        surfaceHolder = holder

        world = KakiPWorld(width.toFloat(), height.toFloat())

        val bitmaps = loadKakiPBitmaps()

        world.setup(bitmaps)

        val screenWidth = world.screenWidth
        val screenHeight = world.screenHeight
        val worldWidth = world.worldWidth
        val worldHeight = world.worldHeight

        // START TIMER
        job = viewModelScope.launch {
            while (true) {
                delay((1000 / 60).toLong())
                world.step()

                // 描画
                surfaceHolder?.let { surfaceHolder ->
                    val canvas = surfaceHolder.lockCanvas()

                    val paint = Paint()

                    // background
                    paint.color = Color.WHITE
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(
                        Rect(
                            0,
                            0,
                            screenWidth.toInt(),
                            screenHeight.toInt()
                        ), paint
                    )

                    paint.color = Color.RED
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f

                    for (kakip in world.kakipObjects) {
                        val (sx, sy) = world.toScreenPos(kakip.x, kakip.y)
                        // radian -> degree
                        // Canvas : 反時計回りで degree
                        // Box2D: 時計回りで radian
                        val degree = (-kakip.rotRadian * (360f / (2 * Math.PI))).toFloat()

                        canvas.save()
                        canvas.translate(sx, sy)
                        canvas.rotate(degree)

                        // Box2Dワールドの大きさを画面サイズに変換
                        val hbw = ((screenWidth / worldWidth) * kakip.width).toInt() / 2
                        val hbh = ((screenHeight / worldHeight) * kakip.height).toInt() / 2
                        val rect = Rect(-hbw, -hbh, hbw, hbh)

                        val bitmap = kakip.bitmap

                        canvas.drawBitmap(bitmap, null, rect, paint)
                        //canvas.drawRect(rect, paint)
                        canvas.restore()
                    }

                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        // stop timer
        job?.cancel()
        job = null
        surfaceHolder = null
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
    }

    fun onTouch(sx: Float, sy: Float) {
        world.onTouch(sx, sy)
    }

    fun onMove(sx: Float, sy: Float) {
        world.onMove(sx, sy)
    }

    fun onUp() {
        world.onUp()
    }
}