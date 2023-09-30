/*
 * Copyright (c) 2018 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.proxy.ProxyBuilder;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.framework.db.AbstractEntityConnectionProvider.AbstractBuilder;
import is.codion.framework.domain.entity.Entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void connectClose() {
    TestProviderBuilder builder = new TestProviderBuilder()
            .user(UNIT_TEST_USER)
            .domainType(TestDomain.DOMAIN);
    EntityConnectionProvider provider = builder.build();
    assertEquals("description", provider.description());
    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_LOCAL, provider.connectionType());
    assertEquals(provider.entities(), ENTITIES);
    assertEquals(UNIT_TEST_USER, provider.user());

    EntityConnection connection1 = provider.connection();
    assertTrue(provider.connectionValid());
    provider.close();
    assertFalse(provider.connectionValid());

    EntityConnection connection2 = provider.connection();
    assertTrue(provider.connectionValid());
    assertNotEquals(connection1, connection2);

    connection2.close();
    assertFalse(provider.connectionValid());
    EntityConnection connection3 = provider.connection();
    assertNotEquals(connection2, connection3);

    EntityConnection connection4 = provider.connection();
    assertTrue(provider.connectionValid());
    assertNotEquals(connection3, connection4);
  }

  private static final class TestProvider extends AbstractEntityConnectionProvider {

    public TestProvider(AbstractBuilder<?, ?> builder) {
      super(builder);
    }

    @Override
    protected EntityConnection connect() {
      State connected = State.state(true);

      return ProxyBuilder.builder(EntityConnection.class)
              .method("equals", Object.class, parameters -> TestProvider.this == parameters.arguments().get(0))
              .method("entities", parameters -> ENTITIES)
              .method("connected", parameters -> connected.get())
              .method("close", parameters -> {
                connected.set(false);
                return null;
              })
              .build();
    }

    @Override
    protected void close(EntityConnection connection) {
      connection.close();
    }

    @Override
    public String connectionType() {
      return EntityConnectionProvider.CONNECTION_TYPE_LOCAL;
    }

    @Override
    public String description() {
      return "description";
    }
  }

  private static final class TestProviderBuilder extends AbstractBuilder<TestProvider, TestProviderBuilder> {

    private TestProviderBuilder() {
      super(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
    }

    @Override
    public TestProvider build() {
      return new TestProvider(this);
    }
  }
}
