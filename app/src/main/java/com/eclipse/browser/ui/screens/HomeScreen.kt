package com.eclipse.browser.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.components.*
import com.eclipse.browser.ui.theme.*
import com.eclipse.browser.ui.viewmodel.EclipseUiState

@Composable
fun HomeScreen(
    state: EclipseUiState,
    onSearch: (String) -> Unit,
    onTabSelected: (String) -> Unit,
    onSiteClick: (String) -> Unit,
    onSiteLongPress: (Int) -> Unit,
    onAddSiteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    val heroOffset by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0) scrollState.firstVisibleItemScrollOffset * 0.5f
            else 0f
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background layers
        if (state.starsOn) {
            StarFieldCanvas(
                isNight = state.isNight,
                bgTheme = state.bgTheme
            )
        }
        if (state.particlesOn) {
            ParticleSystem(accentColor = state.accentColor)
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // Top bar
            item {
                EclipseEnterAnimation(index = 0) {
                    TopBar(
                        adBlockOn = state.adBlockOn,
                        adsBlocked = state.adsBlocked
                    )
                }
            }

            // Hero block
            item {
                EclipseEnterAnimation(index = 1) {
                    HeroBlock(
                        greeting = state.greeting,
                        greetingWord = state.greetingWord,
                        date = state.date,
                        clock = state.clock,
                        isNight = state.isNight,
                        accentColor = state.accentColor,
                        orbOn = state.orbOn,
                        weather = state.weather,
                        weatherOn = state.weatherOn,
                        modifier = Modifier.graphicsLayer { translationY = -heroOffset }
                    )
                }
            }

            // Search
            item {
                EclipseEnterAnimation(index = 2) {
                    SearchBar(
                        accentColor = state.accentColor,
                        onSearch = onSearch
                    )
                }
            }

            // Filter tabs
            item {
                EclipseEnterAnimation(index = 3) {
                    FilterTabs(
                        activeTab = state.activeSearchTab,
                        accentColor = state.accentColor,
                        onTabSelected = onTabSelected
                    )
                }
            }

            // Quick sites
            item {
                EclipseEnterAnimation(index = 4) {
                    QuickSitesGrid(
                        sites = state.quickSites,
                        accentColor = state.accentColor,
                        onSiteClick = onSiteClick,
                        onSiteLongPress = onSiteLongPress,
                        onAddClick = onAddSiteClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    adBlockOn: Boolean,
    adsBlocked: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "ECLIPSE",
            style = BrandText,
            color = TextPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (adBlockOn) "🛡 Shield ON · $adsBlocked blocked" else "Shield OFF",
                color = if (adBlockOn) AccentGold.copy(alpha = 0.7f) else TextMuted,
                fontSize = 10.sp,
                fontFamily = SpaceMono,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun HeroBlock(
    greeting: String,
    greetingWord: String,
    date: String,
    clock: String,
    isNight: Boolean,
    accentColor: Color,
    orbOn: Boolean,
    weather: com.eclipse.browser.ui.viewmodel.WeatherData,
    weatherOn: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Date
            Text(
                text = date.uppercase(),
                style = DateLabel,
                color = TextMuted2
            )
            Spacer(Modifier.height(6.dp))

            // Greeting with animated fade-in
            val greetingText = buildAnnotatedString {
                append("Good ")
                withStyle(SpanStyle(color = accentColor)) {
                    append(greetingWord)
                }
                append(", Explorer")
            }

            var greetingVisible by remember { mutableStateOf(false) }
            LaunchedEffect(greeting) {
                greetingVisible = false
                kotlinx.coroutines.delay(100)
                greetingVisible = true
            }

            val greetingAlpha by animateFloatAsState(
                targetValue = if (greetingVisible) 1f else 0f,
                animationSpec = tween(500),
                label = "greetAlpha"
            )
            val greetingOffset by animateFloatAsState(
                targetValue = if (greetingVisible) 0f else 12f,
                animationSpec = tween(500),
                label = "greetOffset"
            )

            Text(
                text = greetingText,
                style = GreetingMain,
                color = TextPrimary,
                modifier = Modifier.graphicsLayer {
                    alpha = greetingAlpha
                    translationY = greetingOffset
                }
            )

            Spacer(Modifier.height(4.dp))

            // Clock
            Text(
                text = clock,
                style = ClockText,
                color = TextMuted
            )

            // Night tagline
            if (isNight) {
                Spacer(Modifier.height(8.dp))
                NightTagline(accentColor = accentColor)
            }

            // Weather
            if (weatherOn && weather.temp != "--°") {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${weather.temp} ${weather.condition}",
                    fontSize = 11.sp,
                    fontFamily = SpaceMono,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }
        }

        // Eclipse Orb
        EclipseOrb(visible = orbOn)
    }
}

@Composable
private fun NightTagline(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "nightTagline")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nightAlpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("·", color = accentColor.copy(alpha = 0.5f), fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "FIND THE UNSEEN",
            fontFamily = SpaceMono,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = Color.White.copy(alpha = alpha)
        )
        Spacer(Modifier.width(8.dp))
        Text("·", color = accentColor.copy(alpha = 0.5f), fontSize = 18.sp)
    }
}
