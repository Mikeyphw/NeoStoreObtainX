package io.github.mikeyphw.neostoreobtainx

import org.junit.Assert.*
import org.junit.Test

class SourceMapperTest {
    @Test fun officialFdroidIsNotRewrittenToGithub() {
        assertEquals(
            SourceMapping.Mapped("https://f-droid.org/packages/org.example.app"),
            SourceMapper.map("org.example.app", "https://f-droid.org/repo")
        )
    }

    @Test fun izzyHasItsOwnAdapter() {
        assertEquals(
            SourceMapping.Mapped("https://apt.izzysoft.de/fdroid/index/apk/org.example.app"),
            SourceMapper.map("org.example.app", "https://apt.izzysoft.de/fdroid/repo")
        )
    }

    @Test fun customRepoPreservesSourceAndRequiresExplicitAdapter() {
        assertEquals(
            SourceMapping.Mapped(
                "https://repo.example.org/fdroid/repo?appId=org.example.app",
                "FDroidRepo"
            ),
            SourceMapper.map("org.example.app", "https://repo.example.org/fdroid/repo/index.xml")
        )
    }

    @Test fun insecureOrCredentialedRepoFailsClosed() {
        assertTrue(SourceMapper.map("org.example.app", "http://repo.example.org/") is SourceMapping.Unsupported)
        assertTrue(SourceMapper.map("org.example.app", "https://u:p@repo.example.org/") is SourceMapping.Unsupported)
        assertTrue(SourceMapper.map("org.example.app", "https://repo.example.org/?token=secret") is SourceMapping.Unsupported)
    }

    @Test fun malformedPackageNameFailsClosed() {
        assertTrue(SourceMapper.map("../bad", "https://f-droid.org/repo") is SourceMapping.Unsupported)
    }
    @Test fun officialArchiveAndAlternatePortCannotSilentlyChangeSource() {
        assertTrue(SourceMapper.map("org.example.app", "https://f-droid.org/archive") is SourceMapping.Unsupported)
        assertTrue(SourceMapper.map("org.example.app", "https://f-droid.org:8443/repo") is SourceMapping.Unsupported)
        assertTrue(SourceMapper.map("org.example.app", "https://apt.izzysoft.de/other/repo") is SourceMapping.Unsupported)
    }
}
