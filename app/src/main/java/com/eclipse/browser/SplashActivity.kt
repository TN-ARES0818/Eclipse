package com.eclipse.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.*
import android.view.animation.*
import android.widget.*
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val r: Float, val alpha: Float)

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor     = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // ── 1. Starfield background ──────────────────────────────────────
        val starField = object : View(this) {
            val stars = (0 until 220).map {
                Star(Random.nextFloat(), Random.nextFloat(),
                    Random.nextFloat() * 1.8f + 0.2f,
                    Random.nextFloat() * 0.7f + 0.15f)
            }
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            override fun onDraw(canvas: Canvas) {
                stars.forEach { s ->
                    p.alpha = (s.alpha * 255).toInt()
                    canvas.drawCircle(s.x * width, s.y * height, s.r, p)
                }
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            alpha = 0f
        }
        root.addView(starField)

        // ── 2. Outer ambient glow behind logo ────────────────────────────
        val ambientGlow = object : View(this) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: Canvas) {
                val cx = width / 2f
                val cy = height / 2f
                val grad = RadialGradient(cx, cy, width * 0.55f,
                    intArrayOf(
                        Color.parseColor("#18D4A830"),
                        Color.parseColor("#08D4A810"),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                p.shader = grad
                canvas.drawCircle(cx, cy, width * 0.55f, p)
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            alpha = 0f
        }
        root.addView(ambientGlow)

        // ── 3. Eclipse Logo Image (the actual PNG) ────────────────────────
        val logoSize = dp(260)
        val logoImg  = ImageView(this).apply {
            try {
                val stream = assets.open("eclipse_logo.png")
                setImageBitmap(BitmapFactory.decodeStream(stream))
                stream.close()
            } catch (e: Exception) {
                // fallback: draw logo in code
                setImageDrawable(buildFallbackLogo())
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(logoSize, logoSize, Gravity.CENTER).apply {
                topMargin = -dp(70)
            }
            alpha = 0f
        }
        root.addView(logoImg)

        // ── 4. Bottom text: ECLIPSE ───────────────────────────────────────
        val textWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply { topMargin = dp(100) }
            alpha = 0f
        }

        val taglineTv = TextView(this).apply {
            text = "browse beyond"
            setTextColor(Color.parseColor("#88C8A030"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            letterSpacing = 0.42f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        textWrap.addView(taglineTv)
        root.addView(textWrap)

        // ── 5. Version chip at bottom ─────────────────────────────────────
        val versionTv = TextView(this).apply {
            text = "DAWN  0.1"
            setTextColor(Color.parseColor("#33D4A840"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            letterSpacing = 0.35f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = dp(52) }
            alpha = 0f
        }
        root.addView(versionTv)

        // ── 6. Loading dots at very bottom ────────────────────────────────
        val loadingRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = dp(30) }
            alpha = 0f
        }
        val dotColors = listOf("#55D4A840", "#88D4A840", "#55D4A840")
        val dots = dotColors.map { c ->
            View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(Color.parseColor(c))
                }
                layoutParams = LinearLayout.LayoutParams(dp(4), dp(4)).apply { setMargins(dp(3), 0, dp(3), 0) }
            }
        }
        dots.forEach { loadingRow.addView(it) }
        root.addView(loadingRow)

        setContentView(root)

        // ═══════════════════════════════════════════════════════════════════
        //  ANIMATION SEQUENCE — orchestrated like a cinematic reveal
        // ═══════════════════════════════════════════════════════════════════

        val h = Handler(Looper.getMainLooper())

        // 0ms  — Stars fade in slowly
        h.postDelayed({ fadeIn(starField, 1200) }, 0)

        // 150ms — Ambient glow blooms
        h.postDelayed({ fadeIn(ambientGlow, 800) }, 150)

        // 300ms — Logo scales in from 0.7 with elegant ease
        h.postDelayed({ scaleIn(logoImg, 900, 0.72f) }, 300)

        // 900ms — Tagline fades in
        h.postDelayed({ fadeIn(textWrap, 700) }, 900)

        // 1100ms — Version
        h.postDelayed({ fadeIn(versionTv, 600) }, 1100)

        // 1300ms — Loading dots with stagger
        h.postDelayed({ fadeIn(loadingRow, 400) }, 1300)
        h.postDelayed({ pulseDot(dots[0]) }, 1500)
        h.postDelayed({ pulseDot(dots[1]) }, 1700)
        h.postDelayed({ pulseDot(dots[2]) }, 1900)

        // 1500ms — Logo breathes gently
        h.postDelayed({ breathe(logoImg) }, 1500)

        // 3200ms — Fade out everything → launch
        h.postDelayed({
            fadeOut(root, 600) {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }, 3200)
    }

    // ── Fallback logo drawn in code (if PNG not in assets) ───────────────────
    private fun buildFallbackLogo(): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            val cp = Paint(Paint.ANTI_ALIAS_FLAG)
            val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.parseColor("#D4A840"); alpha = 200 }
            val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 10f; color = Color.parseColor("#FFE878"); alpha = 35 }
            val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            override fun draw(canvas: Canvas) {
                val b = bounds
                val cx = b.exactCenterX(); val cy = b.exactCenterY(); val r = b.width() * 0.37f
                val g = RadialGradient(cx - r*0.15f, cy, r*1.1f,
                    intArrayOf(Color.parseColor("#40FFE080"), Color.parseColor("#10FF9900"), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                cp.shader = g; canvas.drawCircle(cx, cy, r*1.15f, cp)
                canvas.drawCircle(cx, cy, r, gp); canvas.drawCircle(cx, cy, r, rp)
                canvas.drawCircle(cx + r*0.2f, cy, r*0.96f, mp)
            }
            override fun setAlpha(a: Int) {}; override fun setColorFilter(cf: ColorFilter?) {}
            @Deprecated("") override fun getOpacity() = PixelFormat.TRANSLUCENT
        }
    }

    // ── Animation helpers ─────────────────────────────────────────────────────
    private fun fadeIn(v: View, duration: Long) {
        v.alpha = 1f
        v.startAnimation(AlphaAnimation(0f, 1f).apply {
            this.duration = duration; fillAfter = true
            interpolator = AccelerateDecelerateInterpolator()
        })
    }

    private fun fadeOut(v: View, duration: Long, after: () -> Unit) {
        v.startAnimation(AlphaAnimation(1f, 0f).apply {
            this.duration = duration; fillAfter = true
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(a: Animation?) {}
                override fun onAnimationRepeat(a: Animation?) {}
                override fun onAnimationEnd(a: Animation?) { after() }
            })
        })
    }

    private fun scaleIn(v: View, duration: Long, from: Float = 0.6f) {
        v.alpha = 1f
        val set = AnimationSet(true).apply {
            addAnimation(AlphaAnimation(0f, 1f).apply { this.duration = duration; fillAfter = true })
            addAnimation(ScaleAnimation(from, 1f, from, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                this.duration = duration; fillAfter = true
            })
            fillAfter = true
            interpolator = DecelerateInterpolator(1.6f)
        }
        v.startAnimation(set)
    }

    private fun breathe(v: View) {
        ScaleAnimation(1f, 1.04f, 1f, 1.04f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 2000; repeatCount = Animation.INFINITE; repeatMode = Animation.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            v.startAnimation(this)
        }
    }

    private fun pulseDot(v: View) {
        AlphaAnimation(0.3f, 1f).apply {
            duration = 400; repeatCount = Animation.INFINITE; repeatMode = Animation.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            v.startAnimation(this)
        }
    }

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()
}
