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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.manual;

import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.demos.chinook.ui.CustomerEditPanel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.EntitySearchField;
import is.codion.swing.framework.ui.component.EntitySearchField.SearchIndicator;

import java.util.List;

final class FrameworkUIDemo {

	void basicSearchField(EntityConnectionProvider connectionProvider) {
		// tag::basicSearchField[]
		EntitySearchModel searchModel = EntitySearchModel.builder(Customer.TYPE, connectionProvider)
						.searchColumns(List.of(Customer.FIRSTNAME, Customer.EMAIL))
						.build();

		EntitySearchField searchField = EntitySearchField.builder(searchModel)
						.multiSelection()
						.columns(20)
						.build();
		// end::basicSearchField[]
	}

	void singleSelectionSearchField(EntityConnectionProvider connectionProvider) {
		// tag::singleSelectionSearchField[]
		EntitySearchModel searchModel = EntitySearchModel.builder(Customer.TYPE, connectionProvider)
						.singleSelection(true)  // Must match field configuration
						.build();

		EntitySearchField searchField = EntitySearchField.builder(searchModel)
						.singleSelection()  // Must match model configuration
						.build();
		// end::singleSelectionSearchField[]
	}

	void multiSelectionSearchField(EntityConnectionProvider connectionProvider) {
		// tag::multiSelectionSearchField[]
		EntitySearchModel searchModel = EntitySearchModel.builder(Track.TYPE, connectionProvider)
						.singleSelection(false)  // Must match field configuration
						.build();

		EntitySearchField searchField = EntitySearchField.builder(searchModel)
						.multiSelection()  // Must match model configuration
						.build();
		// end::multiSelectionSearchField[]
	}

	void customTableSelector(EntityConnectionProvider connectionProvider) {
		// tag::customTableSelector[]
		EntitySearchModel searchModel = EntitySearchModel.builder(Customer.TYPE, connectionProvider).build();

		EntitySearchField searchField = EntitySearchField.builder(searchModel)
						.multiSelection()
						.selectorFactory(EntitySearchField::tableSelector)
						.build();
		// end::customTableSelector[]
	}

	void searchFieldWithAddEdit(EntityConnectionProvider connectionProvider) {
		// tag::searchFieldWithAddEdit[]
		EntitySearchModel searchModel = EntitySearchModel.builder(Customer.TYPE, connectionProvider).build();
		SwingEntityEditModel editModel = new SwingEntityEditModel(Customer.TYPE, connectionProvider);

		EntitySearchField searchField = EntitySearchField.builder(searchModel)
						.singleSelection()
						.editPanel(() -> new CustomerEditPanel(editModel))
						.confirmAdd(true)     // Confirm before adding
						.confirmEdit(true)    // Confirm before editing
						.build();

		// Access controls
		searchField.addControl();   // INSERT key by default
		searchField.editControl();  // CTRL+INSERT by default
		// end::searchFieldWithAddEdit[]
	}

	void searchFieldConfiguration(EntityConnectionProvider connectionProvider) {
		// tag::searchFieldConfiguration[]
		EntitySearchModel searchModel = EntitySearchModel.builder(Customer.TYPE, connectionProvider).build();

		EntitySearchField searchField = EntitySearchField.builder(searchModel)
						.singleSelection()
						.columns(20)                      // Field width
						.upperCase(true)                  // Force uppercase
						.searchHintEnabled(true)          // Show "Search..." hint
						.searchOnFocusLost(true)          // Auto-search when focus lost
						.selectionToolTip(true)           // Show selection as tooltip
						.editable(false)                  // Make read-only
						.stringFactory(entity ->    // Custom display text
										entity.get(Customer.LASTNAME) + " - " + entity.get(Customer.CITY))
						.separator(" | ")                 // Multi-selection separator
						.build();
		// end::searchFieldConfiguration[]
	}

	void searchFieldProgrammaticControl(EntityConnectionProvider connectionProvider) {
		// tag::searchFieldProgrammaticControl[]
		EntitySearchModel searchModel = EntitySearchModel.builder(Customer.TYPE, connectionProvider).build();
		EntitySearchField searchField = EntitySearchField.builder(searchModel)
						.multiSelection()
						.build();

		// Get search control
		Control searchControl = searchField.searchControl();

		// Use in toolbar or menu
		Controls.builder()
						.control(searchControl)
						.build();
		// end::searchFieldProgrammaticControl[]
	}

	void searchFieldReactiveBinding(EntityConnectionProvider connectionProvider) {
		// tag::searchFieldReactiveBinding[]
		EntitySearchModel searchModel = EntitySearchModel.builder(Customer.TYPE, connectionProvider).build();
		SwingEntityEditModel editModel = new SwingEntityEditModel(Invoice.TYPE, connectionProvider);

		ComponentValue<Entity, EntitySearchField> searchFieldValue =
						EntitySearchField.builder(searchModel)
										.singleSelection()
										.buildValue();

		EntitySearchField searchField = searchFieldValue.component();
		// React to selection changes
		searchField.model().selection().entities().addConsumer(selectedEntities ->
						System.out.println("Selected: " + selectedEntities));

		// Link to edit model
		editModel.editor().value(Invoice.CUSTOMER_FK).link(searchFieldValue);
		// end::searchFieldReactiveBinding[]
	}

	void searchFieldProgressBar(EntityConnectionProvider connectionProvider) {
		// tag::searchFieldProgressBar[]
		EntitySearchModel searchModel = EntitySearchModel.builder(Customer.TYPE, connectionProvider).build();

		EntitySearchField searchField = EntitySearchField.builder(searchModel)
						.multiSelection()
						.searchIndicator(SearchIndicator.PROGRESS_BAR)
						.build();
		// end::searchFieldProgressBar[]
	}

	void searchFieldMismatchError(EntityConnectionProvider connectionProvider) {
		// tag::searchFieldMismatchError[]
		// Wrong - will throw IllegalArgumentException
		EntitySearchModel model = EntitySearchModel.builder(Customer.TYPE, connectionProvider)
						.singleSelection(true)
						.build();

		EntitySearchField field = EntitySearchField.builder(model)
						.multiSelection()  // Mismatch!
						.build();
		// end::searchFieldMismatchError[]
	}

	void searchFieldProperConfiguration(EntityConnectionProvider connectionProvider) {
		// tag::searchFieldProperConfiguration[]
		EntitySearchModel model = EntitySearchModel.builder(Customer.TYPE, connectionProvider)
						.searchColumns(List.of(Customer.LASTNAME, Customer.EMAIL, Customer.PHONE))
						.build();
		// end::searchFieldProperConfiguration[]
	}
}