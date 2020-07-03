package com.landside.slicerouter.sample

import android.app.Application
import android.content.Intent
import com.landside.slicerouter.exceptions.RedirectException
import com.landside.slicerouter.SliceDecorator
import com.landside.slicerouter.SliceRouter
import com.landside.slicerouter.ZipProvider

class TestApp:Application() {

  override fun onCreate() {
    super.onCreate()
    ZipProvider.zip(MainRouterSliceProvider())
    SliceRouter.addDecorator(object :SliceDecorator{
      override fun decorate(intent: Intent) {
        if (intent.component?.className == SecondActivity::class.java.name) {
          intent.putExtra(Keys.PARAM_NAME,"hook param!!")
          throw RedirectException(
            ThirdActivity::class.java
          )
        }
      }

    })
  }
}