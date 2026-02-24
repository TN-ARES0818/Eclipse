package com.eclipse.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.util.TypedValue
import android.view.*
import android.view.animation.*
import android.widget.*

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pure black, fully immersive
        window.apply {
            statusBarColor     = Color.BLACK
            navigationBarColor = Color.BLACK
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT)
        }

        // ── Star field canvas ──────────────────────────────────────────────
        val starField = StarFieldView(this)
        root.addView(starField, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT))

        // ── Outer glow rings ───────────────────────────────────────────────
        val ring1 = makeRing(dp(220), "#18ff6b1a")
        val ring2 = makeRing(dp(160), "#2aff6b1a")
        val ring3 = makeRing(dp(105), "#40ff6b1a")
        listOf(ring1, ring2, ring3).forEach { v ->
            root.addView(v, FrameLayout.LayoutParams(dp(220), dp(220), Gravity.CENTER).also { lp ->
                lp.width  = (v.tag as Int)
                lp.height = (v.tag as Int)
                lp.topMargin = -dp(50)
            })
        }
        // Fix layout after adding
        ring1.layoutParams = FrameLayout.LayoutParams(dp(220), dp(220), Gravity.CENTER).apply { topMargin = -dp(50) }
        ring2.layoutParams = FrameLayout.LayoutParams(dp(160), dp(160), Gravity.CENTER).apply { topMargin = -dp(50) }
        ring3.layoutParams = FrameLayout.LayoutParams(dp(105), dp(105), Gravity.CENTER).apply { topMargin = -dp(50) }
        listOf(ring1, ring2, ring3).forEach { it.alpha = 0f }

        // ── Logo image ─────────────────────────────────────────────────────
        val logoImg = ImageView(this).apply {
            setImageResource(R.drawable.splash_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0f
        }
        root.addView(logoImg, FrameLayout.LayoutParams(dp(200), dp(200), Gravity.CENTER).apply {
            topMargin = -dp(60)
        })

        // ── "browse beyond" tagline ────────────────────────────────────────
        val tagline = TextView(this).apply {
            text = "browse beyond"
            setTextColor(Color.parseColor("#88ff6b1a"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            letterSpacing = 0.30f
            gravity = Gravity.CENTER
            alpha = 0f
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
        }
        root.addView(tagline, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER).apply { topMargin = dp(100) })

        // ── Loading dots ───────────────────────────────────────────────────
        val dotsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            alpha = 0f
        }
        val dots = (0..2).map {
            View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#33ff6b1a"))
                }
                layoutParams = LinearLayout.LayoutParams(dp(6), dp(6)).apply {
                    setMargins(dp(5), 0, dp(5), 0)
                }
            }.also { dotsRow.addView(it) }
        }
        root.addView(dotsRow, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, dp(20), Gravity.CENTER).apply {
            topMargin = dp(160)
        })

        // ── Version ────────────────────────────────────────────────────────
        val version = TextView(this).apply {
            text = "v5.0"
            setTextColor(Color.parseColor("#22ffffff"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            alpha = 0f
        }
        root.addView(version, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(48) })

        setContentView(root)

        // ── Animate sequence ───────────────────────────────────────────────
        val h = Handler(Looper.getMainLooper())

        // 1. Rings expand in (staggered)
        h.postDelayed({ fadeScaleIn(ring3, 350) }, 200)
        h.postDelayed({ fadeScaleIn(ring2, 420) }, 340)
        h.postDelayed({ fadeScaleIn(ring1, 500) }, 480)

        // 2. Logo pops in
        h.postDelayed({
            fadeScaleIn(logoImg, 500)
        }, 600)

        // 3. Tagline + version fade in
        h.postDelayed({
            fadeIn(tagline, 500)
            fadeIn(version, 700)
            fadeIn(dotsRow, 400)
        }, 1000)

        // 4. Animate loading dots
        h.postDelayed({
            animateDots(dots, h)
        }, 1100)

        // 5. Pulse rings gently
        h.postDelayed({
            pulseRings(listOf(ring1, ring2, ring3))
        }, 1000)

        // 6. Launch after 2.8s
        h.postDelayed({
            root.animate().alpha(0f).setDuration(380).withEndAction {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }.start()
        }, 2800)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun makeRing(size: Int, colorHex: String): View {
        return View(this).apply {
            tag = size
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.parseColor(colorHex))
            }
        }
    }

    private fun fadeScaleIn(v: View, dur: Long) {
        v.alpha = 1f
        AnimationSet(false).apply {
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = dur; fillAfter = true })
            addAnimation(ScaleAnimation(0.5f, 1f, 0.5f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                duration = dur; interpolator = DecelerateInterpolator(2f); fillAfter = true
            })
            fillAfter = true
        }.also { v.startAnimation(it) }
    }

    private fun fadeIn(v: View, dur: Long) {
        v.alpha = 1f
        v.startAnimation(AlphaAnimation(0f, 1f).apply {
            duration = dur; interpolator = DecelerateInterpolator(); fillAfter = true
        })
    }

    private fun pulseRings(rings: List<View>) {
        rings.forEachIndexed { i, v ->
            v.startAnimation(ScaleAnimation(1f, 1.04f + i * 0.02f, 1f, 1.04f + i * 0.02f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                duration = 1200L + i * 200
                repeatCount = Animation.INFINITE; repeatMode = Animation.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            })
        }
    }

    private fun animateDots(dots: List<View>, h: Handler) {
        dots.forEachIndexed { i, dot ->
            h.postDelayed(object : Runnable {
                override fun run() {
                    dot.animate().scaleX(1.6f).scaleY(1.6f)
                        .setDuration(250)
                        .withEndAction {
                            dot.animate().scaleX(1f).scaleY(1f).setDuration(250).start()
                        }.start()
                    dot.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#88ff6b1a"))
                    }
                    h.postDelayed({
                        dot.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#33ff6b1a"))
                        }
                        h.postDelayed(this, 900)
                    }, 500)
                }
            }, i * 300L)
        }
    }

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    // ── Inner class: animated star field ──────────────────────────────────
    inner class StarFieldView(context: android.content.Context) : View(context) {
        data class Star(var x: Float, var y: Float, val r: Float, val alpha: Float, val speed: Float)
        private val stars = mutableListOf<Star>()
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }

        init {
            repeat(90) {
                stars.add(Star(
                    (Math.random() * 1080).toFloat(),
                    (Math.random() * 2400).toFloat(),
                    (Math.random() * 1.8f + 0.3f),
                    (Math.random() * 0.7f + 0.1f).toFloat(),
                    (Math.random() * 0.4f + 0.05f).toFloat()
                ))
            }
            // Twinkling
            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() { invalidate(); handler.postDelayed(this, 80) }
            }
            handler.post(runnable)
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            val t = System.currentTimeMillis() / 1000.0
            stars.forEach { s ->
                val twinkle = (0.3f + 0.7f * ((Math.sin(t * s.speed * 3 + s.x) + 1) / 2)).toFloat()
                paint.alpha = (s.alpha * twinkle * 255).toInt().coerceIn(0, 255)
                canvas.drawCircle(s.x % width, s.y % height, s.r, paint)
            }
        }
    }
}
