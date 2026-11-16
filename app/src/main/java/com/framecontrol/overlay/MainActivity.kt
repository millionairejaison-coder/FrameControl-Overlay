package com.framecontrol.overlay

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var selectedGrid = GridType.RULE_OF_THIRDS
    private var selectedColor = Color.parseColor("#00E5FF")
    private var selectedAlpha = 200
    private var selectedStroke = 1.8f
    private var flipSpiral = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(this).apply {
            text = "FrameControl Overlay Pro"
            textSize = 24f
            setTextColor(Color.WHITE)
        }
        val subtitle = TextView(this).apply {
            text = "System-wide photography composition & line overlays for any Android Camera app."
            textSize = 14f
            setTextColor(Color.parseColor("#8B949E"))
            setPadding(0, (4 * density).toInt(), 0, (20 * density).toInt())
        }

        val toggleBtn = Button(this).apply {
            text = "START OVERLAY ON SCREEN"
            setBackgroundColor(Color.parseColor("#00E5FF"))
            setTextColor(Color.BLACK)
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(
                        this@MainActivity,
                        "Please grant 'Display over other apps' permission first",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    if (OverlayService.isRunning) {
                        stopService(Intent(this@MainActivity, OverlayService::class.java))
                        text = "START OVERLAY ON SCREEN"
                    } else {
                        pushOverlayUpdate(startIfNotRunning = true)
                        text = "STOP OVERLAY"
                    }
                }
            }
        }

        val gridSectionTitle = TextView(this).apply {
            text = "1. Select FrameControl Composition Guide"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, (20 * density).toInt(), 0, (8 * density).toInt())
        }

        val radioGroup = RadioGroup(this)
        GridType.values().forEachIndexed { index, grid ->
            val rb = RadioButton(this).apply {
                id = index
                text = "${grid.title}  (${grid.category})"
                setTextColor(Color.parseColor("#E6EDF3"))
                isChecked = (grid == selectedGrid)
                setOnClickListener {
                    selectedGrid = grid
                    pushOverlayUpdate()
                }
            }
            radioGroup.addView(rb)
        }

        val flipCheck = CheckBox(this).apply {
            text = "Flip / Mirror Orientation (Spiral, S-Curve, Dutch Tilt)"
            setTextColor(Color.parseColor("#8B949E"))
            setOnCheckedChangeListener { _, isChecked ->
                flipSpiral = isChecked
                pushOverlayUpdate()
            }
        }

        val colorTitle = TextView(this).apply {
            text = "2. Overlay Line Color"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, (16 * density).toInt(), 0, (8 * density).toInt())
        }

        val colorRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val colors = listOf(
            "Cyan" to "#00E5FF",
            "Gold" to "#FFD600",
            "Green" to "#00E676",
            "Red" to "#FF5252",
            "White" to "#FFFFFF"
        )

        colors.forEach { (name, hex) ->
            val btn = Button(this).apply {
                text = name
                textSize = 11f
                setBackgroundColor(Color.parseColor(hex))
                setTextColor(Color.BLACK)
                setOnClickListener {
                    selectedColor = Color.parseColor(hex)
                    pushOverlayUpdate()
                }
            }
            colorRow.addView(btn)
        }

        val alphaTitle = TextView(this).apply {
            text = "3. Line Opacity & Stroke Width"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, (16 * density).toInt(), 0, (8 * density).toInt())
        }

        val alphaSeek = SeekBar(this).apply {
            max = 255
            progress = selectedAlpha
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedAlpha = progress.coerceAtLeast(30)
                    pushOverlayUpdate()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(toggleBtn)
        container.addView(gridSectionTitle)
        container.addView(radioGroup)
        container.addView(flipCheck)
        container.addView(colorTitle)
        container.addView(colorRow)
        container.addView(alphaTitle)
        container.addView(alphaSeek)

        rootScroll.addView(container)
        setContentView(rootScroll)
    }

    private fun pushOverlayUpdate(startIfNotRunning: Boolean = false) {
        if (!OverlayService.isRunning && !startIfNotRunning) return
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_UPDATE
            putExtra(OverlayService.EXTRA_GRID_ORDINAL, selectedGrid.ordinal)
            putExtra(OverlayService.EXTRA_COLOR, selectedColor)
            putExtra(OverlayService.EXTRA_ALPHA, selectedAlpha)
            putExtra(OverlayService.EXTRA_STROKE, selectedStroke)
            putExtra(OverlayService.EXTRA_FLIP, flipSpiral)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
