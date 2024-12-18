package com.yanggang.coroutine.continuation.v2

class UserService2 {

    private val userProfileRepositoryV2 =  UserProfileRepositoryV2()
    private val userImageRepositoryV2 = UserImageRepositoryV2()

    suspend fun findUser(userId: Long): UserDtoV2 {
        println("프로필을 가져오겠습니다")
        val userProfile = userProfileRepositoryV2.findProfile(userId)
        println("이미지를 가져오겠습니다")
        val userImage = userImageRepositoryV2.findImage(userProfile)
        return UserDtoV2(userProfile, userImage)
    }

}

data class UserDtoV2(
    val profileV1: ProfileV2,
    val imageV1: ImageV2
)
