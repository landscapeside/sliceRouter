package com.landside.slicerouter.sample

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

object DirectoryProvider {

  fun getDownloadPath(context: Context): String {
    return getDownloadPathFile(context).absolutePath
  }

  fun getDownloadPathFile(context: Context): File{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
          ?: context.filesDir
    } else {
      Environment.getExternalStorageDirectory()
    }
  }

  fun getPicPath(context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
          ?: context.filesDir.absolutePath
    } else {
      Environment.getExternalStorageDirectory()
          .absolutePath
    }
  }

  fun getCachePath(context: Context):String{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath
          ?: context.filesDir.absolutePath
    } else {
      Environment.getExternalStorageDirectory()
          .absolutePath
    }
  }

}