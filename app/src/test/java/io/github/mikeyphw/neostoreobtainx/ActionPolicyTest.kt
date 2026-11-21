package io.github.mikeyphw.neostoreobtainx

import org.junit.Assert.*
import org.junit.Test

class ActionPolicyTest {
    private val route = Any()
    private fun binding(key: Any = route, pkg: String = "org.example.app", address: String = "https://f-droid.org/repo") =
        PageBinding(key, ProbePolicy.resolve(pkg, pkg, pkg, 9L, 9L, address))

    @Test fun sameVisibleRouteAndSourceAllowsExplicitTap() {
        assertTrue(ActionPolicy.allowsTap(binding(), binding(), true))
    }
    @Test fun hiddenOrPausedNeverAllowsTap() {
        assertFalse(ActionPolicy.allowsTap(binding(), binding(), false))
    }
    @Test fun noActiveRouteNeverAllowsTap() {
        assertFalse(ActionPolicy.allowsTap(binding(), null, true))
        assertFalse(ActionPolicy.allowsTap(null, binding(), true))
    }
    @Test fun samePackageOnNewRouteRefusesOldButton() {
        assertFalse(ActionPolicy.allowsTap(binding(), binding(Any()), true))
    }
    @Test fun differentPackageOrSourceRefusesOldButton() {
        assertFalse(ActionPolicy.allowsTap(binding(), binding(pkg = "org.other.app"), true))
        assertFalse(ActionPolicy.allowsTap(binding(), binding(address = "https://repo.example.org/repo"), true))
    }
    @Test fun unsafeOrForgedSnapshotCannotCreateAction() {
        assertFalse(ActionPolicy.ready(binding(address = "https://a:b@repo.example.org/repo")))
        val valid = binding()
        assertFalse(ActionPolicy.ready(valid.copy(snapshot = valid.snapshot.copy(sourceUrl = "https://bad.example.org"))))
    }
    @Test fun waitingMetadataCannotAuthorize() {
        assertFalse(ActionPolicy.allowsTap(binding(), PageBinding(route, ProbeSnapshot(ProbePhase.WAITING)), true))
    }
}
