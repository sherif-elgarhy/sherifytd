package com.sherifytd.app.downloader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DownloadViewModel(context: Context) : ViewModel() {

    private val runner = YtdlpRunner(context)

    private val _lines   = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    private val _isDone  = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    private var job: Job? = null

    fun start(url: String, fmt: String, playlist: Boolean,
              subs: Boolean, sponsorblock: Boolean, archive: Boolean, subLangs: String) {
        job = viewModelScope.launch {
            runner.download(url, fmt, playlist, subs, sponsorblock, archive, subLangs)
                .collect { line ->
                    when (line) {
                        "__DONE__"  -> _isDone.value = true
                        "__ERROR__" -> _isError.value = true
                        else        -> _lines.value = _lines.value + line
                    }
                }
        }
    }

    fun cancel() {
        job?.cancel()
        runner.cancel()
    }
}

class DownloadViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DownloadViewModel(context.applicationContext) as T
}
