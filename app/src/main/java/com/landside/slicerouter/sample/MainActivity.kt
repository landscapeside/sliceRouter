package com.landside.slicerouter.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.landside.slicerouter.SliceDecorator
import com.landside.slicerouter.SliceRouter
import kotlinx.android.synthetic.main.activity_main.to_second

class MainActivity : AppCompatActivity() {

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
            val name = it[Keys.PARAM_NAME]
            Toast.makeText(this, name?.toString(), Toast.LENGTH_LONG)
                .show()
          }
    }
  }

  override fun onBackPressed() {
    SliceRouter.of(this).pop()
  }
}