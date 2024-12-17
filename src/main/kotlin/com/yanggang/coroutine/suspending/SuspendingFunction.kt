package com.yanggang.coroutine.suspending

import com.yanggang.coroutine.routine.printWithThread
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

/*
suspending function
- susp리end 가 붙은 함수
- 다른 suspend 가 붙은 함수를 호출할 수 있다

Q. 어떻게 fun main() 에서 suspend 함수인 delay() 를 호출했을까?
launch 함수가 가지고 있는 마지막 함수형 파라미터 block 이 suspend lambda 이기 때문에 가능
즉, suspend delay() 를 호출하기 위해선 부르는 함수가 suspend 여야 하는데
main() 함수가 suspend 일 필요는 없는 것이고 launch() 에서 쓰이는 마지막 함수 파라미터인 block 이 suspend 함수임
이것을 suspending lambda 라고 부른다

public fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
*/
fun main1(): Unit = runBlocking {
    launch {
        delay(1_00L)
    }
}


/*
Q. suspending function 의 또 다른 기능은 없을까?
- 코루틴이 중지 되었다가 재개 될 수 있는 지점 (suspension point)
- 중지가 될 수도 있고 안될 수도 있다

suspending function 을 중단 지점으로만 이해하게 되면 아래 코드는
'A' -> 중단 -> 'C' -> 'B' 순으로 출력될것 같지만
실제 결과는 a(), b() 가 실행되고 c() 가 실행됨

실행 결과:
[main] A
[main] B
[main] C
*/
fun main2(): Unit = runBlocking {
    launch {
        a()
        b()
    }

    launch {
        c()
    }
}

suspend fun a() {
    printWithThread("A")
}

suspend fun b() {
    printWithThread("B")
}

suspend fun c() {
    printWithThread("C")
}


/*
Q. 이런 suspending function 은 어디에 활용할 수 있을까?
- 비동기 프로그래밍을 할때 콜백 지옥에서 벗어날 수 있는 동기식 코드를 작성하는데 도움을 준다
- 여러 비동기 라이브러리를 사용할 수 있게 도와준다

아래 main() 함수는 runBlocking 에 의해 하나의 코루틴으로 이루어져 있고
다시 내부에는 async 로 인한 코루틴 2개가 존재함 (async 는 결과값을 반환하는 코루틴 빌더)
call1(), call()2 는 외부 IO 호출을 한다고 했을때,
첫번째 async 를 통해 외부 IO 콜을 한 다음 결과를 얻어와 다시 두번째 외부 IO 콜을 하고 결과를 출력

하지만 이 코드를 main() 입장에서 바라봤을때 아쉬운 점이 있다
-> 이 코드는 Job 인터페이스의 하위 타입인 Deferred 에 의존하고 있다는 것
*/
fun main3(): Unit = runBlocking {
    val result1: Deferred<Int> = async {
        call1()
    }

    val result2 = async {
        call2(result1.await())
    }

    printWithThread(result2.await())
}

fun call1(): Int {
    Thread.sleep(1_000L)
    return 100
}

fun call2(num: Int): Int {
    Thread.sleep(1_000L)
    return num * 2
}


/*
Q. 만약, 코루틴의 async 를 쓰는게 아니라 자바의 CompletableFuture 를 쓰거나
Reactor 같은 다른 비동기 라이브러리로 갈아끼워야 한다면?
라이브러리 변경의 여파가 main() 함수에 까지 전파되어 버린다

이럴때 suspend function 을 활용할 수 있음
main() 함수 입장에서 내부 구현은 모르고 suspend function 을 통해 중단될 수도 있는 지점을 호출하게됨
결과는 Int 라는 순수한 코틀린 타입에만 의존하게 됨
call() 함수를 구현하는 측에서는 코루틴의 async 를 사용하거나 CompletableFuture 를 사용하거나
또 다른 비동기 라이브러리를 사용하거나 상관없게 됨
*/
fun main4(): Unit = runBlocking {
    val result1 = call3()
    val result2 = call2(result1)

    printWithThread(result2)
}

