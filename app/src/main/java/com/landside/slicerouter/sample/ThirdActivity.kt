package com.landside.slicerouter.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.landside.slicerouter.SliceRouter
import com.landside.slicerouter_annotation.Url

@Url("test")
class ThirdActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_third)
    Toast.makeText(this, intent.getStringExtra(Keys.PARAM_NAME), Toast.LENGTH_LONG)
        .show()
  }

  override fun onBackPressed() {
    SliceRouter.of(this)
        .pop(2) {
          val result = Bundle()
          result.putString(Keys.PARAM_NAME, "from third!!")
          result
        }
  }

  fun toForth(view: View) {
//    SliceRouter.of(this)
//        .pushBySystem(
//            clazz = ForthActivity::class.java,
//            assembleParams = {
////        it.putExtra("name","from third")
//            }
//        ) {
//          Toast.makeText(this, it.getString("result"), Toast.LENGTH_LONG)
//              .show()
//        }

    SliceRouter.of(this)
        .pushAction(
            Intent.ACTION_GET_CONTENT,
            assembleParams = {
//        it.putExtra("name","from third")

              it.type = "*/*"
              it.addCategory(Intent.CATEGORY_OPENABLE)
            }
        ) {
          Toast.makeText(this, it.getParcelable<Uri>(SliceRouter.BUNDLE_DATA).toString(), Toast.LENGTH_LONG)
              .show()
        }
  }
}
