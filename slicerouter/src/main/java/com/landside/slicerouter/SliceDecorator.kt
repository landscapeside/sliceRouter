package com.landside.slicerouter

import android.content.Intent
import com.landside.slicerouter.exceptions.BlockException
import com.landside.slicerouter.exceptions.RedirectException

interface SliceDecorator {
    @Throws(BlockException::class,
        RedirectException::class)
    fun decorate(intent:Intent)
}