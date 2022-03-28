package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.common.ui.component.textfield.IntegerField;
import is.codion.swing.framework.model.SwingEntityEditModel;

import org.junit.jupiter.api.Test;

public final class DefaultEntityComponentFactoryTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  private final SwingEntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);

  @Test
  void test() {
    EntityComponentFactory<Entity, ForeignKey, EntitySearchField> foreignKeyComponentFactory = new DefaultEntityComponentFactory<>();
    foreignKeyComponentFactory.createComponentValue(TestDomain.DETAIL_MASTER_FK, editModel, null);
    foreignKeyComponentFactory.createComponentValue(TestDomain.DETAIL_DETAIL_FK, editModel, null);

    EntityComponentFactory<Integer, Attribute<Integer>, IntegerField> integerComponentFactory = new DefaultEntityComponentFactory<>();
    integerComponentFactory.createComponentValue(TestDomain.DETAIL_INT, editModel, null);
    integerComponentFactory.createComponentValue(TestDomain.DETAIL_INT_DERIVED, editModel, null);
  }
}