package com.landside.slicerouter

import android.app.Activity
import android.os.Bundle

class PointRunner {

  var createInvoker: (Activity) -> Unit = {}
  fun create(createInvoker: (Activity) -> Unit) {
    this.createInvoker = createInvoker
  }

  var destroyInvoker: (Activity) -> Unit = {}
  fun destroy(destroyInvoker: (Activity) -> Unit) {
    this.destroyInvoker = destroyInvoker
  }

  var resumeInvoker: (Activity) -> Unit = {}
  fun resume(resumeInvoker: (Activity) -> Unit) {
    this.resumeInvoker = resumeInvoker
  }

  var stopInvoker: (Activity) -> Unit = {}
  fun stop(stopInvoker: (Activity) -> Unit) {
    this.stopInvoker = stopInvoker
  }

  var startInvoker: (Activity) -> Unit = {}
  fun start(startInvoker: (Activity) -> Unit) {
    this.startInvoker = startInvoker
  }

  var pauseInvoker: (Activity) -> Unit = {}
  fun pause(pauseInvoker: (Activity) -> Unit) {
    this.pauseInvoker = pauseInvoker
  }

  var saveInstanceStateInvoker: (Activity, Bundle?) -> Unit = { activity, outState -> }
  fun saveInstanceState(saveInstanceStateInvoker: (Activity, Bundle?) -> Unit) {
    this.saveInstanceStateInvoker = saveInstanceStateInvoker
  }
}