package org.nunocky.kakip2

import android.graphics.Bitmap
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import kotlin.random.Random

class KakiPWorld(val screenWidth: Float, val screenHeight: Float) {
    companion object {
        const val TAG = "KakiPWorld"

        const val timeStep = 1 / 60f
        const val velocityIterations = 8
        const val positionIterations = 3

        const val wallThickness = 10f

        const val mag = 20f // ワールド座標系の最大値
        const val numObjects = 100
    }

    val worldWidth: Float
    val worldHeight: Float
    private val kakiPs = ArrayList<KakiP>()
    private var focusObj: KakiP? = null
    private lateinit var world: World

    val kakipObjects: List<KakiP>
        get() = kakiPs

    init {
        if (screenWidth > screenHeight) {
            worldWidth = mag
            worldHeight = (screenHeight / screenWidth) * mag
        } else {
            worldWidth = (screenWidth / screenHeight) * mag
            worldHeight = mag
        }
    }

    fun toScreenPos(worldX: Float, worldY: Float): Pair<Float, Float> {
        val sx = (screenWidth / worldWidth) * worldX
        val sy = screenHeight - (screenHeight / worldHeight) * worldY
        return Pair(sx, sy)
    }

    fun toWorldPos(screenX: Float, screenY: Float): Pair<Float, Float> {
        val wx = (worldWidth / screenWidth) * screenX
        val wy = worldHeight - (worldHeight / screenHeight) * screenY
        return Pair(wx, wy)
    }


    fun setup(bitmaps: List<Bitmap>) {
        // 重力ベクトル
        val gravity = Vec2(0f, 0f)

        // ワールドオブジェクト
        world = World(gravity)

        createWallU()
        createWallR()
        createWallD()
        createWallL()

        kakiPs.clear()
        for (i in 0 until numObjects) {
            val id = Random.nextInt(bitmaps.size)
            val bitmap = bitmaps[id]

            val x = Random.nextFloat() * worldWidth
            val y = Random.nextFloat() * worldHeight
            val w = bitmap.width * 2f / 480
            val h = bitmap.height * w / bitmap.width
            val rad = Random.nextFloat() * (2 * Math.PI).toFloat()
            val kakiP = createKakiP(bitmap, x, y, w, h, rad)
            kakiPs.add(kakiP)
        }

    }

    fun step() {
        world.step(timeStep, velocityIterations, positionIterations)
    }

    fun onTouch(sx: Float, sy: Float) {
        // 画面座標系 → ワールド座標系変換
        val (wx, wy) = toWorldPos(sx, sy)

        for (kakip in kakiPs) {
            // kakipオブジェクトと 点(wx, wy)の交差チェック
            if (kakip.body.fixtureList.testPoint(Vec2(wx, wy))) {
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

    fun onUp() {
        focusObj = null
    }

    /**
     * 上部の壁
     */
    private fun createWallU() {
        createWall(worldWidth / 2, worldHeight + wallThickness / 2, worldWidth, wallThickness)
    }

    /**
     * 右側の壁
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
     * 下側の壁
     */
    private fun createWallD() {
        createWall(worldWidth / 2, -wallThickness / 2, worldWidth, wallThickness)
    }

    /**
     * 左側の壁
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
        bitmap: Bitmap,
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

        return KakiP(bitmap, body, w, h)
    }
}