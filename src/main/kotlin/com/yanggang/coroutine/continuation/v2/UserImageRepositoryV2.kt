package com.yanggang.coroutine.continuation.v2

import kotlinx.coroutines.delay

class UserImageRepositoryV2 {
    suspend fun findImage(profile: ProfileV2): ImageV2 {
        delay(100L)
        return ImageV2()
    }
}

class ImageV2 {
}
