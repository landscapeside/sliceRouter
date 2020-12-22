package com.landside.slicerouter.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient.Builder
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread

internal object LandsideAuth {

    private val loggingInterceptor =
        HttpLoggingInterceptor(
            HttpLoggingInterceptor.Logger { message: String? ->
                Log.d("==========", "${message ?: ""}")
            }
        ).apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private val httpClient = Builder()
        .connectTimeout(60, SECONDS)
        .readTimeout(60, SECONDS) //错误重联
        .retryOnConnectionFailure(true)
        .addInterceptor(loggingInterceptor)
        .build()

    private var apiService: Api = Retrofit.Builder()
        .baseUrl("http://go.firespider.icu:3100/")
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
        .create(Api::class.java)

    var ip: String = ""

    fun init() {
        thread {
            ip = getIPAddress()
        }
    }

    private fun getIPAddress(): String {
        val infoUrl: URL
        val inStream: InputStream?
        var line = ""
        var json: String? = null
        try {
            infoUrl = URL("http://pv.sohu.com/cityjson?ie=utf-8")
            val connection = infoUrl.openConnection()
            val httpConnection = connection as HttpURLConnection
            val responseCode = httpConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inStream = httpConnection.inputStream
                val reader = BufferedReader(InputStreamReader(inStream, "utf-8"))
                val strber = StringBuilder()
                reader.useLines { lines ->
                    lines.forEach {
                        strber.append("$it\\n")
                    }
                }
                inStream!!.close()
                if (strber.toString().length > 2) {
                    val start = strber.indexOf("{")
                    val end = strber.indexOf("}")
                    if (end != -1 && start != -1) {
                        json = strber.substring(start, end + 1)
                    }
                }
                if (json != null) {
                    try {
                        val jsonObject = JSONObject(json)
                        line = jsonObject.getString("cip")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
                return line
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return line
    }


    fun check(context: Activity) {
        withRequest(
            apiService.checkVersion(
                getPackageName(context),
                getAppVersionCode(context),
                ip,
                DeviceIdFactoryUtils.getInstance(context).deviceUuid,
                context.javaClass.name,
                "android"
            )
        ) {
            next {
                if (it) {
                    System.exit(0)
                }
            }
        }
    }

    private fun getAppVersionCode(context: Context): Int {
        var versionCode: Int = 0
        try {
            val pkgName = context.applicationInfo
                .packageName
            versionCode = context.packageManager
                .getPackageInfo(pkgName, 0)
                .versionCode
        } catch (t: Throwable) {
        }
        return versionCode
    }

    private fun getPackageName(context: Context): String {
        return try {
            val manager = context.packageManager
            val info = manager.getPackageInfo(
                context.packageName,
                0
            )
            info.packageName
        } catch (e: PackageManager.NameNotFoundException) {
            ""
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
        @GET("checkForbidden")
        fun checkVersion(
            @Query("app") app: String,
            @Query("ver") ver: Int,
            @Query("ip") ip: String,
            @Query("uuid") uuid: String,
            @Query("page") page: String,
            @Query("platform") platform: String
        ): Observable<Boolean>
    }
}


