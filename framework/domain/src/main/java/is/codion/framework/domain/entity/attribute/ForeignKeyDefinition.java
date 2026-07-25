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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.DefaultForeignKeyDefinition.DefaultForeignKeyDefinitionBuilder;
import is.codion.framework.domain.entity.exception.AttributeValidationException;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

import static is.codion.common.utilities.Configuration.integerValue;

/**
 * Represents a reference to another entity, typically but not necessarily based on a foreign key.
 * <p>
 * ForeignKeyDefinition configures how foreign key relationships behave, including reference depth
 * for automatic loading, soft references for logical relationships, and attribute selection
 * for referenced entities.
 * <p>
 * Foreign key definitions control the loading strategy and behavior of entity relationships:
 * {@snippet :
 * public class Store extends DomainModel {
 *
 *     interface Customer {
 *         EntityType TYPE = DOMAIN.entityType("store.customer");
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<String> NAME = TYPE.stringColumn("name");
 *         Column<String> EMAIL = TYPE.stringColumn("email");
 *     }
 *
 *     interface Order {
 *         EntityType TYPE = DOMAIN.entityType("store.order");
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<Integer> CUSTOMER_ID = TYPE.integerColumn("customer_id");
 *         Column<LocalDateTime> ORDER_DATE = TYPE.localDateTimeColumn("order_date");
 *         Column<BigDecimal> TOTAL = TYPE.bigDecimalColumn("total");
 *
 *         ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
 *     }
 *
 *     interface OrderLine {
 *         EntityType TYPE = DOMAIN.entityType("store.order_line");
 *         Column<Integer> ORDER_ID = TYPE.integerColumn("order_id");
 *         Column<Integer> PRODUCT_ID = TYPE.integerColumn("product_id");
 *         Column<Integer> QUANTITY = TYPE.integerColumn("quantity");
 *
 *         ForeignKey ORDER_FK = TYPE.foreignKey("order_fk", ORDER_ID, Order.ID);
 *     }
 *
 *     void defineEntities() {
 *         Order.TYPE.as()
 *             .attributes(
 *                 Order.ID.as()
 *                     .primaryKey(),
 *                 Order.CUSTOMER_ID.as()
 *                     .column(),
 *                 Order.ORDER_DATE.as()
 *                     .column(),
 *                 Order.TOTAL.as()
 *                     .column(),
 *
 *                 // Basic foreign key with default reference depth (1)
 *                 Order.CUSTOMER_FK.as()
 *                     .foreignKey()
 *                     .caption("Customer"))
 *             .build();
 *
 *         OrderLine.TYPE.as()
 *             .attributes(
 *                 OrderLine.ORDER_ID.as()
 *                     .primaryKey(),
 *                 OrderLine.PRODUCT_ID.as()
 *                     .column(),
 *                 OrderLine.QUANTITY.as()
 *                     .column(),
 *
 *                 // Foreign key with deeper reference depth to load customer info.
 *                 // Note that only the foreign keys among the included attributes are populated,
 *                 // so CUSTOMER_FK must be included for the reference depth of 2 to reach the customer.
 *                 OrderLine.ORDER_FK.as()
 *                     .foreignKey()
 *                     .caption("Order")
 *                     .referenceDepth(2)  // Load order AND its customer
 *                     .include(Order.ORDER_DATE, Order.TOTAL, Order.CUSTOMER_FK))
 *             .build();
 *     }
 * }
 *
 * // Reference depth behavior examples:
 *
 * // Reference depth 0: No automatic loading
 * List<Entity> orders = connection.select(
 *     Select.where(all(Order.TYPE))
 *         .referenceDepth(0)
 *         .build());
 *
 * Entity order = orders.get(0);
 * Entity customer = order.get(Order.CUSTOMER_FK); // null - not loaded
 * Entity customerEntity = order.entity(Order.CUSTOMER_FK); // Contains only primary key
 *
 * // Reference depth 1: Load referenced entity only
 * List<Entity> ordersWithCustomers = connection.select(
 *     Select.where(all(Order.TYPE))
 *         .referenceDepth(1) // Default
 *         .build());
 *
 * Entity orderWithCustomer = ordersWithCustomers.get(0);
 * Entity loadedCustomer = orderWithCustomer.get(Order.CUSTOMER_FK); // Fully loaded customer
 *
 * // Reference depth 2: Load referenced entity and its references
 * List<Entity> orderLines = connection.select(all(OrderLine.TYPE));
 *
 * Entity orderLine = orderLines.get(0);
 * Entity orderWithCustomer = orderLine.get(OrderLine.ORDER_FK); // Order is loaded
 * Entity customer = orderWithCustomer.get(Order.CUSTOMER_FK);   // Customer is also loaded
 *
 * // WARNING: Circular references with referenceDepth(-1)
 * // If Order had a foreign key back to OrderLine, using referenceDepth(-1) would cause infinite recursion:
 * // OrderLine → Order → OrderLine → Order → ... → StackOverflowError
 *}
 * @see #referenceDepth()
 * @see #soft()
 * @see #attributes()
 * @see Entities#VALIDATE_FOREIGN_KEYS
 */
public sealed interface ForeignKeyDefinition extends AttributeDefinition<Entity> permits DefaultForeignKeyDefinition {

