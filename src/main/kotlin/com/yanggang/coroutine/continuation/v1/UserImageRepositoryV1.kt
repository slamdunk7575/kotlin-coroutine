package com.yanggang.coroutine.continuation.v1

import kotlinx.coroutines.delay

class UserImageRepositoryV1 {
    suspend fun findImage(profile: ProfileV1, continuation: Continuation) {
        delay(100L)
        continuation.resumeWith(ImageV1())
    }
}

class ImageV1 {
}
