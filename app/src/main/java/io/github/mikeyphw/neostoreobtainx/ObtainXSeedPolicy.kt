package io.github.mikeyphw.neostoreobtainx

/**
 * M2 pure payload policy. This module cannot add an app by itself: the seed is
 * only a request for ObtainX's own import/duplicate-review UI.
 * No app labels, repository credentials, fingerprints or installed APK data
 * leave Neo Store. The identifier and mapped source come from M1's validated
 * current-page snapshot and are rechecked immediately before preparation.
 */
enum class SeedRefusal {
    NOT_READY, ROUTE_CHANGED, INVALID_PACKAGE, SOURCE_MISSING,
    SOURCE_MISMATCH, SOURCE_TOO_LONG, UNSUPPORTED_ADAPTER,
}

sealed interface ObtainXSeed {
    data class Prepared(val json: String) : ObtainXSeed
    data class Refused(val reason: SeedRefusal) : ObtainXSeed
}

object ObtainXSeedPolicy {
    private const val MAX_SOURCE_CHARS = 2048
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

    /** Caller must supply the *currently visible* route, not the cached route. */
    fun prepare(snapshot: ProbeSnapshot, currentRoutePackage: String?): ObtainXSeed {
        if (snapshot.phase != ProbePhase.READY) return ObtainXSeed.Refused(SeedRefusal.NOT_READY)
        val pkg = snapshot.packageName ?: return ObtainXSeed.Refused(SeedRefusal.INVALID_PACKAGE)
        if (pkg != currentRoutePackage) return ObtainXSeed.Refused(SeedRefusal.ROUTE_CHANGED)
        if (!packagePattern.matches(pkg)) return ObtainXSeed.Refused(SeedRefusal.INVALID_PACKAGE)
        val address = snapshot.repositoryAddress
            ?: return ObtainXSeed.Refused(SeedRefusal.SOURCE_MISSING)
        if (address.length > MAX_SOURCE_CHARS || address.any { it.isISOControl() }) {
            return ObtainXSeed.Refused(SeedRefusal.SOURCE_TOO_LONG)
        }
        val canonical = SourceMapper.map(pkg, address)
        if (canonical !is SourceMapping.Mapped ||
            canonical.url != snapshot.sourceUrl ||
            canonical.overrideSource != snapshot.overrideSource
        ) return ObtainXSeed.Refused(SeedRefusal.SOURCE_MISMATCH)
        if (canonical.url.length > MAX_SOURCE_CHARS) {
            return ObtainXSeed.Refused(SeedRefusal.SOURCE_TOO_LONG)
        }
        if (canonical.overrideSource != null && canonical.overrideSource != "FDroidRepo") {
            return ObtainXSeed.Refused(SeedRefusal.UNSUPPORTED_ADAPTER)
        }
        // ObtainX's App.fromJson requires additionalSettings to be a JSON STRING.
        // Custom F-Droid repositories also require appIdOrName; its adapter is
        // never auto-selected, hence the explicit FDroidRepo override.
        val settings = if (canonical.overrideSource == "FDroidRepo") {
            "{\"appIdOrName\":${quote(pkg)}}"
        } else "{}"
        val appJson = buildString {
            append("{\"id\":").append(quote(pkg))
            append(",\"url\":").append(quote(canonical.url))
            append(",\"author\":\"\",\"name\":").append(quote(pkg))
            append(",\"latestVersion\":\"\",\"preferredApkIndex\":-1")
            append(",\"additionalSettings\":").append(quote(settings))
            if (canonical.overrideSource != null) {
                append(",\"overrideSource\":").append(quote(canonical.overrideSource))
            }
            append('}')
        }
        return ObtainXSeed.Prepared(appJson)
    }

    /** Minimal RFC 8259 JSON quoting, unit-tested without Android dependencies. */
    internal fun quote(raw: String): String = buildString {
        append('"')
        for (c in raw) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c.code < 0x20 || c == '\u2028' || c == '\u2029') {
                    append("\\u").append(c.code.toString(16).padStart(4, '0'))
                } else append(c)
            }
        }
        append('"')
    }
}
