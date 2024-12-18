package com.yanggang.coroutine.usage

/*
코루틴 활용과 마무리

코루틴 특징 정리
- 비동기 프로그래밍을 할때 계속 코드가 indentation 되는 CallBack Hell 을 해결
- Kotlin 언어의 키워드가 아닌 라이브러리의 기능으로 Coroutine 이 존재

코루틴은 이런 특징으로 비동기 Non-Blocking 이나 동시성이 필요한 곳에서 활용될 수 있다
예:
- client 사이드(안드로이드)에서 여러 API 를 활용해 화면을 렌더링 할때도 사용 (Asynchronous UI)
- server 사이드에서도 여러 API 를 동시에 호출할때 활용
  - Blocking 클라이언트인 RestTemplate 를 사용한다면 코루틴을 여러개 만들어 멀티쓰레드에 각각 배정하는 방식으로 사용
  - Non-Blocking 클라이언트인 WebClient 를 사용한다면 하나의 스레드에서 여러 API 를 동시에 호출해 전체 성능을 끌어올릴 수 있음
  - WebFlux 같은 비동기 Non-Blocking 프레임워크 에서도 활용
  - 동시성 테스트 에서도 활용
*/
