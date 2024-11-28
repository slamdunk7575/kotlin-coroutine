package com.yanggang.coroutine.builder

import com.yanggang.coroutine.routine.printWithThread
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

/*
코틀린에서 코루틴을 만드는 방법
-> 코루틴을 새로 만드는 함수를 '코루틴 빌더' 라고함

1. runBlocking
- 새로운 코루틴을 만들고 루틴 세계와 코루틴 세계를 이어준다
- 이름에 blocking 이 있듯이, runBlocking 이 만든 코루틴과 안에서 만든 코루틴이 모두 완료될때까지 쓰레드를 블로킹 시킨다
- 해당 스레드는 runBlocking 에 의해 블로킹 된게 풀릴 때까지 다른 코드를 실행할 수 없음

아래 코드 실행순서
1. runBlocking {} 에 의해 코루틴 생성
2. "START" 출력
3. launch {} 에 의해 새로운 코루틴 만듦
4. 2초간 멈췄다가 다른 코루틴으로 넘김
5. launch {} 에서 delay() 를 통해 다른 코루틴으로 넘기더라도
runBlocking 의 성질에 의해 본인과 본인 안에있는(launch) 코루틴이 모두 끝날때가지 스레드 전체를 블로킹
6. 따라서 "LAUNCH END" 출력
7. "END" 출력

-> 이런 runBlocking 특성에 따라 코루틴을 만들고 싶을 때마다 사용해서는 안되고
프로그램에 진입할때 최초로 작성하거나 (예: fun main(): Unit = runBlocking {... )
테스트 코드를 시작할때 특정 테스트 코드에서만 사용하는 것이 좋음

*/
fun example_runblocking() {
    runBlocking {
        printWithThread("START")

        launch {
            /*
            yield() 는 아무것도 하지 않고 다른 코루틴으로 넘긴다면(양보),
            delay(시간) 는 특정 시간만큼 멈췄다가 다른 코루틴으로 넘김
            */
            delay(2_000L)
            printWithThread("LAUNCH END")
        }
    }

    printWithThread("END")
}


/*
2. launch
- 새로운 코루틴을 시작하는 Builder
- 반환값이 없는 코루틴을 실행

Job 객체
- launch 에 의한 코드 결과물이 아니라, launch 가 만들어낸 코루틴 자체를 제어할 수 있는 객체
- 우리가 만든 코루틴을 시작(start), 취소(cancle), 종료시까지 대기(join) 할 수 있다는 의미

아래 코드 실행순서
1. runBlocking {} 에 의해 코루틴 생성
2. launch {} 에 의해 새로운 코루틴 만듦 (start = CoroutineStart.LAZY 옵션이 있기 때문에 job.start() 가 호출될때 까지 대기)
3. 1초간 멈췄다가 다른 코루틴으로 넘김
4. Job 객체에 의해 코루틴 start
 */
fun example_job_start(): Unit = runBlocking {
    /*
    코루틴은 생성 즉시 실행시킬 수 있기 때문에
    명확한 시작 신호를 줄때까지 대기하기 할 수 있게 start = CoroutineStart.LAZY 옵션을 줌
    */
    val job = launch(start = CoroutineStart.LAZY) {
        printWithThread("HELLO LAUNCH")
    }

    delay(1_000L)
    // Job 객체를 통해 start() 함수를 호출해주지 않으면 해당 코루틴은 실행될 수 없는 상태가 됨
    job.start()
}

/*
아래 코드 실행순서
1. runBlocking {} 에 의해 코루틴 생성
2. launch {} 에 의해 새로운 코루틴 만듦 (start = CoroutineStart.LAZY 옵션 없기 때문에 바로 실행)
3. for 문에 의해 5번 까지 반복문 실행
4. printWithThread(it) 출력
5. 0.5 초 대기후 다른 코루틴으로 넘김
6. 1 초 대기호 코루틴 넘김
7. printWithThread(it) 출력
8. job.cancel() 에 의해 코루틴 종료
*/
fun example_job_cancle(): Unit = runBlocking {
    val job = launch {
        (1..5).forEach {
            printWithThread(it)
            delay(500L)
        }
    }

    delay(1_000L)
    job.cancel()
}

/*
아래 코드가 실행되는데 각 1초씩 delay 후 총 2초가 걸릴것으로 예상할 수 있지만 실제로 1.1 초 정도에 실행됨
- job1 코루틴이 시작되고 1초를 기다리는 동안 job2 코루틴이 시작되고 함께 기다렸다가 "JOB 1", "JOB 2" 가 출력되고 종료되기 때문에 (대략 0.1 초 간격 차이)

Job 객체의 join() 를 사용하면, 코루틴1 이 끝날때까지 대기했다가 코루틴2 가 실행된다

Job 객체 활용 정리
- start(): 시작 신호
- cancel(): 종료 신호
- join(): 코루틴이 완료될때까지 대기
*/
fun example_job_join(): Unit = runBlocking {
    val job1 = launch {
        delay(1_000L)
        printWithThread("JOB 1")
    }

    job1.join()

    val job2 = launch {
        delay(1_000L)
        printWithThread("JOB 2")
    }

}

