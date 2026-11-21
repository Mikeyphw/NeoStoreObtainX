package io.github.mikeyphw.neostoreobtainx

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Contract-only source mapping. No APK installation or ObtainX dispatch here. */
sealed interface SourceMapping {
    data class Mapped(val url: String, val overrideSource: String? = null) : SourceMapping
    data class Unsupported(val reason: String) : SourceMapping
}

object SourceMapper {
    private val packageNamePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

    /** Prefer the actual upstream source repository only when its URL is a verified
     * ObtainX host adapter shape. Everything else falls back to the exact selected
     * Neo Store listing, never an arbitrary default store or HTML catch-all. */
    fun preferred(packageName: String, repositoryAddress: String, sourceSite: String?): SourceMapping {
        val store = map(packageName, repositoryAddress)
        if (store !is SourceMapping.Mapped) return store
        val upstream = sourceSite?.let(::supportedSourceSite) ?: return store
        return SourceMapping.Mapped(upstream)
    }

    /** Restrict to stable repository-root URLs ObtainX's source adapters understand.
     * Do not send credentialed links, file paths, issues, branches or generic HTML. */
    internal fun supportedSourceSite(raw: String): String? {
        if (raw.length > 2048 || raw.any { it.isISOControl() }) return null
        val uri = try { URI(raw.trim()) } catch (_: Exception) { return null }
        if (uri.scheme?.lowercase() != "https" || uri.userInfo != null ||
            uri.rawQuery != null || uri.rawFragment != null ||
            (uri.port != -1 && uri.port != 443)) return null
        val host = uri.host?.lowercase() ?: return null
        val segments = (uri.rawPath ?: "").split('/').filter { it.isNotEmpty() }
        if (segments.any { it == "." || it == ".." || it.contains('%') ||
                !it.matches(Regex("[A-Za-z0-9_.~-]+")) }) return null
        val two = segments.size == 2 && segments.none { it in setOf("issues", "pull", "tree", "blob") }
        val valid = when (host) {
            "github.com", "codeberg.org" -> two
            "gitlab.com" -> segments.size in 2..7 && segments.none { it in setOf("-", "issues", "merge_requests", "tree", "blob") }
            "git.sr.ht" -> segments.size == 2 && segments[0].startsWith("~")
            "sourceforge.net" -> segments.size == 2 && segments[0] == "projects"
            "www.sourceforge.net" -> segments.size == 2 && segments[0] == "projects"
            else -> false
        }
        if (!valid) return null
        return "https://$host/${segments.joinToString("/")}"
    }

    fun map(packageName: String, repositoryAddress: String): SourceMapping {
        if (!packageNamePattern.matches(packageName)) {
            return SourceMapping.Unsupported("Invalid Android package ID")
        }
        val uri = try {
            URI(repositoryAddress)
        } catch (_: Exception) {
            return SourceMapping.Unsupported("Invalid repository URI")
        }
        if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null ||
            uri.rawQuery != null || uri.rawFragment != null
        ) {
            return SourceMapping.Unsupported("Requires plain HTTPS repository URL without credentials or query")
        }
        val host = uri.host.lowercase()
        // A canonical site adapter must not silently substitute its main repo
        // for an archive, alternate path or nonstandard port on the same host.
        val path = (uri.rawPath ?: "").trimEnd('/').removeSuffix("/index.xml")
        if ((host == "f-droid.org" || host == "www.f-droid.org") &&
            (path != "/repo" || (uri.port != -1 && uri.port != 443))) {
            return SourceMapping.Unsupported("Unrecognized official repository path or port")
        }
        if (host == "apt.izzysoft.de" &&
            (path != "/fdroid/repo" || (uri.port != -1 && uri.port != 443))) {
            return SourceMapping.Unsupported("Unrecognized Izzy repository path or port")
        }
        val id = URLEncoder.encode(packageName, StandardCharsets.UTF_8.name())
        if (host == "f-droid.org" || host == "www.f-droid.org") {
            return SourceMapping.Mapped("https://f-droid.org/packages/$id")
        }
        if (host == "apt.izzysoft.de") {
            return SourceMapping.Mapped("https://apt.izzysoft.de/fdroid/index/apk/$id", overrideSource = "IzzyOnDroid")
        }
        val rawPath = (uri.rawPath ?: "").trimEnd('/')
        val repoPath = if (rawPath.endsWith("/index.xml")) rawPath.removeSuffix("/index.xml") else rawPath
        val hostWithPort = if (uri.port < 0) uri.host else "${uri.host}:${uri.port}"
        val repoBase = "https://$hostWithPort$repoPath"
        return SourceMapping.Mapped("$repoBase?appId=$id", overrideSource = "FDroidRepo")
    }
}
