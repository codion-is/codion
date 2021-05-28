package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.framework.model.SwingEntityEditModel;

import org.junit.jupiter.api.Test;

public class EntityComponentValuesTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final SwingEntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);

  @Test
  public void test() {
    final EntityComponentValues componentValues = new EntityComponentValues();
    final EntityDefinition definition = CONNECTION_PROVIDER.getEntities().getDefinition(TestDomain.T_DETAIL);
    definition.getColumnProperties()
            .forEach(property -> componentValues.createComponentValue(property.getAttribute(), editModel, null));

    componentValues.createForeignKeyComponentValue(TestDomain.DETAIL_MASTER_FK, editModel, null);
    componentValues.createForeignKeyComponentValue(TestDomain.DETAIL_DETAIL_FK, editModel, null);
  }
}
