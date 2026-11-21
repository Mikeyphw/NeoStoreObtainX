package io.github.mikeyphw.neostoreobtainx

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * M2 opt-in API for the future M3 button. Nothing invokes it from an Xposed
 * hook or lifecycle callback. Android launches only on a deliberate user tap.
 */
enum class BridgeFailure { STALE_OR_UNSAFE_SOURCE, NO_EXPORTED_TARGET, LAUNCH_REJECTED }

sealed interface BridgeOutcome {
    data object DispatchedForPreview : BridgeOutcome
    data class Refused(val reason: BridgeFailure) : BridgeOutcome
}

object ObtainXIntentBridge {
    /** Strict package routing; never allow a chooser or unrelated handler. */
    fun makeIntent(snapshot: ProbeSnapshot, currentRoutePackage: String?): Intent? {
        val seed = ObtainXSeedPolicy.prepare(snapshot, currentRoutePackage)
        if (seed !is ObtainXSeed.Prepared) return null
        val uri = Uri.parse("obtainium://app/${Uri.encode(seed.json)}")
        if (uri.scheme != "obtainium" || uri.host != "app") return null
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(TargetPackages.OBTAIN_X)
        }
    }

    /**
     * Call from M3's explicit click handler only, after re-reading the active
     * route and M1 snapshot. A successful startActivity is NOT an import proof.
     */
    fun launchFromUserTap(
        activity: Activity,
        snapshot: ProbeSnapshot,
        currentRoutePackage: String?,
    ): BridgeOutcome {
        val intent = makeIntent(snapshot, currentRoutePackage)
            ?: return BridgeOutcome.Refused(BridgeFailure.STALE_OR_UNSAFE_SOURCE)
        val resolved = try {
            activity.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        } catch (_: SecurityException) {
            null
        }
        val target = resolved?.activityInfo
        if (target == null || target.packageName != TargetPackages.OBTAIN_X ||
            !target.exported || !target.enabled
        ) return BridgeOutcome.Refused(BridgeFailure.NO_EXPORTED_TARGET)
        return try {
            activity.startActivity(intent)
            BridgeOutcome.DispatchedForPreview
        } catch (_: ActivityNotFoundException) {
            BridgeOutcome.Refused(BridgeFailure.LAUNCH_REJECTED)
        } catch (_: SecurityException) {
            BridgeOutcome.Refused(BridgeFailure.LAUNCH_REJECTED)
        }
    }
}
