package com.landside.slicerouter.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.landside.slicerouter.SliceRouter

class SecondActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_second)
//    Toast.makeText(this, intent.getStringExtra(Keys.PARAM_NAME), Toast.LENGTH_LONG)
//        .show()
  }

  override fun onBackPressed() {
    SliceRouter.of(this)
        .popToCls(MainActivity::class.java) {
          val result = Bundle()
          result.putString(Keys.PARAM_NAME, "hahahah")

//          val result = bundleOf(Keys.PARAM_NAME to "others")

          result
        }
//    val intent = Intent()
//    intent.putExtra("second_result",10)
//    setResult(3, intent)
//    finish()
  }

  fun toThird(v: View) {
    SliceRouter.of(this)
        .push(ThirdActivity::class.java) {
          Toast.makeText(this, "从第3页返回第2页", Toast.LENGTH_LONG)
              .show()
        }
  }
}
