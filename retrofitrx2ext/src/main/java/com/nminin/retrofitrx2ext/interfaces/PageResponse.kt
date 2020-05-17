package com.nminin.retrofitrx2ext.interfaces

interface PageResponse<T> {

    fun getCurrentPage(): Int

    fun getLastPage(): Int

    fun getData(): List<T>

}