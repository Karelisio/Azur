package com.karelisio.azur.ui.places

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karelisio.azur.ui.components.cascade

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(vm: PlacesViewModel, onBack: () -> Unit, onAdded: (Long) -> Unit) {
    val state by vm.search.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour") } },
            title = {
                TextField(
                    value = state.query,
                    onValueChange = vm::onQuery,
                    placeholder = { Text("Rechercher une ville") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    trailingIcon = {
                        if (state.query.isNotEmpty()) IconButton(onClick = { vm.onQuery("") }) { Icon(Icons.Rounded.Close, "Effacer") }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            },
        )
    }) { padding ->
        Column(Modifier.padding(padding)) {
            if (state.loading) LoadingIndicator(Modifier.padding(16.dp).align(androidx.compose.ui.Alignment.CenterHorizontally))
            state.error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                itemsIndexed(state.results, key = { _, r -> "${r.id}-${r.latitude}" }) { i, r ->
                    ListItem(
                        modifier = Modifier.cascade(i).clickable { vm.add(r, onAdded) },
                        leadingContent = { Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary) },
                        headlineContent = { Text(r.name) },
                        supportingContent = {
                            Text(listOfNotNull(r.admin2, r.admin1, r.country).distinct().joinToString(", ") +
                                (r.elevation?.let { " · ${it.toInt()} m" } ?: ""))
                        },
                    )
                }
            }
        }
    }
}
