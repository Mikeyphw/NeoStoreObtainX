package io.github.mikeyphw.neostoreobtainx

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.ref.WeakReference

/**
 * Additive Droid-ify 0.7.8 legacy app-detail toolbar action. No Droid-ify data writes.
 * This is independent of Neo Store's Compose anchor and never hooks ObtainX.
 *
 * Version pin is deliberately strict: only com.looker.droidify 0.7.8 (780).
 * Droid-ify's pinned release disables class-name obfuscation in app/proguard.pro.
 * The host fragment owns its toolbar, so there is no geometry guessing or overlay.
 * The host's AndroidX class loader may expose different inherited methods:
 * resolve Activity by ContextWrapper and use public inherited reflection. 
 * Every tap rebuilds a ProbeSnapshot from the *visible* fragment and paired
 * Product/Repository instances before invoking the existing trusted bridge.
 */
internal object DroidifyPageHook {
    private const val FRAGMENT = "com.looker.droidify.ui.appDetail.AppDetailFragment"
    private const val TARGET_VERSION = "0.7.8"
    private const val TARGET_CODE = 780L
    private const val APP_ROUTE_KEY = "package_name"
    private const val REPO_ROUTE_KEY = "repo_address"
    private const val ITEM_ID = 0x4E534F58 // 'NSOX', not used by Droid-ify
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
    private var warnedVersion = false

