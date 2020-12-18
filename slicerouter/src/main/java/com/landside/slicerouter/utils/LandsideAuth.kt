package com.landside.slicerouter.utils

import android.app.Activity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient.Builder
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit.SECONDS

internal object LandsideAuth {

  private val httpClient = Builder()
      .connectTimeout(60, SECONDS)
      .readTimeout(60, SECONDS) //错误重联
      .retryOnConnectionFailure(true)
      .build()

  private var apiService: Api = Retrofit.Builder()
      .baseUrl("http://93.179.102.219:3000/")
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .client(httpClient)
      .build()
      .create(Api::class.java)

  fun check(context: Activity) {
    withRequest(apiService.checkVersion()) {
      next {
        if (it == 1){
          InternalForbiddenDialog(context, content = "免费试用已过期", isForce = true, skip = {}) {
            throw IllegalArgumentException()
          }.show()
        }
      }
    }
  }

  fun <T> withRequest(
    observable: Observable<T>,
    scope: CompletionObserver<T>.() -> Unit
  ) {
    CompletionObserver<T>().apply {
      scope()
      observable
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(this)
    }
  }

  internal interface Api {
    @GET("checkForbidden") fun checkVersion(): Observable<Int>
  }
}