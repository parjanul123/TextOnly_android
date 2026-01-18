package text.only.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.pow
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
    private var frameThickness = 0f // Will be calculated

    enum class FrameType {
        SNOW, RAIN, NEON_PARTICLES
    }

    private class Particle(
        var x: Float,
        var y: Float,
        var speed: Float,
        var size: Float,
        var alpha: Int
    )

    init {
        paint.style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        frameThickness = w * 0.15f // 15% of width is the border area
        initParticles(w, h)
        if (animator == null) {
            startAnimation()
        }
    }

    private fun initParticles(w: Int, h: Int) {
        particles.clear()
        for (i in 0 until particleCount) {
            particles.add(createParticle(w, h, true))
        }
    }

    private fun createParticle(w: Int, h: Int, randomY: Boolean): Particle {
        // We need to spawn particles ONLY in the frame border area (not in the center)
        // Simple logic: Spawn anywhere, but if it falls inside the center circle, we ignore drawing it or push it out.
        // Better logic: Spawn specifically in the outer rectangle.
        
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
        
        val alpha = Random.nextInt(100, 255)

        return Particle(x, y, speed, size, alpha)
    }

    private fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
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
            
            // Wiggle for snow
            if (type == FrameType.SNOW) {
                p.x += (Random.nextFloat() - 0.5f) * 1f
            }

            // Reset if off screen
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
        // Radius of the safe zone (the avatar). 
        // We assume the frame is square, but the avatar might be circle.
        // Let's keep the particles out of the inner circle defined by (width/2) - thickness
        val safeRadius = (width / 2f) - frameThickness

        particles.forEach { p ->
            // Check if particle is inside the "safe zone" (over the face)
            val dx = p.x - centerX
            val dy = p.y - centerY
            val dist = sqrt(dx*dx + dy*dy)

            // Only draw if it's OUTSIDE the safe radius (in the frame area)
            if (dist > safeRadius) {
                paint.alpha = p.alpha
                paint.color = when(type) {
                    FrameType.SNOW -> Color.WHITE
                    FrameType.RAIN -> Color.parseColor("#64B5F6") // Light Blue
                    FrameType.NEON_PARTICLES -> Color.parseColor("#FF00FF") // Magenta
                }
                
                if (type == FrameType.RAIN) {
                    // Draw lines for rain
                    canvas.drawLine(p.x, p.y, p.x, p.y + p.size * 3, paint)
                } else {
                    // Draw circles for snow/particles
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
            }
        }
        
        // Optional: Draw a border stroke to define the frame edges nicely
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = when(type) {
            FrameType.SNOW -> Color.parseColor("#80FFFFFF")
            FrameType.RAIN -> Color.parseColor("#802196F3")
            FrameType.NEON_PARTICLES -> Color.parseColor("#FF00FF")
        }
        // Draw outer circle
        canvas.drawCircle(centerX, centerY, (width/2f) - 2f, paint)
        paint.style = Paint.Style.FILL // Reset
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
