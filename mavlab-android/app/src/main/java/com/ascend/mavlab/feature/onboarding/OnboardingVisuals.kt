package com.ascend.mavlab.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.mavlab.core.ui.components.TelemetryCard
import com.ascend.mavlab.feature.controller.TiltVisualizer
import com.ascend.mavlab.feature.drone3d.AltitudeInstrument

// Shared palette, kept in sync with core.ui.theme.MavLabColorScheme so the hand-drawn
// canvases match the Material surfaces around them.
private val Blue = Color(0xFF60A5FA)
private val Green = Color(0xFF34D399)
private val Amber = Color(0xFFFBBF24)
private val Slate = Color(0xFF94A3B8)
private val CardBg = Color(0xFF141B2D)
private val CardBorder = Color(0xFF1E2740)
private val Mono = FontFamily.Monospace

/** Renders the app-accurate visual for an onboarding page below its copy. */
@Composable
fun OnboardingVisualContent(
    visual: OnboardingVisual,
    onOpenQGroundControl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (visual) {
            OnboardingVisual.DroneHero -> DroneHeroVisual()
            OnboardingVisual.PfdTelemetry -> PfdTelemetryVisual()
            OnboardingVisual.SurfaceGallery -> SurfaceGalleryVisual()
            OnboardingVisual.QgcLink -> QgcLinkVisual(onOpenQGroundControl)
            OnboardingVisual.ArmTakeoffLand -> ArmTakeoffLandVisual()
            OnboardingVisual.TiltPad -> TiltPadVisual()
            OnboardingVisual.MissionPlot -> MissionPlotVisual()
            OnboardingVisual.FailurePanel -> FailurePanelVisual()
            OnboardingVisual.TelemetryChart -> TelemetryChartVisual()
        }
    }
}

// ---------------------------------------------------------------------------
// 1 · Drone hero — stylized quadcopter hovering over a radar pad on a grid floor.
// ---------------------------------------------------------------------------
@Composable
private fun DroneHeroVisual() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        val cx = size.width / 2f
        val droneY = size.height * 0.30f
        drawDroneGlyph(center = Offset(cx, droneY), scale = size.width / 330f, glow = Blue)

        // Perspective grid floor.
        val horizonY = size.height * 0.60f
        val floorY = size.height
        val gridColor = Color.White.copy(alpha = 0.06f)
        // Converging verticals toward a vanishing point.
        val vanish = Offset(cx, horizonY)
        for (i in -4..4) {
            val bx = cx + i * (size.width / 8f)
            drawLine(gridColor, start = Offset(bx, floorY), end = vanish, strokeWidth = 1.5f)
        }
        // Horizontal bands, denser toward the horizon.
        for (i in 1..5) {
            val t = i / 5f
            val y = horizonY + (floorY - horizonY) * (t * t)
            drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.5f)
        }

        // Radar rings + center blip on the pad.
        val padY = horizonY + (floorY - horizonY) * 0.42f
        val padCenter = Offset(cx, padY)
        drawOval(
            color = Blue,
            topLeft = Offset(cx - 120f, padY - 42f),
            size = Size(240f, 84f),
            style = Stroke(width = 3f),
        )
        drawOval(
            color = Blue.copy(alpha = 0.5f),
            topLeft = Offset(cx - 62f, padY - 22f),
            size = Size(124f, 44f),
            style = Stroke(width = 2f),
        )
        drawCircle(color = Blue.copy(alpha = 0.25f), center = padCenter, radius = 20f)
        drawCircle(color = Blue, center = padCenter, radius = 9f)
    }
}

// ---------------------------------------------------------------------------
// 2 · PFD + telemetry — the real attitude instrument plus live telemetry cards.
// ---------------------------------------------------------------------------
@Composable
private fun PfdTelemetryVisual() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AltitudeInstrument(
            altitudeMeters = 12.4f,
            verticalSpeedMetersPerSecond = 0.35f,
            yawRadians = 0.21f,
            rollRadians = -0.12f,
            pitchRadians = 0.06f,
            armed = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryCard(label = "BATTERY", value = "98% Normal", accent = false, modifier = Modifier.weight(1f))
            TelemetryCard(label = "GPS", value = "Locked · 12 sats", accent = true, modifier = Modifier.weight(1f))
            TelemetryCard(label = "MODE", value = "ALT_HOLD", accent = false, modifier = Modifier.weight(1f))
        }
        DotSeparatedRow(listOf("Physics", "Sensors", "Telemetry"))
    }
}

