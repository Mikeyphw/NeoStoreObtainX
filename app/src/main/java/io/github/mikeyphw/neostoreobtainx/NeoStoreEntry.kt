package io.github.mikeyphw.neostoreobtainx

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** Neo Store 1.3.3 only: M1 read-only page/repository probe, no UI or Intent. */
class NeoStoreEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TargetPackages.NEO_STORE) return
        if (lpparam.processName != TargetPackages.NEO_STORE) return
        val loader = lpparam.classLoader ?: return
        XposedBridge.log("NSOX_M1_PROCESS=NEO_STORE_ONLY")
        NeoStorePageProbe.install(loader)
    }
}
