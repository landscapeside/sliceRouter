package com.landside.slicerouter.sample

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_forth.*

class ForthActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forth)
//        tv_params.text = intent.getStringExtra("name")?:"没有参数"
        tv_params.text = intent.getParcelableExtra<SomeObj>("name")?.name?:"没有参数"
    }

    override fun onBackPressed() {
//        val result = Intent()
//        result.putExtra("result", "result from forth")
//        setResult(Activity.RESULT_OK, result)
        finish()
    }

}