// ---------------------------------------------------------------------------
// 3 · Surface gallery — the five app surfaces as a scrollable card rail.
// ---------------------------------------------------------------------------
private data class Surface(val name: String, val blurb: String, val icon: ImageVector, val tint: Color)

@Composable
private fun SurfaceGalleryVisual() {
    val surfaces = listOf(
        Surface("Cockpit", "Telemetry, safety state, mission awareness", Icons.Default.Analytics, Blue),
        Surface("Controller", "Phone sensors, manual input, quick test", Icons.Default.ControlCamera, Blue),
        Surface("SIM", "3D attitude, altitude, motors", Icons.Default.Height, Green),
        Surface("Mission", "QGC upload, waypoints, AUTO", Icons.Default.Route, Amber),
        Surface("Ops", "MAVLink status, logs, export", Icons.Default.Settings, Slate),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            surfaces.forEach { surface ->
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .height(196.dp)
                        .background(CardBg, RoundedCornerShape(20.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(surface.tint.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(surface.icon, contentDescription = null, tint = surface.tint, modifier = Modifier.size(24.dp))
                    }
                    Text(surface.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E8F0))
                    Text(surface.blurb, fontSize = 13.sp, color = Slate)
                }
            }
        }
        // Position dots (first active).
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(surfaces.size) { index ->
                Box(
                    modifier = Modifier
                        .height(7.dp)
                        .width(if (index == 0) 20.dp else 7.dp)
                        .background(if (index == 0) Blue else CardBorder, CircleShape),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4 · QGC link — phone ⇢ MAVLink arc ⇢ ground station, with a live-link chip.
// ---------------------------------------------------------------------------
// Exact port of the Pencil "Diagram" (Cur+4): design space is 312×170 units, mapped
// 1:1 onto the canvas via aspectRatio. A phone running MAVLab and a monitor showing a
// GCS map, linked by a MAVLink-UDP arc.
@Composable
private fun QgcLinkVisual(onOpenQGroundControl: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(312f / 170f),
        ) {
            val s = size.width / 312f
            fun p(v: Float) = v * s
            fun corner(r: Float) = androidx.compose.ui.geometry.CornerRadius(p(r), p(r))
            fun rr(x: Float, y: Float, w: Float, h: Float, r: Float, color: Color) =
                drawRoundRect(color, Offset(p(x), p(y)), Size(p(w), p(h)), corner(r))
            fun ov(x: Float, y: Float, w: Float, h: Float, color: Color) =
                drawOval(color, Offset(p(x), p(y)), Size(p(w), p(h)))
            val canvas = drawContext.canvas.nativeCanvas
            fun blurOval(x: Float, y: Float, w: Float, h: Float, argb: Int, blur: Float) {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = argb
                    maskFilter = android.graphics.BlurMaskFilter((blur * s).coerceAtLeast(0.5f), android.graphics.BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawOval(p(x), p(y), p(x + w), p(y + h), paint)
            }

            val bezel = Color(0xFF3B4A66)
            val screenDark = Color(0xFF0B1120)
            fun deviceGradient(x: Float, y: Float, w: Float, h: Float) =
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF1E2A44), Color(0xFF0E1524)),
                    start = Offset(p(x), p(y)),
                    end = Offset(p(x + w), p(y + h)),
                )

            // Soft device glows (behind everything).
            blurOval(10f, 48f, 80f, 70f, Color(0x1F60A5FA).toArgb(), 16f)
            blurOval(186f, 40f, 128f, 76f, Color(0x1F60A5FA).toArgb(), 16f)

            // Phone.
            drawRoundRect(deviceGradient(24f, 26f, 52f, 104f), Offset(p(24f), p(26f)), Size(p(52f), p(104f)), corner(14f))
            drawRoundRect(bezel, Offset(p(24f), p(26f)), Size(p(52f), p(104f)), corner(14f), style = Stroke(1.5f * s))
            rr(41f, 30f, 18f, 4f, 2f, Color(0xFF33415A)) // notch
            rr(28f, 36f, 44f, 84f, 10f, screenDark) // screen
            ov(44f, 56f, 12f, 8f, Blue) // on-screen vehicle marker
            rr(34f, 98f, 32f, 4f, 2f, Color(0xFF1E3A5F)) // telemetry bar 1
            rr(34f, 106f, 20f, 4f, 2f, Color(0xFF33415A)) // telemetry bar 2
            rr(75.5f, 54f, 2.5f, 16f, 1f, Color(0xFF33415A)) // side button

            // Monitor (GCS map view).
            drawRoundRect(deviceGradient(196f, 28f, 108f, 72f), Offset(p(196f), p(28f)), Size(p(108f), p(72f)), corner(10f))
            drawRoundRect(bezel, Offset(p(196f), p(28f)), Size(p(108f), p(72f)), corner(10f), style = Stroke(1.5f * s))
            rr(202f, 34f, 96f, 58f, 6f, screenDark) // screen
            drawRect(CardBorder, Offset(p(202f), p(45f)), Size(p(96f), p(1f))) // toolbar divider
            // Waypoint route.
            drawLine(Amber, Offset(p(214f), p(84f)), Offset(p(244f), p(66f)), strokeWidth = 1.5f * s, cap = StrokeCap.Round)
            drawLine(Amber, Offset(p(244f), p(66f)), Offset(p(286f), p(48f)), strokeWidth = 1.5f * s, cap = StrokeCap.Round)
            ov(211f, 81f, 6f, 6f, Green)
            ov(241f, 63f, 6f, 6f, Blue)
            ov(283f, 45f, 6f, 6f, Slate)
            drawRect(Color(0xFF33415A), Offset(p(243f), p(100f)), Size(p(14f), p(12f))) // stand
            rr(228f, 112f, 44f, 5f, 3f, Color(0xFF33415A)) // base

            // MAVLink-UDP arc of dots (exact Pencil coordinates).
            val arc = listOf(
                74f to 60f, 89.75f to 43.16f, 105.5f to 28.89f, 121.25f to 19.35f, 137f to 16f,
                152.75f to 19.35f, 168.5f to 28.89f, 184.25f to 43.16f, 200f to 60f,
            )
            arc.forEach { (x, y) -> ov(x, y, 5f, 5f, Blue.copy(alpha = 0.9f)) }

            // Labels (drawn in-canvas so they track the diagram at any width).
            val monoPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = p(11f)
                color = Blue.toArgb()
            }
            canvas.drawText("MAVLink UDP", p(137f), p(12f), monoPaint)
            val lblPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = p(12f)
                color = Slate.toArgb()
            }
            canvas.drawText("Phone", p(50f), p(150f), lblPaint)
            canvas.drawText("GCS", p(250f), p(150f), lblPaint)
        }
        // Live-link chip.
        Row(
            modifier = Modifier
                .background(CardBg, CircleShape)
                .border(1.dp, CardBorder, CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(Green, CircleShape))
            Text("GCS linked", color = Color(0xFFE2E8F0), fontFamily = Mono, fontSize = 15.sp)
        }
        Text("UDP :14550 · GCS 255 · MAVLab 1", color = Slate, fontFamily = Mono, fontSize = 13.sp)
        OutlinedButton(onClick = onOpenQGroundControl, modifier = Modifier.fillMaxWidth()) {
            Text("Open QGroundControl")
        }
    }
}

