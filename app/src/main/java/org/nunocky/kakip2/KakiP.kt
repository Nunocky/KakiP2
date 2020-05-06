package org.nunocky.kakip2

import org.jbox2d.dynamics.Body

data class KakiP(val bitmapId: Int, val body: Body, val width: Float, val height: Float) {
    val x: Float
        get() = body.position.x

    val y: Float
        get() = body.position.y

    val rotRadian: Float
        get() = body.angle
}