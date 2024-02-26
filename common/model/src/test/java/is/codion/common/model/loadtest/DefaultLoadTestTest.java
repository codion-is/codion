/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.loadtest;

import is.codion.common.model.loadtest.LoadTest.Scenario;
import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultLoadTestTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Scenario<Object> SCENARIO = Scenario.builder(new Performer<Object>() {
    int counter = 0;
    @Override
    public void perform(Object application) throws Exception {
      if (counter++ % 2 == 0) {
        throw new Exception();
      }
    }
  }).build();

  private static final Scenario<Object> SCENARIO_II = Scenario.builder(application -> {}).build();

  @Test
  void unknownScenario() {
    LoadTest<Object> model = LoadTest.builder(user -> new Object(), object -> {})
            .user(User.user("test"))
            .scenarios(asList(SCENARIO, SCENARIO_II))
            .minimumThinkTime(25)
            .maximumThinkTime(50)
            .loginDelayFactor(2)
            .applicationBatchSize(2)
            .build();
    assertThrows(IllegalArgumentException.class, () -> model.scenario("bla"));
  }

  @Test
  void test() throws Exception {
    LoadTest<Object> model = LoadTest.builder(user -> new Object(), object -> {})
            .user(UNIT_TEST_USER)
            .scenarios(asList(SCENARIO, SCENARIO_II))
            .minimumThinkTime(25)
            .maximumThinkTime(50)
            .loginDelayFactor(2)
            .applicationBatchSize(2)
            .build();
    Map<String, List<Scenario.Result>> results = new HashMap<>();
    model.addResultListener(result -> results.computeIfAbsent(result.scenario(), scenarioName -> new ArrayList<>()).add(result));
    assertEquals(2, model.applicationBatchSize().get());

    assertEquals(2, model.loginDelayFactor().get());
    model.loginDelayFactor().set(3);
    assertEquals(3, model.loginDelayFactor().get());
    assertEquals(2, model.applicationBatchSize().get());

    assertEquals(25, model.minimumThinkTime().get());
    assertEquals(50, model.maximumThinkTime().get());
    model.maximumThinkTime().set(40);
    model.minimumThinkTime().set(20);
    assertEquals(20, model.minimumThinkTime().get());
    assertEquals(40, model.maximumThinkTime().get());

    model.applicationBatchSize().set(5);
    assertTrue(model.scenarios().contains(SCENARIO));
    model.user().set(UNIT_TEST_USER);
    assertEquals(UNIT_TEST_USER, model.user().get());
    assertNotNull(model.scenarioChooser());
    model.setWeight(SCENARIO.name(), 2);
    model.setScenarioEnabled(SCENARIO_II.name(), false);
    model.addApplicationBatch();
    Thread.sleep(500);
    model.paused().set(true);
    Thread.sleep(200);
    model.paused().set(false);
    assertEquals(5, model.applicationCount().get());
    assertNull(results.get(SCENARIO_II.name()));
    assertFalse(results.get(SCENARIO.name()).isEmpty());

    model.removeApplicationBatch();
    assertEquals(0, model.applicationCount().get());

    results.values().forEach(result -> assertFalse(result.isEmpty()));

    AtomicInteger exitCounter = new AtomicInteger();
    model.addShutdownListener(exitCounter::incrementAndGet);
    model.shutdown();
    assertEquals(1, exitCounter.get());
  }

  @Test
  void setLoginDelayFactorNegative() {
    LoadTest<Object> model = LoadTest.builder(user -> new Object(), object -> {})
            .user(User.user("test"))
            .build();
    assertThrows(IllegalArgumentException.class, () -> model.loginDelayFactor().set(-1));
  }
}
