package com.constantin.microflux.module

import androidx.lifecycle.viewModelScope
import com.constantin.microflux.data.*
import com.constantin.microflux.database.Account
import com.constantin.microflux.database.EntryListPreview
import com.constantin.microflux.module.util.BaseViewModel
import com.constantin.microflux.module.util.load
import com.constantin.microflux.repository.ConstafluxRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class EntryViewModel(
    context: CoroutineContext,
    private val repository: ConstafluxRepository
) : BaseViewModel() {

    val isLogin: Boolean get() = repository.accountRepository.isAccountAvailable
    val currentAccount: Account get() = repository.accountRepository.currentAccount

    val currentTheme = viewModelScope.async(context) {
        repository.settingsRepository.getTheme().first()
    }

    protected val protectedEntriesId = MutableStateFlow<Flow<List<EntryId>>>(emptyFlow())
    val entriesId: StateFlow<Flow<List<EntryId>>?> = protectedEntriesId

    abstract fun getEntriesId(
        entryStatus: EntryStatus,
        entryStarred: EntryStarred
    )

    protected val protectedEntries = MutableStateFlow<Flow<List<EntryListPreview>>>(emptyFlow())
    val entries: StateFlow<Flow<List<EntryListPreview>>> = protectedEntries

    abstract fun getEntries(
        entryStatus: EntryStatus,
        entryStarred: EntryStarred
    )

    protected val protectedFetchEntryProgression = MutableStateFlow<Result<Unit>>(Result.complete())
    val fetchEntryProgression: StateFlow<Result<Unit>> = protectedFetchEntryProgression

    abstract fun fetchEntry(
        entryStatus: EntryStatus = EntryStatus.ALL,
        entryStarred: EntryStarred = EntryStarred.UN_STARRED,
        entryAfter: EntryPublishedAtUnix = EntryPublishedAtUnix.EMPTY,
        clearPrevious: Boolean = true,
        showAnimations: Boolean = true
    ): Job

    private val _updateEntryStatusProgression = MutableStateFlow<Result<Unit>>(Result.complete())
    val updateEntryStatusProgression = _updateEntryStatusProgression

    fun updateEntryStatus(
        entryIds: List<EntryId>,
        entryStatus: EntryStatus
    ) = viewModelScope.launch {
        _updateEntryStatusProgression.load {
            repository.entryRepository.updateStatus(
                entryIds = entryIds,
                entryStatus = entryStatus.not()
            )
        }
    }

    private val _updateEntryStarredProgression = MutableStateFlow<Result<Unit>>(Result.complete())
    val updateEntryStarredProgression: StateFlow<Result<Unit>> = _updateEntryStarredProgression

    fun updateEntryStarred(
        entryIds: List<EntryId>
    ) = viewModelScope.launch {
        _updateEntryStarredProgression.load {
            repository.entryRepository.updateStarred(
                entryIds = entryIds
            )
        }
    }

    fun onEmptyStateFetch(
        entryStatus: EntryStatus,
        entryStarred: EntryStarred
    ) = entries.flatMapLatest { it }
        .combine(updateEntryStatusProgression) { list: List<EntryListPreview>, statusResult: Result<Unit> ->
            list.isEmpty() && statusResult is Result.Complete
        }.combine(updateEntryStarredProgression) { previous: Boolean, starResult: Result<Unit> ->
            previous && starResult is Result.Complete
        }.distinctUntilChanged().onEach {
            if (it) fetchEntry(
                entryStatus = entryStatus,
                entryStarred = entryStarred,
                clearPrevious = false,
                showAnimations = false
            )
        }
}

class AllEntryViewModel(
    context: CoroutineContext,
    private val repository: ConstafluxRepository
) : EntryViewModel(context, repository) {

    override fun getEntriesId(
        entryStatus: EntryStatus,
        entryStarred: EntryStarred
    ) {
        protectedEntriesId.value = repository.entryRepository.getAllEntriesId(
            entryStatus = entryStatus,
            entryStarred = entryStarred
        )
    }

    override fun getEntries(
        entryStatus: EntryStatus,
        entryStarred: EntryStarred
    ) {
        protectedEntries.value = repository.entryRepository.getAllEntries(
            entryStatus = entryStatus,
            entryStarred = entryStarred
        )
    }

    override fun fetchEntry(
        entryStatus: EntryStatus,
        entryStarred: EntryStarred,
        entryAfter: EntryPublishedAtUnix,
        clearPrevious: Boolean,
        showAnimations: Boolean
    ) = viewModelScope.launch {
        protectedFetchEntryProgression.load(showAnimations) {
            repository.entryRepository.fetch(
                entryStarred = entryStarred,
                entryAfter = entryAfter,
                entryStatus = entryStatus,
                clearPrevious = clearPrevious
            )
        }
    }
}

class FeedEntryViewModel(
    context: CoroutineContext,
    private val repository: ConstafluxRepository,
    private val feedId: FeedId
) : EntryViewModel(context, repository) {

    override fun getEntriesId(
        entryStatus: EntryStatus,
        entryStarred: EntryStarred
    ) {
        protectedEntriesId.value =
            repository.entryRepository.getAllEntriesId(
                entryStatus = entryStatus,
                entryStarred = entryStarred,
                feedId = feedId
            )
    }

    override fun getEntries(
        entryStatus: EntryStatus,
        entryStarred: EntryStarred
    ) {
        protectedEntries.value =
            repository.entryRepository.getAllEntries(
                entryStatus = entryStatus,
                entryStarred = entryStarred,
                feedId = feedId
            )
    }

    override fun fetchEntry(
        entryStatus: EntryStatus,
        entryStarred: EntryStarred,
        entryAfter: EntryPublishedAtUnix,
        clearPrevious: Boolean,
        showAnimations: Boolean
    ) = viewModelScope.launch {
        protectedFetchEntryProgression.load(showAnimations) {
            repository.entryRepository.fetchFeed(
                feedId = feedId,
                entryStarred = entryStarred,
                entryAfter = entryAfter,
                entryStatus = entryStatus,
                clearPrevious = clearPrevious
            )
        }
    }
}