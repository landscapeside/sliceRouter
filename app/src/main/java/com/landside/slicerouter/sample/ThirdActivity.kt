package com.landside.slicerouter.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.landside.slicerouter.SliceRouter
import com.landside.slicerouter.sample.Keys
import com.landside.slicerouter_annotation.Url

@Url("test")
class ThirdActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_third)
    Toast.makeText(this, intent.getStringExtra(Keys.PARAM_NAME), Toast.LENGTH_LONG).show()
  }

  override fun onBackPressed() {
    SliceRouter.of(this)
        .pop(2) {
          val result = Bundle()
          result.putString(Keys.PARAM_NAME, "from third!!")
          result
        }
  }
}
