package re.notifica.sample.ui.tags

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.ktx.device
import re.notifica.sample.models.BaseViewModel
import timber.log.Timber

class TagsViewModel : BaseViewModel() {
    private val _fetchedTags = MutableLiveData<List<String>>()
    val fetchedTag: LiveData<List<String>> = _fetchedTags

    val defaultTags = listOf("Kotlin", "Java", "Swift", "Python")
    val selectedTags = mutableListOf<String>()

    fun fetchTags() {
        viewModelScope.launch {
            try {
                val tags = Notificare.device().fetchTags()
                _fetchedTags.postValue(tags)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch tags.")
                showSnackBar("Failed to fetch tags: ${e.message}")
            }
        }
    }

    fun addTags() {
        viewModelScope.launch {
            try {
                Notificare.device().addTags(selectedTags)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add tags.")
                showSnackBar("Failed to add tags: ${e.message}")

                selectedTags.clear()
                return@launch
            }

            selectedTags.clear()
            fetchTags()
        }
    }

    fun removeTag(tag: String) {
        viewModelScope.launch {
            try {
                Notificare.device().removeTag(tag)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove tag.")
                showSnackBar("Failed to remove tag: ${e.message}")

                return@launch
            }

            fetchTags()
        }
    }

    fun clearTags() {
        viewModelScope.launch {
            try {
                Notificare.device().clearTags()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear tags.")
                showSnackBar("Failed to clear tags: ${e.message}")

                return@launch
            }

            fetchTags()
        }
    }
}
