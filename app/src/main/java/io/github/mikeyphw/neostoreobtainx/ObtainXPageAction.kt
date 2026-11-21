package io.github.mikeyphw.neostoreobtainx

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import java.lang.ref.WeakReference

/** Only the module's own small native View is added or removed; no host children are altered. */
internal class ObtainXPageAction {
    private var button: WeakReference<Button>? = null
    private var binding: PageBinding? = null
    private var settingsDialog: AlertDialog? = null
    private var settingsRoute: Any? = null
    private var lastTap = -1000L
    private val margin = 12

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density + 0.5f).toInt()

    private fun pt(activity: Activity) = activity.resources.configuration.locales[0].language == "pt"
    private fun text(activity: Activity, en: String, br: String) = if (pt(activity)) br else en
    private fun enabled(activity: Activity): Boolean = activity.getSharedPreferences(
        "io.github.mikeyphw.neostoreobtainx.ui", Context.MODE_PRIVATE,
    ).getBoolean("show_action", true)

    private data class Edges(val left: Int, val right: Int, val bottom: Int, val ime: Boolean)
    private fun edges(root: View): Edges? {
        val insets = root.rootWindowInsets ?: return null
        return if (Build.VERSION.SDK_INT >= 30) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            Edges(bars.left, bars.right, bars.bottom, insets.isVisible(WindowInsets.Type.ime()))
        } else {
            @Suppress("DEPRECATION")
            Edges(insets.systemWindowInsetLeft, insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom, insets.systemWindowInsetBottom > insets.stableInsetBottom)
        }
    }

    fun visible(activity: Activity): Boolean = !activity.isFinishing && !activity.isDestroyed &&
        activity.hasWindowFocus() && !activity.isInPictureInPictureMode

    fun render(activity: Activity, page: PageBinding?, onTap: (PageBinding) -> Unit) {
        // The dialog itself temporarily takes focus; close it only on actual
        // navigation, unready data or lifecycle stop, not its own focus change.
        if (settingsDialog != null && (settingsRoute !== page?.route || !ActionPolicy.ready(page))) {
            settingsDialog?.dismiss(); settingsDialog = null; settingsRoute = null
        }
        if (!visible(activity) || !ActionPolicy.ready(page)) { removeButton(); return }
        val root = activity.findViewById<View>(android.R.id.content) as? FrameLayout
        val insets = root?.let(::edges)
        if (root == null || insets == null || insets.ime || !root.isShown ||
            root.width < dp(activity, 160) || root.height < dp(activity, 160)) {
            removeButton(); return
        }
        val showAction = enabled(activity)
        val existing = button?.get()
        if (existing != null && existing.parent === root && binding?.route === page?.route &&
            binding?.snapshot == page?.snapshot && existing.tag == showAction) {
            position(activity, root, existing, insets)
            return
        }
        removeButton()
        val currentPage = page ?: return
        val view = Button(activity).apply {
            tag = showAction
            text = if (showAction) this@ObtainXPageAction.text(activity, "Add to ObtainX", "Adicionar ao ObtainX") else "⚙"
            contentDescription = if (showAction) this@ObtainXPageAction.text(activity,
                "Add to ObtainX. Long press for settings.", "Adicionar ao ObtainX. Toque e segure para configurações.")
                else this@ObtainXPageAction.text(activity, "ObtainX action settings", "Configurações da ação ObtainX")
            setAllCaps(false)
            textSize = 14f
            minHeight = dp(activity, 48)
            minimumHeight = dp(activity, 48)
            minWidth = dp(activity, 48)
            minimumWidth = dp(activity, 48)
            setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8))
            // A neutral high-contrast surface works independently of the host's Compose theme.
            val dark = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val fg = if (dark) Color.BLACK else Color.WHITE
            val bg = if (dark) Color.WHITE else Color.rgb(28, 28, 28)
            setTextColor(fg)
            backgroundTintList = null
            val shape = GradientDrawable().apply {
                setColor(bg); cornerRadius = dp(activity, 24).toFloat()
                setStroke(dp(activity, 1), if (dark) Color.DKGRAY else Color.LTGRAY)
            }
            background = RippleDrawable(ColorStateList.valueOf(if (dark) 0x22000000 else 0x33ffffff), shape, null)
            elevation = dp(activity, 6).toFloat()
            isFocusable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        val weakActivity = WeakReference(activity)
        view.setOnClickListener {
            val host = weakActivity.get() ?: return@setOnClickListener
            if (!visible(host) || button?.get() !== view || !view.isAttachedToWindow ||
                edges(view)?.ime != false || !view.isShown) return@setOnClickListener
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
        root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END))
        position(activity, root, view, insets)
    }

    private fun position(activity: Activity, root: FrameLayout, view: Button, insets: Edges) {
        val space = dp(activity, margin)
        val max = (root.width - insets.left - insets.right - 2 * space).coerceAtLeast(dp(activity, 48))
        if (view.maxWidth != max) view.maxWidth = max
        val params = view.layoutParams as? FrameLayout.LayoutParams ?: return
        val end = space + if (root.layoutDirection == View.LAYOUT_DIRECTION_RTL) insets.left else insets.right
        val bottom = space + insets.bottom
        if (params.marginEnd != end || params.bottomMargin != bottom) {
            params.marginEnd = end; params.bottomMargin = bottom; view.layoutParams = params
        }
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
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
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
