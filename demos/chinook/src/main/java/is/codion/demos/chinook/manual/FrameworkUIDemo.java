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

import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.demos.chinook.ui.CustomerEditPanel;
import is.codion.demos.chinook.ui.MediaTypeEditPanel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.component.EntitySearchField;
import is.codion.swing.framework.ui.component.EntitySearchField.SearchIndicator;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_SPACE;

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

	// EntityEditPanel examples
	void editPanelConfiguration(EntityConnectionProvider connectionProvider) {
		// tag::editPanelConfiguration[]
		class InvoiceEditPanel extends EntityEditPanel {
			public InvoiceEditPanel(SwingEntityEditModel editModel) {
				super(editModel, config ->
								// Keep displaying newly inserted invoice since we'll continue
								// working with it by adding invoice lines
								config.clearAfterInsert(false));
			}

			@Override
			protected void initializeUI() {
				// UI setup
			}
		}
		// end::editPanelConfiguration[]
	}

	void editPanelFocusManagement(EntityConnectionProvider connectionProvider) {
		// tag::editPanelFocusManagement[]
		class CustomerEditPanel extends EntityEditPanel {
			public CustomerEditPanel(SwingEntityEditModel editModel) {
				super(editModel);
			}

			@Override
			protected void initializeUI() {
				focus().initial().set(Customer.FIRSTNAME);
				focus().afterInsert().set(Customer.ADDRESS);

				// Create your input components...
			}
		}
		// end::editPanelFocusManagement[]
	}

	void editPanelComboBoxPanel(EntityConnectionProvider connectionProvider) {
		// tag::editPanelComboBoxPanel[]
		class TrackEditPanel extends EntityEditPanel {
			public TrackEditPanel(SwingEntityEditModel editModel) {
				super(editModel);
			}

			@Override
			protected void initializeUI() {
				createComboBoxPanel(Track.MEDIATYPE_FK, this::createMediaTypeEditPanel)
								.preferredWidth(160)
								.includeAddButton(true)
								.includeEditButton(true);

				createSearchFieldPanel(Track.MEDIATYPE_FK, this::createMediaTypeEditPanel)
								.preferredWidth(160)
								.includeAddButton(true)
								.includeEditButton(true);
			}

			private EntityEditPanel createMediaTypeEditPanel() {
				return new MediaTypeEditPanel(new SwingEntityEditModel(Chinook.MediaType.TYPE, editModel().connectionProvider()));
			}
		}
		// end::editPanelComboBoxPanel[]
	}

	void editPanelCustomComponent(EntityConnectionProvider connectionProvider) {
		// tag::editPanelCustomComponent[]
		class TrackEditPanel extends EntityEditPanel {
			public TrackEditPanel(SwingEntityEditModel editModel) {
				super(editModel);
			}

			@Override
			protected void initializeUI() {
				// Create a custom component
				DurationComponentValue durationValue = createDurationComponent();

				// Link it to the attribute value
				editModel().editor().value(Track.MILLISECONDS).link(durationValue);
				// And set the component it as the attribute component
				component(Track.MILLISECONDS).set(durationValue.component());
			}

			private DurationComponentValue createDurationComponent() {
				// Custom implementation
				return new DurationComponentValue();
			}
		}
		// end::editPanelCustomComponent[]
	}

	void editPanelKeyboardShortcuts(EntityConnectionProvider connectionProvider) {
		// tag::editPanelKeyboardShortcuts[]
		class CustomerEditPanel extends EntityEditPanel {
			public CustomerEditPanel(SwingEntityEditModel editModel) {
				super(editModel);
			}

			@Override
			protected void initializeUI() {
				createTextField(Customer.STATE)
								.keyEvent(KeyEvents.builder(VK_SPACE)
												.modifiers(CTRL_DOWN_MASK)
												.action(Control.action(this::selectStateFromExistingValues)));
			}

			private void selectStateFromExistingValues(ActionEvent event) {
				JTextField stateField = (JTextField) event.getSource();

				Dialogs.listSelectionDialog(editModel().connection().select(Customer.STATE))
								.owner(stateField)
								.selectSingle()
								.ifPresent(stateField::setText);
			}
		}
		// end::editPanelKeyboardShortcuts[]
	}

	void editPanelDetailIntegration(EntityConnectionProvider connectionProvider) {
		// tag::editPanelDetailIntegration[]
		class InvoiceEditPanel extends EntityEditPanel {
			private final EntityPanel invoiceLinePanel;

			public InvoiceEditPanel(SwingEntityEditModel editModel, SwingEntityModel invoiceLineModel) {
				super(editModel, config -> config.clearAfterInsert(false));
				this.invoiceLinePanel = createInvoiceLinePanel(invoiceLineModel);
			}

			@Override
			protected void initializeUI() {
				// Initialize main edit controls...

				// Add detail panel
				add(invoiceLinePanel, BorderLayout.SOUTH);
			}

			private EntityPanel createInvoiceLinePanel(SwingEntityModel invoiceLineModel) {
				// Create and return invoice line panel
				return new EntityPanel(invoiceLineModel);
			}
		}
		// end::editPanelDetailIntegration[]
	}

	static class DurationComponentValue extends AbstractComponentValue<Integer, JPanel> {

		DurationComponentValue() {
			super(new JPanel());
		}

		@Override
		protected Integer getComponentValue() {
			return 0;
		}

		@Override
		protected void setComponentValue(Integer value) {}
	}

	private class CustomerSelectorFactory implements java.util.function.Function<EntitySearchField, EntitySearchField.Selector> {
		@Override
		public EntitySearchField.Selector apply(EntitySearchField entitySearchField) {
			return null;
		}
	}
}