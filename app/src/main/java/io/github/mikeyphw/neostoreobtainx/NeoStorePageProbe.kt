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
 * M4 observes Neo Store 1.3.3 release through retained, source-pinned fields.
 * R8 removes the Kotlin getters and inlines AppPageVM.setApp in the production APK.
 * Constructor observation and route-matched StateFlows replace that brittle hook.
 * Never writes host objects or launches ObtainX except from an explicit tap.
 */
internal class NeoStorePageProbe private constructor(private val symbols: Symbols) :
    Application.ActivityLifecycleCallbacks {

    private data class Symbols(
        val activityClass: Class<*>, val navStack: Field,
        val appRouteClass: Class<*>, val routePackage: Field,
        val vmPackageFlow: Field, val vmCoreStateFlow: Field,
        val stateValue: Method,
        val suggestedRepo: Field, val embeddedProduct: Field,
        val productPackage: Field, val productRepositoryId: Field, val productSourceSite: Field, val productWebSite: Field,
        val repositoryId: Field, val repositoryAddress: Field, val repositoryAuthentication: Field,
        val pairFirst: Field, val pairSecond: Field,
    )

    companion object {
        private const val TARGET_VERSION = "1.3.3"
        private const val TARGET_VERSION_CODE = 1304L
        private const val POLL_MS = 600L

        fun install(classLoader: ClassLoader) {
            try {
                fun host(name: String) = Class.forName(name, false, classLoader)
                // Every field below was observed in the installed 1.3.3 release DEX.
                // Strict declared-field lookup is intentionally fail-closed.
                fun releaseField(owner: Class<*>, name: String) =
                    owner.getDeclaredField(name).apply { isAccessible = true }

                val activityClass = host("com.machiav3lli.fdroid.NeoActivity")
                val navField = releaseField(activityClass, "navStack")
                val appRoute = host("com.machiav3lli.fdroid.ui.navigation.NavRoute\$App")
                val viewModel = host("com.machiav3lli.fdroid.viewmodels.AppPageVM")
                val core = host("com.machiav3lli.fdroid.viewmodels.CoreAppState")
                val embedded = host("com.machiav3lli.fdroid.data.database.entity.EmbeddedProduct")
                val product = host("com.machiav3lli.fdroid.data.database.entity.Product")
                val repository = host("com.machiav3lli.fdroid.data.database.entity.Repository")
                val stateFlow = host("kotlinx.coroutines.flow.StateFlow")
                val hostPair = host("kotlin.Pair")
                val onResume = activityClass.getDeclaredMethod("onResume").apply { isAccessible = true }
                val symbols = Symbols(
                    activityClass, navField,
                    appRoute, releaseField(appRoute, "packageName"),
                    releaseField(viewModel, "packageName"), releaseField(viewModel, "coreAppState"),
                    stateFlow.getMethod("getValue"),
                    releaseField(core, "suggestedProductRepo"), releaseField(embedded, "product"),
                    releaseField(product, "packageName"), releaseField(product, "repositoryId"), releaseField(product, "source"), releaseField(product, "web"),
                    releaseField(repository, "id"), releaseField(repository, "address"),
                    releaseField(repository, "authentication"),
                    releaseField(hostPair, "first"), releaseField(hostPair, "second"),
                )
                val watcher = NeoStorePageProbe(symbols)
                // Release APK contains the VM constructor (arity 6), not setApp.
                // Hook all constructors so no compiler-generated setter is required.
                val constructorHooks = XposedBridge.hookAllConstructors(viewModel, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.throwable == null) {
                            watcher.observeViewModel(param.thisObject)
                        }
                    }
                })
                if (constructorHooks.isEmpty()) {
                    XposedBridge.log("NSOX_M4_REFUSED=NO_VM_CONSTRUCTOR_HOOK")
                    return
                }
                try {
                    XposedBridge.hookMethod(onResume, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (param.throwable == null) {
                                (param.thisObject as? Activity)?.let(watcher::attachOnResume)
                            }
                        }
                    })
                } catch (error: RuntimeException) {
                    constructorHooks.forEach { it.unhook() }
                    throw error
                }
                XposedBridge.log("NSOX_M4_HOOKS=INSTALLED_R8_FIELDS")
            } catch (error: ReflectiveOperationException) {
                XposedBridge.log("NSOX_M1_REFUSED=SOURCE_SYMBOLS_" + error.javaClass.simpleName)
            } catch (error: LinkageError) {
                XposedBridge.log("NSOX_M1_REFUSED=SOURCE_LINKAGE_" + error.javaClass.simpleName)
            } catch (error: RuntimeException) {
                XposedBridge.log("NSOX_M3_REFUSED=HOOK_RUNTIME_" + error.javaClass.simpleName)
            }
        }
    }

    private val main = Handler(Looper.getMainLooper())
    private val action = ObtainXPageAction()
    private var registered = false
    private var currentActivity: WeakReference<Activity>? = null
    // Only weak references are retained; a bounded list accommodates navigation
    // between simultaneously retained AppPageVM instances without stale packages.
    private val viewModels = mutableListOf<WeakReference<Any>>()
    private var pending: Runnable? = null
    private var generation = 0
    private var lastPhase = ProbePhase.IDLE
    private var versionDenied = false

    private fun observeViewModel(vm: Any) {
        val reference = WeakReference(vm)
        main.post {
            val candidate = reference.get() ?: return@post
            viewModels.removeAll { it.get() == null || it.get() === candidate }
            viewModels.add(reference)
            while (viewModels.size > 24) viewModels.removeAt(0)
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
            val page = currentBinding(activity)
            val snapshot = page?.snapshot ?: ProbeSnapshot(ProbePhase.IDLE)
            action.render(activity, page) { displayed -> launchForTap(token, displayed) }
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
        } catch (error: RuntimeException) {
            XposedBridge.log("NSOX_M3_REFUSED=RUNTIME_" + error.javaClass.simpleName)
            stop()
            return
        }
        // Runnable captures only this probe and a generation, not the Activity.
        val next = Runnable { poll(token) }
        pending = next
        main.postDelayed(next, POLL_MS)
    }

    private fun currentBinding(activity: Activity): PageBinding? {
        val stack = symbols.navStack.get(activity) as? List<*>
        val top = stack?.lastOrNull() ?: return null
        if (!symbols.appRouteClass.isInstance(top)) return null
        return PageBinding(top, sample(top))
    }

    /** Called only from the attached button's click listener, on the main thread. */
    private fun launchForTap(token: Int, displayed: PageBinding) {
        val activity = currentActivity?.get() ?: return
        if (token != generation || !action.visible(activity)) return
        try {
            val fresh = currentBinding(activity)
            if (!ActionPolicy.allowsTap(displayed, fresh, action.visible(activity)) || fresh == null) {
                action.result(activity, BridgeOutcome.Refused(BridgeFailure.STALE_OR_UNSAFE_SOURCE))
                return
            }
            val outcome = ObtainXIntentBridge.launchFromUserTap(activity, fresh.snapshot, fresh.snapshot.packageName)
            action.result(activity, outcome)
            val diagnostic = when (outcome) {
                BridgeOutcome.DispatchedForPreview -> "DISPATCHED_FOR_PREVIEW"
                is BridgeOutcome.Refused -> outcome.reason.name
            }
            XposedBridge.log("NSOX_M3_TAP=" + diagnostic)
        } catch (error: Exception) {
            XposedBridge.log("NSOX_M3_REFUSED=TAP_" + error.javaClass.simpleName)
            action.clear()
        } catch (error: LinkageError) {
            XposedBridge.log("NSOX_M3_REFUSED=TAP_LINKAGE_" + error.javaClass.simpleName)
            action.clear()
        }
    }

    private fun sample(top: Any): ProbeSnapshot {
        val active = symbols.routePackage.get(top) as? String
        // A constructor callback is not a package-selection signal. Read the
        // host MutableStateFlow anew and select only a VM matching this route.
        var matchingVm: Any? = null
        for (reference in viewModels.asReversed()) {
            val vm = reference.get() ?: continue
            val packageFlow = symbols.vmPackageFlow.get(vm) ?: continue
            if ((symbols.stateValue.invoke(packageFlow) as? String) == active) {
                matchingVm = vm
                break
            }
        }
        if (matchingVm == null) {
            return ProbePolicy.resolve(active, null, null, null, null, null)
        }
        val coreFlow = symbols.vmCoreStateFlow.get(matchingVm)
        val state = coreFlow?.let { symbols.stateValue.invoke(it) }
        val pair = state?.let { symbols.suggestedRepo.get(it) }
        val embedded = pair?.let { symbols.pairFirst.get(it) }
        val repo = pair?.let { symbols.pairSecond.get(it) }
        // Fail closed for null/credentialed authentication even for plain URLs.
        if (repo != null && (symbols.repositoryAuthentication.get(repo) as? String) != "") {
            return ProbeSnapshot(ProbePhase.UNSUPPORTED_SOURCE)
        }
        val product = embedded?.let { symbols.embeddedProduct.get(it) }
        val productPkg = product?.let { symbols.productPackage.get(it) as? String }
        val productId = product?.let { symbols.productRepositoryId.get(it) as? Long }
        val productSource = product?.let { (symbols.productSourceSite.get(it) as? String)?.takeIf(String::isNotBlank)
            ?: (symbols.productWebSite.get(it) as? String) }
        val repoId = repo?.let { symbols.repositoryId.get(it) as? Long }
        val repoUrl = repo?.let { symbols.repositoryAddress.get(it) as? String }
        return ProbePolicy.resolve(active, active, productPkg, productId, repoId, repoUrl, productSource)
    }

    private fun stop() {
        generation += 1
        pending?.let(main::removeCallbacks)
        pending = null
        currentActivity = null
        action.clear()
        if (lastPhase != ProbePhase.IDLE) {
            lastPhase = ProbePhase.IDLE
            XposedBridge.log("NSOX_M1_PAGE_STATE=IDLE")
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity?.get() === activity) stop()
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (!symbols.activityClass.isInstance(activity)) return
        if (currentActivity?.get() === activity) stop()
        // Do not retain dead VMs; live references remain weak and bounded.
        if (currentActivity?.get() == null) {
            viewModels.removeAll { it.get() == null }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
