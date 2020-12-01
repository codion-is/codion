package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.domain.entity.Entities;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.util.Collection;
import java.util.HashMap;

public final class ViewCustomerReport extends EntityLoadTestModel.AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityTableModel customerModel = application.getEntityModel(Chinook.Customer.TYPE).getTableModel();
      customerModel.refresh();
      EntityLoadTestModel.selectRandomRow(customerModel);

      final Collection<Long> customerIDs =
              Entities.getDistinctValues(Chinook.Customer.ID, customerModel.getSelectionModel().getSelectedItems());
      final HashMap<String, Object> reportParameters = new HashMap<>();
      reportParameters.put("CUSTOMER_IDS", customerIDs);
      customerModel.getConnectionProvider().getConnection().fillReport(Chinook.Customer.REPORT, reportParameters);
    }
    catch (final Exception e) {
      throw new ScenarioException(e);
    }
  }

  @Override
  public int getDefaultWeight() {
    return 2;
  }
}
