package com.yanggang.coroutine.cancel

import com.yanggang.coroutine.routine.printWithThread
import kotlinx.coroutines.*

/*
코루틴 취소
코루틴을 적절히 취소하는 것은 중요하다
왜? 필요하지 않은 코루틴을 적절히 취소해 컴퓨터 자원을 아껴야 한다

취소 대상 코루틴도 취소에 협조해줘야 한다
코루틴이 취소에 협조하는 방법 1
- delay(), yeild() 같은 kotlinx.coroutines 패키지의 suspend 사용하는 것
이 suspend 함수를 사용하는 코루틴은 함수를 호출할때 자동으로 취소 여부를 체크해서 취소에 협조하도록 되어있음
*/
fun example_cancel_1(): Unit = runBlocking {
    val job1 = launch {
        delay(1_000)
        printWithThread("Job 1")
    }

    val job2 = launch {
        delay(1_000)
        printWithThread("Job 2")
    }

    delay(100)
    job1.cancel()
}

/*
아래 코드는 취소가 안된게 아니라 job1 코루틴이 시작되고
0.01 초만에 완료되어 0.1 초를 기다린 뒤에 취소를 못시킨것임
*/
fun example_cancel_2(): Unit = runBlocking {
    val job1 = launch {
        delay(10)
        printWithThread("Job 1")
    }

    delay(100)
    job1.cancel()
}

/*
아래 코루틴은 suspend 함수를 사용하고 있지 않기 때문에
job1.cancel() 이 있음에도 취소가 되지 않는
- 협력하는 코루틴이여야 취소가 된다

runBlocking 코루틴에서 0.1초 정도 delay 를 건뒤 cancel() 을 하려고 하지만
launch 의 코루틴은 suspend 함수를 사용하지 않고 1~5 까지 계속 출력
Thread 입장에서 0.1초 delay -> launch 코루틴 에서 1~5 출력 -> job1.cancel() 시점에는 이미 취소된 상태

예:
[main] 1번째 출력!
[main] 2번째 출력!
[main] 3번째 출력!
[main] 4번째 출력!
[main] 5번째 출력!
*/
fun example_cancle_3(): Unit = runBlocking {
    val job1 = launch(Dispatchers.Default) {
        var i = 1
        var nextPrintTime = System.currentTimeMillis()

        // while (i <= 5) {
        // isActive 상태가 아니면 아예 완료되도록 할 수도 있음
        while (isActive && i <= 5) {
            if (nextPrintTime <= System.currentTimeMillis()) {
                printWithThread("${i++}번째 출력!")
                nextPrintTime += 1_000L
            }

            /*
            코루틴이 취소에 협조하는 방법 2
            - 코루틴 스스로 본인의 상태를 확인해 취소 요청을 받았으면 CancellationException 을 던지기
            isActive 를 사용하면 launch 에 생긴 나자신 코루틴이 취소 명령을 받았는지? 여전히 활성화 상태인지? 알 수 있음

            하지만 이상태로는 job1 launch 코틀린이 취소되지 않는다
            왜? launch 입장에서 본인이 취소가 되었는지 취소 되지 않았는지 확인을 하더라도 cancel 명령이 아직 실행되지 않았기 때문에
            스레드가 cancel() 명령을 받아서 실행하거나 cancel() 명령을 실행해줄 다른 쓰레드 하나가 필요
            - launch(Dispatchers.Default) 라고 넣어주면 launch 코루틴은 main 쓰레드와는 다른 쓰레드에서 실행됨
            */
            if (!isActive) {
                throw CancellationException()
            }
        }
    }

    delay(100)
    job1.cancel()
}

/*
사실 delay(), yeild() 같은 함수도 CancellationException 예외를 던져 취소를 하고있다
- 그래서 try-catch 를 통해 CancellationException 예외를 잡아버리면 코틀린이 취소되지 않는다

아래 코드는 원래 launch 코틀린에서 delay() 가 걸리면 취소 신호를 받아서 CancellationException 을 던지며
launch 코루틴을 취소시켜야 하는데 지금은 try-catch 로 잡고 아무것도 하지 않기 때문에 정상적으로 아래 "delay() 에 의해 취소되지 않았다" 가 출력됨
반대로 이걸 이용하면 finally 를 사용해서 필요한 자원을 닫는 처리를 할 수 있다

즉, suspend fun 을 호출하는 방법은 try-catch-finally 의 영향을 받는다

예:
[main] 취소 시작
[main] delay() 에 의해 취소되지 않았다
*/
fun main(): Unit = runBlocking {
    val job1 = launch {
        try {
            delay(1_00L)
        } catch (e: CancellationException) {
            // 아무것도 안한다
        }

        printWithThread("delay() 에 의해 취소되지 않았다")
    }

    delay(100L)
    printWithThread("취소 시작")
    job1.cancel()
}