// ---------------------------------------------------------------------------
// 5 · Arm / takeoff / land — hovering drone with an altitude chip and controls.
// ---------------------------------------------------------------------------
@Composable
private fun ArmTakeoffLandVisual() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val cx = size.width * 0.42f
                drawDroneGlyph(center = Offset(cx, size.height * 0.30f), scale = size.width / 330f, glow = Blue)
                // Descent trail dots + fading ground shadow.
                for (i in 1..3) {
                    val y = size.height * (0.30f + 0.18f * i)
                    drawCircle(Blue.copy(alpha = 0.6f - i * 0.15f), radius = 5f, center = Offset(cx, y))
                }
                drawOval(
                    color = Blue.copy(alpha = 0.10f),
                    topLeft = Offset(cx - 80f, size.height * 0.86f),
                    size = Size(160f, 30f),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp)
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("ALT", color = Slate, fontFamily = Mono, fontSize = 13.sp)
                Text("3.2 m", color = Color(0xFFE2E8F0), fontFamily = Mono, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillChip("Arm", selected = true)
            PillChip("Takeoff", selected = false)
            PillChip("Land", selected = false)
        }
        DotSeparatedRow(listOf("MANUAL", "STABILIZE", "Disarmed"), mono = true)
    }
}

// ---------------------------------------------------------------------------
// 6 · Tilt pad — the real tilt crosshair plus throttle / yaw-trim sliders.
// ---------------------------------------------------------------------------
@Composable
private fun TiltPadVisual() {
    var throttle by remember { mutableFloatStateOf(0.42f) }
    var yawTrim by remember { mutableFloatStateOf(0f) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.width(220.dp)) {
            TiltVisualizer(
                rollNormalized = 0.42f,
                pitchNormalized = 0.28f,
                deadzoneRadius = 0.12f,
            )
        }
        Text("Roll 6.2° · Pitch -3.1° · Yaw 0.0°", color = Color(0xFFE2E8F0), fontFamily = Mono, fontSize = 15.sp)
        ControlSlider(
            label = "Throttle",
            value = throttle,
            valueLabel = " ${(throttle * 100).toInt()}%",
            onValueChange = { throttle = it },
        )
        ControlSlider(
            label = "Yaw trim",
            value = yawTrim,
            valueLabel = "%+.2f".format(yawTrim),
            min = -1f,
            max = 1f,
            onValueChange = { yawTrim = it },
        )
    }
}

