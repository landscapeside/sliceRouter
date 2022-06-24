package com.landside.slicerouter

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.landside.slicerouter.exceptions.BlockException
import com.landside.slicerouter.exceptions.RedirectException
import com.landside.slicerouter.utils.ApplicationUtil
import timber.log.Timber
import zlc.season.rxrouter.RxRouter
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.*

class SliceRouter : FileProvider() {

    private var activity: FragmentActivity? = null
    private var fragment: Fragment? = null
    private var appContext: Context? = null
    private val decorators = arrayListOf<SliceDecorator>()
    private var inheritGlobalDecorator: Boolean = true

    companion object {
        const val BUNDLE_DATA = "BUNDLE_DATA"
        const val BUNDLE_RESULT_CODE = "BUNDLE_RESULT_CODE"
        const val BUNDLE_NOT_EXIST = "BUNDLE_NOT_EXIST"
        private const val BUNDLE_RECREATE_INDEX = "BUNDLE_RECREATE_INDEX"

        val routeTraces = arrayListOf<String>()
        val activities = arrayListOf<Activity>()
        val clsPointExecutions = hashMapOf<Class<*>, PointRunner>()
        val clsResolveDataMap: HashMap<Class<*>, HashMap<String, MutableLiveData<Bundle>>> =
            hashMapOf()
        val clsRejectDataMap: HashMap<Class<*>, HashMap<String, MutableLiveData<Throwable>>> =
            hashMapOf()
        val globalDecorators = arrayListOf<SliceDecorator>()

        private fun install(app: Application) {
            app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity) {
                    activity.javaClass.let {
                        clsPointExecutions[it]?.apply {
                            pauseInvoker(activity)
                            pauseInvoker = {}
                        }
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    activity.javaClass.let {
                        clsPointExecutions[it]?.apply {
                            resumeInvoker(activity)
                            resumeInvoker = {}
                        }
                    }
                }

                override fun onActivityStarted(activity: Activity) {
                    activity.javaClass.let {
                        clsPointExecutions[it]?.apply {
                            startInvoker(activity)
                            startInvoker = {}
                        }
                    }
                }

                override fun onActivityDestroyed(activity: Activity) {
                    activity.javaClass.let {
                        clsPointExecutions[it]?.apply {
                            destroyInvoker(activity)
                            destroyInvoker = {}
                        }
                    }
                    activity.let {
                        synchronized(activities) {
                            activities.remove(it)
                        }
                        clsResolveDataMap.remove(it.javaClass)
                        clsRejectDataMap.remove(it.javaClass)
                        clsPointExecutions.remove(it.javaClass)
                    }
                }

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle
                ) {
                    activity.javaClass.let {
                        synchronized(activities) {
                            outState.putInt(BUNDLE_RECREATE_INDEX, activities.indexOf(activity))
                        }
                        clsPointExecutions[it]?.apply {
                            saveInstanceStateInvoker(
                                activity,
                                outState
                            )
                            saveInstanceStateInvoker = { _, _ -> }
                        }
                    }
                }

                override fun onActivityStopped(activity: Activity) {
                    activity.javaClass.let {
                        clsPointExecutions[it]?.apply {
                            stopInvoker(activity)
                            stopInvoker = {}
                        }
                    }
                }

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?
                ) {
                    activity.javaClass.let {
                        clsPointExecutions[it]?.apply {
                            createInvoker(activity)
                            createInvoker = {}
                        }
                    }
                    activity.let {
                        synchronized(activities) {
                            if (savedInstanceState != null) {
                                val insertIdx = savedInstanceState.getInt(BUNDLE_RECREATE_INDEX, -1)
                                if (insertIdx == -1) {
                                    activities.add(it)
                                    routeTraces.add(it.localClassName)
                                } else {
                                    try {
                                        activities.add(insertIdx, it)
                                        routeTraces.add(insertIdx, it.localClassName)
                                    } catch (e: IndexOutOfBoundsException) {
                                        activities.add(
                                            if (activities.size == 0) 0 else activities.size - 1,
                                            it
                                        )
                                        routeTraces.add(
                                            if (routeTraces.size == 0) 0 else routeTraces.size - 1,
                                            it.localClassName
                                        )
                                    }
                                }
                            } else {
                                activities.add(it)
                                routeTraces.add(it.localClassName)
                            }
                        }
                        clsResolveDataMap[it.javaClass] = hashMapOf()
                        clsRejectDataMap[it.javaClass] = hashMapOf()
                    }
                }

            })
        }

        fun of(context: Any): SliceRouter =
            when (context) {
                is FragmentActivity -> {
                    of(context)
                }
                is Fragment -> {
                    of(context)
                }
                is Context -> {
                    of(context)
                }
                else -> throw IllegalArgumentException("context must be activity or fragment")
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
        resultGenerator: () -> Bundle = { bundleOf(BUNDLE_NOT_EXIST to true) }
    ) {
        finishAndDispatch(step) { targetClz, destClz ->
            val result = resultGenerator()
            if (!result.getBoolean(BUNDLE_NOT_EXIST)) {
                clsResolveDataMap[targetClz]?.get(destClz.name)
                    ?.postValue(result)
            }
        }
    }

    fun popToCls(
        cls: Class<*>,
        resultGenerator: () -> Bundle = { bundleOf(BUNDLE_NOT_EXIST to true) }
    ) {
        val targetIdx = activities.indexOfLast { it.javaClass == cls }
        if (targetIdx == -1) {
            Timber.e("there is no instance with target class in page stack")
            return
        }
        val step = activities.size - targetIdx - 1
        if (step <= 0) {
            Timber.e("cannot navigate to current page")
            return
        }
        pop(step, resultGenerator)
    }

    fun reject(
        step: Int = 1,
        throwable: Throwable
    ) {
        finishAndDispatch(step) { targetClz, destClz ->
            clsRejectDataMap[targetClz]?.get(destClz.name)
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
            val targetClz = activities[_currentIdx - _step].javaClass
            val destClz = activities[_currentIdx - _step + 1].javaClass
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
            if (fragment != null) {
                if (activities.size == 0) {
                    return -1
                } else {
                    return if (activities.indexOf(
                            fragment!!.requireActivity()
                        ) == -1
                    ) {
                        activities.size - 1
                    } else activities.indexOf(fragment!!.requireActivity())
                }
            } else {
                return if (activities.size == 0) -1 else activities.size - 1
            }
        } else {
            if (activities.size == 0) {
                return -1
            } else {
                return if (activities.indexOf(
                        activity!!
                    ) == -1
                ) {
                    activities.size - 1
                } else activities.indexOf(activity!!)
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
                { observeResult(ctx, cls.name, it, resolve) },
                { observeResult(ctx, cls.name, it, reject) }
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
                                { cancelObserve(ctx, clazz.name, it) },
                                { cancelObserve(ctx, clazz.name, it) }
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
                                { cancelObserve(ctx, clazz.name, it) },
                                { cancelObserve(ctx, clazz.name, it) }
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

    fun pushIntent(
        intent: Intent,
        assembleParams: (Intent) -> Unit = {},
        scopePointRunner: PointRunner.() -> Unit = {},
        reject: (Throwable) -> Unit = { /*ignore*/ },
        resolve: (Bundle) -> Unit
    ) {
        val bindCallback: (LifecycleOwner, Class<*>) -> Unit = { ctx, cls ->
            onResolverAndReject(
                { observeResult(ctx, cls.name, it, resolve) },
                { observeResult(ctx, cls.name, it, reject) }
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
                        assembleParams(intent)
                        try {
                            decorate(intent)
                        } catch (e: RedirectException) {
                            clsPointExecutions.remove(Class.forName(intent.component!!.className))
                            onResolverAndReject(
                                {
                                    cancelObserve(
                                        ctx,
                                        Class.forName(intent.component!!.className).name,
                                        it
                                    )
                                },
                                {
                                    cancelObserve(
                                        ctx,
                                        Class.forName(intent.component!!.className).name,
                                        it
                                    )
                                }
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
                        assembleParams(intent)
                        try {
                            decorate(intent)
                        } catch (e: RedirectException) {
                            clsPointExecutions.remove(Class.forName(intent.component!!.className))
                            onResolverAndReject(
                                {
                                    cancelObserve(
                                        ctx,
                                        Class.forName(intent.component!!.className).name,
                                        it
                                    )
                                },
                                {
                                    cancelObserve(
                                        ctx,
                                        Class.forName(intent.component!!.className).name,
                                        it
                                    )
                                }
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
        assembleParams: (Intent) -> Unit = {},
        reject: (Throwable) -> Unit = { /*ignore*/ },
        resolve: (Bundle) -> Unit
    ) {
        val bindCallback: (LifecycleOwner, Class<*>) -> Unit = { ctx, cls ->
            onResolverAndReject(
                { observeResult(ctx, cls.name, it, resolve) },
                { observeResult(ctx, cls.name, it, reject) }
            )
        }
        val navigate: (LifecycleOwner) -> Unit = { ctx ->
            bindCallback(ctx, InternalRouteActivity::class.java)
            when (ctx) {
                is Activity -> {
                    try {
                        val intent = Intent(action)
                        assembleParams(intent)
                        try {
                            decorate(intent)
                        } catch (e: RedirectException) {
                            onResolverAndReject(
                                { cancelObserve(ctx, action, it) },
                                { cancelObserve(ctx, action, it) }
                            )
                            intent.setClass(ctx, e.redirectCls)
                        }
                        InternalRouteActivity.routeAction(ctx, intent)
                    } catch (e: BlockException) {
                    }
                }
                is Fragment -> {
                    try {
                        val intent = Intent(action)
                        assembleParams(intent)
                        try {
                            decorate(intent)
                        } catch (e: RedirectException) {
                            onResolverAndReject(
                                { cancelObserve(ctx, action, it) },
                                { cancelObserve(ctx, action, it) }
                            )
                            intent.setClass(ctx.context!!, e.redirectCls)
                        }
                        InternalRouteActivity.routeAction(ctx.context!!, intent)
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
        onResolve: (HashMap<String, MutableLiveData<Bundle>>) -> Unit,
        onReject: (HashMap<String, MutableLiveData<Throwable>>) -> Unit
    ) {
        if (activities.isEmpty()) {
            return
        }
        val currentResolves = clsResolveDataMap[activities[currentPageIdx()].javaClass]
        currentResolves?.let {
            onResolve(it)
        }
        val currentRejects = clsRejectDataMap[activities[currentPageIdx()].javaClass]
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
                { observeResult(ctx, cls.name, it, resolve) },
                { observeResult(ctx, cls.name, it, reject) }
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
                                { cancelObserve(ctx, clazz.name, it) },
                                { cancelObserve(ctx, clazz.name, it) }
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
                                { cancelObserve(ctx, clazz.name, it) },
                                { cancelObserve(ctx, clazz.name, it) }
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
        clazz: String,
        liveDataMap: HashMap<String, MutableLiveData<T>>,
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
        clazz: String,
        liveDataMap: HashMap<String, MutableLiveData<T>>
    ) {
        liveDataMap[clazz]?.removeObservers(ctx)
        liveDataMap.remove(clazz)
    }
}