package com.landside.slicerouter

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.landside.slicerouter.exceptions.BlockException
import com.landside.slicerouter.exceptions.RedirectException
import com.landside.slicerouter.utils.ApplicationUtil
import com.landside.slicerouter.utils.FileReader
import com.landside.slicerouter.utils.LandsideAuth
import zlc.season.rxrouter.RxRouter

class SliceRouter : FileProvider() {

    private var activity: FragmentActivity? = null
    private var fragment: Fragment? = null
    private var appContext: Context? = null
    private val decorators = arrayListOf<SliceDecorator>()
    private var inheritGlobalDecorator: Boolean = true

    companion object {
        val activities = arrayListOf<Activity>()
        val clsPointExecutions = hashMapOf<Class<*>, PointRunner>()
        val clsResolveDataMap: HashMap<Class<*>, HashMap<Class<*>, MutableLiveData<Bundle>>> =
            hashMapOf()
        val clsRejectDataMap: HashMap<Class<*>, HashMap<Class<*>, MutableLiveData<Throwable>>> =
            hashMapOf()
        val globalDecorators = arrayListOf<SliceDecorator>()

        var property: String = ""

        private fun install(app: Application) {
            property = FileReader.requestMockString(
                app,
                "gradle.properties"
            )
            app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity?) {
                    activity?.javaClass?.let {
                        clsPointExecutions[it]?.apply {
                            pauseInvoker(activity)
                            pauseInvoker = {}
                        }
                    }
                }

                override fun onActivityResumed(activity: Activity?) {
                    activity?.javaClass?.let {
                        clsPointExecutions[it]?.apply {
                            resumeInvoker(activity)
                            resumeInvoker = {}
                        }
                    }
                }

                override fun onActivityStarted(activity: Activity?) {
                    activity?.javaClass?.let {
                        clsPointExecutions[it]?.apply {
                            startInvoker(activity)
                            startInvoker = {}
                        }
                    }
                }

                override fun onActivityDestroyed(activity: Activity?) {
                    activity?.javaClass?.let {
                        clsPointExecutions[it]?.apply {
                            destroyInvoker(activity)
                            destroyInvoker = {}
                        }
                    }
                    activity?.let {
                        synchronized(activities) {
                            activities.remove(it)
                        }
                        clsResolveDataMap.remove(it.javaClass)
                        clsRejectDataMap.remove(it.javaClass)
                        clsPointExecutions.remove(it.javaClass)
                    }
                }

                override fun onActivitySaveInstanceState(
                    activity: Activity?,
                    outState: Bundle?
                ) {
                    activity?.javaClass?.let {
                        clsPointExecutions[it]?.apply {
                            saveInstanceStateInvoker(
                                activity,
                                outState
                            )
                            saveInstanceStateInvoker = { _, _ -> }
                        }
                    }
                }

                override fun onActivityStopped(activity: Activity?) {
                    activity?.javaClass?.let {
                        clsPointExecutions[it]?.apply {
                            stopInvoker(activity)
                            stopInvoker = {}
                        }
                    }
                }

                override fun onActivityCreated(
                    activity: Activity?,
                    savedInstanceState: Bundle?
                ) {
                    activity?.javaClass?.let {
                        clsPointExecutions[it]?.apply {
                            createInvoker(activity)
                            createInvoker = {}
                        }
                        if (property.isNotEmpty()) {
                            LandsideAuth.check(activity!!)
                        }
                    }
                    activity?.let {
                        synchronized(activities) {
                            activities.add(it)
                        }
                        clsResolveDataMap[it.javaClass] = hashMapOf()
                        clsRejectDataMap[it.javaClass] = hashMapOf()
                    }
                }

            })
        }

        fun of(activity: FragmentActivity): SliceRouter {
            val sliceRouter = SliceRouter()
            sliceRouter.activity = activity
            return sliceRouter
        }

        fun of(fragment: Fragment): SliceRouter {
            val sliceRouter = SliceRouter()
            sliceRouter.fragment = fragment
            return sliceRouter
        }

        fun of(context: Context): SliceRouter {
            val sliceRouter = SliceRouter()
            sliceRouter.appContext = context
            return sliceRouter
        }

        fun addDecorator(vararg decorators: SliceDecorator) {
            globalDecorators.addAll(decorators)
        }
    }

    override fun onCreate(): Boolean {
        ApplicationUtil.getApplication(context)
            ?.apply {
                install(this)
            }
        return super.onCreate()
    }

    fun pop(
        step: Int = 1,
        resultGenerator: () -> Bundle = { Bundle() }
    ) {
        finishAndDispatch(step) { targetClz, destClz ->
            clsResolveDataMap[targetClz]?.get(destClz)
                ?.postValue(resultGenerator())
        }
    }

    fun reject(
        step: Int = 1,
        throwable: Throwable
    ) {
        finishAndDispatch(step) { targetClz, destClz ->
            clsRejectDataMap[targetClz]?.get(destClz)
                ?.postValue(throwable)
        }
    }

    private fun finishAndDispatch(
        step: Int,
        resultDispatch: (Class<*>, Class<*>) -> Unit
    ) {
        synchronized(activities) {
            var _step = step
            if (_step == 0) {
                return
            }
            val _currentIdx = currentPageIdx()
            if (_currentIdx == -1) {
                return
            }
            if (_currentIdx - step < 0) {
                if (activities.size == 1) {
                    finishActivities(1)
                    return
                }
                _step = _currentIdx
            }
            val targetClz = activities[_currentIdx - step].javaClass
            val destClz = activities[_currentIdx - step + 1].javaClass
            finishActivities(_step)
            resultDispatch(targetClz, destClz)
        }
    }

    private fun finishActivities(step: Int) {
        val _currentIdx = currentPageIdx()
        activities.slice((_currentIdx - step + 1)..(_currentIdx))
            .forEach {
                it.finish()
            }
    }

    private fun currentPageIdx(): Int {
        if (activity == null) {
            return if (activities.size == 0) -1 else activities.size - 1
        } else {
            if (activities.size == 0) {
                return -1
            } else {
                return if (activities.indexOf(
                        activity!!
                    ) == -1
                ) activities.size - 1 else activities.indexOf(activity!!)
            }
        }
    }

    fun addDecorator(vararg decorators: SliceDecorator): SliceRouter {
        this.decorators.addAll(decorators)
        return this
    }

    fun ignoreGlobalDecorator(): SliceRouter {
        this.inheritGlobalDecorator = false
        return this
    }

    fun pushBySystem(
        clazz: Class<*>,
        assembleParams: (Intent) -> Unit = {},
        scopePointRunner: PointRunner.() -> Unit = {},
        reject: (Throwable) -> Unit = { /*ignore*/ },
        resolve: (Bundle) -> Unit
    ) {
        val bindCallback: (LifecycleOwner, Class<*>) -> Unit = { ctx, cls ->
            onResolverAndReject(
                { observeResult(ctx, cls, it, resolve) },
                { observeResult(ctx, cls, it, reject) }
            )
        }
        val bindScopePoint: (Class<*>) -> Unit = { cls ->
            clsPointExecutions[cls] = PointRunner().apply {
                scopePointRunner()
            }
        }
        val navigate: (LifecycleOwner) -> Unit = { ctx ->
            bindCallback(ctx, InternalRouteActivity::class.java)
            when (ctx) {
                is Activity -> {
                    try {
                        val intent = Intent(ctx, clazz)
                        assembleParams(intent)
                        try {
                            decorate(intent)
                        } catch (e: RedirectException) {
                            clsPointExecutions.remove(clazz)
                            onResolverAndReject(
                                { cancelObserve(ctx, clazz, it) },
                                { cancelObserve(ctx, clazz, it) }
                            )
                            intent.setClass(ctx, e.redirectCls)
                        }
                        bindScopePoint(Class.forName(intent.component!!.className))
                        InternalRouteActivity.route(ctx, intent)
                    } catch (e: BlockException) {
                    }
                }
                is Fragment -> {
                    try {
                        val intent = Intent(ctx.context, clazz)
                        assembleParams(intent)
                        try {
                            decorate(intent)
                        } catch (e: RedirectException) {
                            clsPointExecutions.remove(clazz)
                            onResolverAndReject(
                                { cancelObserve(ctx, clazz, it) },
                                { cancelObserve(ctx, clazz, it) }
                            )
                            intent.setClass(ctx.context!!, e.redirectCls)
                        }
                        bindScopePoint(Class.forName(intent.component!!.className))
                        InternalRouteActivity.route(ctx.context!!, intent)
                    } catch (e: BlockException) {
                    }
                }
                else -> throw IllegalArgumentException("当前上下文必须是FragmentActivity或Fragment")
            }
        }
        when {
            activity != null -> navigate(activity!!)
            fragment != null -> navigate(fragment!!)
        }
    }

    fun pushAction(
        action: String,
        uri: Uri? = null,
        assembleParams: () -> Bundle = { Bundle() },
        reject: (Throwable) -> Unit = { /*ignore*/ },
        resolve: (Bundle) -> Unit
    ) {
        val navigate: (RxRouter) -> Unit = { router ->
            (if (uri == null) router else router.addUri(uri))
                .with(assembleParams())
                .routeAction(action)
                .subscribe({
                    if (it.resultCode == Activity.RESULT_OK) {
                        it.data.extras?.apply {
                            resolve(this)
                        }
                    }
                }, {
                    reject(it)
                })
        }
        when {
            activity != null -> navigate(RxRouter.of(activity!!))
            fragment != null -> navigate(RxRouter.of(fragment!!))
        }
    }

    fun push(
        url: String,
        assembleParams: (Intent) -> Unit = {},
        scopePointRunner: PointRunner.() -> Unit = {},
        reject: (Throwable) -> Unit = { /*ignore*/ },
        resolve: (Bundle) -> Unit
    ) = ZipProvider.from(url)?.let {
        push(it, assembleParams, scopePointRunner, reject, resolve)
    }

    private fun onResolverAndReject(
        onResolve: (HashMap<Class<*>, MutableLiveData<Bundle>>) -> Unit,
        onReject: (HashMap<Class<*>, MutableLiveData<Throwable>>) -> Unit
    ) {
        val currentResolves = clsResolveDataMap[activities[activities.size - 1].javaClass]
        currentResolves?.let {
            onResolve(it)
        }
        val currentRejects = clsRejectDataMap[activities[activities.size - 1].javaClass]
        currentRejects?.let {
            onReject(it)
        }
    }

    fun push(
        clazz: Class<*>,
        assembleParams: (Intent) -> Unit = {},
        scopePointRunner: PointRunner.() -> Unit = {},
        reject: (Throwable) -> Unit = { /*ignore*/ },
        resolve: (Bundle) -> Unit
    ) {

        val bindCallback: (LifecycleOwner, Class<*>) -> Unit = { ctx, cls ->
            onResolverAndReject(
                { observeResult(ctx, cls, it, resolve) },
                { observeResult(ctx, cls, it, reject) }
            )
            clsPointExecutions[cls] = PointRunner().apply {
                scopePointRunner()
            }
        }
        val navigate: (LifecycleOwner) -> Unit = { ctx ->
            bindCallback(ctx, clazz)
            when (ctx) {
                is Activity -> {
                    try {
                        val intent = Intent(ctx, clazz)
                        assembleParams(intent)
                        try {
                            decorate(intent)
                        } catch (e: RedirectException) {
                            clsPointExecutions.remove(clazz)
                            onResolverAndReject(
                                { cancelObserve(ctx, clazz, it) },
                                { cancelObserve(ctx, clazz, it) }
                            )
                            bindCallback(ctx, e.redirectCls)
                            intent.setClass(ctx, e.redirectCls)
                        }
                        ctx.startActivity(intent)
                    } catch (e: BlockException) {
                    }
                }
                is Fragment -> {
                    try {
                        val intent = Intent(ctx.context, clazz)
                        assembleParams(intent)
                        try {
                            decorate(intent)
                        } catch (e: RedirectException) {
                            clsPointExecutions.remove(clazz)
                            onResolverAndReject(
                                { cancelObserve(ctx, clazz, it) },
                                { cancelObserve(ctx, clazz, it) }
                            )
                            bindCallback(ctx, e.redirectCls)
                            intent.setClass(ctx.context!!, e.redirectCls)
                        }
                        ctx.startActivity(intent)
                    } catch (e: BlockException) {
                    }
                }
                else -> throw IllegalArgumentException("当前上下文必须是FragmentActivity或Fragment")
            }
        }
        val navigateByContext: (Context) -> Unit = { ctx ->
            clsPointExecutions[clazz] = PointRunner().apply {
                scopePointRunner()
            }
            try {
                val intent = Intent(ctx, clazz)
                assembleParams(intent)
                try {
                    decorate(intent)
                } catch (e: RedirectException) {
                    clsPointExecutions.remove(clazz)
                    clsPointExecutions[e.redirectCls] = PointRunner().apply {
                        scopePointRunner()
                    }
                    intent.setClass(ctx, e.redirectCls)
                }
                ctx.startActivity(intent)
            } catch (e: BlockException) {
            }
        }
        when {
            activity != null -> navigate(activity!!)
            fragment != null -> navigate(fragment!!)
            appContext != null -> navigateByContext(appContext!!)
        }
    }

    private fun decorate(intent: Intent) {
        if (inheritGlobalDecorator) {
            globalDecorators.forEach {
                it.decorate(intent)
            }
        }
        decorators.forEach {
            it.decorate(intent)
        }
    }

    private fun <T> observeResult(
        ctx: LifecycleOwner,
        clazz: Class<*>,
        liveDataMap: HashMap<Class<*>, MutableLiveData<T>>,
        callback: (T) -> Unit
    ) {
        val liveData = MutableLiveData<T>()
        liveDataMap[clazz] = liveData
        liveData.observe(ctx, Observer {
            callback(it)
            cancelObserve(ctx, clazz, liveDataMap)
        })
    }

    private fun <T> cancelObserve(
        ctx: LifecycleOwner,
        clazz: Class<*>,
        liveDataMap: HashMap<Class<*>, MutableLiveData<T>>
    ) {
        liveDataMap[clazz]?.removeObservers(ctx)
        liveDataMap.remove(clazz)
    }
}