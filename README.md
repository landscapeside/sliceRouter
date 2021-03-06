[![](https://www.jitpack.io/v/landscapeside/sliceRouter.svg)](https://www.jitpack.io/#landscapeside/sliceRouter)

面向切面的路由库，特性：

* 支持跨级返回
* 回调方式处理结果，摒弃安卓本身onActivityResult逻辑分离弊端
* 支持跳转熔断
* 支持重定向
* 支持参数hook
* 支持目标页面生命周期hook
* 支持url方式跳转（可做轻量级组件化）

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
* action跳转，用于隐式跳转页面
```kotlin
SliceRouter.of(this)
    .pushAction(
        "action name",
        /*assembleParams -- 添加入参，可为空*/
        assembleParams = {
          it.putExtra(Keys.PARAM_NAME, "第三页入参!!!")
        }) {
        /*处理返回结果，内容包装在bundle里面*/
      val name = it[Keys.PARAM_NAME]
      Toast.makeText(this, name?.toString(), Toast.LENGTH_LONG)
          .show()
        /*如果某些页面在intent.data内返回了结果，可通过bundle.getParcelable<Uri>(SliceRouter.BUNDLE_DATA)获取*/
      Toast.makeText(
          this,
          it.getParcelable<Uri>(SliceRouter.BUNDLE_DATA).toString(), Toast.LENGTH_LONG).show()
    }
```

* 跳转第三方页面
```kotlin
    SliceRouter.of(this)
        .pushBySystem(
            clazz = ForthActivity::class.java,
            assembleParams = {
//        it.putExtra("name","from third")
            }
        ) {
          Toast.makeText(this, it.getString("result"), Toast.LENGTH_LONG)
              .show()
        }

```

* 混淆配置

```
-keep class com.landside.slicerouter.** { *; }
-keep interface com.landside.slicerouter.** { *; }
-keep interface com.landside.slicerouter_annotation.** { *; }
-keep @com.landside.slicerouter_annotation.Router class * {*;}
-keep @com.landside.slicerouter_annotation.Url class * {*;}
-keep class **.**SliceProvider {*;}

```
