package com.landside.slicerouter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

internal class InternalRouteFragment : Fragment() {


    companion object {
        private const val ROUTE_TAG = "slicerouter.internal.route.fragment"
        const val IN_DATAGRAM = "slicerouter.datagram"

        fun route(activity: FragmentActivity, param: Intent) {
            val fm = activity.supportFragmentManager
            route(fm, param)
        }

        fun route(fragment: Fragment, param: Intent) {
            val fm = fragment.childFragmentManager
            route(fm, param)
        }

        private fun route(fm: FragmentManager, param: Intent) {
            val routeFragment = findFragment(fm)
            if (routeFragment != null) {
                routeFragment.realRoute(param)
            } else {
                createFragment(fm, param)
            }
        }

        private fun findFragment(fm: FragmentManager): InternalRouteFragment? {
            if (fm.isDestroyed) {
                throw IllegalStateException("Fragment Manager has been destroyed")
            }

            val fragment = fm.findFragmentByTag(ROUTE_TAG)
            return fragment as InternalRouteFragment?
        }

        private fun createFragment(fm: FragmentManager, param: Intent): InternalRouteFragment {
            val fragment = InternalRouteFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(IN_DATAGRAM, param)
                }
            }
            fm.beginTransaction().add(fragment, ROUTE_TAG).commitAllowingStateLoss()
            return fragment
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val param = it.getParcelable<Intent>(IN_DATAGRAM)
            param?.let {
                realRoute(it)
            }
        }
    }

    private fun realRoute(param: Intent) {
        val realIntent = Intent()
        realIntent.setClass(context!!, Class.forName(param.component?.className ?: return))
        realIntent.putExtras(param.extras ?: return)
        realIntent.flags = param.flags
        startActivityForResult(realIntent, InternalRouteActivity.REQUEST_ROUTE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == InternalRouteActivity.REQUEST_ROUTE && resultCode == Activity.RESULT_OK) {
            SliceRouter.of(this).pop {
                if (data == null) {
                    return@pop Bundle()
                }
                return@pop data.extras!!
            }
        }
    }

}