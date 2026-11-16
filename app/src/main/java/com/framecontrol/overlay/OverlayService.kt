package com.framecontrol.overlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: CompositionOverlayView? = null
    private var floatingPill: View? = null

    companion object {
        const val ACTION_UPDATE = "com.framecontrol.overlay.ACTION_UPDATE"
        const val ACTION_STOP = "com.framecontrol.overlay.ACTION_STOP"
        const val EXTRA_GRID_ORDINAL = "extra_grid_ordinal"
        const val EXTRA_COLOR = "extra_color"
        const val EXTRA_ALPHA = "extra_alpha"
        const val EXTRA_STROKE = "extra_stroke"
        const val EXTRA_FLIP = "extra_flip"

        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        createFullScreenPassthroughOverlay()
        createFloatingQuickSwitchPill()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                overlayView?.let { view ->
                    val ord = intent.getIntExtra(EXTRA_GRID_ORDINAL, view.gridType.ordinal)
                    view.gridType = GridType.values()[ord]
                    view.lineColor = intent.getIntExtra(EXTRA_COLOR, view.lineColor)
                    view.lineAlpha = intent.getIntExtra(EXTRA_ALPHA, view.lineAlpha)
                    view.strokeWidthDp = intent.getFloatExtra(EXTRA_STROKE, view.strokeWidthDp)
                    view.flipSpiral = intent.getBooleanExtra(EXTRA_FLIP, view.flipSpiral)
                }
            }
        }
        return START_STICKY
    }

    private fun createFullScreenPassthroughOverlay() {
        overlayView = CompositionOverlayView(this)

        // FLAG_NOT_TOUCHABLE + FLAG_NOT_FOCUSABLE allows all touches to pass directly
        // through to the Camera app or any screen underneath (just like Crosshair Pro)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(overlayView, params)
    }

    private fun createFloatingQuickSwitchPill() {
        val density = resources.displayMetrics.density
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (6 * density).toInt(), (12 * density).toInt(), (6 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC141824"))
                cornerRadius = 24 * density
                setStroke((1 * density).toInt(), Color.parseColor("#00E5FF"))
            }
        }

        val label = TextView(this).apply {
            text = "⊞ Next Grid"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 0, (12 * density).toInt(), 0)
        }

        val flipBtn = TextView(this).apply {
            text = "⇄"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 14f
            setPadding(0, 0, (12 * density).toInt(), 0)
            setOnClickListener {
                overlayView?.let { it.flipSpiral = !it.flipSpiral }
            }
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 13f
            setOnClickListener { stopSelf() }
        }

        container.addView(label)
        container.addView(flipBtn)
        container.addView(closeBtn)

        val pillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (24 * density).toInt()
            y = (80 * density).toInt()
        }

        // Drag floating controller or tap to cycle through FrameControl grids while inside Camera app
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        label.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = pillParams.x
                    initialY = pillParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (dx * dx + dy * dy > 25) isClick = false
                    pillParams.x = initialX + dx
                    pillParams.y = initialY + dy
                    windowManager.updateViewLayout(container, pillParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        overlayView?.let { view ->
                            val nextIdx = (view.gridType.ordinal + 1) % GridType.values().size
                            view.gridType = GridType.values()[nextIdx]
                            label.text = "⊞ ${view.gridType.title.take(14)}"
                        }
                    }
                    true
                }
                else -> false
            }
        }

        floatingPill = container
        windowManager.addView(container, pillParams)
    }

    private fun startForegroundNotification() {
        val channelId = "framecontrol_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FrameControl Camera Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FrameControl Overlay Active")
            .setContentText("Showing photography composition guides over your screen")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Overlay", stopPending)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    override fun onDestroy() {
        isRunning = false
        overlayView?.let { windowManager.removeView(it) }
        floatingPill?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
