package com.landside.slicerouter.utils

import io.reactivex.Observer
import io.reactivex.disposables.Disposable

open class CompletionObserver<T> : Observer<T> {
  var doneInvoker: () -> Unit = { /*nothing*/ }

  fun done(doneInvoker: () -> Unit) {
    this.doneInvoker = doneInvoker
  }

  var nextInvoker: (T) -> Unit = { /*nothing*/ }
  fun next(nextInvoker: (T) -> Unit) {
    this.nextInvoker = nextInvoker
  }

  override fun onNext(t: T) {
    nextInvoker(t)
  }

  var errInvoker: (Throwable) -> Unit = {}
  fun error(errInvoker: (Throwable) -> Unit) {
    this.errInvoker = errInvoker
  }

  var obtainDisposableInvoker:(Disposable)->Unit = {}
  fun obtainDisposable(obtainDisposableInvoker:(Disposable)->Unit){
    this.obtainDisposableInvoker = obtainDisposableInvoker
  }
  override fun onSubscribe(d: Disposable) {
    obtainDisposableInvoker(d)
  }

  override fun onError(t: Throwable) {
    onDone()
    errInvoker(t)
  }

  override fun onComplete() {
    onDone()
  }

  open fun onDone() {
    doneInvoker()
  }
}