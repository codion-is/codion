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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.javadoc;

import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.EntityEditor;
import is.codion.framework.model.EntityEditor.EditorTask;

/**
 * The {@link EntityEditor} javadoc snippets, each the region of the same name.
 */
final class EntityEditorSnippets {

	void task(EntityEditor<?> editor) throws EntityValidationException {
		// Must be called on the UI thread, fires "before" events and captures entity state // @start region=task
		EditorTask<Entity> task = editor.tasks().insert();

		// Can safely be called in a background thread
		EditorTask.Result<Entity> result = task.perform();

		// Must be called on the UI thread, fires "after" events
		Entity insertedEntity = result.handle(); // @end
	}

	void propagate(EntityEditor<?> editor) {
		// Populate billing address fields when customer changes // @start region=propagate
		editor.value(Invoice.CUSTOMER_FK).propagate(Invoice.BILLINGADDRESS,
						customer -> customer == null ? null : customer.get(Customer.ADDRESS));
		editor.value(Invoice.CUSTOMER_FK).propagate(Invoice.BILLINGCITY,
						customer -> customer == null ? null : customer.get(Customer.CITY)); // @end
	}
}
