package com.ssafy.a305.be;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"ai.server.url=http://localhost:8000",
		"ai.internal.api.key=test-key"
})
class BeApplicationTests {

	@Test
	void contextLoads() {
	}

}