suspend fun call3(): Int {
    return CoroutineScope(Dispatchers.Default).async {
        Thread.sleep(1_000L)
        100
    }.await()
}

suspend fun call4(num: Int): Int {
    return CompletableFuture.supplyAsync {
        Thread.sleep(1_000L)
        num * 2
    /*
    여기서 await() 은 코루틴이 만든것
    CompletableFuture 의 상위 타입인 CompletionStage 의 확장함수로써 만들어둔것
    코루틴은 Future, Reactor 등 다양한 비동기 라이브러리에 대한 일종의 어댑터를 활발하게 지원함
    */
    }.await()
}


/*
suspend function 은 인터페이스 에서도 사용할 수 있다
인터페이스 구현체에서 반드시 suspend fun 함수를 구현해야 함으로
특정 구현체는 A 비동기 라이브러리를 쓰고 다른 구현체틑 B 비동기 라이브러리를 사용할 수 있게됨
*/
interface AsyncCaller {
    suspend fun call()
}

class AsyncCallerImpl : AsyncCaller {
    override suspend fun call() {
        TODO("Not yet implemented")
    }
}


/*
코루틴 라이브러리 에서 제공하는 suspend 함수들

coroutineScope suspend 함수
- 추가적인 코루틴을 만들고 주어진 함수 블록이 바로 실행된다 (launch, async 는 바로 실행x)
- 만들어진 코루틴이 모두 완료되면 다음 코드로 넘어간다

아래 코드는 총 4개의 코루틴이 존재
runBlocking 코루틴 1개, coroutineScope 코루틴 1개, async 코루틴 2개

public suspend fun <R> coroutineScope(block: suspend CoroutineScope.() -> R): R {

원래 launch, async 를 쓰면 "START" -> "END" -> launch, async 코루틴 코드 순서로 실행되지만
coroutineScope 는 사용된 즉시 실행되고 그 coroutineScope 에 의해 만들어진 코루틴이 종료될때 까지
다음 코드로 넘어가지 않기 때문에 아래처럼 실행됨

실행 결과:
[main] START
[main] 30
[main] END

coroutineScope 은 또 다른 코루틴들을 동시에 여러개 사용하고 싶은데
그러한 기능을 또 다른 함수로 분리하고 싶을때 사용할 수 있음
일종의 병렬처리를 위한 가교(다리)역할
*/
fun main5(): Unit = runBlocking {
    printWithThread("START")
    printWithThread(calculateResult2())
    printWithThread("END")
}

suspend fun calculateResult1(): Int = coroutineScope {
    val num1 = async {
        delay(1_000L)
        10
    }

    val num2 = async {
        delay(1_000L)
        20
    }

    num1.await() + num2.await()
}

/*
withContext
- coroutineScope 과 기본적으로 유사하다
- context 에 변화를 주는 기능이 추가적으로 있다

아래처럼 withContext 를 활용하면 main() 는 메인 쓰레드에서 돌고
calculateResult2() 는 디스패처가 제공하는 쓰레드에서 돌게됨
coroutineScope 과 동일한데 coroutineContext 에 대해 추가적인 옵션을 덮어쓰고 싶을때 사용
*/
suspend fun calculateResult2(): Int = withContext(Dispatchers.Default) {
    val num1 = async {
        delay(1_000L)
        10
    }

    val num2 = async {
        delay(1_000L)
        20
    }

    num1.await() + num2.await()
}


/*
withTimeout, withTimeoutOrNull
- coroutineScope 과 기본적으로 유사하다
- 주어진 시간 안에 새로 생긴 코루틴이 완료되어야 한다
- 주어진 시간 안에 코루틴이 완료되지 못하면 예외를 던지거나 null 을 반환한다

실행 결과:
Exception in thread "main" kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1000 ms

val result: Int? = withTimeoutOrNull(1_000) 변경후
실행 결과:
[main] null
*/
fun main(): Unit = runBlocking {
    val result: Int? = withTimeoutOrNull(1_000) {
        delay(1_500L)
        10 + 20
    }

    printWithThread(result)
}
