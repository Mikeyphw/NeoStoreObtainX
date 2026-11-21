package io.github.mikeyphw.neostoreobtainx

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * M1 intentionally does not create any View, launch Intent or change downloads.
 * Reads the host's NavBackStack and the matching AppPageVM StateFlow, in-process.
 * All retained Activity/ViewModel references are weak; polling stops on pause.
 */
internal class NeoStorePageProbe private constructor(private val symbols: Symbols) :
    Application.ActivityLifecycleCallbacks {

    private data class Symbols(
        val activityClass: Class<*>, val navStack: Field,
        val appRouteClass: Class<*>, val routePackage: Method,
        val coreState: Method, val stateValue: Method,
        val suggestedRepo: Method, val embeddedProduct: Method,
        val productPackage: Method, val productRepositoryId: Method,
        val repositoryId: Method, val repositoryAddress: Method,
    )

    companion object {
        private const val TARGET_VERSION = "1.3.3"
        private const val TARGET_VERSION_CODE = 1304L
        private const val POLL_MS = 600L

        fun install(classLoader: ClassLoader) {
            try {
                fun host(name: String) = Class.forName(name, false, classLoader)
                fun publicMethod(owner: Class<*>, name: String, vararg args: Class<*>) =
                    owner.getMethod(name, *args)

                val activityClass = host("com.machiav3lli.fdroid.NeoActivity")
                val navField = activityClass.getDeclaredField("navStack").apply { isAccessible = true }
                val appRoute = host("com.machiav3lli.fdroid.ui.navigation.NavRoute\$App")
                val viewModel = host("com.machiav3lli.fdroid.viewmodels.AppPageVM")
                val core = host("com.machiav3lli.fdroid.viewmodels.CoreAppState")
                val embedded = host("com.machiav3lli.fdroid.data.database.entity.EmbeddedProduct")
                val product = host("com.machiav3lli.fdroid.data.database.entity.Product")
                val repository = host("com.machiav3lli.fdroid.data.database.entity.Repository")
                val stateFlow = host("kotlinx.coroutines.flow.StateFlow")
                val setApp = publicMethod(viewModel, "setApp", String::class.java)
                val onResume = activityClass.getDeclaredMethod("onResume").apply { isAccessible = true }

                val symbols = Symbols(
                    activityClass, navField,
                    appRoute, publicMethod(appRoute, "getPackageName"),
                    publicMethod(viewModel, "getCoreAppState"), publicMethod(stateFlow, "getValue"),
                    publicMethod(core, "getSuggestedProductRepo"), publicMethod(embedded, "getProduct"),
                    publicMethod(product, "getPackageName"), publicMethod(product, "getRepositoryId"),
                    publicMethod(repository, "getId"), publicMethod(repository, "getAddress"),
                )
                val watcher = NeoStorePageProbe(symbols)
                // Hook only a named, source-pinned VM setter and an actual NeoActivity override.
                XposedBridge.hookMethod(setApp, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.throwable == null) {
                            watcher.observeViewModel(param.thisObject, param.args[0] as? String)
                        }
                    }
                })
                XposedBridge.hookMethod(onResume, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.throwable == null) {
                            (param.thisObject as? Activity)?.let(watcher::attachOnResume)
                        }
                    }
                })
                XposedBridge.log("NSOX_M1_HOOKS=INSTALLED_READ_ONLY")
            } catch (error: ReflectiveOperationException) {
                XposedBridge.log("NSOX_M1_REFUSED=SOURCE_SYMBOLS_" + error.javaClass.simpleName)
            } catch (error: LinkageError) {
                XposedBridge.log("NSOX_M1_REFUSED=SOURCE_LINKAGE_" + error.javaClass.simpleName)
            }
        }
    }

    private val main = Handler(Looper.getMainLooper())
    private var registered = false
    private var currentActivity: WeakReference<Activity>? = null
    private var currentVm: WeakReference<Any>? = null
    private var viewModelPackage: String? = null
    private var pending: Runnable? = null
    private var generation = 0
    private var lastPhase = ProbePhase.IDLE
    private var versionDenied = false

    private fun observeViewModel(vm: Any, packageName: String?) {
        main.post {
            // Only retain a weak reference; version and route must also match at sampling time.
            currentVm = WeakReference(vm)
            viewModelPackage = packageName
        }
    }

    private fun attachOnResume(activity: Activity) {
        if (!registered) {
            registered = true
            activity.application.registerActivityLifecycleCallbacks(this)
        }
        onActivityResumed(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        if (!symbols.activityClass.isInstance(activity)) return
        if (!versionSupported(activity)) return
        if (currentActivity?.get() === activity && pending != null) return
        stop()
        currentActivity = WeakReference(activity)
        poll(generation)
    }

    private fun versionSupported(activity: Activity): Boolean {
        if (versionDenied) return false
        return try {
            @Suppress("DEPRECATION")
            val info = activity.packageManager.getPackageInfo(TargetPackages.NEO_STORE, 0)
            @Suppress("DEPRECATION")
            val actualCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode
                else info.versionCode.toLong()
            if (info.versionName == TARGET_VERSION && actualCode == TARGET_VERSION_CODE) true
            else {
                versionDenied = true
                XposedBridge.log("NSOX_M1_REFUSED=UNPINNED_NEO_STORE_VERSION")
                false
            }
        } catch (error: Exception) {
            versionDenied = true
            XposedBridge.log("NSOX_M1_REFUSED=VERSION_CHECK_" + error.javaClass.simpleName)
            false
        }
    }

    private fun poll(token: Int) {
        if (token != generation) return
        val activity = currentActivity?.get() ?: return
        try {
            val snapshot = sample(activity)
            if (lastPhase != snapshot.phase) {
                lastPhase = snapshot.phase
                // Do not log package IDs, repository URLs, credentials or app labels.
                XposedBridge.log("NSOX_M1_PAGE_STATE=" + snapshot.phase.name)
            }
        } catch (error: ReflectiveOperationException) {
            XposedBridge.log("NSOX_M1_REFUSED=RUNTIME_SYMBOLS_" + error.javaClass.simpleName)
            stop()
            return
        } catch (error: LinkageError) {
            XposedBridge.log("NSOX_M1_REFUSED=RUNTIME_LINKAGE_" + error.javaClass.simpleName)
            stop()
            return
        } catch (error: ClassCastException) {
            XposedBridge.log("NSOX_M1_REFUSED=RUNTIME_TYPE_MISMATCH")
            stop()
            return
        }
        // Runnable captures only this probe and a generation, not the Activity.
        val next = Runnable { poll(token) }
        pending = next
        main.postDelayed(next, POLL_MS)
    }

    private fun sample(activity: Activity): ProbeSnapshot {
        val stack = symbols.navStack.get(activity) as? List<*>
        val top = stack?.lastOrNull()
        if (top == null || !symbols.appRouteClass.isInstance(top)) return ProbeSnapshot(ProbePhase.IDLE)
        val active = symbols.routePackage.invoke(top) as? String
        val vm = currentVm?.get()
        if (vm == null || viewModelPackage != active) {
            return ProbePolicy.resolve(active, viewModelPackage, null, null, null, null)
        }
        val flow = symbols.coreState.invoke(vm)
        val state = symbols.stateValue.invoke(flow)
        val pair = symbols.suggestedRepo.invoke(state) as? Pair<*, *>
        val embedded = pair?.first
        val repo = pair?.second
        val product = embedded?.let { symbols.embeddedProduct.invoke(it) }
        val productPkg = product?.let { symbols.productPackage.invoke(it) as? String }
        val productId = product?.let { symbols.productRepositoryId.invoke(it) as? Long }
        val repoId = repo?.let { symbols.repositoryId.invoke(it) as? Long }
        val repoUrl = repo?.let { symbols.repositoryAddress.invoke(it) as? String }
        return ProbePolicy.resolve(active, viewModelPackage, productPkg, productId, repoId, repoUrl)
    }

    private fun stop() {
        generation += 1
        pending?.let(main::removeCallbacks)
        pending = null
        currentActivity = null
        if (lastPhase != ProbePhase.IDLE) {
            lastPhase = ProbePhase.IDLE
            XposedBridge.log("NSOX_M1_PAGE_STATE=IDLE")
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity?.get() === activity) stop()
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity?.get() === activity) stop()
        // Recomposition and rotation must not keep dead ViewModels alive.
        currentVm = null
        viewModelPackage = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
