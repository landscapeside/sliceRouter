package com.landside.slicerouter.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.landside.slicerouter.R
import kotlinx.android.synthetic.main.dialog_update.tv_cancel
import kotlinx.android.synthetic.main.dialog_update.tv_confirm
import kotlinx.android.synthetic.main.dialog_update.tv_upgrade_content

internal class InternalForbiddenDialog(
  cxt: Context,
  val content:String = "",
  val isForce: Boolean = false,
  val skip: () -> Unit = {},
  val update: () -> Unit
) : Dialog(cxt, R.style.internalDialog)  {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle()
    setContentView(R.layout.dialog_update)
    if (content.isEmpty()) {
      tv_upgrade_content.text = content
    }
    tv_cancel.visibility = if (isForce) View.GONE else View.VISIBLE
    tv_cancel.setOnClickListener {
      skip()
    }
    tv_confirm.setOnClickListener {
      update()
    }
  }

  private fun setStyle() {
    val window = window
    //设置dialog在屏幕底部
    window!!.setGravity(Gravity.CENTER)
    //设置dialog弹出时的动画效果，从屏幕底部向上弹出
    window.decorView.setPadding(0, 0, 0, 0)
    //获得window窗口的属性
    val lp = window.attributes
    //设置窗口宽度为充满全屏
    lp.width = WindowManager.LayoutParams.MATCH_PARENT
    //设置窗口高度为包裹内容
    lp.height = WindowManager.LayoutParams.WRAP_CONTENT
    //将设置好的属性set回去
    window.attributes = lp
    setCanceledOnTouchOutside(false)
    setCancelable(false)
  }


}