/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Detail;

import org.junit.jupiter.api.Test;

public final class DefaultEntityComponentFactoryTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  private final SwingEntityEditModel editModel = new SwingEntityEditModel(Detail.TYPE, CONNECTION_PROVIDER);

  @Test
  void test() {
    EntityComponentFactory<Entity, ForeignKey, EntitySearchField> foreignKeyComponentFactory = new DefaultEntityComponentFactory<>();
    foreignKeyComponentFactory.componentValue(Detail.MASTER_FK, editModel, null);
    foreignKeyComponentFactory.componentValue(Detail.DETAIL_FK, editModel, null);

    EntityComponentFactory<Integer, Attribute<Integer>, NumberField<Integer>> integerComponentFactory = new DefaultEntityComponentFactory<>();
    integerComponentFactory.componentValue(Detail.INT, editModel, null);
    integerComponentFactory.componentValue(Detail.INT_DERIVED, editModel, null);
  }
}