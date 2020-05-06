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
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application),
    SurfaceHolder.Callback {
    companion object {
        const val TAG = "MainViewModel"

        const val timeStep = 1 / 60f
        const val velocityIterations = 8
        const val positionIterations = 3

        const val wallThickness = 10f

        const val mag = 20 // ワールド座標系の最大値
    }

    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(application) as T
        }
    }

    private val bitmaps = ArrayList<Bitmap>()

    init {
        val options = BitmapFactory.Options()
        options.inScaled = false
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip0, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip1, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip2, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip3, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip4, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip5, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip6, options))
        bitmaps.add(BitmapFactory.decodeResource(application.resources, R.drawable.kakip7, options))
    }

    private var surfaceHolder: SurfaceHolder? = null

    private var job: Job? = null

    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f
    private var worldWidth: Float = 0f
    private var worldHeight: Float = 0f
    private lateinit var world: World

    private val kakiPs = ArrayList<KakiP>()

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        //Log.d(TAG, "surfaceChanged $width x $height")
        surfaceHolder = holder

        screenWidth = width.toFloat()
        screenHeight = height.toFloat()

        if (width > height) {
            worldWidth = 1f
            worldHeight = height / width.toFloat()
        } else {
            worldWidth = width / height.toFloat()
            worldHeight = 1f
        }

        worldWidth *= mag
        worldHeight *= mag

        setupSim()

        // START TIMER
        job = viewModelScope.launch {
            while (true) {
                delay((1000 / 60).toLong())
                world.step(timeStep, velocityIterations, positionIterations)

                // Now print the position and angle of the body.

                // 描画
                surfaceHolder?.let { surfaceHolder ->
                    val canvas = surfaceHolder.lockCanvas()

                    val paint = Paint()

                    // background
                    paint.color = Color.WHITE
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(Rect(0, 0, screenWidth.toInt(), screenHeight.toInt()), paint)

                    paint.color = Color.RED
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f

                    for (kakip in kakiPs) {
                        //val body = kakip.body
                        val (sx, sy) = toScreenPos(kakip.x, kakip.y)
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

                        val bitmap = bitmaps[kakip.bitmapId]

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
        // 画面座標系 → ワールド座標系変換
        val (wx, wy) = toWorldPos(sx, sy)
        //Log.d(TAG, "onTouch: ($sx, $sy) -> ($wx, $wy)")

        for (kakip in kakiPs) {
            if (kakip.body.fixtureList.testPoint(Vec2(wx, wy))) {
                //Log.d(TAG, "(ﾟ∀ﾟ)")
                focusObj = kakip
                break
            }
        }
    }

    fun onMove(sx: Float, sy: Float) {
        focusObj?.let { kakip ->
            val (wx, wy) = toWorldPos(sx, sy)
            kakip.body.setTransform(Vec2(wx, wy), kakip.body.angle)
        }
    }

    private var focusObj: KakiP? = null
    fun onUp() {
        focusObj = null
    }

    private fun toScreenPos(worldX: Float, worldY: Float): Pair<Float, Float> {
        val sx = (screenWidth / worldWidth) * worldX
        val sy = screenHeight - (screenHeight / worldHeight) * worldY
        return Pair(sx, sy)
    }

    private fun toWorldPos(screenX: Float, screenY: Float): Pair<Float, Float> {
        val wx = (worldWidth / screenWidth) * screenX
        val wy = worldHeight - (worldHeight / screenHeight) * screenY
        return Pair(wx, wy)
    }


    private fun setupSim() {

        // 重力ベクトル
        val gravity = Vec2(0f, 0f)

        // ワールドオブジェクト
        world = World(gravity)

        createWallU()
        createWallR()
        createWallD()
        createWallL()

        createStone()

        for (i in 0 until 100) {
            val id = Random.nextInt(8)
            val bitmap = bitmaps[id]

            val x = Random.nextFloat() * worldWidth
            val y = Random.nextFloat() * worldHeight
            val w = if (bitmap.width == 480) 2f else 1.5f
            val h = bitmap.height.toFloat() * w / bitmap.width
            val rad = Random.nextFloat() * (2 * Math.PI).toFloat()
            val kakiP = createKakiP(id, x, y, w, h, rad)
            kakiPs.add(kakiP)
        }
    }


    private fun createStone() {
        createWall(worldWidth / 2 + 0.2f, 0.1f, 0.1f, 0.1f)
    }

    /**
     * 上部のフタ
     */
    private fun createWallU() {
        createWall(worldWidth / 2, worldHeight + wallThickness / 2, worldWidth, wallThickness)
    }


    /**
     * 右側のフタ
     */
    private fun createWallR() {
        createWall(
            worldWidth + wallThickness / 2,
            worldHeight / 2,
            wallThickness,
            worldHeight + wallThickness * 2
        )
    }

    /**
     * 下側のフタ
     */
    private fun createWallD() {
        createWall(worldWidth / 2, -wallThickness / 2, worldWidth, wallThickness)
    }

    /**
     * 左側のフタ
     */
    private fun createWallL() {
        createWall(
            -wallThickness / 2,
            worldHeight / 2,
            wallThickness,
            worldHeight + wallThickness * 2
        )
    }


    private fun createWall(cx: Float, cy: Float, w: Float, h: Float) {
        val bodyDef = BodyDef()
        bodyDef.type = BodyType.STATIC
        bodyDef.position.set(cx, cy)

        val body = world.createBody(bodyDef)
        val box = PolygonShape()

        box.setAsBox(w / 2, h / 2)
        body.createFixture(box, 0f)
    }

    private fun createKakiP(
        bitmapId: Int,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        angle: Float
    ): KakiP {
        // Define the dynamic body. We set its position and call the body factory.
        val bodyDef = BodyDef()
        bodyDef.type = BodyType.DYNAMIC
        bodyDef.position.set(x, y)
        bodyDef.angle = angle

        val body = world.createBody(bodyDef)

        // Define another box shape for our dynamic body.
        val dynamicBox = PolygonShape()
        dynamicBox.setAsBox(w / 2, h / 2)

        // Define the dynamic body fixture.
        val fixtureDef = FixtureDef()
        fixtureDef.shape = dynamicBox

        // Set the box density to be non-zero, so it will be dynamic.
        fixtureDef.density = 1f

        // Override the default friction.
        fixtureDef.friction = 0.3f

        // Add the shape to the body.
        body.createFixture(fixtureDef)

        return KakiP(bitmapId, body, w, h)
    }
}