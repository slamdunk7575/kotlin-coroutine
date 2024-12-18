package com.yanggang.coroutine.continuation.v2

import kotlinx.coroutines.delay

class UserProfileRepositoryV2 {
    suspend fun findProfile(userId: Long): ProfileV2 {
        delay(100L)
        return ProfileV2()
    }
}

class ProfileV2 {
}
