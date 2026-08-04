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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.manual;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.EntityConditionModel;
import is.codion.framework.model.EntityConditionModel.AdditionalConditions;
import is.codion.framework.model.EntityEditor.EditorValue;
import is.codion.framework.model.EntityEditor.PersistEvents;
import is.codion.framework.model.EntityQueryModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.ForeignKeyModelLink;
import is.codion.framework.model.ModelLink;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityEditor;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static is.codion.framework.domain.entity.condition.Condition.and;

public final class FrameworkModelDemo {

	void entityModel(EntityConnection connection) {
		// tag::entityModel[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityModel invoiceModel = new SwingEntityModel(Invoice.TYPE, connection);

		// Establish master-detail relationship
		customerModel.detail().add(invoiceModel);
		// end::entityModel[]
	}

	void entityEditModel(EntityConnection connection) throws EntityValidationException {
		// tag::entityEditModel[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityEditor editor = customerModel.editor();

		// Access entity values
		Value<String> nameValue = editor.value(Customer.FIRSTNAME);

		// Perform operations
		editor.insert();
		editor.update();
		editor.delete();
		// end::entityEditModel[]
	}

	void entityTableModel(EntityConnection connection) {
		// tag::entityTableModel[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityTableModel tableModel = customerModel.tableModel();

		// Refresh data
		tableModel.items().refresh();

		// Access selection
		Collection<Entity> selected = tableModel.selection().items().get();
		// end::entityTableModel[]
	}

	void observableState(EntityConnection connection) {
		// tag::observableState[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityEditModel editModel = customerModel.editModel();
		SwingEntityTableModel tableModel = customerModel.tableModel();

		// Edit model states
		State updateEnabled = editModel.editor().settings().updateEnabled();
		State updateMultipleEnabled = editModel.editor().settings().updateMultipleEnabled();
		ObservableState modified = editModel.editor().entity().modified();

		// Table model states
		ObservableState refreshing = tableModel.items().refresher().active();
		ObservableState hasSelection = tableModel.selection().empty().not();

		// Combine states
		ObservableState canDelete = State.and(hasSelection, refreshing.not());
		// end::observableState[]
	}

	void eventSystem(EntityConnection connection) {
		// tag::eventSystem[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityEditModel editModel = customerModel.editModel();
		SwingEntityTableModel tableModel = customerModel.tableModel();

		// Listen for entity changes
		editModel.editor().events().after().insert().addConsumer(entities -> {
			System.out.println("Inserted: " + entities);
		});

		// Listen for selection changes
		tableModel.selection().item()
						.when(Objects::nonNull)
						.addConsumer(this::loadDetails);
		// end::eventSystem[]
	}

	void valueObservers(EntityConnection connection) {
		// tag::valueObservers[]
		SwingEntityModel trackModel = new SwingEntityModel(Track.TYPE, connection);
		SwingEntityEditModel editModel = trackModel.editModel();

		// Bind edit model value to UI state
		EditorValue<BigDecimal> priceValue = editModel.editor().value(Track.UNITPRICE);
		ObservableState priceValid = editModel.editor().value(Track.UNITPRICE).valid();

		// React to value changes
		priceValue.addConsumer(this::updateTotalPrice);

		// React to value edits
		priceValue.edited().addConsumer(newPrice -> System.out.println("Price: " + newPrice));
		priceValid.when(false)
						.addListener(() -> System.out.println("Invalid price: " + priceValue.get()));
		// end::valueObservers[]
	}

	void masterDetail(EntityConnection connection) {
		// tag::masterDetail[]
		// Three-level hierarchy
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityModel invoiceModel = new SwingEntityModel(Invoice.TYPE, connection);
		SwingEntityModel invoiceLineModel = new SwingEntityModel(InvoiceLine.TYPE, connection);

		customerModel.detail().add(invoiceModel);
		invoiceModel.detail().add(invoiceLineModel);

		// Selection cascades down the hierarchy
		Entity customer = getCustomer(connection);
		customerModel.tableModel().selection().item().set(customer);
		// Invoices for selected customer are loaded
		Entity invoice = invoiceModel.tableModel().items().included().get(0);
		invoiceModel.tableModel().selection().item().set(invoice);
		// Invoice lines for selected invoice are loaded
		// end::masterDetail[]
	}

