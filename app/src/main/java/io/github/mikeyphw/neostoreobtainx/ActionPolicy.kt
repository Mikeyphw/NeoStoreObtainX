package io.github.mikeyphw.neostoreobtainx

/** Pure, independently testable tap authorization. Never launches anything. */
internal data class PageBinding(val route: Any, val snapshot: ProbeSnapshot)

internal object ActionPolicy {
    fun ready(binding: PageBinding?): Boolean = binding != null &&
        binding.snapshot.phase == ProbePhase.READY &&
        ObtainXSeedPolicy.prepare(binding.snapshot, binding.snapshot.packageName) is ObtainXSeed.Prepared

    fun allowsTap(displayed: PageBinding?, fresh: PageBinding?, visible: Boolean): Boolean =
        visible && ready(displayed) && ready(fresh) && displayed != null && fresh != null &&
            displayed.route === fresh.route && displayed.snapshot == fresh.snapshot
}
