package com.landside.slicerouter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

internal class InternalRouteActivity : FragmentActivity() {

    companion object {
        const val REQUEST_ROUTE = 1002
        const val IN_DATAGRAM = "slicerouter.datagram"

        fun route(context: Context, param: Intent) {
            val intent = Intent(context, InternalRouteActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(IN_DATAGRAM, param)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val param = intent.getParcelableExtra<Intent>(IN_DATAGRAM)
        param?.let {
            val realIntent = Intent()
            realIntent.setClass(this, Class.forName(param.component?.className ?: return))
            realIntent.putExtras(it.extras ?: return)
            realIntent.flags = param.flags
            startActivityForResult(realIntent, REQUEST_ROUTE)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ROUTE) {
            SliceRouter.of(this).pop {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    return@pop data.extras!!
                } else {
                    return@pop Bundle()
                }
            }
        }
    }

}