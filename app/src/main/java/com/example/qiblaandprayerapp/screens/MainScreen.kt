
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.qiblaandprayerapp.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.tasks.geocode.GeocodeParameters
import com.arcgismaps.tasks.geocode.GeocodeResult
import com.arcgismaps.tasks.geocode.LocatorTask
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.geoviewcompose.rememberLocationDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.res.colorResource
import com.example.qiblaandprayerapp.R

class MapViewModel() : ViewModel() {
    val map = ArcGISMap(BasemapStyle.ArcGISDarkGray).apply {
        initialViewpoint = Viewpoint(
            center = Point(
                x = -1.510556,
                y = 5.2,
                spatialReference = SpatialReference.wgs84()
            ),
            scale = 50_000.0
        )
    }

    val mapViewProxy = MapViewProxy()

    val graphicsOverlay = GraphicsOverlay()

    var geoViewExtent: Envelope = Envelope(Point(0.0, 0.0, SpatialReference.wgs84()), 0.1, 0.1)

    private val _tapLocation = MutableStateFlow<Point?>(null)
    val tapLocation: StateFlow<Point?> = _tapLocation.asStateFlow()

    private val _selectedGeoElement = MutableStateFlow<GeoElement?>(null)
    val selectedGeoElement: StateFlow<GeoElement?> = _selectedGeoElement.asStateFlow()

    private var currentIdentifyJob: Job? = null

    val locator = LocatorTask(uri = "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer")

    enum class Category(val label: String, val color: Color) {
        Mosque(label = "Mosque", color = Color.fromRgba(r =150, g = 75, b= 0, a = 255)),
    }

    fun autoFindPlaces(category: Category) {
        viewModelScope.launch {
            findPlaces(category)
        }
    }

    private var periodicUpdateJob: Job? = null

    fun startPeriodicUpdate(category: Category, interval: Long) {
        periodicUpdateJob?.cancel() // Cancel the previous job before starting a new one
        periodicUpdateJob = viewModelScope.launch {
            while (true) {
                findPlaces(category)
                delay(interval)
            }
        }
    }

    private val addedPlaces = mutableSetOf<String>()

    suspend fun findPlaces(category: Category) {
        val geocodeParameters = GeocodeParameters().apply {
            searchArea = geoViewExtent
            resultAttributeNames.addAll(listOf("Place_addr", "PlaceName"))
        }

        val geocodeResultsList = locator.geocode(
            searchText = category.label,
            parameters = geocodeParameters
        ).getOrElse { error ->
            return logError(error)
        }

        if (geocodeResultsList.isNotEmpty()) {
            val placeSymbol = PictureMarkerSymbol("https://i.ibb.co/08BQtJp/mos.png").apply {
                height = 70f
                width = 70f
                opacity = 1f
            }

            geocodeResultsList.forEach { geocodeResult ->
                val placeName = geocodeResult.attributes["PlaceName"] as? String
                if (placeName != null && !addedPlaces.contains(placeName)) {
                    val graphic = Graphic(
                        geometry = geocodeResult.displayLocation,
                        attributes = geocodeResult.attributes,
                        symbol = placeSymbol
                    )
                    graphicsOverlay.graphics.add(graphic)
                    addedPlaces.add(placeName)
                }
            }
        }
    }

    fun clearAddedPlaces() {
        graphicsOverlay.graphics.clear()
        addedPlaces.clear()
    }

    fun identify(singleTapConfirmedEvent: SingleTapConfirmedEvent) {
        currentIdentifyJob?.cancel()
        currentIdentifyJob = viewModelScope.launch {
            val result = mapViewProxy.identify(
                graphicsOverlay = graphicsOverlay,
                screenCoordinate = singleTapConfirmedEvent.screenCoordinate,
                tolerance = 2.dp
            )
            result.onSuccess { identifyGraphicsOverlayResult ->
                _selectedGeoElement.value = identifyGraphicsOverlayResult.geoElements.firstOrNull()
            }.onFailure { error ->
                logError(error)
            }
        }
    }

    fun clearSelectedGeoElement() {
        _selectedGeoElement.value = null
    }

