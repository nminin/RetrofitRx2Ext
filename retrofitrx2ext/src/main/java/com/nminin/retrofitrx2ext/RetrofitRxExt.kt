package com.nminin.retrofitrx2ext

import com.nminin.retrofitext.response
import com.nminin.retrofitrx2ext.interfaces.Api
import com.nminin.retrofitrx2ext.interfaces.PageResponse
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call

fun <T> Call<T>.singleResponse(): Single<T> = Single.create { emitter ->
    this.response(
        onSuccess = { code, item ->
            if (!emitter.isDisposed) {
                emitter.onSuccess(item)
            }
        },
        onError = { code, message ->
            if (!emitter.isDisposed) {
                emitter.onError(Exception(message))
            }
        })
}

fun <T> Call<T>.maybeResponse(): Maybe<T> = Maybe.create { emitter ->
    this.response(
        onSuccess = { code, item ->
            if (!emitter.isDisposed) {
                emitter.onSuccess(item)
                emitter.onComplete()
            }
        },
        onError = { code, message ->
            if (!emitter.isDisposed) {
                emitter.onError(Exception(message))
            }
        },
        onSuccessEmpty = {
            if (!emitter.isDisposed) {
                emitter.onComplete()
            }
        })
}

fun <T> Call<T>.observableResponse(): Observable<T> = Observable.create { emitter ->
    this.response(
        onSuccess = { code, item ->
            if (!emitter.isDisposed) {
                emitter.onNext(item)
                emitter.onComplete()
            }
        },
        onError = { code, message ->
            if (!emitter.isDisposed) {
                emitter.onError(Exception(message))
            }
        },
        onSuccessEmpty = {
            if (!emitter.isDisposed) {
                emitter.onComplete()
            }
        })
}

fun <A : Api, T> A.paginationResponse(
    perPage: Int,
    apiCall: (api: A, page: Int, perPage: Int) -> Call<PageResponse<T>>
) = PagePagination(this, perPage, apiCall)

fun <A : Api, T> A.singleAllPagesResponse(apiCall: (api: A, page: Int) -> Call<PageResponse<T>>): Single<List<T>> =
    Observable.create<List<T>> { emitter ->
        this.recursiveApiCall(apiCall, 1,
            onNext = {
                if (!emitter.isDisposed) {
                    emitter.onNext(it)
                }
            },
            onComplete = {
                if (!emitter.isDisposed) {
                    emitter.onComplete()
                }
            },
            onError = {
                if (!emitter.isDisposed) {
                    emitter.onError(Exception(it))
                }
            })
    }
        .collectInto(mutableListOf()) { list, result ->
            list.toMutableList().addAll(result)
        }

private fun <A : Api, T> A.recursiveApiCall(
    apiCall: (api: A, page: Int) -> Call<PageResponse<T>>,
    page: Int,
    onNext: (List<T>) -> Unit,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {
    apiCall.invoke(this, page).response(
        onSuccess = { code, pageData ->
            onNext.invoke(pageData.getData())
            if (pageData.getCurrentPage() < pageData.getLastPage()) {
                this.recursiveApiCall(apiCall, page + 1, onNext, onComplete, onError)
            } else {
                onComplete.invoke()
            }
        },
        onError = { code, error ->
            onError.invoke(error)
        }
    )
}



