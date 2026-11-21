package io.github.mikeyphw.neostoreobtainx

import org.junit.Assert.*
import org.junit.Test

class ObtainXSeedPolicyTest {
    private fun snapshot(address: String = "https://f-droid.org/repo"): ProbeSnapshot {
        return ProbePolicy.resolve("org.example.app", "org.example.app", "org.example.app", 7L, 7L, address)
    }

    private fun prepared(state: ProbeSnapshot, current: String? = "org.example.app"): String {
        val result = ObtainXSeedPolicy.prepare(state, current)
        assertTrue(result is ObtainXSeed.Prepared)
        return (result as ObtainXSeed.Prepared).json
    }

    @Test fun officialFdroidBuildsMinimalImportSeed() {
        assertEquals(
            "{\"id\":\"org.example.app\",\"url\":\"https://f-droid.org/packages/org.example.app\"," +
                "\"author\":\"\",\"name\":\"org.example.app\",\"latestVersion\":\"\"," +
                "\"preferredApkIndex\":-1,\"additionalSettings\":\"{}\"}",
            prepared(snapshot())
        )
    }

    @Test fun customRepoHasNestedJsonStringAndForcedAdapter() {
        val json = prepared(snapshot("https://repo.example.org/fdroid/repo/index.xml"))
        assertTrue(json.contains("\"url\":\"https://repo.example.org/fdroid/repo?appId=org.example.app\""))
        assertTrue(json.contains("\"additionalSettings\":\"{\\\"appIdOrName\\\":\\\"org.example.app\\\"}\""))
        assertTrue(json.contains("\"overrideSource\":\"FDroidRepo\""))
    }

    @Test fun izzyDoesNotUseGenericRepoAdapter() {
        val json = prepared(snapshot("https://apt.izzysoft.de/fdroid/repo"))
        assertTrue(json.contains("https://apt.izzysoft.de/fdroid/index/apk/org.example.app"))
        assertTrue(json.contains("\"overrideSource\":\"IzzyOnDroid\""))
        assertTrue(json.contains("appIdOrName"))
    }

    @Test fun routeChangeRefusesStaleSnapshot() {
        assertEquals(ObtainXSeed.Refused(SeedRefusal.ROUTE_CHANGED),
            ObtainXSeedPolicy.prepare(snapshot(), "org.example.other"))
    }

    @Test fun notReadyOrUnsupportedRefuses() {
        val missing = ProbeSnapshot(ProbePhase.WAITING, "org.example.app")
        assertEquals(ObtainXSeed.Refused(SeedRefusal.NOT_READY),
            ObtainXSeedPolicy.prepare(missing, "org.example.app"))
        val hostile = ProbePolicy.resolve("org.example.app", "org.example.app", "org.example.app",
            7L, 7L, "https://user:secret@repo.example.org/repo")
        assertEquals(ObtainXSeed.Refused(SeedRefusal.NOT_READY),
            ObtainXSeedPolicy.prepare(hostile.copy(phase = ProbePhase.WAITING), "org.example.app"))
    }

    @Test fun forgedSourceOrChangedRepositoryIsRejected() {
        val base = snapshot()
        assertEquals(ObtainXSeed.Refused(SeedRefusal.SOURCE_MISMATCH),
            ObtainXSeedPolicy.prepare(base.copy(sourceUrl = "https://unrelated.example.org/app"), "org.example.app"))
        assertEquals(ObtainXSeed.Refused(SeedRefusal.SOURCE_MISMATCH),
            ObtainXSeedPolicy.prepare(base.copy(repositoryAddress = "https://repo.example.org/fdroid/repo"), "org.example.app"))
    }

    @Test fun oversizedSourceIsRejectedBeforeUriEncoding() {
        val base = snapshot()
        val long = base.copy(repositoryAddress = "https://example.org/" + "x".repeat(2100))
        assertEquals(ObtainXSeed.Refused(SeedRefusal.SOURCE_TOO_LONG),
            ObtainXSeedPolicy.prepare(long, "org.example.app"))
    }

    @Test fun jsonQuoteEscapesSpecialAndControlCharacters() {
        assertEquals("\"a\\\"b\\\\c\\n\\u2028\"",
            ObtainXSeedPolicy.quote("a\"b\\c\n\u2028"))
    }
    @Test fun sourceCodeRepositoryPreferredWhenSupported() {
        val preferred = ProbePolicy.resolve("org.example.app", "org.example.app", "org.example.app",
            7L, 7L, "https://apt.izzysoft.de/fdroid/repo", "https://github.com/acme/client")
        val json = prepared(preferred)
        assertTrue(json.contains("https://github.com/acme/client"))
        assertFalse(json.contains("IzzyOnDroid"))
    }

    @Test fun unsupportedSourceCodeSitePreservesIzzyAdapter() {
        val preferred = ProbePolicy.resolve("org.example.app", "org.example.app", "org.example.app",
            7L, 7L, "https://apt.izzysoft.de/fdroid/repo", "https://project.example.org")
        assertTrue(prepared(preferred).contains("IzzyOnDroid"))
    }

}
