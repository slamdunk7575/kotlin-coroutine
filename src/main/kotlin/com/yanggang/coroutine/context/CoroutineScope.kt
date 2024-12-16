package com.yanggang.coroutine.context

import com.yanggang.coroutine.routine.printWithThread
import kotlinx.coroutines.*
import java.util.concurrent.Executors

/*
우리는 벌써 CoroutineScope 를 활용한 적이 있다
: runBlocking 을 사용했을때, runBlocking 의 자식 코루틴이 아니라
루트 코루틴으로 새로운 코루틴을 만들기 위해 CoroutineScope 함수를 이용해서 새영역에 launch 라는 코루틴 빌더를 써서
새로운 코루틴을 루트 코루틴으로 만들었음

launch / async 는 CoroutineScope 의 확장함수이다
예:
public fun CoroutineScope.launch(
는 CoroutineScope 의 확장함수이기 때문에 CoroutineScope 이 없으면 사용할 수 없다

fun main() {
    launch() // 사용X
}
*/
fun main1(): Unit = runBlocking {
    val job1 = CoroutineScope(Dispatchers.Default).launch {
        delay(1_000L)
        printWithThread("JOB 1")
    }
}

/*
지금까지 runBlocking 이 코루틴과 루틴의 세계를 이어주며 CoroutineScope 을 제공해주고 있었다
반대로 우리가 직접 CoroutineScope 을 만들면 runBlocking 이 필요하지 않다
*/
suspend fun main2(): Unit {
    val job2 = CoroutineScope(Dispatchers.Default).launch {
        delay(1_000L)
        printWithThread("JOB 2")
    }

    /*
    runBlocking 이 있을땐, 안에서 실행된 다른 코루틴이 끝날때까지 쓰레드를 블로킹 해주지만
    지금 같은 경우는 블로킹 해주는 코드가 없기 때문에 Thread.sleep() 을 통해 기다려줌
    */
    // Thread.sleep(1_500L)

    /*
    또는 join() 은 suspend fun 함수이기 때문에 main 함수를 suspend fun 으로 변경해서
    Thread.sleep() 을 사용하지 않아도 job2.join() 에서 기다리게 할 수 있다
    */
    job2.join()
}

/*
Q. CoroutineScope 의 주요 역할은 무엇일까?
-> CoroutineContext 라는 데이터를 보관하는 것

public interface CoroutineScope {
    public val coroutineContext: CoroutineContext
}

Q. CoroutineContext 란?
-> 코루틴과 관련된 여러가지 데이터를 갖고 있다
예: 코루틴 이름, CoroutineExceptionHandler, 코루틴 그 자체, CoroutineDispatcher 등

Q. Dispatcher 란?
-> 코루틴이 어떤 스레드에 배정될지를 관리하는 역할

(정리)
CoroutineScope: 코루틴이 탄생할 수 있는 영역
CoroutineContext: 코루틴과 관련된 데이터를 보관

CoroutineScope, CoroutineContext 는 코루틴의 Structured Concyrrency 를 이루는 기반이 됨

           (부모 코루틴: CoroutineScope 존재)  (Context)
           /                                /
       (자식 코루틴)                       (Context)

부모(루트) 코루틴이 존재한다는 것은 CoroutineScope 이 존재한다는 것
CoroutineScope 은 CoroutineContext 를 가지고 있다
따라서 이 부모 코루틴의 Context 에는 부모 코틴 그 자체, 이름, Dispatchers.Default 등이 있음

부모 코루틴에서 자식 코루틴을 만들게 되면 같은 영역에 생성되는데
이때 CoroutineScope 를 통해 부모 코루틴의 Context를 가져오게 되고 필요한 내용을 덮어써
새로운 자식 코루틴 Context 를 만듦
자식 코루틴 Context 에는 자식 코루틴 그 자체, 새로운 이름, Dispatchers.Default 등이 있음

-> 이런 과정에서 부모 - 자식 코루틴 관계가 설정된다
즉, 이 원리가 코루틴에서 Structured Concurrency 를 작동시킬 수 있는 기반이 됨
*/

