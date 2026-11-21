package io.github.mikeyphw.neostoreobtainx

import org.junit.Assert.*
import org.junit.Test

class ProbePolicyTest {
    private fun resolve(
        active: String? = "org.example.app", vm: String? = active,
        product: String? = active, productRepo: Long? = 7L, repo: Long? = 7L,
        address: String? = "https://f-droid.org/repo",
    ) = ProbePolicy.resolve(active, vm, product, productRepo, repo, address)

    @Test fun noAppRouteIsIdle() {
        assertEquals(ProbePhase.IDLE, resolve(active = null).phase)
    }

    @Test fun viewModelOrProductCannotBelongToPreviousApp() {
        assertEquals(ProbePhase.WAITING, resolve(vm = "org.other.app").phase)
        assertEquals(ProbePhase.WAITING, resolve(product = "org.other.app").phase)
    }

    @Test fun repositoryIdMustMatchTheSuggestedProduct() {
        assertEquals(ProbePhase.WAITING, resolve(repo = 8L).phase)
    }

    @Test fun missingMetadataDoesNotCreateAReadySnapshot() {
        assertEquals(ProbePhase.WAITING, resolve(address = null).phase)
        assertEquals(ProbePhase.WAITING, resolve(productRepo = null).phase)
    }

    @Test fun sourceIsMappedOnlyAfterAllIdentitiesAgree() {
        val state = resolve()
        assertEquals(ProbePhase.READY, state.phase)
        assertEquals("org.example.app", state.packageName)
        assertEquals("https://f-droid.org/packages/org.example.app", state.sourceUrl)
        assertNull(state.overrideSource)
    }

    @Test fun customRepositoryKeepsItsOwnAdapter() {
        val state = resolve(address = "https://repo.example.org/fdroid/repo")
        assertEquals(ProbePhase.READY, state.phase)
        assertEquals("https://repo.example.org/fdroid/repo?appId=org.example.app", state.sourceUrl)
        assertEquals("FDroidRepo", state.overrideSource)
    }

    @Test fun unsafeSourceDoesNotLeakItsCredentialsInSnapshot() {
        val state = resolve(address = "https://user:secret@repo.example.org/fdroid/repo")
        assertEquals(ProbePhase.UNSUPPORTED_SOURCE, state.phase)
        assertNull(state.repositoryAddress)
        assertNull(state.sourceUrl)
    }
}
