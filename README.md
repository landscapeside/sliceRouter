[![](https://www.jitpack.io/v/landscapeside/sliceRouter.svg)](https://www.jitpack.io/#landscapeside/sliceRouter)

引入：

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://www.jitpack.io' }
    }
}
```

```groovy
implementation "com.github.landscapeside.sliceRouter:slicerouter:<version>"
kapt 'com.github.landscapeside.sliceRouter:slicerouter-compiler:<version>'
```

使用：

* 页面跳转

```kotlin
SliceRouter.of(this)
    .push(
        ThirdActivity::class.java,
        /*assembleParams -- 添加入参，可为空*/
        assembleParams = {
          it.putExtra(Keys.PARAM_NAME, "第三页入参!!!")
        },
        /*scopePointRunner -- 可定制目标页面生命周期钩子*/
        scopePointRunner = {
            create {
              Toast.makeText(it, "目标页面create里面应该执行此处代码", Toast.LENGTH_SHORT)
                  .show()
            }
          }) {
        /*处理返回结果，内容包装在bundle里面*/
      val name = it[Keys.PARAM_NAME]
      Toast.makeText(this, name?.toString(), Toast.LENGTH_LONG)
          .show()
    }
```

* 关闭页面并返回结果

```kotlin
SliceRouter.of(this)
    .pop {
      val result = Bundle()
      result.putString(Keys.PARAM_NAME, "返回结果给前一个页面")
      result
    }
```

* 跨页面返回
```kotlin
SliceRouter.of(this)
    .pop(2) {
      val result = Bundle()
      result.putString(Keys.PARAM_NAME, "from third!!")
      result
    }
```
* 全局添加装饰器
```kotlin
//application的onCreate生命周期里
SliceRouter.addDecorator(object :SliceDecorator{
  override fun decorate(intent: Intent) {
    if (intent.component?.className == SecondActivity::class.java.name) {
      intent.putExtra(Keys.PARAM_NAME,"hook param!!")
      //重定向
      throw RedirectException(
        ThirdActivity::class.java
      )
    }else if(intent.component?.className == SecondActivity::class.java.name){
    //终止跳转
      throw BlockException()
    }else{
        intent.putExtra(Keys.PARAM_NAME,"改变传参!")
    }
  }
})
```
* 局部装饰器
```kotlin
SliceRouter.of(this)
      .ignoreGlobalDecorator()/*忽略全局装饰器，可选*/
      .addDecorator(object :SliceDecorator{
        override fun decorate(intent: Intent) {
          intent.putExtra(Keys.PARAM_NAME,"hook param from part!!")
        }

      }).push(....)  
```

* url方式跳转
```kotlin
/*
* 给目标页面添加url注解
* 
* */
@Url("this is a url")
class UrlActivity : AppCompatActivity() {
    ...
}
```
```kotlin
/**
* 定义空Router类
*/
@Router
class MainRouter {
}
```
rebuild之后会生成`MainRouterSliceProvider`
```kotlin
//application的onCreate中
ZipProvider.zip(MainRouterSliceProvider())
```
跳转方式：
```kotlin
SliceRouter.of(this)
    .push(
        "this is a url",
        /*assembleParams -- 添加入参，可为空*/
        assembleParams = {
          it.putExtra(Keys.PARAM_NAME, "第三页入参!!!")
        },
        /*scopePointRunner -- 可定制目标页面生命周期钩子*/
        scopePointRunner = {
            create {
              Toast.makeText(it, "目标页面create里面应该执行此处代码", Toast.LENGTH_SHORT)
                  .show()
            }
          }) {
        /*处理返回结果，内容包装在bundle里面*/
      val name = it[Keys.PARAM_NAME]
      Toast.makeText(this, name?.toString(), Toast.LENGTH_LONG)
          .show()
    }
```
* action跳转
```kotlin
SliceRouter.of(this)
    .pushAction(
        "action name",
        uri = null,
        /*assembleParams -- 添加入参，可为空*/
        assembleParams = {
          it.putExtra(Keys.PARAM_NAME, "第三页入参!!!")
        }) {
        /*处理返回结果，内容包装在bundle里面*/
      val name = it[Keys.PARAM_NAME]
      Toast.makeText(this, name?.toString(), Toast.LENGTH_LONG)
          .show()
    }
```
