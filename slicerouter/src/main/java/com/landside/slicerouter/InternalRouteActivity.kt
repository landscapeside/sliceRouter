package com.landside.slicerouter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity

internal class InternalRouteActivity : FragmentActivity() {

  companion object {
    const val REQUEST_ROUTE = 1002
    const val IN_DATAGRAM = "slicerouter.datagram"
    const val IS_ACTION = "slicerouter.isaction"

    var param:Intent? = null

    fun route(
      context: Context,
      param: Intent
    ) {
      val intent = Intent(context, InternalRouteActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.putExtra(IN_DATAGRAM, param)
      this.param = param
      context.startActivity(intent)
    }

    fun routeAction(
      context: Context,
      param: Intent
    ) {
      val intent = Intent(context, InternalRouteActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.putExtra(IN_DATAGRAM, param)
      intent.putExtra(IS_ACTION, true)
      this.param = param
      context.startActivity(intent)
    }
  }

  private var isAction: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isAction = intent.getBooleanExtra(IS_ACTION, false)
    param?.let {
      val realIntent = Intent()
      if (isAction) {
        realIntent.action = it.action
      } else {
        realIntent.setClass(this, Class.forName(it.component?.className ?: return))
      }
      if (it.extras != null) {
        realIntent.putExtras(it.extras!!)
      }
      it.type?.let { realIntent.type = it }
      it.data?.let { realIntent.data = it }
      it.categories?.forEach {
        realIntent.addCategory(it)
      }
      realIntent.flags = it.flags
      startActivityForResult(realIntent, REQUEST_ROUTE)
    }

  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_ROUTE) {
      SliceRouter.of(this)
          .pop {
            if (resultCode != Activity.RESULT_CANCELED && data != null) {
              val realData = data.extras?.clone() as? Bundle
              realData?.putInt(SliceRouter.BUNDLE_RESULT_CODE,resultCode)
              if (isAction) {
                return@pop realData?.apply {
                  putParcelable(SliceRouter.BUNDLE_DATA, data.data)
                } ?: bundleOf(
                    SliceRouter.BUNDLE_DATA to data.data
                )
              } else {
                return@pop realData ?: Bundle()
              }
            } else {
              return@pop Bundle()
            }
          }
    }
  }

}