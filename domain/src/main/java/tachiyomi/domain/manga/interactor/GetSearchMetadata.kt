package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import exh.metadata.sql.models.SearchMetadata
import tachiyomi.domain.manga.repository.MangaMetadataRepository

class GetSearchMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(mangaId: Long): SearchMetadata? {
        return mangaMetadataRepository.getMetadataById(mangaId)
    }

    suspend fun await(): List<SearchMetadata> {
        return mangaMetadataRepository.getSearchMetadata()
    }

    fun subscribe(): Flow<List<SearchMetadata>> {
        return mangaMetadataRepository.subscribeSearchMetadata()
    }
}
