package travel.rewardo.rewardapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import travel.rewardo.rewardapi.config.TestConfig;

@SpringBootTest(classes = {RewardoApiApplication.class, TestConfig.class})
@ActiveProfiles("test")
class RewardApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
