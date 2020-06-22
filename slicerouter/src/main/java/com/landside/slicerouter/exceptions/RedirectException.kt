package com.landside.slicerouter.exceptions

import java.lang.IllegalStateException

class RedirectException(val redirectCls:Class<*>):IllegalStateException() {
}