package com.example.messenger.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.messenger.App
import com.example.messenger.ui.auth.LoginScreen
import com.example.messenger.ui.auth.RegisterScreen
import com.example.messenger.ui.chat.ChatScreen
import com.example.messenger.ui.conversations.ConversationListScreen
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CONVERSATIONS = "conversations"
    const val CHAT = "chat/{conversationId}/{peerName}/{isGroup}/{peerUsername}"

    fun chat(conversationId: String, peerName: String, isGroup: Boolean, peerUsername: String) =
        "chat/${conversationId.encodeUrl()}/${peerName.encodeUrl()}/$isGroup/${peerUsername.encodeUrl()}"

    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as App
    val scope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val saved = app.tokenStore.load()
        startDestination = if (saved != null) Routes.CONVERSATIONS else Routes.LOGIN
    }

    val start = startDestination ?: return

    suspend fun handleAuthExpired() {
        app.tokenStore.clear()
        // Do NOT clear identityStore or archiveStore here: login flow
        // regenerates keys only when a different user signs in.
        app.eventService.reset()
        navController.navigate(Routes.LOGIN) { popUpTo(0) }
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Routes.CONVERSATIONS) { popUpTo(0) } },
                onGoRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CONVERSATIONS) {
            ConversationListScreen(
                onOpenChat = { convId, peerName, isGroup, peerUsername ->
                    navController.navigate(Routes.chat(convId, peerName, isGroup, peerUsername))
                },
                onAuthExpired = {
                    scope.launch { handleAuthExpired() }
                },
                onLogout = {
                    scope.launch { handleAuthExpired() }
                }
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("peerName") { type = NavType.StringType },
                navArgument("isGroup") { type = NavType.BoolType },
                navArgument("peerUsername") { type = NavType.StringType }
            )
        ) { backStack ->
            val args = backStack.arguments!!
            val convId = java.net.URLDecoder.decode(args.getString("conversationId")!!, "UTF-8")
            val peerName = java.net.URLDecoder.decode(args.getString("peerName")!!, "UTF-8")
            val isGroup = args.getBoolean("isGroup")
            val peerUsername = java.net.URLDecoder.decode(args.getString("peerUsername")!!, "UTF-8")
            ChatScreen(
                conversationId = convId,
                peerName = peerName,
                peerUsername = peerUsername,
                isGroup = isGroup,
                onAuthExpired = {
                    scope.launch { handleAuthExpired() }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