// Exact copy of feature/controller's ControlSlider: a Material3 Slider with a
// labelLarge label/value row. Interactive so it behaves like the live Controller.
@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    valueLabel: String,
    min: Float = 0f,
    max: Float = 1f,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
        )
    }
}

// ---------------------------------------------------------------------------
// 7 · Mission plot — a diagonal waypoint route with reached/active/queued states.
// ---------------------------------------------------------------------------
@Composable
private fun MissionPlotVisual() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(CardBg, RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(300.dp).padding(20.dp)) {
                val p1 = Offset(size.width * 0.18f, size.height * 0.84f)
                val p2 = Offset(size.width * 0.46f, size.height * 0.52f)
                val p3 = Offset(size.width * 0.80f, size.height * 0.22f)
                // Dotted route line.
                val dots = 22
                for (i in 0..dots) {
                    val t = i / dots.toFloat()
                    val p = if (t < 0.5f) lerp(p1, p2, t * 2f) else lerp(p2, p3, (t - 0.5f) * 2f)
                    drawCircle(Amber, radius = 3f, center = p)
                }
                drawWaypoint(p1, Green, filled = true)
                drawWaypoint(p2, Blue, filled = true)
                drawWaypoint(p3, Slate, filled = false)

                val numPaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 26f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val labelPaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.LEFT
                    textSize = 26f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                val canvas = drawContext.canvas.nativeCanvas
                numPaint.color = Color(0xFF061A12).toArgb()
                canvas.drawText("1", p1.x, p1.y + 9f, numPaint)
                numPaint.color = Color(0xFF081526).toArgb()
                canvas.drawText("2", p2.x, p2.y + 9f, numPaint)
                numPaint.color = Slate.toArgb()
                canvas.drawText("3", p3.x, p3.y + 9f, numPaint)

                labelPaint.color = Green.toArgb()
                canvas.drawText("Reached", p1.x + 34f, p1.y + 9f, labelPaint)
                labelPaint.color = Blue.toArgb()
                canvas.drawText("Active", p2.x + 34f, p2.y + 9f, labelPaint)
                labelPaint.color = Slate.toArgb()
                canvas.drawText("Queued", p3.x + 34f, p3.y - 30f, labelPaint)
            }
            // AUTO badge.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .border(1.dp, Blue, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.size(7.dp).background(Blue, CircleShape))
                Text("AUTO", color = Blue, fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "Fly to WP2 · 2/5 reached · AUTO",
            color = Slate,
            fontFamily = Mono,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------------
// 8 · Failure panel — perturbation preview plus fault-injection controls.
// ---------------------------------------------------------------------------
@Composable
private fun FailurePanelVisual() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(CardBg, RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
                val s = size.width / 312f
                // Drone, tilted 8° like the pen FailDrone (ciMMK rotation:8).
                val droneCenter = Offset(size.width * 0.52f, size.height * 0.48f)
                rotate(degrees = -8f, pivot = droneCenter) {
                    drawDroneGlyph(center = droneCenter, scale = size.width / 420f, glow = Blue)
                }
                // GPS-loss drift: four graduated red dashes on the left (pen Drift0..3).
                val dashY = size.height * 0.64f
                listOf(0.35f, 0.48f, 0.61f, 0.74f).forEachIndexed { i, alpha ->
                    drawRoundRect(
                        color = Color(0xFFEF4444).copy(alpha = alpha),
                        topLeft = Offset((40f + i * 14f) * s, dashY),
                        size = Size(8f * s, 2.5f * s),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * s, 1.5f * s),
                    )
                }
            }
            // Wind icon top-right (pen uses lucide "wind"; Material "Air" is its twin).
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = null,
                tint = Slate.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 26.dp)
                    .size(30.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text("Motor failure", color = Color(0xFFE2E8F0), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PillChip("Motor 1", selected = false, modifier = Modifier.weight(1f))
                PillChip("Motor 2", selected = true, check = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PillChip("Motor 3", selected = false, modifier = Modifier.weight(1f))
                PillChip("Motor 4", selected = false, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("GPS available", color = Color(0xFFE2E8F0), fontSize = 15.sp)
                Switch(checked = false, onCheckedChange = null)
            }
            Text("Wind preset", color = Color(0xFFE2E8F0), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillChip("None", selected = false)
                PillChip("Light", selected = false)
                PillChip("Strong", selected = true, check = true)
            }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Reset perturbations") }
        }
    }
}

