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
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.ForeignKeyDetailModelLink;
import is.codion.swing.framework.model.SwingEntityModel;

public final class InvoiceModel extends SwingEntityModel {

  public InvoiceModel(EntityConnectionProvider connectionProvider) {
    super(new InvoiceEditModel(connectionProvider));

    InvoiceLineEditModel invoiceLineEditModel = new InvoiceLineEditModel(connectionProvider);

    SwingEntityModel invoiceLineModel = new SwingEntityModel(invoiceLineEditModel);
    ForeignKeyDetailModelLink<?, ?, ?> detailModelLink = addDetailModel(invoiceLineModel);
    detailModelLink.clearForeignKeyOnEmptySelection().set(true);
    detailModelLink.active().set(true);

    invoiceLineEditModel.addTotalsUpdatedListener(tableModel()::replace);
  }
}
