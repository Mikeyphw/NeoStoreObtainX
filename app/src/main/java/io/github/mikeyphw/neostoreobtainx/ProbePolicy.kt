package io.github.mikeyphw.neostoreobtainx

/** No Android or LSPosed dependency: pure policy used by the read-only probe. */
enum class ProbePhase { IDLE, WAITING, READY, UNSUPPORTED_SOURCE }

data class ProbeSnapshot(
    val phase: ProbePhase,
    val packageName: String? = null,
    val repositoryAddress: String? = null,
    val sourceUrl: String? = null,
    val overrideSource: String? = null,
    val productSourceSite: String? = null,
)

object ProbePolicy {
    /** Never associate cached product or repository metadata with another route. */
    fun resolve(
        activePackage: String?,
        viewModelPackage: String?,
        productPackage: String?,
        productRepositoryId: Long?,
        repositoryId: Long?,
        repositoryAddress: String?,
        productSourceSite: String? = null,
    ): ProbeSnapshot {
        if (activePackage.isNullOrBlank()) return ProbeSnapshot(ProbePhase.IDLE)
        val waiting = ProbeSnapshot(ProbePhase.WAITING, packageName = activePackage)
        if (activePackage != viewModelPackage || activePackage != productPackage ||
            productRepositoryId == null || repositoryId == null ||
            productRepositoryId != repositoryId || repositoryAddress.isNullOrBlank()
        ) return waiting
        return when (val mapped = SourceMapper.preferred(activePackage, repositoryAddress, productSourceSite)) {
            is SourceMapping.Mapped -> ProbeSnapshot(
                ProbePhase.READY, activePackage, repositoryAddress, mapped.url, mapped.overrideSource, productSourceSite,
            )
            is SourceMapping.Unsupported -> ProbeSnapshot(
                ProbePhase.UNSUPPORTED_SOURCE, packageName = activePackage,
            )
        }
    }
}