// ---------------------------------------------------------------------------
// 9 · Telemetry log panel — exact port of the pen "LogPanel" (YbMHy): a mono
// header + report chip, three colored line paths over a mid axis, a legend, and
// a caption, all in one card. Chart geometry is the pen's exact polylines.
// ---------------------------------------------------------------------------
@Composable
private fun TelemetryChartVisual() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: mono "Telemetry" label + report.md chip.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Telemetry",
                fontFamily = Mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = Slate,
            )
            Row(
                modifier = Modifier
                    .background(CardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = Blue, modifier = Modifier.size(13.dp))
                Text("report.md", fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFCBD5E1))
            }
        }
        // Chart: mid axis + three colored line paths (exact pen 276×90 polylines).
        val altitudeDy = List(27) { if (it < 12) -5.5f else 0f }
        val rollDy = listOf(
            11.6f, 6.1f, -2.2f, -9.5f, -12.3f, -9.4f, -2f, 6.3f, 11.7f, 11.5f, 6f, -2.4f, -9.7f, -12.3f,
            -9.2f, -1.8f, 6.5f, 11.7f, 11.5f, 5.7f, -2.6f, -9.7f, -12.4f, -9.1f, -1.6f, 6.7f, 11.8f,
        )
        val pitchDy = listOf(
            1.6f, -0.9f, -3.1f, -4.6f, -4.9f, -4.1f, -2.2f, 0.2f, 2.5f, 4.3f, 5f, 4.4f, 2.8f, 0.5f,
            -1.9f, -3.9f, -4.9f, -4.6f, -3.4f, -1.2f, 1.2f, 3.4f, 4.7f, 4.9f, 3.8f, 1.9f, -0.6f,
        )
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(276f / 90f)) {
            val s = size.height / 90f
            fun pathOf(startY: Float, dys: List<Float>): Path {
                val path = Path()
                val stepX = size.width / dys.size
                var y = startY
                path.moveTo(0f, y * s)
                dys.forEachIndexed { i, dy ->
                    y += dy
                    path.lineTo((i + 1) * stepX, y * s)
                }
                return path
            }
            drawLine(Color(0x29FFFFFF), Offset(0f, 44f * s), Offset(size.width, 44f * s), strokeWidth = 1f * s)
            val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(pathOf(78f, altitudeDy), color = Blue, style = stroke)
            drawPath(pathOf(45f, rollDy), color = Green, style = stroke)
            drawPath(pathOf(53.4f, pitchDy), color = Amber, style = stroke)
        }
        // Legend.
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendItem(Blue, "Altitude")
            LegendItem(Green, "Roll")
            LegendItem(Amber, "Pitch")
        }
        Text("peak 10.0 m · 1m 48s", fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Slate)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(12.dp).height(2.dp).background(color, CircleShape))
        Text(label, fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Slate)
    }
}

// ---------------------------------------------------------------------------
// Shared building blocks.
// ---------------------------------------------------------------------------
@Composable
private fun PillChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    check: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(if (selected) Blue.copy(alpha = 0.16f) else Color.Transparent, CircleShape)
            .border(1.dp, if (selected) Blue else CardBorder, CircleShape)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (check) Icon(Icons.Default.Check, contentDescription = null, tint = Blue, modifier = Modifier.size(16.dp))
        Text(text, color = if (selected) Color(0xFFE2E8F0) else Slate, fontSize = 15.sp)
    }
}

@Composable
private fun DotSeparatedRow(items: List<String>, mono: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, item ->
            if (index > 0) Box(modifier = Modifier.size(4.dp).background(Green, CircleShape))
            Text(item, color = Slate, fontSize = 15.sp, fontFamily = if (mono) Mono else FontFamily.Default)
        }
    }
}

