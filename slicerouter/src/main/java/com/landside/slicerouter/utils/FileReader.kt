package com.landside.slicerouter.utils

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by landscape on 2016/10/20.
 */
internal object FileReader {
  @Throws(IOException::class)
  fun openMockFile(
    appContext: Context,
    relativePath: String,
    supportSDCard: Boolean
  ): InputStream {
    val inputStream: InputStream
    inputStream = if (supportSDCard) {
      val file = File(relativePath)
      FileInputStream(file)
    } else {
      appContext.assets.open(relativePath)
    }
    return inputStream
  }

  @JvmOverloads
  @Throws(IOException::class)
  fun requestMockString(
    appContext: Context,
    relativePath: String,
    supportSDCard: Boolean = false
  ): String {
    val sbJson = StringBuilder()
    val inputStream =
      openMockFile(appContext, relativePath, supportSDCard)
    val reader =
      BufferedReader(InputStreamReader(inputStream))
    try {
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        sbJson.append(line)
      }
    } finally {
      try {
        reader.close()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
    return sbJson.toString()
  }
}