    private fun logError(error: Throwable) {
        Log.e(this.javaClass.simpleName, error.message.toString(), error.cause)
    }

}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var queryText by remember { mutableStateOf("") }
    val currentJob = remember { mutableStateOf<Job?>(null) }
    val graphicsOverlay = remember { GraphicsOverlay() }
    val graphicsOverlays = remember { listOf(graphicsOverlay) }
    val mapViewProxy = remember { MapViewProxy() }
    val currentSpatialReference = remember { mutableStateOf<SpatialReference?>(null) }

    ArcGISEnvironment.applicationContext = context.applicationContext

    val mapViewModel: MapViewModel = viewModel()
    val selectedGeoElement = mapViewModel.selectedGeoElement.collectAsState().value

    LaunchedEffect(Unit) {
        mapViewModel.autoFindPlaces(MapViewModel.Category.Mosque)
    }
    LaunchedEffect(mapViewModel.geoViewExtent) {
        mapViewModel.startPeriodicUpdate(MapViewModel.Category.Mosque, 1000L) // Update every 5 seconds
    }

    val locationDisplay = rememberLocationDisplay().apply {
        setAutoPanMode(LocationDisplayAutoPanMode.Off) // Initially recenter on device location
    }

    val hasStartedLocation = remember { mutableStateOf(false) }

    if (checkPermissions(context)) {
        LaunchedEffect(Unit) {
            if (!hasStartedLocation.value) {
                locationDisplay.dataSource.start()
                hasStartedLocation.value = true
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Recenter)
            }
        }
    } else {
        RequestPermissions(
            context = context,
            onPermissionsGranted = {
                coroutineScope.launch {
                    locationDisplay.dataSource.start()
                }
            }
        )
    }

    Scaffold() { paddingValues ->
        SearchBar(
            colors = SearchBarDefaults.colors(
                containerColor = colorResource(id = R.color.darkishgreen),
                inputFieldColors = SearchBarDefaults.inputFieldColors(
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                    disabledTextColor = androidx.compose.ui.graphics.Color.White,
                    focusedPlaceholderColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedPlaceholderColor = androidx.compose.ui.graphics.Color.White,
                    disabledPlaceholderColor = androidx.compose.ui.graphics.Color.White,
                    cursorColor = androidx.compose.ui.graphics.Color.White,
                    focusedLeadingIconColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedLeadingIconColor = androidx.compose.ui.graphics.Color.White,
                    disabledLeadingIconColor = androidx.compose.ui.graphics.Color.White,
                    focusedTrailingIconColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTrailingIconColor = androidx.compose.ui.graphics.Color.White,
                    disabledTrailingIconColor = androidx.compose.ui.graphics.Color.White
                )),
            modifier = Modifier.fillMaxWidth(),
            query = queryText,
            onQueryChange = { query -> queryText = query },
            onSearch = { currentQuery ->
                focusManager.clearFocus()
                // Cancel any previous search job.
                currentJob.value?.cancel()
                // Start a new search job.
                currentJob.value = coroutineScope.launch {
                    currentSpatialReference.value?.let {
                        searchAddress(
                            context,
                            coroutineScope,
                            currentQuery,
                            it,
                            graphicsOverlay,
                            mapViewModel.mapViewProxy
                        )
                    }
                }
            },
            active = false,
            onActiveChange = { /* Use this for dynamic search results */ },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            placeholder = { Text("Search for an address") }
        ) {}

        MapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            arcGISMap = mapViewModel.map,
            locationDisplay = locationDisplay,
            onVisibleAreaChanged = { newVisibleArea ->
                mapViewModel.geoViewExtent = newVisibleArea.extent
            },
            onSpatialReferenceChanged = { spatialReference ->
                currentSpatialReference.value = spatialReference
            },
            mapViewProxy = mapViewModel.mapViewProxy,
            graphicsOverlays = listOf(mapViewModel.graphicsOverlay),
            onSingleTapConfirmed = { singleTapConfirmedEvent ->
                mapViewModel.identify(singleTapConfirmedEvent)
                locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.Off) // Disable recenter after a tap
            },
            content = if (selectedGeoElement != null) {
                {
                    Callout(
                        modifier = Modifier
                            .wrapContentSize()
                            .height(120.dp)
                            .widthIn(max = 300.dp),
                        geoElement = selectedGeoElement,
                        tapLocation = mapViewModel.tapLocation.value
                    ) {
                        CalloutContent(
                            selectedElementAttributes = selectedGeoElement.attributes
                        )
                    }
                }
            } else {
                null
            }
        )
    }
}



