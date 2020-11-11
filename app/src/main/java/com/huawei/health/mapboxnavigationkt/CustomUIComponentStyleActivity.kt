package com.huawei.health.mapboxnavigationkt

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.ui.NavigationButton
import com.mapbox.navigation.ui.NavigationConstants
import com.mapbox.navigation.ui.SoundButton
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheetListener
import com.mapbox.navigation.ui.feedback.FeedbackItem
import com.mapbox.navigation.ui.instruction.NavigationAlertView
import com.mapbox.navigation.ui.internal.utils.ViewUtils
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.summary.SummaryBottomSheet
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader
import kotlinx.android.synthetic.main.activity_custom_ui_component_style.*
import okhttp3.Cache
import timber.log.Timber
import java.io.File
import java.util.*

/**
 * 自定义的导航ui
 */
class CustomUIComponentStyleActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    FeedbackBottomSheetListener {

    private val routeOverviewPadding by lazy { buildRouteOverviewPadding() }

    private lateinit var mapboxNavigation: MapboxNavigation // 告诉编译器，这个变量会被初始化，并且不会为null，但是在声明这里，我暂时还不知道什么时候会被初始化
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private lateinit var speechPlayer: NavigationSpeechPlayer
    private lateinit var destination: LatLng
    private lateinit var summaryBehavior: BottomSheetBehavior<SummaryBottomSheet>  // 底部弹窗
    private lateinit var routeOverviewButton: ImageButton
    private lateinit var cancelBtn: AppCompatImageButton
    private lateinit var feedbackButton: NavigationButton
    private lateinit var instructionSoundButton: NavigationButton
    private lateinit var alertView: NavigationAlertView
    private val mapboxReplayer = MapboxReplayer()

    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null

    private var feedbackItem: FeedbackItem? = null
    private var feedbackEncodedScreenShot: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate savedInstanceState=%s", savedInstanceState)
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_custom_ui_component_style)
        initViews()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        initNavigation()
        initializeSpeechPlayer()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        Timber.d("onMapReady")
        // 初始化并且设置zoom
        this.mapboxMap = mapboxMap
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))

        // 长按事件
        mapboxMap.addOnMapLongClickListener { latLng ->
            Timber.d("onMapLongClickListener position=%s", latLng)
            destination = latLng
            locationComponent?.lastKnownLocation?.let { location ->  }

            locationComponent?.lastKnownLocation?.let { originLocation ->
                mapboxNavigation.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(originLocation.toPoint(), null, latLng.toPoint())
                        .alternatives(true)
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build(),
                    routesReqCallback
                )
            }
            true
        }

        // 设置样式
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            // Map已设置，样式已加载。现在您可以添加数据或进行其他地图调整。
        }

