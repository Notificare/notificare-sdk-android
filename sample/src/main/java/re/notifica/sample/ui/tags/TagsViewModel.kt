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

    fun fetchTags() {
        viewModelScope.launch {
            try {
                val tags = Notificare.device().fetchTags()
                _fetchedTags.postValue(tags)

                Timber.i("Tags fetched successfully.")
                showSnackBar("Tags fetched successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch tags.")
                showSnackBar("Failed to fetch tags: ${e.message}")
            }
        }
    }

    fun addTags(tags: List<String>) {
        viewModelScope.launch {
            try {
                Notificare.device().addTags(tags)
                fetchTags()

                Timber.i("Tags add successfully.")
                showSnackBar("Tags add successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to add tags.")
                showSnackBar("Failed to add tags: ${e.message}")
            }
        }
    }

    fun removeTag(tag: String) {
        viewModelScope.launch {
            try {
                Notificare.device().removeTag(tag)
                fetchTags()

                Timber.i("Tag removed successfully.")
                showSnackBar("Tag removed successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove tag.")
                showSnackBar("Failed to remove tag: ${e.message}")
            }
        }
    }

    fun clearTags() {
        viewModelScope.launch {
            try {
                Notificare.device().clearTags()
                fetchTags()

                Timber.i("Cleared tags successfully.")
                showSnackBar("Cleared tags successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear tags.")
                showSnackBar("Failed to clear tags: ${e.message}")
            }
        }
    }
}