	/**
	 * Specifies the reference depth to use for foreign keys which do not specify one
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 1
	 * </ul>
	 * @see Builder#referenceDepth(int)
	 */
	PropertyValue<Integer> REFERENCE_DEPTH = integerValue("codion.domain.referenceDepth", 1);

	/**
	 * @return the foreign key attribute this foreign key is based on.
	 */
	@Override
	ForeignKey attribute();

	/**
	 * Returns the reference depth specified for this foreign key, an empty {@link OptionalInt} if none was
	 * specified, in which case {@link #REFERENCE_DEPTH} applies.
	 * <p>
	 * A specified depth of 0 means this foreign key is never populated automatically, not even when it is
	 * reached via another foreign key specifying a greater depth.
	 * @return the reference depth specified for this foreign key
	 * @see Builder#referenceDepth(int)
	 */
	OptionalInt referenceDepth();

	/**
	 * @return true if this foreign key is not based on a physical (table) foreign key and should not prevent deletion
	 */
	boolean soft();

	/**
	 * Returns true if the given foreign key reference column is read-only, as in, not updated when the foreign key value is set.
	 * @param referenceColumn the reference column
	 * @return true if the given foreign key reference column is read-only
	 */
	boolean readOnly(Column<?> referenceColumn);

	/**
	 * @return the {@link ForeignKey.Reference}s that comprise this foreign key
	 */
	List<ForeignKey.Reference<?>> references();

	/**
	 * @return the attributes to select when fetching entities referenced via this foreign key, an empty list in case of all attributes
	 */
	List<Attribute<?>> attributes();

	/**
	 * Validates the value of this attribute as found in the given entity.
	 * @param entity the entity containing the value to validate
	 * @param nullable true if null values are allowed in this validation context,
	 * false if null should trigger a null validation exception
	 * @throws AttributeValidationException in case of an invalid value
	 */
	void validate(Entity entity, boolean nullable) throws AttributeValidationException;

	/**
	 * Builds a {@link ForeignKeyDefinition}.
	 */
	sealed interface Builder extends AttributeDefinition.Builder<Entity, Builder> permits DefaultForeignKeyDefinitionBuilder {

		/**
		 * @throws UnsupportedOperationException Foreign key values are compared using the comparator of the underlying entity
		 */
		@Override
		Builder comparator(Comparator<Entity> comparator);

		/**
		 * Marks this foreign key as being soft, that is, not based on a physical (table) foreign key and should not prevent deletion
		 * @param soft if true then this foreign key is marked as a non-physical soft key
		 * @return this instance
		 */
		Builder soft(boolean soft);

		/**
		 * Marks the given foreign key reference column as read-only, which causes an exception being
		 * thrown when the given column value is modified via this foreign key.
		 * @param column the reference column
		 * @return this instance
		 */
		Builder readOnly(Column<?> column);

		/**
		 * <p>By default, all attributes are included when the referenced entity
		 * is selected. Use this method to restrict the included attributes.
		 * <p>Note that the primary key attributes are always included and do
		 * not have to be specified when using this method.
		 * <p>One thing to consider is to include the attributes used by the
		 * formatter for the referenced entity, to guarantee a correct string
		 * representation if required (such as in table column display).
		 * @param attributes the attributes to include in the referenced entity
		 * @return this instance
		 */
		Builder include(Attribute<?>... attributes);

		/**
		 * Specifies the query reference depth for this foreign key, that is, how many levels of the referenced
		 * entity's foreign key graph are fetched along with it. Foreign keys not specifying a depth use
		 * {@link #REFERENCE_DEPTH}.
		 * <pre>
		 * Reference depth:
		 * -1: the full foreign key graph of the referenced entity is fetched.
		 *  0: the referenced entity not fetched.
		 *  1: the referenced entity is fetched, without any foreign key references.
		 *  2: the referenced entity is fetched, with a single level of foreign key references.
		 *  3: the referenced entity is fetched, with two levels of foreign key references.
		 *  etc...
		 * </pre>
		 * <p>
		 * A depth greater than 1 is a <i>budget for the whole subtree</i>, not merely a limit on this foreign key:
		 * the referenced entity's own foreign keys are fetched to the same depth, whatever depth <i>they</i> specify.
		 * This is what makes a depth of 2 fetch the referenced entity's references, since those typically specify
		 * the default depth of 1, which on its own would stop the fetching.
		 * <p>
		 * A depth of 0 is the exception, and is absolute: such a foreign key is never populated automatically,
		 * not even when reached via another foreign key specifying a greater depth. This is how a foreign key is
		 * kept from being fetched at all, whether to prevent a child from fetching its parent back, to leave an
		 * expensive reference alone or to break a reference cycle. It can still be fetched by asking for it
		 * explicitly, by specifying a reference depth for it when selecting.
		 * <p>
		 * <b>Warning:</b> Using referenceDepth(-1) when the actual data contains circular references
		 * (e.g., record A references B, B references C, C references A) will cause infinite recursion
		 * and StackOverflowError. Self-referential foreign keys (like Employee.manager_id) are safe with
		 * unlimited depth as long as the data forms a tree/hierarchy without cycles.
		 * @param referenceDepth the number of levels of foreign key references to fetch for this foreign key
		 * @return this instance
		 * @throws IllegalArgumentException in case reference depth is less than -1
		 * @see Entities#VALIDATE_FOREIGN_KEYS
		 */
		Builder referenceDepth(int referenceDepth);
	}
}
