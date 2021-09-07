package com.landside.slicerouter.sample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.landside.slicerouter.SliceDecorator
import com.landside.slicerouter.SliceRouter
import kotlinx.android.synthetic.main.activity_main.et_test
import kotlinx.android.synthetic.main.activity_main.to_second
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    val handler = object :Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            startActivity(Intent(this@MainActivity,SecondActivity::class.java))
        }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    to_second.setOnClickListener {
      SliceRouter.of(this)
          .ignoreGlobalDecorator()
          .addDecorator(object :SliceDecorator{
            override fun decorate(intent: Intent) {
              intent.putExtra(Keys.PARAM_NAME,"hook param again!!")
            }

          })
          .push(
              SecondActivity::class.java,
              assembleParams = {
                it.putExtra(Keys.PARAM_NAME, "to second!!!")
              },
              scopePointRunner = {
                create {
                  Toast.makeText(it, "别闹", Toast.LENGTH_SHORT)
                      .show()
                }
              }) {
            if (it.getInt(SliceRouter.BUNDLE_RESULT_CODE) == 3){
              val name = it["second_result"]
//            val name = it[Keys.PARAM_NAME]
              Toast.makeText(this, name?.toString(), Toast.LENGTH_LONG)
                  .show()
            }
          }
    }

    et_test.setOnFocusChangeListener { v, hasFocus ->
      Timber.d("输入框${v}焦点状态：${hasFocus}")
    }

//      handler.sendEmptyMessageDelayed(1,3000)
  }

  override fun onBackPressed() {
    SliceRouter.of(this).pop()
  }
}
