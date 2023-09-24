/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Collection;
import java.util.Random;

import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel.selectRandomRows;

public final class UpdateTotals extends AbstractEntityUsageScenario<ChinookAppModel> {

  private final Random random = new Random();

  @Override
  protected void perform(ChinookAppModel application) throws Exception {
    SwingEntityModel customerModel = application.entityModel(Customer.TYPE);
    customerModel.tableModel().refresh();
    selectRandomRows(customerModel.tableModel(), random.nextInt(6) + 2);
    SwingEntityModel invoiceModel = customerModel.detailModel(Invoice.TYPE);
    selectRandomRows(invoiceModel.tableModel(), random.nextInt(6) + 2);
    SwingEntityTableModel invoiceLineTableModel =
            invoiceModel.detailModel(InvoiceLine.TYPE).tableModel();
    Collection<Entity> invoiceLines = invoiceLineTableModel.items();
    Entity.put(InvoiceLine.QUANTITY, random.nextInt(4) + 1, invoiceLines);

    invoiceLineTableModel.editModel().update(invoiceLines);
  }
}
