package io.github.mikeyphw.neostoreobtainx

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** Neo Store 1.3.3 only: M3 page probe and explicit, additive ObtainX action. */
class NeoStoreEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.processName != lpparam.packageName) return
        val loader = lpparam.classLoader ?: return
        when (lpparam.packageName) {
            TargetPackages.NEO_STORE -> {
                XposedBridge.log("NSOX_M1_PROCESS=NEO_STORE_ONLY")
                NeoStorePageProbe.install(loader)
            }
            TargetPackages.DROIDIFY -> {
                XposedBridge.log("NSOX_M8_PROCESS=DROIDIFY")
                DroidifyPageHook.install(loader)
            }
        }
    }
}
