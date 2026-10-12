package com.edu.unal.tictactoe.ui.online.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.R
import kotlinx.coroutines.launch

/** A transient notice the lobby shows over its content. */
enum class LobbyMessage {
    /** The chosen game was taken or removed, or the own waiting game vanished on its own. */
    GameUnavailable,

    /** The service did not complete the creation of the own game. */
    CreateFailed,
}

internal object OnlineLobbyTags {
    const val CONNECTING = "lobby_connecting"
    const val RETRY = "lobby_retry"
    const val OFFLINE = "lobby_offline"
    const val HEADER = "lobby_games_header"
    const val LIST = "lobby_games_list"
    const val NO_GAMES = "lobby_no_games"
    const val CREATE = "lobby_create_game"
    const val CHANGE_NICKNAME = "lobby_change_nickname"
    const val SHARE_APP = "lobby_share_app"
    const val BACK_TO_START = "lobby_back_to_start"

    fun game(id: String): String = "lobby_game_$id"
}

private val MinActionHeight = 48.dp
private val ActionsPaneWidth = 240.dp

/**
 * Lobby of the online mode. It is stateless: everything it shows comes from the parameters and
 * every action only calls its callback. It has no board and no top bar and respects the system
 * window insets, like the start screen.
 *
 * What it shows depends on [connection]: only "connecting" while it is pending; only the failure
 * and [onRetry] when the identity could not be established; otherwise the "available games"
 * header with one item per entry of [games] (or the empty notice), [onCreateGame] and
 * [onChangeNickname] and, only when [onShareApp] is not null, "share app" right below the latter
 * with the same emphasis. While offline it adds the offline notice and disables creating and
 * joining ([onJoinGame]), exposing that state to assistive technologies.
 *
 * In every state it offers [onBackToStart], a low-emphasis text button that is always enabled
 * because it does not need the service: after the "connecting" text, after "retry", and below
 * [onChangeNickname], so it never competes with "create game".
 *
 * In portrait the games list takes the free height and the actions sit below it; in landscape the
 * list takes the left part and the actions a scrollable column on the right, so a long list or a
 * large font scale never leaves anything unreachable.
 *
 * [message] is the notice to show over the content, or null. The screen calls [onMessageShown]
 * once it has handed the notice to the snackbar, so the caller can consume it; the snackbar then
 * goes away on its own.
 */
@Composable
fun OnlineLobbyScreen(
    connection: LobbyConnection,
    games: List<LobbyGame>,
    message: LobbyMessage?,
    onMessageShown: () -> Unit,
    onRetry: () -> Unit,
    onCreateGame: () -> Unit,
    onJoinGame: (String) -> Unit,
    onChangeNickname: () -> Unit,
    onShareApp: (() -> Unit)?,
    onBackToStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messageText = when (message) {
        LobbyMessage.GameUnavailable -> stringResource(R.string.lobby_game_unavailable)
        LobbyMessage.CreateFailed -> stringResource(R.string.lobby_create_failed)
        null -> null
    }
    LaunchedEffect(message) {
        if (messageText != null) {
            scope.launch { snackbarHostState.showSnackbar(messageText) }
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            when (connection) {
                LobbyConnection.Connecting -> CenteredContent {
                    Text(
                        text = stringResource(R.string.lobby_connecting),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(OnlineLobbyTags.CONNECTING),
                    )
                    BackToStartButton(onClick = onBackToStart)
                }

                LobbyConnection.IdentityFailed -> CenteredContent {
                    Text(
                        text = stringResource(R.string.lobby_connection_failed),
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .heightIn(min = MinActionHeight)
                            .testTag(OnlineLobbyTags.RETRY),
                    ) {
                        Text(text = stringResource(R.string.lobby_retry))
                    }
                    BackToStartButton(onClick = onBackToStart)
                }

                LobbyConnection.Offline, LobbyConnection.Connected -> GamesContent(
                    online = connection == LobbyConnection.Connected,
                    games = games,
                    onCreateGame = onCreateGame,
                    onJoinGame = onJoinGame,
                    onChangeNickname = onChangeNickname,
                    onShareApp = onShareApp,
                    onBackToStart = onBackToStart,
                )
            }
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun GamesContent(
    online: Boolean,
    games: List<LobbyGame>,
    onCreateGame: () -> Unit,
    onJoinGame: (String) -> Unit,
    onChangeNickname: () -> Unit,
    onShareApp: (() -> Unit)?,
    onBackToStart: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val list = @Composable { listModifier: Modifier ->
            GamesList(
                online = online,
                games = games,
                onJoinGame = onJoinGame,
                modifier = listModifier,
            )
        }
        val actions = @Composable {
            LobbyActions(
                createEnabled = online,
                onCreateGame = onCreateGame,
                onChangeNickname = onChangeNickname,
                onShareApp = onShareApp,
                onBackToStart = onBackToStart,
            )
        }
        if (landscape) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                list(Modifier.weight(1f))
                Column(
                    modifier = Modifier
                        .width(ActionsPaneWidth)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    actions()
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                list(Modifier.weight(1f))
                actions()
            }
        }
    }
}

@Composable
private fun GamesList(
    online: Boolean,
    games: List<LobbyGame>,
    onJoinGame: (String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!online) {
            Text(
                text = stringResource(R.string.lobby_offline),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(OnlineLobbyTags.OFFLINE),
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(OnlineLobbyTags.LIST),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.lobby_games_header),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag(OnlineLobbyTags.HEADER),
                )
            }
            if (games.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lobby_no_games),
                        modifier = Modifier.testTag(OnlineLobbyTags.NO_GAMES),
                    )
                }
            } else {
                items(games, key = { it.id }) { game ->
                    GameItem(game = game, enabled = online, onClick = { onJoinGame(game.id) })
                }
            }
        }
    }
}

@Composable
private fun GameItem(game: LobbyGame, enabled: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinActionHeight)
            .testTag(OnlineLobbyTags.game(game.id)),
    ) {
        Text(
            text = stringResource(R.string.lobby_game_item, game.hostNickname),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinActionHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun LobbyActions(
    createEnabled: Boolean,
    onCreateGame: () -> Unit,
    onChangeNickname: () -> Unit,
    onShareApp: (() -> Unit)?,
    onBackToStart: () -> Unit,
) {
    Button(
        onClick = onCreateGame,
        enabled = createEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinActionHeight)
            .testTag(OnlineLobbyTags.CREATE),
    ) {
        Text(text = stringResource(R.string.lobby_create_game))
    }
    OutlinedButton(
        onClick = onChangeNickname,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinActionHeight)
            .testTag(OnlineLobbyTags.CHANGE_NICKNAME),
    ) {
        Text(text = stringResource(R.string.nickname_change))
    }
    if (onShareApp != null) {
        OutlinedButton(
            onClick = onShareApp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinActionHeight)
                .testTag(OnlineLobbyTags.SHARE_APP),
        ) {
            Text(text = stringResource(R.string.share_app))
        }
    }
    BackToStartButton(onClick = onBackToStart, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun BackToStartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = MinActionHeight)
            .testTag(OnlineLobbyTags.BACK_TO_START),
    ) {
        Text(text = stringResource(R.string.back_to_start))
    }
}
