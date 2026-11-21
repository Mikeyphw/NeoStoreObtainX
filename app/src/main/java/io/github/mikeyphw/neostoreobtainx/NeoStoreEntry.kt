package io.github.mikeyphw.neostoreobtainx

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/** Bootstrap proof of correct LSPosed scoping. No UI or download hooks yet. */
class NeoStoreEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TargetPackages.NEO_STORE) return
        if (lpparam.processName != TargetPackages.NEO_STORE) return
        XposedBridge.log("NeoStoreObtainX: scoped bootstrap loaded; feature hooks disabled")
    }
}
