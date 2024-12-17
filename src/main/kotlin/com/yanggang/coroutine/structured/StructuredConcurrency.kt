package com.yanggang.coroutine.structured

import com.yanggang.coroutine.routine.printWithThread
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/*
Structured Concurrency

Q. 코루틴을 표현하는 Job 객체의 Life Cycle 은 왜 COMPLETING - COMPLETE 2단계로 나눠져 있을까?

NEW - ACTIVE - COMPLETING - COMPLETE
   예외발생 \     /
        CANCELLING - CANCELLED

-> 부모 코루틴에서 자식 코루틴을 기다려야 하기 때문임
부모 코루틴 입장에서 자식 코루틴이 여려개 있을 수 있고
첫번째 자식 코루틴이 끝났더라도 두번째 자식 코루틴에서 예외가 발생하면
부모 코루틴에서 실행할 코드가 더이상 없더라도 자식 코루틴을 기다렸다가 CANCELLING 상태로 돌아가야 하기 때문

실행 결과:
[main] A 코루틴
Exception in thread "main" java.lang.IllegalArgumentException: 코루틴 실패!!!

두번때 자식 코루틴에서 예외가 발생하여 부모 코루틴에서 COMPLETING 상태로 대기하고 있는데
부모 코루틴으로 예외를 전파해서 부모 코루틴이 CANCELLING 상태로 변경되면서 취소됨

Q. 만약 자식 코루틴의 delay() 시간을 서로 바꾼다면?
즉, 예외가 발생하는 두번째 자식 코루틴에서 예외가 먼저 실행되도록 한다면

실행 결과:
Exception in thread "main" java.lang.IllegalArgumentException: 코루틴 실패!!!

        run Blocking
       /            \
    launch         launch

첫번째 자식 코루틴에서 delay(600L) 를 호출함으로써 suspend fun() 을 호출하고 있고 따라서 취소 협조적임
전체 부모 코루틴에서 두번째 자식 코루틴이 먼저 실패하여 예외가 전파 -> 첫번째 코루틴에게 취소 요청 -> "A 코루틴" 출력되지 않고 부모 코루틴이 종료됨

자식 코루틴을 기다리다 예외가 발생하면 예외가 부모로 전파되고 본인(부모 코루틴)이 취소되어야 하기 때문에
다른 자식 코루틴에게 취소 요청을 보낸다

-> 부모 - 자식 관계의 코루틴이 한 몸 처럼 움직이는 것을 Structured Concurrency 라고 함
(코틑린 공식 문서)
- Structured Concurrency 는 수많은 코루틴이 유실되거나 누수되지 않도록 보장한다
- Structured Concurrency 는 코드 내의 에러가 유실되지 않고 적절히 보고될 수 있도록 보장한다

(정리) 취소와 예외, Structured Concurrency
- 자식 코루틴에서 예외가 발생할 경우, Structured Concurrency 에 의해 부모 코루틴이 취소되 부모 코루틴의 다른 자식 코루틴들도 취소된다
- 자식 코루틴에서 예외가 발생하지 않더라도, cancel() 같은걸 통해 부모 코루틴이 취소되면 자식 코루틴들이 취소된다
- 다만, CancellationException 은 정상적인 취소로 간주하기 때문에 부모 코루틴에게 예외가 전파되지 않고
부모 코루틴의 다른 자식 코루틴을 취소시키지도 않는다

*/
fun main(): Unit = runBlocking {

    launch {
        delay(500L)
        printWithThread("A 코루틴")
    }

    launch {
        delay(600L)
        throw IllegalArgumentException("코루틴 실패!!!")
    }
}
