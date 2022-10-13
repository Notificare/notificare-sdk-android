package re.notifica.sample.ui.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import re.notifica.Notificare
import re.notifica.authentication.ktx.authentication
import re.notifica.authentication.models.NotificareUser
import re.notifica.authentication.models.NotificareUserPreference
import re.notifica.authentication.models.NotificareUserSegment
import re.notifica.sample.models.BaseViewModel
import timber.log.Timber

class AuthenticationViewModel : BaseViewModel() {
    private val _currentUser = MutableLiveData<NotificareUser?>()
    val currentUser: LiveData<NotificareUser?> = _currentUser

    private val _fetchedSegments = MutableLiveData<List<NotificareUserSegment>>()
    val fetchedSegments: LiveData<List<NotificareUserSegment>> = _fetchedSegments

    private val _fetchedPreferences = MutableLiveData<List<NotificareUserPreference>>()
    val fetchedPreferences: LiveData<List<NotificareUserPreference>> = _fetchedPreferences

    init {
        fetchUserSegments()
        fetchUserPreferences()
        fetchUserDetails()
    }

    fun createAccount(email: String, password: String, name: String) {
        viewModelScope.launch {
            try {
                Notificare.authentication().createAccount(email, password, name)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create account.")
                showSnackBar("Failed to create account: ${e.message}")

                return@launch
            }

            Timber.i("Created account successfully.")
            showSnackBar("Created account successfully.")
        }
    }

    fun userLogin(email: String, password: String) {
        viewModelScope.launch {
            try {
                Notificare.authentication().login(email, password)
            } catch (e: Exception) {
                Timber.e(e, "Failed to login.")
                showSnackBar("Failed to login: ${e.message}")

                return@launch
            }

            fetchUserDetails()
        }
    }

    fun userLogout() {
        viewModelScope.launch {
            try {
                Notificare.authentication().logout()
            } catch (e: Exception) {
                Timber.e(e, "Failed to logout.")
                showSnackBar("Failed to logout: ${e.message}")

                return@launch
            }

            _currentUser.postValue(null)
        }
    }

    fun generatePushEmail() {
        viewModelScope.launch {
            try {
                Notificare.authentication().generatePushEmailAddress()
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate push email.")
                showSnackBar("Failed to generate push email: ${e.message}")

                return@launch
            }

            Timber.i("Push email generated successfully.")
            showSnackBar("Push email generated successfully.")

            fetchUserDetails()
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            try {
                Notificare.authentication().sendPasswordReset(email)
            } catch (e: Exception) {
                Timber.e(e, "Failed to send password reset.")
                showSnackBar("Failed to send password reset: ${e.message}")

                return@launch
            }

            Timber.i("Sent password reset successfully.")
            showSnackBar("Sent password reset successfully.")
        }
    }

    fun resetPassword(password: String, token: String) {
        viewModelScope.launch {
            try {
                Notificare.authentication().resetPassword(password, token)
            } catch (e: Exception) {
                Timber.e(e, "Failed to reset password.")
                showSnackBar("Failed to reset password: ${e.message}")

                return@launch
            }

            Timber.i("Reset password successfully.")
            showSnackBar("Reset password successfully.")
        }
    }

    fun changePassword(password: String) {
        viewModelScope.launch {
            try {
                Notificare.authentication().changePassword(password)
            } catch (e: Exception) {
                Timber.e(e, "Failed to change password.")
                showSnackBar("Failed to change password: ${e.message}")

                return@launch
            }

            Timber.i("Changed password successfully.")
            showSnackBar("Changed password successfully.")
        }
    }

    fun fetchUserDetails() {
        viewModelScope.launch {
            try {
                val userDetails = Notificare.authentication().fetchUserDetails()
                _currentUser.postValue(userDetails)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch user details.")
                showSnackBar("Failed to fetch user details: ${e.message}")

                return@launch
            }
        }
    }

    fun fetchUserSegments() {
        viewModelScope.launch {
            try {
                val userSegments = Notificare.authentication().fetchUserSegments()
                _fetchedSegments.postValue(userSegments)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch user segments.")
                showSnackBar("Failed to fetch user segments: ${e.message}")

                return@launch
            }
        }
    }

    fun addUserSegment(segment: NotificareUserSegment) {
        viewModelScope.launch {
            try {
                Notificare.authentication().addUserSegment(segment)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add user segment.")
                showSnackBar("Failed to add user segment: ${e.message}")

                return@launch
            }

            fetchUserDetails()
        }
    }

    fun removeUserSegment(segmentId: String) {
        val segment = _fetchedSegments.value?.filter { it.id == segmentId }?.get(0)

        if (segment != null) {
            viewModelScope.launch {
                try {
                    Notificare.authentication().removeUserSegment(segment)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to remove user segment.")
                    showSnackBar("Failed to remove user segment: ${e.message}")

                    return@launch
                }

                fetchUserDetails()
            }

            return
        }

        Timber.i("Segment to remove not found.")
        showSnackBar("Segment to remove not found.")
    }

    fun fetchUserPreferences() {
        viewModelScope.launch {
            try {
                val userPreferences = Notificare.authentication().fetchUserPreferences()
                _fetchedPreferences.postValue(userPreferences)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch user preferences.")
                showSnackBar("Failed to fetch user preferences: ${e.message}")

                return@launch
            }
        }
    }

    fun addUserSegmentToPreferences(preference: NotificareUserPreference, option: NotificareUserPreference.Option) {
        viewModelScope.launch {
            try {
                Notificare.authentication().addUserSegmentToPreference(option, preference)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add user segment to preference.")
                showSnackBar("Failed to add user segment to preference: ${e.message}")

                return@launch
            }

            fetchUserDetails()
        }
    }

    fun removeSegmentFromPreferences(preference: NotificareUserPreference, option: NotificareUserPreference.Option) {
        viewModelScope.launch {
            try {
                Notificare.authentication().removeUserSegmentFromPreference(option, preference)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove user segment from preference.")
                showSnackBar("Failed to remove segment from preferences: ${e.message}")

                return@launch
            }

            fetchUserDetails()
        }
    }
}
