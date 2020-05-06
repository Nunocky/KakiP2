package org.nunocky.kakip2

import android.graphics.Bitmap
import org.jbox2d.dynamics.Body

data class KakiP(val bitmap: Bitmap, val body: Body, val width: Float, val height: Float) {
    val x: Float
        get() = body.position.x

    val y: Float
        get() = body.position.y

    val rotRadian: Float
        get() = body.angle
}