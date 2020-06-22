package com.landside.slicerouter.sample

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.landside.slicerouter.SliceRouter

class SecondActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_second)
    Toast.makeText(this, intent.getStringExtra(Keys.PARAM_NAME), Toast.LENGTH_LONG)
        .show()
  }

  override fun onBackPressed() {
    SliceRouter.of(this)
        .pop {
          val result = Bundle()
          result.putString(Keys.PARAM_NAME, "hahahah")
          result
        }
  }

  fun toThird(v: View) {
    SliceRouter.of(this)
        .push(ThirdActivity::class.java) {
          Toast.makeText(this, "从第3页返回第2页", Toast.LENGTH_LONG)
              .show()
        }
  }
}