// ---------------------------------------------------------------------------
// Canvas helpers.
// ---------------------------------------------------------------------------
// Exact reproduction of the Pencil "Drone Small" component (ciMMK, 132×90 design units),
// anchored at the frame centre (66,45). `scale` = canvas pixels per design unit.
private fun DrawScope.drawDroneGlyph(center: Offset, scale: Float, glow: Color) {
    val s = scale
    fun tx(dx: Float) = center.x + (dx - 66f) * s
    fun ty(dy: Float) = center.y + (dy - 45f) * s
    val canvas = drawContext.canvas.nativeCanvas

    // Native blurred fill — used for the under-glow and the spinning-prop discs.
    fun blurOval(cxD: Float, cyD: Float, wD: Float, hD: Float, argb: Int, blurD: Float) {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = argb
            maskFilter = android.graphics.BlurMaskFilter(
                (blurD * s).coerceAtLeast(0.5f),
                android.graphics.BlurMaskFilter.Blur.NORMAL,
            )
        }
        canvas.drawOval(tx(cxD - wD / 2f), ty(cyD - hD / 2f), tx(cxD + wD / 2f), ty(cyD + hD / 2f), paint)
    }

    // 1 · under-glow (#60A5FA30, blur 10)
    blurOval(66f, 72f, 84f, 28f, Color(0x3060A5FA).toArgb(), 10f)

    // 2 · arms — two crossing diagonals (path "M35 23 l79 29 m-17 -29 l-79 29")
    val armColor = Color(0xFF2A3550)
    drawLine(armColor, Offset(tx(35f), ty(23f)), Offset(tx(114f), ty(52f)), strokeWidth = 4f * s, cap = StrokeCap.Round)
    drawLine(armColor, Offset(tx(97f), ty(23f)), Offset(tx(18f), ty(52f)), strokeWidth = 4f * s, cap = StrokeCap.Round)

    // 3 · motors — flat dark ellipses (12×8) with a rim
    val motorFill = Color(0xFF0E1524)
    val motorRim = Color(0xFF33415A)
    listOf(35f to 21f, 97f to 21f, 18f to 51f, 114f to 51f).forEach { (mx, my) ->
        val tl = Offset(tx(mx - 6f), ty(my - 4f))
        val sz = Size(12f * s, 8f * s)
        drawOval(motorFill, tl, sz)
        drawOval(motorRim, tl, sz, style = Stroke(width = (1f * s).coerceAtLeast(1f)))
    }

    // 4 · body — gradient rounded rect (#26314E→#151D31) with rim
    val bodyTL = Offset(tx(46f), ty(20f))
    val bodySize = Size(41f * s, 29f * s)
    val bodyCorner = androidx.compose.ui.geometry.CornerRadius(10f * s, 10f * s)
    drawRoundRect(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(Color(0xFF26314E), Color(0xFF151D31)),
            start = Offset(tx(46f), ty(20f)),
            end = Offset(tx(87f), ty(49f)),
        ),
        topLeft = bodyTL,
        size = bodySize,
        cornerRadius = bodyCorner,
    )
    drawRoundRect(
        color = Color(0xFF3B4A66),
        topLeft = bodyTL,
        size = bodySize,
        cornerRadius = bodyCorner,
        style = Stroke(width = 1.5f * s),
    )

    // 5 · canopy (#31405F, 24×11)
    drawRoundRect(
        color = Color(0xFF31405F),
        topLeft = Offset(tx(54f), ty(24f)),
        size = Size(24f * s, 11f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * s, 6f * s),
    )

    // 6 · nav light (#60A5FA, 11×7)
    drawOval(glow, Offset(tx(61f), ty(43f)), Size(11f * s, 7f * s))

    // 7 · prop discs — translucent, blurred, drawn on top (#60A5FA2B, blur 3)
    val propArgb = Color(0x2B60A5FA).toArgb()
    listOf(34.5f to 20f, 97.5f to 20f, 18.5f to 50f, 114.5f to 50f).forEach { (px, py) ->
        blurOval(px, py, 53f, 18f, propArgb, 3f)
    }
}

private fun DrawScope.drawWaypoint(center: Offset, color: Color, filled: Boolean) {
    val r = 20f
    if (filled) {
        drawCircle(color, radius = r, center = center)
    } else {
        drawCircle(color, radius = r, center = center, style = Stroke(width = 3f))
    }
}

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
