package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.framework.model.SwingEntityEditModel;

import org.junit.jupiter.api.Test;

public final class DefaultEntityComponentValueFactoryTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final SwingEntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);

  @Test
  void test() {
    final EntityComponentValueFactory<Entity, ForeignKey, EntitySearchField> foreignKeyComponentFactory = new DefaultEntityComponentValueFactory<>();
    foreignKeyComponentFactory.createComponentValue(TestDomain.DETAIL_MASTER_FK, editModel, null);
    foreignKeyComponentFactory.createComponentValue(TestDomain.DETAIL_DETAIL_FK, editModel, null);

    final EntityComponentValueFactory<Integer, Attribute<Integer>, IntegerField> integerComponentFactory = new DefaultEntityComponentValueFactory<>();
    integerComponentFactory.createComponentValue(TestDomain.DETAIL_INT, editModel, null);
    integerComponentFactory.createComponentValue(TestDomain.DETAIL_INT_DERIVED, editModel, null);
  }
}