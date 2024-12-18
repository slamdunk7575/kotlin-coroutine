package com.yanggang.coroutine.continuation.v1

/*
Q. 코루틴은 어떤 원리로 중단과 재개를 반복할 수 있을까?

실제 Decompile 한 코틀린 코드와 작성한 UserServiceV1 코드가 완벽하게 같지는 않지만
핵심은 Continuation 이라는 것을 전달하고 label 을 정해서 callback 패턴으로 함수를 서로 연결한다
이런 스타일을 Continuation Passing Style (CPS) 라고 함


실제 Continuation 인터페이스 확인
- 코루틴 내부 정보를 확인할 수 있게 CoroutineContext 를 필드로 가지고 있음
- 결과가 성공 or 실패에 따라서 다음 단계로 넘어갈 수 있는 콜백 함수인 resumeWith() 가 있음

public interface Continuation<in T> {
    public val context: CoroutineContext

    public fun resumeWith(result: Result<T>)
}


-> 실제로 초기 코드 (UserServiceV2.kt)를 Decompile 해보면 없던 Continuation var3 필드가 추가되고
-> 아래에 switch - case 문을 통해 어떤 label 인지에 따라 코드가 나눠져 있음

   @Nullable
   public final Object findUser(long userId, @NotNull Continuation var3) {
      Object $continuation;
      label27: {
         if (var3 instanceof <undefinedtype>) {
            $continuation = (<undefinedtype>)var3;
            if ((((<undefinedtype>)$continuation).label & Integer.MIN_VALUE) != 0) {
               ((<undefinedtype>)$continuation).label -= Integer.MIN_VALUE;
               break label27;
            }
         }

         $continuation = new ContinuationImpl(var3) {
            // $FF: synthetic field
            Object result;
            int label;
            Object L$0;

            @Nullable
            public final Object invokeSuspend(@NotNull Object $result) {
               this.result = $result;
               this.label |= Integer.MIN_VALUE;
               return UserService2.this.findUser(0L, this);
            }
         };
      }

      ProfileV2 userProfile;
      Object var10000;
      label22: {
         Object $result = ((<undefinedtype>)$continuation).result;
         Object var8 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
         switch (((<undefinedtype>)$continuation).label) {
            case 0:
               ResultKt.throwOnFailure($result);
               String var9 = "프로필을 가져오겠습니다";
               System.out.println(var9);
               UserProfileRepositoryV2 var11 = this.userProfileRepositoryV2;
               ((<undefinedtype>)$continuation).L$0 = this;
               ((<undefinedtype>)$continuation).label = 1;
               var10000 = var11.findProfile(userId, (Continuation)$continuation);
               if (var10000 == var8) {
                  return var8;
               }
               break;
            case 1:
               this = (UserService2)((<undefinedtype>)$continuation).L$0;
               ResultKt.throwOnFailure($result);
               var10000 = $result;
               break;
            case 2:
               userProfile = (ProfileV2)((<undefinedtype>)$continuation).L$0;
               ResultKt.throwOnFailure($result);
               var10000 = $result;
               break label22;
            default:
               throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
         }

-> UserProfileRepositoryV2 에서도 없던 Continuation var3 이 추가되고 아래쪽에서 확인 하고 있음

         public final class UserProfileRepositoryV2 {
            @Nullable
            public final Object findProfile(long var1, @NotNull Continuation var3) {
                Object $continuation;
                label20: {
                    if (var3 instanceof <undefinedtype>) {
                    ...
*/

/*
핵심은 Continuation 을 전달하는 것
1. 최초 findUser() 함수가 호출되면 label = 0 인 continuation 객체를 만듦
2. 다른 suspend fun (예: findProfile()) 을 부르면 continuation 을 전달해 다른 suspend fun 이 종료될 때까지 기다리게 됨
3. 다른 suspend fun (예: findProfile()) 이 완료되면 continuation 에 있는 resumeWith() 함수를 호출해서
continuation 이 다시 findUser() 함수를 재귀적으로 호출함
4. 이때 labal 이 바뀌고 data(예: Profile()) 도 넘어오면서 다음 suspend fun (예: findImage()) 을 호출하개 됨
5. 이렇게 왔다갔다 코드를 실행하다 보면 최종적으로 종료되는 시점을 알게되고 결과를 반환


실행 결과:
프로필을 가져오겠습니다
이미지를 가져오겠습니다
UserDtoV1(profileV1=com.yanggang.coroutine.continuation.v1.ProfileV1@18acd1cb, imageV1=com.yanggang.coroutine.continuation.v1.ImageV1@28c0be75)
*/
suspend fun main(): Unit {
    val userService = UserServiceV1()
    println(userService.findUser(1L, null))
}

class UserServiceV1 {

    private val userProfileRepositoryV1 = UserProfileRepositoryV1()
    private val userImageRepositoryV1 = UserImageRepositoryV1()

    private abstract class FindUserContinuation() : Continuation {
        var label = 0
        var profileV1: ProfileV1? = null
        var imageV1: ImageV1? = null
    }

    suspend fun findUser(userId: Long, continuation: Continuation?): UserDtoV1 {
        /*
        인터페이스 Continuation 을 구현한 익명 클래스를 만들고
        label 필드로 각 단계를 구분

        findUser() 함수를 재귀적으로 호출할때 매번 label = 0 짜리 stateMachine 객체를 만들고 있음
        따라서 continuation 이 null 이 아닌 경우에만 Continuation 객체를 만들도록 수정
        그렇지 않으면 현재 continuation 을 재활용함

        여기서 한발짝 더 나아간다면
        Continuation 을 findUser 전용 abstract 클래스를 만들어 활용할 수 있다
        */
        val stateMachine = continuation as? FindUserContinuation ?: object : FindUserContinuation() {

            /*
            Continuation 을 전달하며 CallBack 으로 활용한다
            */
            override suspend fun resumeWith(data: Any?) {
                when (label) {
                    0 -> {
                        profileV1 = data as ProfileV1
                        label = 1
                    }

                    1 -> {
                        imageV1 = data as ImageV1
                        label = 2
                    }
                }
                findUser(userId, this)
            }
        }

        when (stateMachine.label) {
            0 -> {
                // 0단계 - 초기 시작
                println("프로필을 가져오겠습니다")
                val userProfile = userProfileRepositoryV1.findProfile(userId, stateMachine)
            }

            1 -> {
                // 1단계 - 1차 중단 후 재시작
                println("이미지를 가져오겠습니다")
                val userImage = userImageRepositoryV1.findImage(stateMachine.profileV1!!, stateMachine)
            }
        }

        // 2단계 - 2차 중단 후 재시작
        return UserDtoV1(stateMachine.profileV1!!, stateMachine.imageV1!!)
    }

}

data class UserDtoV1(
    val profileV1: ProfileV1,
    val imageV1: ImageV1
)
