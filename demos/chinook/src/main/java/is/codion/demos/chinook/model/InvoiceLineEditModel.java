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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.common.reactive.observer.Observer;
import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Objects;

public final class InvoiceLineEditModel extends SwingEntityEditModel {

	public InvoiceLineEditModel(EntityConnection connection) {
		super(InvoiceLine.TYPE, connection);
		// tag::editedObserver[]
		// We populate the unit price when the track is edited
		Observer<Entity> trackEdited = editor().value(InvoiceLine.TRACK_FK).edited();
		trackEdited.when(Objects::nonNull)
						.addConsumer(this::setUnitPrice);
		trackEdited.when(Objects::isNull)
						.addListener(this::clearUnitPrice);
		// end::editedObserver[]
	}

	private void setUnitPrice(Entity track) {
		editor().value(InvoiceLine.UNITPRICE).set(track.get(Track.UNITPRICE));
	}

	private void clearUnitPrice() {
		editor().value(InvoiceLine.UNITPRICE).clear();
	}
}
