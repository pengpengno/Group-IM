//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.github.im.group.api.LoginResponse
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//
//class ProxyViewModel (private val handle: SavedStateHandle): ViewModel() {
//
//    init {
//        if (handle.get<List<String>>("items").isNullOrEmpty()) {
//            handle["items"] = sampleItems
//        }
//    }
//
//    val items: List<String>
//        get() = handle["items"] ?: error("Items not found")
//
//    override fun onCleared() {
//        println("ViewModel: clear list")
//    }
//    private val _loginResult = MutableStateFlow<LoginResult?>(null)
//    val loginResult: StateFlow<LoginResult?> get() = _loginResult
//
//    fun proxyLogin(username: String, password: String, targetHost: String) {
//        viewModelScope.launch {
//            try {
//                val response = ProxyApi.proxyLogin(username, password, targetHost)
//                _loginResult.value = LoginResult.Success(response)
//            } catch (e: Exception) {
//                _loginResult.value = LoginResult.Error(e.message)
//            }
//        }
//    }
//}
//
//sealed class LoginResult {
//    data class Success(val response: LoginResponse) : LoginResult()
//    data class Error(val message: String?) : LoginResult()
//}