/*
3. async
- launch 와 비슷하지만, 코루틴 함수의 실행 결과를 반환할 수 있다
- async 역시 async 로 만들어진 코루틴을 제어할 수 있는 Deferred 객체를 반환함
- Deferred 객체는 Job 객체를 상속 받고 있기 때문에 start(), cancel(), join() 기능에 더해서 await() 을 사용할 수 있음
- await() 함수를 통해 async 의 결과를 가져올 수 있다
*/
fun example_async_await(): Unit = runBlocking {
    val job = async {
        2 + 5
    }

    val result = job.await();
    println(result)
}

/*
async 를 활용하면 여러 API 를 동시에 호출해서 소요시간을 최소화 할 수 있다
-> 대기가 필요할때 여러 코루틴을 같이 실행시켜놓고 결과를 한번에 가져오는 원리

아래 코드 실행결과
[main] 3
[main] 소요시간: 1027 ms

apiCall1(), apiCall2() 각각 1초씩 2초가 걸릴것으로 예상했지만 1.02 초 정도 걸림
*/
fun example_async_1(): Unit = runBlocking {
    val time = measureTimeMillis {
        val job1 = async { apiCall1() }
        val job2 = async { apiCall2() }
        printWithThread(job1.await() + job2.await())
    }

    printWithThread("소요시간: $time ms")
}

/*
async 활용
- callback 을 사용하지 않고 동기 방식으로 코드를 작성할 수 있다

예: apiCall2() 에서 apiCall1() 의 결과가 필요한 경우
일반적으로, callback 함수를 작성한다면 CallBack 같은 인터페이스를 구현한 함수 or 객체를 넘겨서 apiCall1 이 자신의 결과를 반환할때
CallBack 함수에서 apiCall1 의 결과를 이어받아 동작함
이렇게 CallBack 을 쓰다보면 CallBack Hell(지옥) 에 빠져 점점 depth 가 늘어나게 됨

apiCall1(object : CallBack {
    apiCall2(object : CallBack {
        apiCall3(object : CallBack {
            ...
})


-> async 를 활용하게 되면 쉽게 CallBack 지옥을 회피해서 동기 스타일의 코드를 작설 할 수 있음

아래 코드 실행결과
[main] 3
[main] 소요시간: 2032 ms
*/
fun example_async_callback(): Unit = runBlocking {
    val time = measureTimeMillis {
        val job1 = async { apiCall1() }
        val job2 = async { apiCall2_with_param(job1.await()) }
        printWithThread(job2.await())
    }

    printWithThread("소요시간: $time ms")
}

/*
async 주의할점
-> async 를 CoroutineStart.LAZY 옵션과 같이 사용하면 await() 함수를 호출했을때 계산결과를 계속 기다림 (소요시간 2초 정도 걸림)

아래 코드 실행결과
[main] 3
[main] 소요시간: 2021 ms

job1.await() 코드에 가서야 job1 코루틴이 시작되고 job1 이 끝날때까지 job2 는 시작도 하지않음
-> CoroutineStart.LAZY 옵션을 쓰고 싶을때, start() 함수를 한번 호출해주면 된다

start() 코드 추가후 실행결과
[main] 3
[main] 소요시간: 1025 ms

이미 job1, job2 가 시작 상태로 되어있기 때문에 뒤에 await() 하더라도 소요시간이 1.02 초 정도만 걸리게 된다
*/
fun main(): Unit = runBlocking {
    val time = measureTimeMillis {
        val job1 = async(start = CoroutineStart.LAZY) { apiCall1() }
        val job2 = async(start = CoroutineStart.LAZY) { apiCall2() }

        job1.start()
        job2.start()
        printWithThread(job1.await() + job2.await())
    }

    printWithThread("소요시간: $time ms")
}


/*
suspend fun 은 또 다른 suspend fun 을 호출 할 수 있음
delay() 함수도 suspend fun 이기 때문에 apiCall1 함수를 suspend fun 으로 정의함
*/
suspend fun apiCall1(): Int {
    delay(1_000L)
    return 1
}

suspend fun apiCall2(): Int {
    delay(1_000L)
    return 2
}

suspend fun apiCall2_with_param(num: Int): Int {
    delay(1_000L)
    return num + 2
}
