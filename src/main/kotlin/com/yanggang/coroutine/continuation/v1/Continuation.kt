package com.yanggang.coroutine.continuation.v1

interface Continuation {
    suspend fun resumeWith(data: Any?)
}