	void queryOptimization(EntityConnection connection) {
		// tag::queryOptimization[]
		class CustomerTableModel extends SwingEntityTableModel {

			public CustomerTableModel(EntityConnection connection) {
				super(new SwingEntityEditModel(Customer.TYPE, connection));
				// Prevent loading entire customer base
				query().limit().set(100);
				query().conditionRequired().set(true);
			}
		}
		// end::queryOptimization[]
	}

	void eventHandling(EntityConnection connection) {
		// tag::eventHandling[]
		SwingEntityModel invoiceLineModel = new SwingEntityModel(InvoiceLine.TYPE, connection);

		// Update summary when details change
		PersistEvents events = invoiceLineModel.editor().events();
		events.after().insert().addConsumer(entities -> updateInvoiceTotal());
		events.after().update().addConsumer(entities -> updateInvoiceTotal());
		events.after().delete().addConsumer(entities -> updateInvoiceTotal());
		// end::eventHandling[]
	}

	void customDataSource(EntityConnection connection) {
		// tag::customDataSource[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityTableModel tableModel = customerModel.tableModel();

		// Fetch only Customers with emails by default
		tableModel.query().dataSource().set(query -> {
			return query.connection().select(and(
							Customer.EMAIL.isNotNull(),
							query.condition().where())
			);
		});
		// end::customDataSource[]
	}

	void conditionConfiguration(EntityConnection connection) {
		// tag::conditionConfiguration[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityTableModel tableModel = customerModel.tableModel();

		// Alternative approach using ConditionModel
		ConditionModel<String> condition = tableModel.query().condition().get(Customer.EMAIL);
		condition.set().isNotNull();
		condition.locked().set(true); // disables the UI condition panel
		// end::conditionConfiguration[]
	}

	// EntityQueryModel examples
	void entityQueryModel(EntityConnection connection) {
		// tag::entityQueryModel[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityTableModel tableModel = customerModel.tableModel();
		EntityQueryModel query = tableModel.query();

		// Configure query behavior
		query.limit().set(200);
		query.conditionRequired().set(true);
		query.orderBy().set(OrderBy.ascending(Customer.LASTNAME));
		// end::entityQueryModel[]
	}

	void tableConditionModel(EntityConnection connection) {
		// tag::tableConditionModel[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		EntityConditionModel condition = customerModel.tableModel().query().condition();

		// Set condition values
		condition.get(Customer.EMAIL).set().isNotNull();
		condition.get(Customer.COUNTRY).set().equalTo("Iceland");

		// The resulting query will include:
		// WHERE email is not null AND country = 'Iceland'
		// end::tableConditionModel[]
	}

	void additionalWhereConditions(EntityConnection connection) {
		// tag::additionalWhereConditions[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		AdditionalConditions additional = customerModel.tableModel().query().condition().additional();

		// Single additional condition
		additional.where().set(() -> Customer.COUNTRY.equalTo("Iceland"));

		// Multiple conditions with custom conjunction
		additional.where().set(() -> Condition.or(
						Customer.CITY.equalTo("Reykjavik"),
						Customer.CITY.equalTo("Akureyri")
		));
		// end::additionalWhereConditions[]
	}

	void queryLimits(EntityConnection connection) {
		// tag::queryLimits[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		EntityQueryModel query = customerModel.tableModel().query();

		// Set a specific limit
		query.limit().set(500);

		// Resets to the default limit specified by the
		// EntityQueryModel.LIMIT configuration setting,
		// if one is specified, otherwise clears the
		// limit and allows fetching of all matching rows
		query.limit().clear();

		// Add a max limit validator
		query.limit().addValidator(newLimit -> {
			if (newLimit > 10.000) {
				throw new IllegalArgumentException("Limit may not exceed 10.000");
			}
		});

		// Listen for limit changes
		query.limit().addConsumer(newLimit ->
						System.out.println("Query limit changed to: " + newLimit));
		// end::queryLimits[]
	}

	void resultOrdering(EntityConnection connection) {
		// tag::resultOrdering[]
		SwingEntityModel invoiceModel = new SwingEntityModel(Invoice.TYPE, connection);
		EntityQueryModel query = invoiceModel.tableModel().query();

		// Single column ordering
		query.orderBy().set(OrderBy.descending(Invoice.DATE));

		// Multiple columns
		query.orderBy().set(OrderBy.builder()
						.ascending(Invoice.BILLINGCOUNTRY)
						.descending(Invoice.DATE)
						.build()
		);
		// end::resultOrdering[]
	}

