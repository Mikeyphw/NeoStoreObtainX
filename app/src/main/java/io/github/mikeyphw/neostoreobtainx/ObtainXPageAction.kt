package io.github.mikeyphw.neostoreobtainx

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import java.lang.ref.WeakReference
import java.util.ArrayDeque

/** Source-button-anchored header action. This adds only one module-owned View.
 * Never changes Compose content, host listeners or existing actions. */
internal class ObtainXPageAction {
    private var button: WeakReference<ImageButton>? = null
    private var binding: PageBinding? = null
    private var settingsDialog: AlertDialog? = null
    private var settingsRoute: Any? = null
    private var lastTap = -1000L

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density + 0.5f).toInt()

    private fun pt(activity: Activity) = activity.resources.configuration.locales[0].language == "pt"
    private fun text(activity: Activity, en: String, br: String) = if (pt(activity)) br else en
    private fun enabled(activity: Activity): Boolean = activity.getSharedPreferences(
        "io.github.mikeyphw.neostoreobtainx.ui", Context.MODE_PRIVATE,
    ).getBoolean("show_action", true)

    private fun imeVisible(root: View): Boolean {
        val insets = root.rootWindowInsets ?: return true
        return if (Build.VERSION.SDK_INT >= 30) insets.isVisible(WindowInsets.Type.ime())
        else {
            @Suppress("DEPRECATION")
            val shown = insets.systemWindowInsetBottom > insets.stableInsetBottom
            shown
        }
    }

    fun visible(activity: Activity): Boolean = !activity.isFinishing && !activity.isDestroyed &&
        activity.hasWindowFocus() && !activity.isInPictureInPictureMode

    /** Scan Compose's accessibility *virtual* children for its already-existing
     * SourceCodeButton. Never infer coordinates from screen sizes or app content.
     * No accessibility service, screen capture or host View mutations needed. */
    private fun anchor(activity: Activity, root: FrameLayout): Rect? {
        val nameId = activity.resources.getIdentifier("source_code", "string", TargetPackages.NEO_STORE)
        if (nameId == 0) return null
        val name = activity.getString(nameId)
        val rootScreen = IntArray(2)
        root.getLocationOnScreen(rootScreen)
        val search = ArrayDeque<AccessibilityNodeInfo>()
        val rootNode = root.createAccessibilityNodeInfo() ?: return null
        search.addLast(rootNode)
        var count = 0
        var found: Rect? = null
        try {
            while (search.isNotEmpty() && count++ < 240) {
                val node = search.removeFirst()
                try {
                    val description = node.contentDescription?.toString()
                    if (description == name) {
                        val rect = Rect()
                        node.getBoundsInScreen(rect)
                        // Source control must be a small header icon, not a link
                        // elsewhere in the scrollable app content.
                        if (rect.width() in dp(activity, 18)..dp(activity, 64) &&
                            rect.height() in dp(activity, 18)..dp(activity, 64) &&
                            rect.top >= rootScreen[1] &&
                            rect.bottom <= rootScreen[1] + dp(activity, 210)) {
                            found = rect
                            break
                        }
                    }
                    for (i in 0 until node.childCount.coerceAtMost(50)) {
                        val child = node.getChild(i)
                        if (child != null) search.addLast(child)
                    }
                } finally {
                    @Suppress("DEPRECATION")
                    node.recycle()
                }
            }
        } catch (_: RuntimeException) {
            found = null
        } finally {
            while (search.isNotEmpty()) {
                @Suppress("DEPRECATION")
                search.removeFirst().recycle()
            }
        }
        return found
    }

    /** Guarded Neo Store 1.3.3 header geometry when detached AccessibilityNodeInfo
     * cannot walk Compose virtual descendants. This is a measured layout fallback,
     * NOT a Compose child or an attempt to invoke hidden Android accessibility APIs.
     * Bound to the pinned host release and deliberately disabled on narrow windows. */
    private fun pinnedHeaderSource(activity: Activity, root: FrameLayout): Rect? {
        if (root.width < dp(activity, 365) || root.height < dp(activity, 160)) return null
        val rootScreen = IntArray(2)
        root.getLocationOnScreen(rootScreen)
        val statusTop = if (Build.VERSION.SDK_INT >= 30)
            root.rootWindowInsets?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
        else {
            @Suppress("DEPRECATION")
            root.rootWindowInsets?.stableInsetTop ?: 0
        }
        // Root may already start below the status bar or extend behind it.
        val uncoveredTop = (statusTop - rootScreen[1]).coerceAtLeast(0)
        val cx = if (root.layoutDirection == View.LAYOUT_DIRECTION_RTL)
            rootScreen[0] + dp(activity, 76)
        else rootScreen[0] + root.width - dp(activity, 76)
        val cy = rootScreen[1] + uncoveredTop + dp(activity, 36)
        val half = dp(activity, 20)
        val rect = Rect(cx - half, cy - half, cx + half, cy + half)
        // Never place the anchor outside the visible header/content area.
        return rect.takeIf {
            it.left >= rootScreen[0] && it.right <= rootScreen[0] + root.width &&
            it.top >= rootScreen[1] && it.bottom <= rootScreen[1] + root.height &&
            it.bottom <= rootScreen[1] + uncoveredTop + dp(activity, 90)
        }
    }

    private fun position(activity: Activity, root: FrameLayout, view: ImageButton, source: Rect): Boolean {
        val rootLocation = IntArray(2)
        root.getLocationOnScreen(rootLocation)
        val size = dp(activity, 48)
        val gap = dp(activity, 8)
        // Place immediately before SourceCodeButton in reading direction.
        val rtl = root.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val xScreen = if (rtl) source.right + gap else source.left - gap - size
        val yScreen = source.centerY() - size / 2
        val x = xScreen - rootLocation[0]
        val y = yScreen - rootLocation[1]
        // No bottom floating fallback and no overlap with source or close icons.
        // Reject insufficient header space (for example narrow screens).
        if (x < dp(activity, 66) || y < 0 || x + size > root.width ||
            y + size > root.height || imeVisible(root)) return false
        val params = view.layoutParams as? FrameLayout.LayoutParams ?: return false
        if (params.leftMargin != x || params.topMargin != y) {
            params.leftMargin = x; params.topMargin = y; view.layoutParams = params
        }
        return true
    }

    private fun tonalSurface(activity: Activity): Pair<Int, Int> {
        val night = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        // Android 12+ system neutral palettes track Material You; otherwise a
        // neutral tonal fallback. Never read private host Compose state.
        val neutralId = activity.resources.getIdentifier(
            if (night) "system_neutral2_700" else "system_neutral2_100", "color", "android")
        val surface = if (Build.VERSION.SDK_INT >= 31 && neutralId != 0)
            activity.resources.getColor(neutralId, activity.theme)
        else if (night) Color.rgb(55, 55, 55) else Color.rgb(235, 235, 235)
        return surface to (if (night) Color.WHITE else Color.rgb(35, 35, 35))
    }

    private class ActionIcon(private val color: Int, private val gear: Boolean) : Drawable() {
        private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = this@ActionIcon.color; style = Paint.Style.STROKE; strokeWidth = 2.5f; strokeCap = Paint.Cap.ROUND
        }
        override fun draw(canvas: Canvas) {
            val rect = bounds
            val cx = rect.exactCenterX(); val cy = rect.exactCenterY()
            val scale = minOf(rect.width(), rect.height()) / 24f
            val oldWidth = pen.strokeWidth
            pen.strokeWidth = 2.5f * scale
            if (gear) {
                canvas.drawCircle(cx, cy, 7f * scale, pen)
                canvas.drawCircle(cx, cy, 2.5f * scale, pen)
                for (index in 0..7) {
                    val a = Math.PI * index / 4
                    val x1 = cx + (9 * kotlin.math.cos(a)).toFloat() * scale
                    val y1 = cy + (9 * kotlin.math.sin(a)).toFloat() * scale
                    val x2 = cx + (11 * kotlin.math.cos(a)).toFloat() * scale
                    val y2 = cy + (11 * kotlin.math.sin(a)).toFloat() * scale
                    canvas.drawLine(x1,y1,x2,y2,pen)
                }
            } else {
                canvas.drawCircle(cx, cy, 9f * scale, pen)
                canvas.drawLine(cx - 5f * scale, cy, cx + 5f * scale, cy, pen)
                canvas.drawLine(cx, cy - 5f * scale, cx, cy + 5f * scale, pen)
            }
            pen.strokeWidth = oldWidth
        }
        override fun setAlpha(alpha: Int) { pen.alpha = alpha }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { pen.colorFilter = colorFilter }
        @Deprecated("Required for Drawable") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        override fun getIntrinsicWidth(): Int = 24
        override fun getIntrinsicHeight(): Int = 24
    }

    fun render(activity: Activity, page: PageBinding?, onTap: (PageBinding) -> Unit,
               onAnchor: (String) -> Unit = {}) {
        if (settingsDialog != null && (settingsRoute !== page?.route || !ActionPolicy.ready(page))) {
            settingsDialog?.dismiss(); settingsDialog = null; settingsRoute = null
        }
        if (!visible(activity) || !ActionPolicy.ready(page)) { removeButton(); return }
        val root = activity.findViewById<View>(android.R.id.content) as? FrameLayout
        if (root == null || !root.isShown || root.width < dp(activity, 230) ||
            root.height < dp(activity, 160) || imeVisible(root)) {
            removeButton(); onAnchor("RENDER_CONDITIONS"); return
        }
        val semanticSource = try { anchor(activity, root) } catch (_: RuntimeException) { null }
        val source = semanticSource ?: pinnedHeaderSource(activity, root)
        if (source == null) { removeButton(); onAnchor("NO_SAFE_HEADER_POSITION"); return }
        val anchorKind = if (semanticSource != null) "SEMANTIC" else "PINNED_HEADER_FALLBACK"
        val showAction = enabled(activity)
        val existing = button?.get()
        if (existing != null && existing.parent === root && binding?.route === page?.route &&
            binding?.snapshot == page?.snapshot && existing.tag == showAction) {
            if (!position(activity, root, existing, source)) {
                removeButton(); onAnchor("NO_ROOM")
            } else onAnchor(anchorKind)
            return
        }
        removeButton()
        val currentPage = page ?: return
        val (surface, foreground) = tonalSurface(activity)
        val view = ImageButton(activity).apply {
            tag = showAction
            contentDescription = if (showAction) this@ObtainXPageAction.text(activity,
                "Add to ObtainX. Long press for settings.", "Adicionar ao ObtainX. Toque e segure para configurações.")
            else this@ObtainXPageAction.text(activity,
                "ObtainX action settings", "Configurações da ação ObtainX")
            setImageDrawable(ActionIcon(foreground, !showAction))
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12))
            backgroundTintList = null
            val shape = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(surface) }
            background = RippleDrawable(ColorStateList.valueOf(if (foreground == Color.WHITE) 0x33ffffff else 0x33000000),shape,null)
            minimumHeight = dp(activity, 48)
            minimumWidth = dp(activity, 48)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            isFocusable = true
        }
        val weakActivity = WeakReference(activity)
        view.setOnClickListener {
            val host = weakActivity.get() ?: return@setOnClickListener
            if (!visible(host) || button?.get() !== view || !view.isAttachedToWindow ||
                imeVisible(view) || !view.isShown) return@setOnClickListener
            if (!showAction) { settings(host, currentPage.route); return@setOnClickListener }
            if (!enabled(host)) return@setOnClickListener
            val now = SystemClock.elapsedRealtime()
            if (now - lastTap < 1000L) return@setOnClickListener
            lastTap = now
            onTap(currentPage)
        }
        view.setOnLongClickListener {
            val host = weakActivity.get() ?: return@setOnLongClickListener false
            if (!visible(host) || button?.get() !== view) return@setOnLongClickListener false
            settings(host, currentPage.route); true
        }
        binding = currentPage
        button = WeakReference(view)
        root.addView(view, FrameLayout.LayoutParams(dp(activity, 48),dp(activity, 48),Gravity.TOP or Gravity.LEFT))
        if (!position(activity,root,view,source)) {
            removeButton(); onAnchor("NO_ROOM")
        } else onAnchor(anchorKind)
    }

    private fun settings(activity: Activity, route: Any) {
        if (settingsDialog?.isShowing == true) return
        val checked = enabled(activity)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(text(activity, "ObtainX action settings", "Configurações da ação ObtainX"))
            .setSingleChoiceItems(arrayOf(text(activity, "Show Add to ObtainX", "Mostrar Adicionar ao ObtainX"),
                text(activity, "Show settings icon only", "Mostrar apenas o ícone de configurações")),
                if (checked) 0 else 1) { d, which ->
                activity.getSharedPreferences("io.github.mikeyphw.neostoreobtainx.ui", Context.MODE_PRIVATE)
                    .edit().putBoolean("show_action", which == 0).apply()
                removeButton()
                d.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null).create()
        settingsDialog = dialog
        settingsRoute = route
        dialog.setOnDismissListener {
            if (settingsDialog === dialog) { settingsDialog = null; settingsRoute = null }
        }
        dialog.show()
    }

    fun result(activity: Activity, result: BridgeOutcome) {
        if (result is BridgeOutcome.Refused) {
            val message = when (result.reason) {
                BridgeFailure.STALE_OR_UNSAFE_SOURCE -> text(activity, "App or source changed. Try again.", "O aplicativo ou a fonte mudou. Tente novamente.")
                BridgeFailure.NO_EXPORTED_TARGET -> text(activity, "ObtainX is unavailable.", "ObtainX indisponível.")
                BridgeFailure.LAUNCH_REJECTED -> text(activity, "Could not open ObtainX.", "Não foi possível abrir o ObtainX.")
            }
            Toast.makeText(activity,message,Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeButton() {
        button?.get()?.let { own ->
            own.setOnClickListener(null); own.setOnLongClickListener(null)
            (own.parent as? ViewGroup)?.removeView(own)
        }
        button = null; binding = null
    }

    fun clear() {
        removeButton()
        settingsDialog?.dismiss(); settingsDialog = null; settingsRoute = null
    }
}
