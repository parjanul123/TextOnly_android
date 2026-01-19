package text.only.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sqrt
import kotlin.random.Random

class AnimatedFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var animator: ValueAnimator? = null

    // Config
    private var particleCount = 100
    private var type = FrameType.SNOW
    private var frameThickness = 0f

    enum class FrameType {
        SNOW, RAIN, NEON_PARTICLES
    }

    private data class Particle(
        var x: Float,
        var y: Float,
        var speed: Float,
        var size: Float,
        var alpha: Int
    )

    init {
        paint.style = Paint.Style.FILL
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animator == null) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        frameThickness = w * 0.15f
        initParticles(w, h)
    }

    private fun initParticles(w: Int, h: Int) {
        particles.clear()
        repeat(particleCount) {
            particles.add(createParticle(w, h, true))
        }
    }

    private fun createParticle(w: Int, h: Int, randomY: Boolean): Particle {
        val x = Random.nextFloat() * w
        val y = if (randomY) Random.nextFloat() * h else -10f

        val speed = when (type) {
            FrameType.SNOW -> Random.nextFloat() * 2f + 1f
            FrameType.RAIN -> Random.nextFloat() * 10f + 15f
            FrameType.NEON_PARTICLES -> Random.nextFloat() * 4f + 2f
        }

        val size = when (type) {
            FrameType.SNOW -> Random.nextFloat() * 4f + 2f
            FrameType.RAIN -> Random.nextFloat() * 2f + 1f
            FrameType.NEON_PARTICLES -> Random.nextFloat() * 6f + 2f
        }

        return Particle(
            x = x,
            y = y,
            speed = speed,
            size = size,
            alpha = Random.nextInt(120, 255)
        )
    }

    private fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    private fun updateParticles() {
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        particles.forEach { p ->
            p.y += p.speed

            if (type == FrameType.SNOW) {
                p.x += (Random.nextFloat() - 0.5f)
            }

            if (p.y > h) {
                val newP = createParticle(w, h, false)
                p.x = newP.x
                p.y = newP.y
                p.speed = newP.speed
                p.alpha = newP.alpha
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val safeRadius = (width / 2f) - frameThickness

        particles.forEach { p ->
            val dx = p.x - centerX
            val dy = p.y - centerY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > safeRadius) {
                paint.alpha = p.alpha
                paint.color = when (type) {
                    FrameType.SNOW -> Color.WHITE
                    FrameType.RAIN -> Color.parseColor("#64B5F6")
                    FrameType.NEON_PARTICLES -> Color.parseColor("#FF00FF")
                }

                if (type == FrameType.RAIN) {
                    canvas.drawLine(p.x, p.y, p.x, p.y + p.size * 3, paint)
                } else {
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
            }
        }

        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = when (type) {
            FrameType.SNOW -> Color.parseColor("#80FFFFFF")
            FrameType.RAIN -> Color.parseColor("#802196F3")
            FrameType.NEON_PARTICLES -> Color.parseColor("#FF00FF")
        }
        canvas.drawCircle(centerX, centerY, (width / 2f) - 2f, paint)
        paint.style = Paint.Style.FILL
    }

    fun setFrameType(frameName: String) {
        type = when (frameName) {
            "frame_snow" -> FrameType.SNOW
            "frame_rain" -> FrameType.RAIN
            "frame_neon" -> FrameType.NEON_PARTICLES
            else -> FrameType.SNOW
        }
        initParticles(width, height)
    }
}
