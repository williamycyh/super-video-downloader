package com.example.tubedown.main.history

import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.example.data.local.room.entity.HistoryItem
import com.example.data.repository.HistoryRepository
import com.example.tubedown.main.base.BaseViewModel
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) :
    BaseViewModel() {

    var historyItems = ObservableField<List<HistoryItem>>(emptyList())

    var searchHistoryItems = ObservableField<List<HistoryItem>>(emptyList())

    val searchQuery = ObservableField("")

    val isLoadingHistory = ObservableField(true)

    val executorSingleHistory = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val historyExecutor = Executors.newFixedThreadPool(1).asCoroutineDispatcher()

    private val additionalExecutor = Executors.newFixedThreadPool(1).asCoroutineDispatcher()

    override fun start() {
        fetchAllHistory()
    }

    override fun stop() {
    }

    private fun fetchAllHistory() {
        isLoadingHistory.set(true)

        viewModelScope.launch(additionalExecutor) {
            val history = historyRepository.getAllHistory().blockingFirst()
            historyItems.set(history)
            isLoadingHistory.set(false)
        }
    }

    fun saveHistory(historyItem: HistoryItem) {
        viewModelScope.launch(historyExecutor) {
            try {
                historyRepository.saveHistory(historyItem)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun deleteHistory(historyItem: HistoryItem) {
        viewModelScope.launch(historyExecutor) {
            try {
                val newItems = historyItems.get()?.filter { it.id != historyItem.id }
                historyItems.set(newItems)
                historyRepository.deleteHistory(historyItem)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun queryHistory(query: String) {
        if (query.isEmpty()) {
            searchHistoryItems.set(emptyList())
        }
        if (query.isNotEmpty()) {
            val filtered = historyItems.get()
                ?.filter { it.url.contains(query) || it.title?.contains(query) ?: false }
            searchHistoryItems.set(filtered ?: emptyList())
        }
    }

    fun clearHistory() {
        viewModelScope.launch(historyExecutor) {
            isLoadingHistory.set(true)
            historyRepository.deleteAllHistory()
            historyItems.set(emptyList())
            searchHistoryItems.set(emptyList())
            isLoadingHistory.set(false)
        }
    }
}