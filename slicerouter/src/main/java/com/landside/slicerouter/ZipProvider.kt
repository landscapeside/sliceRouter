package com.landside.slicerouter

import com.landside.slicerouter_annotation.RouteProvider

object ZipProvider : RouteProvider {
  private val providers = mutableListOf<RouteProvider>()

  override fun from(url: String?): Class<*>? {
    providers.forEach {
      if (it.from(url) != null) {
        return it.from(url)
      }
    }
    return null
  }

  override fun from(cls: Class<*>): String? {
    providers.forEach {
      if (it.from(cls) != null) {
        return it.from(cls)
      }
    }
    return null
  }

  fun zip(vararg providers: RouteProvider) {
    this.providers.addAll(providers)
  }
}