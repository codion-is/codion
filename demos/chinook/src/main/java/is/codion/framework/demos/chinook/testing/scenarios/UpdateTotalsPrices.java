package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.util.List;
import java.util.Random;

public final class UpdateTotalsPrices extends EntityLoadTestModel.AbstractEntityUsageScenario<ChinookApplicationModel> {

  private final Random random = new Random();

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityModel customerModel = application.getEntityModel(Chinook.Customer.TYPE);
      customerModel.getTableModel().refresh();
      EntityLoadTestModel.selectRandomRows(customerModel.getTableModel(), random.nextInt(6) + 2);
      final SwingEntityModel invoiceModel = customerModel.getDetailModel(Chinook.Invoice.TYPE);
      EntityLoadTestModel.selectRandomRows(invoiceModel.getTableModel(), random.nextInt(6) + 2);
      final SwingEntityTableModel invoiceLineTableModel =
              invoiceModel.getDetailModel(Chinook.InvoiceLine.TYPE).getTableModel();
      final List<Entity> invoiceLines = invoiceLineTableModel.getItems();
      Entities.put(Chinook.InvoiceLine.QUANTITY, random.nextInt(4) + 1, invoiceLines);

      invoiceLineTableModel.update(invoiceLines);

      application.updateInvoiceTotals();
    }
    catch (final Exception e) {
      throw new ScenarioException(e);
    }
  }
}
