package com.huawei.health.mapboxnavigationkt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.base.common.logger.model.Message;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.Random;

import timber.log.Timber;

public class Utils {

  /**
   * <p>
   * Returns the Mapbox access token set in the app resources.
   * </p>
   * It will first search for a token in the Mapbox object. If not found it
   * will then attempt to load the access token from the
   * {@code res/values/dev.xml} development file.
   *
   * @param context The {@link Context} of the {@link android.app.Activity} or {@link android.app.Fragment}.
   * @return The Mapbox access token or null if not found.
   */
  public static String getMapboxAccessToken(@NonNull Context context) {
    try {
      // Read out AndroidManifest
      String token = Mapbox.getAccessToken();
      if (token == null || token.isEmpty()) {
        throw new IllegalArgumentException();
      }
      return token;
    } catch (Exception exception) {
      // Use fallback on string resource, used for development
      int tokenResId = context.getResources()
        .getIdentifier("mapbox_access_token", "string", context.getPackageName());
      return tokenResId != 0 ? context.getString(tokenResId) : null;
    }
  }

  /**
   * Demonstrates converting any Drawable to an Icon, for use as a marker icon.
   */
  public static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id) {
    Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), id, context.getTheme());
    Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
      vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    vectorDrawable.draw(canvas);
    return IconFactory.getInstance(context).fromBitmap(bitmap);
  }

  public static LatLng getRandomLatLng(double[] bbox) {
    Random random = new Random();

    double randomLat = bbox[1] + (bbox[3] - bbox[1]) * random.nextDouble();
    double randomLon = bbox[0] + (bbox[2] - bbox[0]) * random.nextDouble();

    LatLng latLng = new LatLng(randomLat, randomLon);
    return latLng;
  }

  public static final String PRIMARY_ROUTE_BUNDLE_KEY = "myPrimaryRouteBundleKey";

  /**
   * Used by the example activities to get a DirectionsRoute from a bundle.
   *
   * @param bundle to get the DirectionsRoute from
   * @return a DirectionsRoute or null
   */
  public static DirectionsRoute getRouteFromBundle(Bundle bundle) {
    try {
      if (bundle.containsKey(PRIMARY_ROUTE_BUNDLE_KEY)) {
        String routeAsJson = bundle.getString(PRIMARY_ROUTE_BUNDLE_KEY);
        return DirectionsRoute.fromJson(routeAsJson);
      }
    } catch (Exception ex) {
      Timber.i(ex);
    }
    return null;
  }
}
