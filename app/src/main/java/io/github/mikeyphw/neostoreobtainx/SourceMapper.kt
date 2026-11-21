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
        val id = URLEncoder.encode(packageName, StandardCharsets.UTF_8.name())
        if (host == "f-droid.org" || host == "www.f-droid.org") {
            return SourceMapping.Mapped("https://f-droid.org/packages/$id")
        }
        if (host == "apt.izzysoft.de") {
            return SourceMapping.Mapped("https://apt.izzysoft.de/fdroid/index/apk/$id")
        }
        val rawPath = (uri.rawPath ?: "").trimEnd('/')
        val repoPath = if (rawPath.endsWith("/index.xml")) rawPath.removeSuffix("/index.xml") else rawPath
        val hostWithPort = if (uri.port < 0) uri.host else "${uri.host}:${uri.port}"
        val repoBase = "https://$hostWithPort$repoPath"
        return SourceMapping.Mapped("$repoBase?appId=$id", overrideSource = "FDroidRepo")
    }
}
