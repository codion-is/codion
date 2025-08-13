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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.ForeignKeyCondition;

import java.util.List;

/**
 * An {@link Attribute} representing a foreign key relation.
 * <p>
 * Foreign keys establish relationships between entities, allowing navigation from one entity
 * to related entities. They represent database foreign key constraints and enable automatic
 * loading of referenced entities based on reference depth configuration.
 * <p>
 * Foreign keys inherit from {@link ForeignKeyCondition.Factory} to provide condition creation methods:
 * {@snippet :
 * public class Store extends DefaultDomain {
 *
 *     interface Customer {
 *         EntityType TYPE = DOMAIN.entityType("store.customer");
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<String> NAME = TYPE.stringColumn("name");
 *     }
 *
 *     interface Order {
 *         EntityType TYPE = DOMAIN.entityType("store.order");
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<Integer> CUSTOMER_ID = TYPE.integerColumn("customer_id");
 *         Column<LocalDateTime> ORDER_DATE = TYPE.localDateTimeColumn("order_date");
 *
 *         // Single-column foreign key
 *         ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
 *     }
 *
 *     interface OrderLine {
 *         EntityType TYPE = DOMAIN.entityType("store.order_line");
 *         Column<Integer> ORDER_ID = TYPE.integerColumn("order_id");
 *         Column<Integer> LINE_NUMBER = TYPE.integerColumn("line_number");
 *         Column<Integer> PRODUCT_ID = TYPE.integerColumn("product_id");
 *
 *         // Composite foreign key (two columns)
 *         ForeignKey ORDER_FK = TYPE.foreignKey("order_fk",
 *             List.of(ForeignKey.reference(ORDER_ID, Order.ID),
 *                     ForeignKey.reference(LINE_NUMBER, Order.LINE_NUMBER)));
 *     }
 *
 *     void defineOrder() {
 *         Order.TYPE.define(
 *                 Order.ID.define()
 *                     .primaryKey(),
 *                 Order.CUSTOMER_ID.define()
 *                     .column(),
 *                 Order.ORDER_DATE.define()
 *                     .column(),
 *                 Order.CUSTOMER_FK.define()
 *                     .foreignKey()
 *                     .caption("Customer")
 *                     .referenceDepth(1))  // Load customer automatically (1 is the default)
 *             .build();
 *     }
 * }
 *
 * // Foreign key navigation and usage
 * List<Entity> orders = connection.select(all(Order.TYPE));
 *
 * for (Entity order : orders) {
 *     // Direct foreign key entity access (loaded automatically with reference depth)
 *     Entity customer = order.get(Order.CUSTOMER_FK);
 *     if (customer != null) {
 *         String customerName = customer.get(Customer.NAME);
 *         System.out.println("Customer: " + customerName);
 *     }
 *
 *     // Or use entity() method to get entity even if not fully loaded
 *     Entity customerEntity = order.entity(Order.CUSTOMER_FK);
 *     if (customerEntity != null) {
 *         Integer customerId = customerEntity.get(Customer.ID); // Always available
 *     }
 * }
 *
 * // Query conditions using foreign keys
 * Entity specificCustomer = connection.selectSingle(Customer.ID.equalTo(42));
 *
 * List<Entity> customerOrders = connection.select(
 *     Order.CUSTOMER_FK.equalTo(specificCustomer));
 *
 * List<Entity> ordersFromActiveCustomers = connection.select(
 *     Order.CUSTOMER_FK.in(connection.select(Customer.ACTIVE.equalTo(true))));
 *}
 * @see ForeignKeyCondition.Factory
 * @see #define()
 * @see #referencedType()
 * @see #references()
 */
public interface ForeignKey extends Attribute<Entity>, ForeignKeyCondition.Factory {

	/**
	 * @return a {@link ForeignKeyDefiner} for this foreign key
	 */
	ForeignKeyDefiner define();

	/**
	 * @return the entity type referenced by this foreign key
	 */
	EntityType referencedType();

	/**
	 * @return the {@link Reference}s that comprise this key
	 */
	List<Reference<?>> references();

	/**
	 * @param column the column
	 * @param <T> the column type
	 * @return the reference that is based on the given column
	 */
	<T> Reference<T> reference(Column<T> column);

	/**
	 * Represents a foreign key reference between columns.
	 * @param <T> the attribute type
	 */
	interface Reference<T> {

		/**
		 * @return the column in the child entity
		 */
		Column<T> column();

		/**
		 * @return the referenced foreign column in the parent entity
		 */
		Column<T> foreign();
	}

	/**
	 * Returns a new {@link Reference} based on the given columns.
	 * @param column the local column
	 * @param foreign the referenced foreign column
	 * @param <T> the column type
	 * @return a new {@link Reference} based on the given columns
	 */
	static <T> Reference<T> reference(Column<T> column, Column<T> foreign) {
		return new DefaultForeignKey.DefaultReference<>(column, foreign);
	}

	/**
	 * Creates a new {@link ForeignKey} based on the given entityType and references.
	 * @param entityType the entityType owning this foreign key
	 * @param name the attribute name
	 * @param references the references
	 * @return a new {@link ForeignKey}
	 * @see ForeignKey#reference(Column, Column)
	 */
	static ForeignKey foreignKey(EntityType entityType, String name, List<ForeignKey.Reference<?>> references) {
		return new DefaultForeignKey(name, entityType, references);
	}

	/**
	 * Provides {@link ForeignKeyDefinition.Builder} instances.
	 */
	interface ForeignKeyDefiner extends AttributeDefiner<Entity> {

		/**
		 * Instantiates a {@link ForeignKeyDefinition.Builder} instance, using the reference depth
		 * specified by {@link ForeignKeyDefinition#REFERENCE_DEPTH}
		 * @return a new {@link ForeignKeyDefinition.Builder}
		 * @see ForeignKeyDefinition#REFERENCE_DEPTH
		 */
		ForeignKeyDefinition.Builder foreignKey();
	}
}