@Composable
fun CalloutContent(
    selectedElementAttributes: Map<String, Any?>
) {
    LazyColumn(contentPadding = PaddingValues(8.dp)) {
        selectedElementAttributes.forEach { attribute ->
            item {
                val style = if (attribute.key == "PlaceName") {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.bodyMedium
                }

                Text(
                    text = "${attribute.value}",
                    fontStyle = FontStyle.Normal,
                    style = style,
                    textAlign = TextAlign.Start
                )

            }
        }
    }
}

fun checkPermissions(context: Context): Boolean {
    val permissionCheckCoarseLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val permissionCheckFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return permissionCheckCoarseLocation && permissionCheckFineLocation
}

@Composable
fun RequestPermissions(context: Context, onPermissionsGranted: () -> Unit) {

    // Create an activity result launcher using permissions contract and handle the result.
    val activityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if both fine & coarse location permissions are true.
        if (permissions.all { it.value }) {
            onPermissionsGranted()
        } else {
            showError(context, "Location permissions were denied")
        }
    }

    LaunchedEffect(Unit) {
        activityResultLauncher.launch(
            // Request both fine and coarse location permissions.
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

}

fun showError(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

suspend fun searchAddress(
    context: Context,
    coroutineScope: CoroutineScope,
    query: String,
    currentSpatialReference: SpatialReference,
    graphicsOverlay: GraphicsOverlay,
    mapViewProxy: MapViewProxy
){
    val geocodeServerUri = "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer"
    val locatorTask = LocatorTask(geocodeServerUri)

    val geocodeParameters = GeocodeParameters().apply{
        resultAttributeNames.add("*")
        maxResults = 1
        outputSpatialReference = currentSpatialReference
    }

    locatorTask.geocode(searchText = query, parameters = geocodeParameters)
        .onSuccess { geocodeResults: List<GeocodeResult> ->
            handleGeocodeResults(
                context, coroutineScope, geocodeResults, graphicsOverlay, mapViewProxy

            )
        }.onFailure { error ->
            showMessage(context,"Call failed: ${error.message}")
        }
}

fun createTextGraphic(geocodeResult: GeocodeResult): Graphic{
    val textSymbol = TextSymbol(
        text = geocodeResult.label,
        color = Color.black,
        size = 18f,
        horizontalAlignment = HorizontalAlignment.Center,
        verticalAlignment = VerticalAlignment.Bottom
    ).apply {
        offsetY = 8f
        haloColor = Color.white
        haloWidth = 2f
    }
    return Graphic(
        geometry = geocodeResult.displayLocation,
        symbol = textSymbol
    )
}

fun createMarkerGraphic(geocodeResult: GeocodeResult): Graphic {
    val simpleMarkerSymbol = SimpleMarkerSymbol(
        style = SimpleMarkerSymbolStyle.Square,
        color = Color.red,
        size = 12.0f
    )
    return Graphic(
        geometry = geocodeResult.displayLocation,
        attributes = geocodeResult.attributes,
        symbol = simpleMarkerSymbol
    )
}

fun handleGeocodeResults(
    context: Context,
    coroutineScope: CoroutineScope,
    geocodeResults: List<GeocodeResult>,
    graphicsOverlay: GraphicsOverlay,
    mapViewProxy: MapViewProxy
) {

    if (geocodeResults.isNotEmpty()) {
        val geocodeResult = geocodeResults[0]
        // Create a Text graphic to display the address text, and add it to the graphics overlay.
        val textGraphic = createTextGraphic(geocodeResult)
        // Create a red square marker graphic, and add it to the graphics overlay.
        val markerGraphic = createMarkerGraphic(geocodeResult)
        // Clear previous results and add graphics.
        graphicsOverlay.graphics.apply {
            clear()
            add(textGraphic)
            add(markerGraphic)
        }

        coroutineScope.launch {
            val centerPoint = geocodeResult.displayLocation
                ?: return@launch showMessage(context, "The locatorTask.geocode() call failed")

            // Animate the map view to the center point.
            mapViewProxy.setViewpointCenter(centerPoint)
                .onFailure { error ->
                    showMessage(context, "Failed to set Viewpoint center: ${error.message}")
                }

        }

    } else {
        showMessage(context, "No address found for the given query")
    }

}

fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
