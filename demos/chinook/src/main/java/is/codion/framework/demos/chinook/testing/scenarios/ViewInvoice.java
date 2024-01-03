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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;

import java.util.List;

import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.RANDOM;
import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.randomCustomerId;

public final class ViewInvoice extends AbstractUsageScenario<EntityConnectionProvider> {

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();
    Entity customer = connection.selectSingle(Customer.ID.equalTo(randomCustomerId()));
    List<Long> invoiceIds = connection.select(Invoice.ID, Invoice.CUSTOMER_FK.equalTo(customer));
    if (!invoiceIds.isEmpty()) {
      Entity invoice = connection.selectSingle(Invoice.ID.equalTo(invoiceIds.get(RANDOM.nextInt(invoiceIds.size()))));
      connection.select(InvoiceLine.INVOICE_FK.equalTo(invoice));
    }
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