    fun install(loader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(FRAGMENT, loader, "onViewCreated",
                View::class.java, Bundle::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.hasThrowable()) return
                        try { attach(param.thisObject, param.args[0] as? View) }
                        catch (error: Throwable) { log("HOOK_REFUSED_" + error.javaClass.simpleName) }
                    }
                })
            XposedBridge.log("NSOX_M8_DROIDIFY_HOOK=INSTALLED")
        } catch (error: Throwable) {
            log("HOOK_UNAVAILABLE_" + error.javaClass.simpleName)
        }
    }

    private fun log(marker: String) = XposedBridge.log("NSOX_M8_DROIDIFY=" + marker)

    private fun pinned(activity: Activity): Boolean {
        if (activity.packageName != TargetPackages.DROIDIFY) return false
        val info = try {
            @Suppress("DEPRECATION")
            activity.packageManager.getPackageInfo(TargetPackages.DROIDIFY, 0)
        } catch (_: Exception) { return false }
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
                   else info.versionCode.toLong()
        val matched = code == TARGET_CODE && info.versionName == TARGET_VERSION
        if (!matched && !warnedVersion) {
            warnedVersion = true
            log("UNSUPPORTED_HOST_VERSION")
        }
        return matched
    }

    /** Resolve Android Activity from the actual host view, not a reflective
     * Fragment.getActivity() call which may have been optimized in the host APK. */
    private fun hostActivity(root: View): Activity? {
        var current: android.content.Context? = root.context
        val seen = HashSet<android.content.Context>()
        for (depth in 0 until 16) {
            val ctx = current ?: return null
            if (!seen.add(ctx)) return null
            if (ctx is Activity) return ctx
            val wrapper = ctx as? ContextWrapper ?: return null
            val next = wrapper.baseContext ?: return null
            if (next === ctx) return null
            current = next
        }
        return null
    }

    /** Public inherited method discovery avoids XposedHelpers.callMethod()
     * failing on a method optimized or moved by R8 in the host hierarchy. */
    private fun inheritedGetter(instance: Any, name: String): Any? {
        val method = instance.javaClass.methods.firstOrNull {
            it.name == name && it.parameterTypes.isEmpty()
        } ?: return null
        return method.invoke(instance)
    }

    private fun fragmentArguments(fragment: Any): Bundle? {
        val fromGetter = try { inheritedGetter(fragment, "getArguments") as? Bundle }
            catch (_: ReflectiveOperationException) { null }
        if (fromGetter != null) return fromGetter
        return try { XposedHelpers.getObjectField(fragment, "mArguments") as? Bundle }
            catch (_: Throwable) { null }
    }

    /** Locate the actual host MaterialToolbar via its native view hierarchy.
     * Avoid depending on R8 retaining a Kotlin property getter. */
    private fun toolbar(view: View?): View? {
        if (view == null) return null
        val todo = ArrayDeque<View>()
        todo.add(view)
        var count = 0
        while (todo.isNotEmpty() && ++count <= 200) {
            val next = todo.removeFirst()
            var type: Class<*>? = next.javaClass
            while (type != null) {
                if (type.name == "com.google.android.material.appbar.MaterialToolbar") return next
                type = type.superclass
            }
            if (next is ViewGroup) for (i in 0 until next.childCount.coerceAtMost(50)) {
                todo.add(next.getChildAt(i))
            }
        }
        return null
    }

    private fun attach(fragment: Any, view: View?) {
        var stage = "VIEW"
        try {
            if (view == null) return
            stage = "HOST_ACTIVITY"
            val activity = hostActivity(view) ?: run {
                log("ATTACH_REFUSED_ACTIVITY_CONTEXT")
                return
            }
            if (!pinned(activity)) return
            stage = "ROUTE_ARGUMENTS"
            val pkg = routePackage(fragment) ?: run {
                log("ATTACH_REFUSED_ROUTE_UNAVAILABLE")
                return
            }
            if (!packagePattern.matches(pkg)) { log("INVALID_ROUTE"); return }
            stage = "NATIVE_TOOLBAR"
            val tb = toolbar(view) ?: run { log("NATIVE_TOOLBAR_MISSING"); return }
            stage = "TOOLBAR_MENU"
            val menu = inheritedGetter(tb, "getMenu") as? Menu ?: run {
                log("ATTACH_REFUSED_TOOLBAR_MENU")
                return
            }
            if (menu.findItem(ITEM_ID) != null) return
            stage = "MENU_ADD"
            val action = menu.add(0, ITEM_ID, 0, "Add to ObtainX")
            stage = "ICON_AND_LISTENER"
            action.setIcon(preferredIcon(activity))
            action.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            action.setContentDescription("Add to ObtainX; opens import or existing app")
            val weakFragment = WeakReference(fragment)
            action.setOnMenuItemClickListener {
                val bound = weakFragment.get()
                if (bound != null) tapped(bound, activity, pkg)
                true
            }
            log("NATIVE_TOOLBAR_ACTION_ADDED")
        } catch (error: Throwable) {
            // Stage only: never log package IDs, repository URLs or app metadata.
            log("ATTACH_REFUSED_${stage}_${error.javaClass.simpleName}")
        }
    }

    private fun routePackage(fragment: Any): String? =
        fragmentArguments(fragment)?.getString(APP_ROUTE_KEY)

    private fun liveSnapshot(fragment: Any, route: String): ProbeSnapshot {
        val pairs = XposedHelpers.getObjectField(fragment, "products") as? List<*>
            ?: return ProbeSnapshot(ProbePhase.WAITING, route)
        // Match Droid-ify's own SOURCE and SHARE actions: they use products[0].
        val selected = pairs.firstOrNull() ?: return ProbeSnapshot(ProbePhase.WAITING, route)
        val product = inheritedGetter(selected, "getFirst")
            ?: return ProbeSnapshot(ProbePhase.WAITING, route)
        val repo = inheritedGetter(selected, "getSecond")
            ?: return ProbeSnapshot(ProbePhase.WAITING, route)
        val productPackage = XposedHelpers.getObjectField(product, "packageName") as? String
        val prodRepoId = (XposedHelpers.getObjectField(product, "repositoryId") as? Number)?.toLong()
        val repoId = (XposedHelpers.getObjectField(repo, "id") as? Number)?.toLong()
        val address = XposedHelpers.getObjectField(repo, "address") as? String
        val auth = XposedHelpers.getObjectField(repo, "authentication") as? String
        if (!auth.isNullOrBlank()) return ProbeSnapshot(ProbePhase.UNSUPPORTED_SOURCE, route)
        // If a source-specific route was requested, do not silently change repos.
        val selectedAddress = fragmentArguments(fragment)?.getString(REPO_ROUTE_KEY)
        if (!selectedAddress.isNullOrBlank() && selectedAddress != address) {
            return ProbeSnapshot(ProbePhase.WAITING, route)
        }
        val source = (XposedHelpers.getObjectField(product, "source") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (XposedHelpers.getObjectField(product, "web") as? String)
        return ProbePolicy.resolve(route, route, productPackage, prodRepoId, repoId, address, source)
    }

    private fun tapped(fragment: Any, activity: Activity, boundPackage: String) {
        try {
            if (!pinned(activity) || activity.isFinishing || activity.isDestroyed ||
                !activity.hasWindowFocus()) return
            val view = inheritedGetter(fragment, "getView") as? View ?: return
            if (!view.isAttachedToWindow || !view.isShown ||
                (inheritedGetter(fragment, "isResumed") as? Boolean) != true ||
                routePackage(fragment) != boundPackage) return
            // Re-read the selected package and source on *this* visible page.
            val snapshot = liveSnapshot(fragment, boundPackage)
            if (snapshot.phase != ProbePhase.READY) {
                Toast.makeText(activity, "ObtainX source not ready or unsupported", Toast.LENGTH_SHORT).show()
                log("SOURCE_NOT_READY")
                return
            }
            val outcome = ObtainXIntentBridge.launchFromUserTap(activity, snapshot, routePackage(fragment))
            if (outcome is BridgeOutcome.Refused) {
                Toast.makeText(activity, "Unable to open ObtainX for this app", Toast.LENGTH_SHORT).show()
                log("BRIDGE_REFUSED_" + outcome.reason.name)
            } else log("IMPORT_PREVIEW_DISPATCHED")
        } catch (error: Throwable) {
            log("TAP_REFUSED_" + error.javaClass.simpleName)
            Toast.makeText(activity, "ObtainX action unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun preferredIcon(activity: Activity): Drawable {
        try {
            return activity.packageManager.getApplicationIcon(TargetPackages.OBTAIN_X).mutate()
        } catch (_: Exception) {
            // Package visibility on Android 11+ can make the launcher icon inaccessible.
            // A download glyph still accurately represents the user-initiated import.
        }
        val attr = activity.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val foreground = try { attr.getColor(0, Color.DKGRAY) } finally { attr.recycle() }
        return DownloadGlyph(foreground)
    }

    private class DownloadGlyph(foreground: Int) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foreground
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            strokeCap = Paint.Cap.ROUND
        }
        override fun draw(canvas: Canvas) {
            val b = bounds
            val cx = b.exactCenterX()
            val cy = b.exactCenterY()
            val k = minOf(b.width(), b.height()) / 24f
            canvas.drawLine(cx, cy-7*k, cx, cy+3*k, paint)
            canvas.drawLine(cx-4*k, cy, cx, cy+4*k, paint)
            canvas.drawLine(cx+4*k, cy, cx, cy+4*k, paint)
            canvas.drawLine(cx-9*k, cy+6*k, cx+9*k, cy+6*k, paint)
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(filter: android.graphics.ColorFilter?) { paint.colorFilter = filter }
        @Deprecated("Required Drawable API") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        override fun getIntrinsicWidth(): Int = 24
        override fun getIntrinsicHeight(): Int = 24
    }
}
