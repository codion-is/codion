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

import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.EntityPersistence;

// tag::persistenceFactory[]
public final class ChinookPersistenceFactory implements EntityPersistence.Factory {

	@Override
	public EntityPersistence persistence(EntityType entityType) {
		// Every editor of invoice lines updates the invoice totals,
		// including the ones the framework creates on its own
		if (entityType.equals(InvoiceLine.TYPE)) {
			return new InvoiceLinePersistence();
		}

		return DEFAULT;
	}

	@Override
	public boolean supports(DomainType domainType) {
		return domainType.equals(Chinook.DOMAIN);
	}
}
// end::persistenceFactory[]