	void customQueryDataSource(EntityConnection connection) {
		// tag::customQueryDataSource[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);

		customerModel.tableModel().query().dataSource().set(query -> {
			// Custom query with complex joins or database-specific features
			return query.connection().select(Select.where(customComplexCondition())
							.attributes(Customer.ADDRESS, Customer.CITY, Customer.COUNTRY));
		});
		// end::customQueryDataSource[]
	}

	void conditionRequired(EntityConnection connection) {
		// tag::conditionRequired[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		EntityQueryModel query = customerModel.tableModel().query();

		// Require at least one condition
		query.conditionRequired().set(true);

		// Specify that a certain condition must be enabled
		query.conditionEnabled().set(query.condition().get(Customer.SUPPORTREP_FK).enabled());
		// end::conditionRequired[]
	}

	void attributeManagement(EntityConnection connection) {
		// tag::attributeManagement[]
		SwingEntityModel albumModel = new SwingEntityModel(Album.TYPE, connection);
		EntityQueryModel query = albumModel.tableModel().query();

		// Exclude large columns by default
		query.attributes().exclude().add(Album.COVER);

		// Include them only when needed
		State detailView = State.state();
		detailView.addConsumer(showDetails -> {
			if (showDetails) {
				query.attributes().exclude().remove(Album.COVER);
			}
			else {
				query.attributes().exclude().add(Album.COVER);
			}
		});
		// end::attributeManagement[]
	}

	// EntitySearchModel examples
	void basicSearchModel(EntityConnection connection) {
		// tag::basicSearchModel[]
		EntitySearchModel searchModel = EntitySearchModel.builder()
						.entityType(Customer.TYPE)
						.connection(connection)
						.search(Customer.FIRSTNAME, Customer.LASTNAME, Customer.EMAIL)
						.limit(50)
						.build();

		// Perform search
		searchModel.condition().set(() -> Customer.FIRSTNAME.equalTo("john"));

		// Get search result
		List<Entity> result = searchModel.search().perform();
		// end::basicSearchModel[]
	}

	void searchSettings(EntityConnection connection) {
		// tag::searchSettings[]
		EntitySearchModel searchModel = EntitySearchModel.builder()
						.entityType(Customer.TYPE)
						.connection(connection)
						.search(Customer.FIRSTNAME, Customer.LASTNAME)
						.build();

		// Get settings for a specific column
		EntitySearchModel.Settings settings = searchModel.settings().get(Customer.LASTNAME);

		// Add wildcards automatically
		settings.wildcardPrefix().set(true);   // Adds % before search text
		settings.wildcardPostfix().set(true);  // Adds % after search text

		// Replace spaces with wildcards
		settings.spaceAsWildcard().set(true);  // "john smith" → "john%smith"

		// Case sensitivity
		settings.caseSensitive().set(false);   // Case-insensitive search
		// end::searchSettings[]
	}

	void singleSelectionSearch(EntityConnection connection) {
		// tag::singleSelectionSearch[]
		EntitySearchModel searchModel = EntitySearchModel.builder()
						.entityType(Album.TYPE)
						.connection(connection)
						.search(Album.TITLE)
						.build();

		// Set selection programmatically
		Entity album = getAlbum(connection);
		searchModel.selection().entity().set(album);

		// React to selection changes
		searchModel.selection().entity().addConsumer(selectedAlbum -> {
			if (selectedAlbum != null) {
				displayAlbumDetails(selectedAlbum);
			}
		});

		// Clear selection
		searchModel.selection().clear();
		// end::singleSelectionSearch[]
	}

	void multiSelectionSearch(EntityConnection connection) {
		// tag::multiSelectionSearch[]
		EntitySearchModel searchModel = EntitySearchModel.builder()
						.entityType(Track.TYPE)
						.connection(connection)
						.search(Track.NAME)
						.build();

		// Get all selected entities
		Collection<Entity> selectedTracks = searchModel.selection().entities().get();

		// Add to selection
		Entity track = getTrack(connection);
		searchModel.selection().entities().add(track);

		// Remove from selection
		searchModel.selection().entities().remove(track);

		// Replace entire selection
		searchModel.selection().entities().set(List.of(track));
		// end::multiSelectionSearch[]
	}

