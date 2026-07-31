package com.jeonghuny.ext_blocker;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("""
        컨텍스트 로드에 실제 DB 연결이 필요하나 CI/빌드 환경에는 DB가 없음.
        Testcontainers 도입 시 활성화 예정. (관련: Dockerfile의 -DskipTests)
        """)
class ExtBlockerApplicationTests {
	@Test
	void contextLoads() { }
}