//
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//
//class ProxyViewModel : BaseViewModel() {
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