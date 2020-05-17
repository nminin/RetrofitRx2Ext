package com.nminin.retrofitrx2ext

import com.nminin.retrofitrx2ext.interfaces.Api
import com.nminin.retrofitrx2ext.interfaces.PageResponse
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import retrofit2.Call


class PagePagination<T, A : Api>(
    private val api: A,
    private val perPage: Int,
    private val call: (A, Int, Int) -> Call<PageResponse<T>>
) {
    private val disposable = CompositeDisposable()
    private val data = BehaviorSubject.create<MutableList<T>>()
    private val isRefreshing = BehaviorSubject.create<Boolean>()
    private val isUpdating = BehaviorSubject.create<Boolean>()
    private var page: Int = 0
    private var lastPage: Int = 1

    fun getData(): Observable<List<T>> = data.map {
        it.toList()
    }

    fun isRefreshing(): Observable<Boolean> = isRefreshing

    fun isUpdating(): Observable<Boolean> = isUpdating

    fun disposable() = disposable

    fun refresh(onError: (Throwable) -> Unit) {
        page = 0
        disposable.add(
            call.invoke(api, page, perPage).singleResponse()
                .doOnSubscribe {
                    isRefreshing.onNext(true)
                }
                .doOnSuccess {
                    isRefreshing.onNext(false)
                }
                .doOnError {
                    isRefreshing.onNext(false)
                }
                .subscribe({
                    page = it.getCurrentPage()
                    lastPage = it.getLastPage()
                    data.onNext(data.value?.apply {
                        this.addAll(it.getData())
                    } ?: mutableListOf())
                }, {
                    onError.invoke(it)
                })
        )
    }

    fun update(onError: (Throwable) -> Unit) {
        disposable.add(
            call.invoke(api, page + 1, perPage).singleResponse()
                .doOnSubscribe {
                    isUpdating.onNext(true)
                }
                .doOnSuccess {
                    isUpdating.onNext(false)
                }
                .doOnError {
                    isUpdating.onNext(false)
                }
                .subscribe({
                    page = it.getCurrentPage()
                    lastPage = it.getLastPage()
                    data.onNext(data.value?.apply {
                        this.addAll(it.getData())
                    } ?: mutableListOf())
                }, {
                    onError.invoke(it)
                })
        )
    }

}