//        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
//            locationComponent = mapboxMap.locationComponent.apply {
//                activateLocationComponent(
//                    LocationComponentActivationOptions.builder(
//                        this@CustomUIComponentStyleActivity,
//                        style
//                    ).build()
//                )
//                cameraMode = CameraMode.TRACKING
//                isLocationComponentEnabled = true
//            }
//
//            navigationMapboxMap = NavigationMapboxMap.Builder(mapView, mapboxMap, this)
//                .vanishRouteLineEnabled(true)
//                .build().apply {
//                    addOnCameraTrackingChangedListener(cameraTrackingChangedListener)
//                    addProgressChangeListener(mapboxNavigation)
//                    setCamera(DynamicCamera(mapboxMap))
//                }
//
//            if (shouldSimulateRoute()) {
//                mapboxNavigation
//                    .registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
//                mapboxReplayer.pushRealLocation(this, 0.0)
//                mapboxReplayer.play()
//            }
//            mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
//                locationListenerCallback
//            )
//
//            Snackbar.make(
//                findViewById(R.id.navigationLayout),
//                R.string.msg_long_press_map_to_place_waypoint,
//                Snackbar.LENGTH_SHORT
//            ).show()
//        }
    }

    private fun initNavigation() {
        val accessToken = Utils.getMapboxAccessToken(this)
        mapboxNavigation = MapboxNavigation(
            MapboxNavigation.defaultNavigationOptionsBuilder(this, accessToken)
                .locationEngine(getLocationEngine())
                .build()
        )
        mapboxNavigation.apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerRouteProgressObserver(routeProgressObserver)
            registerBannerInstructionsObserver(bannerInstructionObserver)
            registerVoiceInstructionsObserver(voiceInstructionsObserver)
        }
    }

    private fun initializeSpeechPlayer() {
        val cache =
            Cache(
                File(application.cacheDir, InstructionViewActivity.VOICE_INSTRUCTION_CACHE),
                10 * 1024 * 1024
            )
        val voiceInstructionLoader =
            VoiceInstructionLoader(application, Mapbox.getAccessToken(), cache)
        val speechPlayerProvider =
            SpeechPlayerProvider(application, Locale.US.language, true, voiceInstructionLoader)
        speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun initViews() {
        // apply一般用于一个对象实例初始化的时候，需要对对象中的属性进行赋值。或者动态inflate出一个XML的View的时候需要给View绑定数据也会用到，这种情景非常常见。特别是在我们开发中会有一些数据model向View model转化实例化的过程中需要用到。
        startNavigation.apply {
            visibility = View.VISIBLE
            isEnabled = false
            // 点击事件
            setOnClickListener {
                Timber.d("start navigation")
                if (mapboxNavigation.getRoutes().isNotEmpty()) {
                    // 如果路由不为空
                    updateCameraOnNavigationStateChange(true)
                    // 启动路线
                    navigationMapboxMap?.startCamera(mapboxNavigation.getRoutes()[0])
                    // 监听位置更新
                    mapboxNavigation.startTripSession()
                }
            }
        }

        summaryBottomSheet.visibility = View.GONE
        summaryBehavior = BottomSheetBehavior.from(summaryBottomSheet).apply {
            isHideable = false
            setBottomSheetCallback(bottomSheetCallback)
        }

        routeOverviewButton = findViewById(R.id.routeOverviewBtn)
        routeOverviewButton.setOnClickListener {
            // 将地图摄像机调整为正在行进的
            navigationMapboxMap?.showRouteOverview(routeOverviewPadding)
            // 显示重新激活按钮
            recenterBtn.show()
        }

        cancelBtn = findViewById(R.id.cancelBtn)
        cancelBtn.setOnClickListener {
            // 停止监听位置更新，并进入“空闲”状态。
            mapboxNavigation.stopTripSession()
        }

        recenterBtn.apply {
            hide()
            addOnClickListener {
                recenterBtn.hide()
                summaryBehavior.isHideable = false
                summaryBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                showWayNameView()
                navigationMapboxMap?.resetPadding()
                navigationMapboxMap
                    ?.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
            }
        }

        wayNameView.apply {
            visibility = View.GONE
        }

        feedbackButton = instructionView.retrieveFeedbackButton().apply {
            hide()
            addOnClickListener {
                showFeedbackBottomSheet()
            }
        }
        instructionSoundButton = instructionView.retrieveSoundButton().apply {
            hide()
            addOnClickListener {
                val soundButton = instructionSoundButton
                if (soundButton is SoundButton) {
                    speechPlayer.isMuted = soundButton.toggleMute()
                }
            }
        }

        alertView = instructionView.retrieveAlertView().apply {
            hide()
        }

    }

    // Callbacks and Observers
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoute(routes[0])
                startNavigation.visibility = View.VISIBLE
                startNavigation.isEnabled = true
            } else {
                startNavigation.isEnabled = false
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    /**
     * 导航状态改变
     */
    private fun updateCameraOnNavigationStateChange(navigationStarted: Boolean) {
        navigationMapboxMap?.apply {
            if (navigationStarted) {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                updateLocationLayerRenderMode(RenderMode.GPS)
            } else {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                updateLocationLayerRenderMode(RenderMode.COMPASS)
            }
        }
    }

    /**
     * 底部窗口事件
     */
    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (summaryBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                recenterBtn.show()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    }

    private fun showWayNameView() {
        wayNameView.updateVisibility(!wayNameView.retrieveWayNameText().isNullOrEmpty())
    }

    private fun showFeedbackBottomSheet() {
        feedbackItem = null
        feedbackEncodedScreenShot = null
        supportFragmentManager.let {
            mapboxMap?.snapshot(this::encodeSnapshot)
            FeedbackBottomSheet.newInstance(
                this,
                NavigationConstants.FEEDBACK_BOTTOM_SHEET_DURATION
            )
                .show(it, FeedbackBottomSheet.TAG)
        }
    }

    private fun encodeSnapshot(snapshot: Bitmap) {
        screenshotView.visibility = View.VISIBLE
        screenshotView.setImageBitmap(snapshot)
        mapView.visibility = View.INVISIBLE
        feedbackEncodedScreenShot = ViewUtils.encodeView(ViewUtils.captureView(mapView))
        screenshotView.visibility = View.INVISIBLE
        mapView.visibility = View.VISIBLE

        sendFeedback()
    }

    private fun sendFeedback() {
        val feedback = feedbackItem
        val screenShot = feedbackEncodedScreenShot
        if (feedback != null && !screenShot.isNullOrEmpty()) {
            mapboxNavigation.postUserFeedback(
                feedback.feedbackType,
                feedback.description,
                FeedbackEvent.UI,
                screenShot,
                feedback.feedbackSubType.toTypedArray()
            )
            showFeedbackSentSnackBar(
                context = this,
                view = if (summaryBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    recenterBtn
                } else {
                    summaryBottomSheet
                },
                setAnchorView = true
            )
        }
    }

    private fun buildRouteOverviewPadding(): IntArray {
        val leftRightPadding =
            resources
                .getDimension(
                    com.mapbox.navigation.ui.R.dimen.mapbox_route_overview_left_right_padding
                )
                .toInt()
        val paddingBuffer =
            resources
                .getDimension(
                    com.mapbox.navigation.ui.R.dimen.mapbox_route_overview_buffer_padding
                )
                .toInt()
        val instructionHeight =
            (
                    resources
                        .getDimension(
                            com.mapbox.navigation.ui.R.dimen.mapbox_instruction_content_height
                        ) + paddingBuffer
                    )
                .toInt()
        val summaryHeight =
            resources
                .getDimension(com.mapbox.navigation.ui.R.dimen.mapbox_summary_bottom_sheet_height)
                .toInt()
        return intArrayOf(leftRightPadding, instructionHeight, leftRightPadding, summaryHeight)
    }

    fun showFeedbackSentSnackBar(
        context: Context,
        view: View,
        @StringRes message: Int = R.string.mapbox_feedback_reported,
        length: Int = Snackbar.LENGTH_SHORT,
        setAnchorView: Boolean = false
    ) {
        val snackBar = Snackbar.make(
            view,
            message,
            length
        )

        if (setAnchorView) {
            snackBar.anchorView = view
        }

        snackBar.view.setBackgroundColor(
            ContextCompat.getColor(
                context,
                com.mapbox.navigation.ui.R.color.mapbox_feedback_bottom_sheet_secondary
            )
        )
        snackBar.setTextColor(
            ContextCompat.getColor(
                context,
                com.mapbox.navigation.ui.R.color.mapbox_feedback_bottom_sheet_primary_text
            )
        )

        snackBar.show()
    }

    override fun onFeedbackDismissed() {
        // do nothing
    }

    /**
     * InstructionView反馈底部表侦听器
     */
    override fun onFeedbackSelected(feedbackItem: FeedbackItem?) {
        feedbackItem?.let { feedback ->
            this.feedbackItem = feedback
            sendFeedback()
        }
    }

    /**
     * 如果shouldSimulateRoute为真，将使用ReplayRouteLocationEngine
     * 对于其他测试，使用了一个真实的位置引擎。
     */
    private fun getLocationEngine(): LocationEngine {
        return if (shouldSimulateRoute()) {
            ReplayLocationEngine(mapboxReplayer)
        } else {
            LocationEngineProvider.getBestLocationEngine(this)
        }
    }

}