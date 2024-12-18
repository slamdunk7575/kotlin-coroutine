package com.yanggang.coroutine.continuation.v1

import kotlinx.coroutines.delay

class UserProfileRepositoryV1 {
    suspend fun findProfile(userId: Long, continuation: Continuation) {
        delay(100L)
        continuation.resumeWith(ProfileV1())
    }
}

class ProfileV1 {
}
