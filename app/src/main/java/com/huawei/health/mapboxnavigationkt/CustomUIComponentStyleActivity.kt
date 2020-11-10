package com.huawei.health.mapboxnavigationkt

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_custom_ui_component_style.*
import timber.log.Timber

/**
 * 自定义的导航ui
 */
class CustomUIComponentStyleActivity : AppCompatActivity() {

    private lateinit var mapboxNavigation: MapboxNavigation // 告诉编译器，这个变量会被初始化，并且不会为null，但是在声明这里，我暂时还不知道什么时候会被初始化
    private var navigationMapboxMap: NavigationMapboxMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate savedInstanceState=%s", savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_ui_component_style)
        initViews()
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

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun initViews() {
        startNavigation.apply {
            visibility = View.VISIBLE
            isEnabled = false
            // 点击事件
            setOnClickListener {
                Timber.d("start navigation")
                if (mapboxNavigation.getRoutes().isNotEmpty()) {
                    // 如果路由为空
                    updateCameraOnNavigationStateChange(true)
                }
            }
        }
    }

    /**
     * 导航状态改变
     */
    private fun updateCameraOnNavigationStateChange(navigationStarted: Boolean) {
        navigationMapboxMap?.
    }



}