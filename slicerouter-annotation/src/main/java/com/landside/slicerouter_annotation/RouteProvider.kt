package com.landside.slicerouter_annotation

interface RouteProvider {
    fun from(url: String?): Class<*>?
    fun from(cls: Class<*>): String?
}