package com.anwesh.uiprojects.increasingstepbarview

/**
 * Created by anweshmishra on 07/06/19.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.content.Context

val nodes : Int = 5
val rects : Int = 4
val scGap : Float = 0f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#311B92")
val backColor : Int = Color.parseColor("#BDBDBD")

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float {
    return mirrorValue(a, b) * dir * scGap
}

fun Canvas.drawStepBar(x : Float, i : Int, sc : Float, size : Float, paint : Paint) {
    save()
    translate(x, 0f)
    drawRect(RectF(0f, 0f, size, -i * size - size * sc), paint)
    restore()
}

fun Canvas.drawISBNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    var x : Float = 0f
    var barSize : Float = (2 * size) / (rects + 1)
    paint.style = Paint.Style.STROKE
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(w / 2, gap * (i + 1))
    save()
    translate(-size, -size)
    for (j in 0..(rects - 1)) {
        val sc : Float = scale.divideScale(j, rects)
        x += barSize * sc.divideScale(0, 2)
        drawStepBar(x, j, sc.divideScale(1, 2), size, paint)
    }
    restore()
    restore()
}

class IncreasingStepBarView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, rects, rects)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class ISBNode(var i : Int, val state : State = State()) {

        private var next : ISBNode? = null
        private var prev : ISBNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = ISBNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawISBNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : ISBNode {
            var curr : ISBNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class IncreasingStepBar(var i : Int) {

        private val root : ISBNode = ISBNode(0)
        private var curr : ISBNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : IncreasingStepBarView) {

        private val animator : Animator = Animator(view)
        private val isb : IncreasingStepBar = IncreasingStepBar(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            isb.draw(canvas, paint)
            animator.animate {
                isb.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            isb.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : IncreasingStepBarView {
            val view : IncreasingStepBarView = IncreasingStepBarView(activity)
            activity.setContentView(view)
            return view 
        }
    }
}