/*
이렇게 한 영역에 있는 코루틴들은 영역 자체를 취소시켜 모든 코루틴을 종료시킬 수 있음
클래스 내부에서 독립적인 CoroutineScope 을 가지고 있다면 해당 클래스에서 사용하던 코루틴을 한번에 종료시킬 수 있다
*/

class AsyncLogic {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun doSomething() {
        scope.launch {
            /*
            무언가 코루틴이 시작되어 작업!
            (예: 여러 코루틴을 트리 구조로 만듦)
            */
        }
    }

    /*
    AsyncLogic 에서 doSomething() 로 어떤 작업을 실행시켜 놓고
    그 작업이 필요 없어지면, AsyncLogic 이 가지고 있는 scope 자체를 취소(cancel) 시킴으로써
    그 scope 에서 돌고 있던 모든 코루틴에게 취소 신호를 보낼 수 있다
    */
    fun destroy() {
        scope.cancel()
    }
}

/*
CoroutineContext
- Map + Set 을 합쳐놓은 형태
- key - value 형태로 데이터 저장
- 같은 key 의 데이터는 유일
- get(Key: key) 를 통해 Element 를 조회

@SinceKotlin("1.3")
public interface CoroutineContext {
    public operator fun <E : Element> get(key: Key<E>): E?

'+' 기호를 통해 여러 Element 를 합칠 수 있다
*/
fun main3(): Unit {

    /*
    CoroutineName 이라는 CoroutineContext 의 Element + SupervisorJob() 역시 하나의 Context Element
    Dispatchers.Default 같은 것도 CoroutineContext 의 Element 이므로 '+' 기호로 조합할 수 있음
    */
    CoroutineName("나만의 코루틴") + SupervisorJob()
    CoroutineName("나만의 코루틴") + Dispatchers.Default
}

suspend fun example() {
    val job3 = CoroutineScope(Dispatchers.Default).launch {
        delay(1_000L)
        printWithThread("JOB 3")

        /*
        coroutineContext 필드를 코루틴 블록 내부에서 사용하면 해당 코루틴이 가지고 있는 context 에 접근할 수 있음
        여기서 CoroutineContext 자체에 Element 를 추가할 수 있음
        minusKey(key 이름) 를 통해 Context 에 존재하는 Element 를 제거할 수도 있음
        */
        coroutineContext + CoroutineName("이름")
        coroutineContext.minusKey(CoroutineName.Key)
    }

    job3.join()
}

/*
CoroutineDispatcher
코루틴을 스레드에 배정하는 역할

예:
코루틴에 존재하는 코드가 특정 스레드에 배정되어 실행된다
중단지점이 존재한다면, 스레드1 에서 코루틴1의 코드1이 돌고
중단 되었다가 코루틴2의 코드3이 배정되어 실행될 수 있다

코루틴1 |    |코드2|
코루틴2 |코즈3|
스레드1 |코드1|중단|

(종류)
Dispatchers.Default
- 가장 기본적인 디스패처
- CPU 자원을 많이 쓸때 권장
- 별다른 설정이 없으면 이 디스패처가 사용된다

Dispatchers.IO
- I/O 작업에 최정화된 디스패처

Dispatchers.Main
- 보통 UI 컴포넌트를 조작하기 위한 디스패처
- 특정 의존성을 갖고 있어야 정상적으로 활용할 수 있다

예:
val job = CoroutineScope(Dispatchers.Main).launch {
...
프로젝트에 Adroid, 다른 UI 관련 의존성이 추가되어 있지 않다면 에러가 발생함

자바의 ExcutorService 를 디스패처로 바꿀 수 있다
asCoroutineDipatcher() 확장함수 활용
*/

fun main(): Unit {
    CoroutineName("나만의 코루틴") + Dispatchers.Default
    val threadPool = Executors.newSingleThreadExecutor()
    /*
    asCoroutineDispatcher() 확장함수를 통해 쓰레드풀을 코루틴 디스패처로 바꾸고 CoroutineScope 에 적용함
    이렇게 하면 여러 코루틴을 내가 만든 쓰레드풀에 돌릴 수 있다
    */
    CoroutineScope(threadPool.asCoroutineDispatcher()).launch {
        printWithThread("새로운 코루틴")
    }
}
