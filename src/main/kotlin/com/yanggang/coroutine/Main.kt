package com.yanggang.coroutine

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

/*
runBlocking: 일반 루틴 세계와 코루틴 세계를 연결한다
예: fun main() 까지 일반 루틴 이고 runBlocking {...} 부터 코루틴을 만듦
-> runBlocking 함수 자체로 새로운 코루틴을 만든다

launch: 반환값이 없는 코루틴을 만든다
-> 아래 코드는 runBlocking 에 의한 전체 코루틴과 launch 에 의해 new 루틴을 부르는 코루틴

suspend fun: 다른 suspend fun 을 호출할 수 있다

yield: 지금 코루틴을 중단하고 다른 코루틴이 실행되도록 한다 (= 스레드를 양보한다)

실행 결과:
START
END
3

실행 순서:
1. main() 를 실행하면 runBlocking 에 코루틴이 하나 생성됨
2. "START" 출력
3. launch {} 호출하면서 새로운 코루틴을 만듦
-> launch 는 만들어진 새로운 코루틴을 바로 실행하지 않음 (newRoutine() 으로 들어가지 않음)
4. yeild 는 지금 코루틴을 중단하고 양보함
5. launch 로 주도권이 넘어가게됨
6. newRoutine() 가 불리고 num1, num2 지역변수 초기화
7. 다시 yeild 를 만나서 양보함
8. "END" 출력
9. 최종 "3" 출력
(yeild() 를 전부 제거해도 결과는 동일)


코루틴은 루틴과 다르게 우리의 직관과 차이가 있다 (일반적인 루틴과 다르게 중단과 재개를 하게됨)
-> main 코루틴에서 yeild(양보) 가 되는 순간 new 코루틴에 진입이 되고
다시 new 코루틴이 yeild(양보) 를 하는 순간 잔깐 일시중단 되고 main 코루틴으로 넘어 왔다가
main 코루틴이 끝나면 다시 new 코루틴으로 들어가 최종적으로 종료


메모리 관점
일반 루틴의 경우 함수가 한번 호출되고 종료되면 끝이기 때문에 그 루틴에서 사용했던 데이터들을 더이상 보관하지 않아도됨
코루틴의 경우 새로운 루틴이 호출된 후 완전히 종료되기 전, 해당 루틴에서 사용했던 정보들을 보관하고 있어야 한다
-> 루틴이 중단되었다가 해당 메모리에 접근이 가능하다

edit configuration 의 VM options 에 -Dkotlinx.coroutines.debug 값을 주면 어떤 코루틴에서 실행됬는지 알 수 있음
예:
[main @coroutine#1] START
[main @coroutine#1] END
[main @coroutine#2] 3

루틴과 코루틴의 차이
루틴:
- 시작되면 끝날때까지 멈추지 않는다
- 한번 끝나면 루틴 내의 정보가 사라짐

코루틴:
- 중단되었다가 재개될 수 있다
- 중단되더라도 루틴 내의 정보가 사라지지 않는다
*/
fun main(): Unit = runBlocking {
    printWithThread("START")
    launch {
        newRoutine()
    }
    yield()
    printWithThread("END")
}

suspend fun newRoutine() {
    val num1 = 1
    val num2 = 2
    yield()
    printWithThread("${num1 + num2}")
}

fun printWithThread(str: Any) {
    println("[${Thread.currentThread().name}] $str")
}