	// ModelLink examples
	void simpleMasterDetail(EntityConnection connection) {
		// tag::simpleMasterDetail[]
		// Invoice -> InvoiceLines
		SwingEntityModel invoiceModel = new SwingEntityModel(Invoice.TYPE, connection);
		SwingEntityModel invoiceLineModel = new SwingEntityModel(InvoiceLine.TYPE, connection);

		invoiceModel.detail().add(invoiceLineModel);

		// Configure detail model for optimal performance
		invoiceLineModel.tableModel().query().conditionRequired().set(true); // Don't load all lines
		invoiceLineModel.tableModel().query().limit().set(1000); // Reasonable limit
		// end::simpleMasterDetail[]
	}

	void multiLevelHierarchy(EntityConnection connection) {
		// tag::multiLevelHierarchy[]
		// Customer -> Invoice -> InvoiceLine
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityModel invoiceModel = new SwingEntityModel(Invoice.TYPE, connection);
		SwingEntityModel invoiceLineModel = new SwingEntityModel(InvoiceLine.TYPE, connection);

		// Build hierarchy
		customerModel.detail().add(invoiceModel);
		invoiceModel.detail().add(invoiceLineModel);

		// Configure each level
		invoiceModel.tableModel().query().conditionRequired().set(true);
		invoiceLineModel.tableModel().query().conditionRequired().set(true);

		// Selection cascades down the hierarchy automatically
		Entity customer = getCustomer(connection);
		customerModel.tableModel().selection().item().set(customer);
		// Invoices for customer are loaded
		// When an invoice is selected, its lines are loaded
		invoiceModel.tableModel().selection().indexes().increment();// selects first
		// end::multiLevelHierarchy[]
	}

	void customModelLink(EntityConnection connection) {
		// tag::customModelLink[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityModel invoiceModel = new SwingEntityModel(Invoice.TYPE, connection);

		ModelLink customLink =
						ForeignKeyModelLink.builder()
										.model(invoiceModel)
										.foreignKey(Invoice.CUSTOMER_FK)
										.active(true)
										.onSelection(selectedCustomers -> {
											// Custom selection logic
											if (selectedCustomers.size() > 1) {
												// Handle multi-selection differently
												invoiceModel.tableModel().query().condition().clear();
												invoiceModel.tableModel().query().condition().additional().where().set(() ->
																Invoice.CUSTOMER_FK.in(selectedCustomers)
												);
											}
										})
										.build();

		customerModel.detail().add(customLink);
		// end::customModelLink[]
	}

	void foreignKeyLink(EntityConnection connection) {
		// tag::foreignKeyLink[]
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connection);
		SwingEntityModel invoiceModel = new SwingEntityModel(Invoice.TYPE, connection);

		// ForeignKeyModelLink is created automatically when foreign key is detected
		customerModel.detail().add(invoiceModel);

		// Or configure explicitly
		customerModel.detail().add(ForeignKeyModelLink.builder()
						.model(invoiceModel)
						.foreignKey(Invoice.CUSTOMER_FK)
						// Clear foreign key value when master has no selection
						.clearValueOnEmptySelection(true)
						// Set foreign key value automatically on insert
						.setValueOnInsert(true)
						// Control when to refresh detail data
						.refreshOnSelection(true)
						// Set search condition based on master insert
						.setConditionOnInsert(true)
						.build());
		// end::foreignKeyLink[]
	}

	// Helper methods referenced in examples
	private void loadDetails(Entity entity) {}

	private void updateTotalPrice(BigDecimal price) {}

	private void updateInvoiceTotal() {}

	private void displayAlbumDetails(Entity album) {}

	private Entity getCustomer(EntityConnection connection) {
		return connection.selectSingle(Customer.ID.equalTo(1L));
	}

	private Entity getAlbum(EntityConnection connection) {
		return connection.selectSingle(Album.ID.equalTo(1L));
	}

	private Entity getTrack(EntityConnection connection) {
		return connection.selectSingle(Track.ID.equalTo(1L));
	}

	private Condition customComplexCondition() {
		return Customer.ADDRESS.isNotNull();
	}
}
