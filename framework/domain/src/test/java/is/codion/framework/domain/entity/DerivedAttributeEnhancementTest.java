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
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.framework.domain.DomainType.domainType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for derived attribute functionality
 */
@DisplayName("DerivedAttributeEnhancement")
public final class DerivedAttributeEnhancementTest {

	private static final DomainType DOMAIN_TYPE = domainType("derived_test");

	private Entities entities;

	interface Product {
		EntityType TYPE = DOMAIN_TYPE.entityType("product");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<BigDecimal> PRICE = TYPE.bigDecimalColumn("price");
		Column<Integer> QUANTITY = TYPE.integerColumn("quantity");
		Column<BigDecimal> TAX_RATE = TYPE.bigDecimalColumn("tax_rate");
		Column<LocalDate> PURCHASE_DATE = TYPE.localDateColumn("purchase_date");

		// Derived attributes
		Attribute<BigDecimal> TOTAL_VALUE = TYPE.bigDecimalAttribute("total_value");
		Attribute<BigDecimal> TAX_AMOUNT = TYPE.bigDecimalAttribute("tax_amount");
		Attribute<BigDecimal> TOTAL_WITH_TAX = TYPE.bigDecimalAttribute("total_with_tax");
		Attribute<Integer> DAYS_SINCE_PURCHASE = TYPE.integerAttribute("days_since_purchase");
		Attribute<String> DISPLAY_NAME = TYPE.stringAttribute("display_name");
	}

	interface Order {
		EntityType TYPE = DOMAIN_TYPE.entityType("order");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<LocalDateTime> ORDER_DATE = TYPE.localDateTimeColumn("order_date");
		Column<String> STATUS = TYPE.stringColumn("status");
	}

	interface OrderLine {
		EntityType TYPE = DOMAIN_TYPE.entityType("order_line");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> ORDER_ID = TYPE.integerColumn("order_id");
		Column<Integer> PRODUCT_ID = TYPE.integerColumn("product_id");
		Column<Integer> QUANTITY = TYPE.integerColumn("quantity");
		Column<BigDecimal> UNIT_PRICE = TYPE.bigDecimalColumn("unit_price");

		ForeignKey ORDER_FK = TYPE.foreignKey("order_fk", ORDER_ID, Order.ID);
		ForeignKey PRODUCT_FK = TYPE.foreignKey("product_fk", PRODUCT_ID, Product.ID);

		// Derived from foreign keys
		Attribute<String> PRODUCT_NAME = TYPE.stringAttribute("product_name");
		Attribute<BigDecimal> LINE_TOTAL = TYPE.bigDecimalAttribute("line_total");
		Attribute<String> ORDER_STATUS = TYPE.stringAttribute("order_status");
		Attribute<LocalDateTime> ORDER_DATE = TYPE.localDateTimeAttribute("order_date");
	}

	interface ComplexEntity {
		EntityType TYPE = DOMAIN_TYPE.entityType("complex");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> VALUE1 = TYPE.stringColumn("value1");
		Column<String> VALUE2 = TYPE.stringColumn("value2");
		Column<Integer> NUMBER1 = TYPE.integerColumn("number1");
		Column<Integer> NUMBER2 = TYPE.integerColumn("number2");

		// Multiple level derived
		Attribute<String> CONCAT_VALUES = TYPE.stringAttribute("concat_values");
		Attribute<Integer> SUM_NUMBERS = TYPE.integerAttribute("sum_numbers");
		Attribute<String> COMPLEX_DERIVED = TYPE.stringAttribute("complex_derived");
	}

	private static final class TestDomain extends DomainModel {
		public TestDomain() {
			super(DOMAIN_TYPE);

			// Product entity definition
			add(Product.TYPE.define(
							Product.ID.define().primaryKey(),
							Product.NAME.define().column().nullable(false),
							Product.PRICE.define().column().nullable(false),
							Product.QUANTITY.define().column().nullable(false),
							Product.TAX_RATE.define().column().nullable(false),
							Product.PURCHASE_DATE.define().column(),

							// Simple calculation
							Product.TOTAL_VALUE.define()
											.derived()
											.from(Product.PRICE, Product.QUANTITY)
											.value(source -> {
												BigDecimal price = source.get(Product.PRICE);
												Integer quantity = source.get(Product.QUANTITY);
												if (price != null && quantity != null) {
													return price.multiply(BigDecimal.valueOf(quantity));
												}
												return null;
											}),

							// Derived from derived
							Product.TAX_AMOUNT.define()
											.derived()
											.from(Product.TOTAL_VALUE, Product.TAX_RATE)
											.value(source -> {
												BigDecimal total = source.get(Product.TOTAL_VALUE);
												BigDecimal taxRate = source.get(Product.TAX_RATE);
												if (total != null && taxRate != null) {
													return total.multiply(taxRate);
												}
												return null;
											}),

							// Complex calculation
							Product.TOTAL_WITH_TAX.define()
											.derived()
											.from(Product.TOTAL_VALUE, Product.TAX_AMOUNT)
											.value(source -> {
												BigDecimal total = source.get(Product.TOTAL_VALUE);
												BigDecimal tax = source.get(Product.TAX_AMOUNT);
												if (total != null && tax != null) {
													return total.add(tax);
												}
												return null;
											}),

							// Date calculation
							Product.DAYS_SINCE_PURCHASE.define()
											.derived()
											.from(Product.PURCHASE_DATE)
											.value(source -> {
												LocalDate purchaseDate = source.get(Product.PURCHASE_DATE);
												if (purchaseDate != null) {
													return (int) ChronoUnit.DAYS.between(purchaseDate, LocalDate.now());
												}
												return null;
											}),

							// String manipulation
							Product.DISPLAY_NAME.define()
											.derived()
											.from(Product.NAME, Product.QUANTITY)
											.value(source -> {
												String name = source.get(Product.NAME);
												Integer quantity = source.get(Product.QUANTITY);
												if (name != null && quantity != null) {
													return name + " (Qty: " + quantity + ")";
												}
												return name;
											})
			).build());

			// Order entity definition
			add(Order.TYPE.define(
							Order.ID.define().primaryKey(),
							Order.ORDER_DATE.define().column().nullable(false),
							Order.STATUS.define().column().nullable(false)
			).build());

			// OrderLine entity definition
			add(OrderLine.TYPE.define(
							OrderLine.ID.define().primaryKey(),
							OrderLine.ORDER_ID.define().column().nullable(false),
							OrderLine.PRODUCT_ID.define().column().nullable(false),
							OrderLine.QUANTITY.define().column().nullable(false),
							OrderLine.UNIT_PRICE.define().column().nullable(false),

							OrderLine.ORDER_FK.define()
											.foreignKey()
											.attributes(Order.STATUS, Order.ORDER_DATE),

							OrderLine.PRODUCT_FK.define()
											.foreignKey()
											.attributes(Product.NAME),

							// Derived from foreign key
							OrderLine.PRODUCT_NAME.define()
											.derived()
											.from(OrderLine.PRODUCT_FK)
											.value(source -> {
												Entity product = source.get(OrderLine.PRODUCT_FK);
												if (product != null) {
													return product.get(Product.NAME);
												}
												return null;
											}),

							// Simple calculation
							OrderLine.LINE_TOTAL.define()
											.derived()
											.from(OrderLine.QUANTITY, OrderLine.UNIT_PRICE)
											.value(source -> {
												Integer quantity = source.get(OrderLine.QUANTITY);
												BigDecimal unitPrice = source.get(OrderLine.UNIT_PRICE);
												if (quantity != null && unitPrice != null) {
													return unitPrice.multiply(BigDecimal.valueOf(quantity));
												}
												return null;
											}),

							// Derived from foreign key attributes
							OrderLine.ORDER_STATUS.define()
											.derived()
											.from(OrderLine.ORDER_FK)
											.value(source -> {
												Entity order = source.get(OrderLine.ORDER_FK);
												if (order != null) {
													return order.get(Order.STATUS);
												}
												return null;
											}),

							OrderLine.ORDER_DATE.define()
											.derived()
											.from(OrderLine.ORDER_FK)
											.value(source -> {
												Entity order = source.get(OrderLine.ORDER_FK);
												if (order != null) {
													return order.get(Order.ORDER_DATE);
												}
												return null;
											})
			).build());

			// Complex entity with multi-level derived
			add(ComplexEntity.TYPE.define(
							ComplexEntity.ID.define().primaryKey(),
							ComplexEntity.VALUE1.define().column(),
							ComplexEntity.VALUE2.define().column(),
							ComplexEntity.NUMBER1.define().column(),
							ComplexEntity.NUMBER2.define().column(),

							ComplexEntity.CONCAT_VALUES.define()
											.derived()
											.from(ComplexEntity.VALUE1, ComplexEntity.VALUE2)
											.value(source -> {
												String v1 = source.get(ComplexEntity.VALUE1);
												String v2 = source.get(ComplexEntity.VALUE2);
												if (v1 != null && v2 != null) {
													return v1 + " - " + v2;
												}
												return v1 != null ? v1 : v2;
											}),

							ComplexEntity.SUM_NUMBERS.define()
											.derived()
											.from(ComplexEntity.NUMBER1, ComplexEntity.NUMBER2)
											.value(source -> {
												Integer n1 = source.get(ComplexEntity.NUMBER1);
												Integer n2 = source.get(ComplexEntity.NUMBER2);
												if (n1 != null && n2 != null) {
													return n1 + n2;
												}
												return n1 != null ? n1 : n2;
											}),

							// Derived from other derived attributes
							ComplexEntity.COMPLEX_DERIVED.define()
											.derived()
											.from(ComplexEntity.CONCAT_VALUES, ComplexEntity.SUM_NUMBERS)
											.value(source -> {
												String concat = source.get(ComplexEntity.CONCAT_VALUES);
												Integer sum = source.get(ComplexEntity.SUM_NUMBERS);
												if (concat != null && sum != null) {
													return concat + " [Sum: " + sum + "]";
												}
												return concat;
											})
			).build());
		}
	}

	@BeforeEach
	void setUp() {
		entities = new TestDomain().entities();
	}

	@Nested
	@DisplayName("Basic Derived Attributes")
	class BasicDerivedAttributesTest {

		@Test
		@DisplayName("simple calculation derived attribute updates correctly")
		void derivedAttribute_simpleCalculation_updatesCorrectly() {
			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Test Product")
							.with(Product.PRICE, new BigDecimal("10.50"))
							.with(Product.QUANTITY, 5)
							.with(Product.TAX_RATE, new BigDecimal("0.15"))
							.build();

			// Check initial calculation
			assertEquals(0, new BigDecimal("52.50").compareTo(product.get(Product.TOTAL_VALUE)));

			// Update quantity
			product.set(Product.QUANTITY, 10);
			assertEquals(0, new BigDecimal("105.00").compareTo(product.get(Product.TOTAL_VALUE)));

			// Update price
			product.set(Product.PRICE, new BigDecimal("20.00"));
			assertEquals(0, new BigDecimal("200.00").compareTo(product.get(Product.TOTAL_VALUE)));
		}

		@Test
		@DisplayName("derived attribute handles null source values")
		void derivedAttribute_nullSourceValues_returnsNull() {
			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Test Product")
							.with(Product.TAX_RATE, new BigDecimal("0.15"))
							.build();

			// Missing price and quantity
			assertNull(product.get(Product.TOTAL_VALUE));

			// Set price only
			product.set(Product.PRICE, new BigDecimal("10.00"));
			assertNull(product.get(Product.TOTAL_VALUE));

			// Set quantity
			product.set(Product.QUANTITY, 5);
			assertNotNull(product.get(Product.TOTAL_VALUE));
			assertEquals(0, new BigDecimal("50.00").compareTo(product.get(Product.TOTAL_VALUE)));
		}

		@Test
		@DisplayName("string manipulation derived attribute works correctly")
		void derivedAttribute_stringManipulation_worksCorrectly() {
			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Laptop")
							.with(Product.QUANTITY, 3)
							.with(Product.PRICE, BigDecimal.ONE)
							.with(Product.TAX_RATE, BigDecimal.ZERO)
							.build();

			assertEquals("Laptop (Qty: 3)", product.get(Product.DISPLAY_NAME));

			// Update name
			product.set(Product.NAME, "Gaming Laptop");
			assertEquals("Gaming Laptop (Qty: 3)", product.get(Product.DISPLAY_NAME));

			// Update quantity
			product.set(Product.QUANTITY, 10);
			assertEquals("Gaming Laptop (Qty: 10)", product.get(Product.DISPLAY_NAME));
		}

		@Test
		@DisplayName("date calculation derived attribute computes correctly")
		void derivedAttribute_dateCalculation_computesCorrectly() {
			LocalDate tenDaysAgo = LocalDate.now().minusDays(10);

			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Test")
							.with(Product.PRICE, BigDecimal.ONE)
							.with(Product.QUANTITY, 1)
							.with(Product.TAX_RATE, BigDecimal.ZERO)
							.with(Product.PURCHASE_DATE, tenDaysAgo)
							.build();

			assertEquals(10, product.get(Product.DAYS_SINCE_PURCHASE));

			// Update purchase date
			product.set(Product.PURCHASE_DATE, LocalDate.now().minusDays(30));
			assertEquals(30, product.get(Product.DAYS_SINCE_PURCHASE));

			// Null date
			product.set(Product.PURCHASE_DATE, null);
			assertNull(product.get(Product.DAYS_SINCE_PURCHASE));
		}
	}

	@Nested
	@DisplayName("Multi-Level Derived Attributes")
	class MultiLevelDerivedAttributesTest {

		@Test
		@DisplayName("derived from derived attribute updates correctly")
		void derivedFromDerived_updatesCorrectly() {
			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Test Product")
							.with(Product.PRICE, new BigDecimal("100.00"))
							.with(Product.QUANTITY, 2)
							.with(Product.TAX_RATE, new BigDecimal("0.20"))
							.build();

			// Check cascade calculation
			assertEquals(0, new BigDecimal("200.00").compareTo(product.get(Product.TOTAL_VALUE)));
			assertEquals(0, new BigDecimal("40.00").compareTo(product.get(Product.TAX_AMOUNT)));
			assertEquals(0, new BigDecimal("240.00").compareTo(product.get(Product.TOTAL_WITH_TAX)));

			// Update base value
			product.set(Product.QUANTITY, 3);
			assertEquals(0, new BigDecimal("300.00").compareTo(product.get(Product.TOTAL_VALUE)));
			assertEquals(0, new BigDecimal("60.00").compareTo(product.get(Product.TAX_AMOUNT)));
			assertEquals(0, new BigDecimal("360.00").compareTo(product.get(Product.TOTAL_WITH_TAX)));

			// Update tax rate
			product.set(Product.TAX_RATE, new BigDecimal("0.10"));
			assertEquals(0, new BigDecimal("30.00").compareTo(product.get(Product.TAX_AMOUNT)));
			assertEquals(0, new BigDecimal("330.00").compareTo(product.get(Product.TOTAL_WITH_TAX)));
		}

		@Test
		@DisplayName("complex multi-level derived attributes work correctly")
		void complexMultiLevel_worksCorrectly() {
			Entity complex = entities.entity(ComplexEntity.TYPE)
							.with(ComplexEntity.ID, 1)
							.with(ComplexEntity.VALUE1, "Hello")
							.with(ComplexEntity.VALUE2, "World")
							.with(ComplexEntity.NUMBER1, 10)
							.with(ComplexEntity.NUMBER2, 20)
							.build();

			assertEquals("Hello - World", complex.get(ComplexEntity.CONCAT_VALUES));
			assertEquals(30, complex.get(ComplexEntity.SUM_NUMBERS));
			assertEquals("Hello - World [Sum: 30]", complex.get(ComplexEntity.COMPLEX_DERIVED));

			// Update base values
			complex.set(ComplexEntity.VALUE1, "Goodbye");
			complex.set(ComplexEntity.NUMBER2, 25);

			assertEquals("Goodbye - World", complex.get(ComplexEntity.CONCAT_VALUES));
			assertEquals(35, complex.get(ComplexEntity.SUM_NUMBERS));
			assertEquals("Goodbye - World [Sum: 35]", complex.get(ComplexEntity.COMPLEX_DERIVED));
		}

		@Test
		@DisplayName("partial null values in multi-level derived")
		void multiLevel_partialNullValues_handledCorrectly() {
			Entity complex = entities.entity(ComplexEntity.TYPE)
							.with(ComplexEntity.ID, 1)
							.with(ComplexEntity.VALUE1, "Only One")
							.with(ComplexEntity.NUMBER1, 42)
							.build();

			assertEquals("Only One", complex.get(ComplexEntity.CONCAT_VALUES));
			assertEquals(42, complex.get(ComplexEntity.SUM_NUMBERS));
			assertEquals("Only One [Sum: 42]", complex.get(ComplexEntity.COMPLEX_DERIVED));
		}
	}

	@Nested
	@DisplayName("Foreign Key Derived Attributes")
	class ForeignKeyDerivedAttributesTest {

		@Test
		@DisplayName("derived from foreign key entity updates when FK changes")
		void derivedFromForeignKey_updatesWhenFKChanges() {
			Entity order = entities.entity(Order.TYPE)
							.with(Order.ID, 1)
							.with(Order.ORDER_DATE, LocalDateTime.now())
							.with(Order.STATUS, "PENDING")
							.build();

			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Laptop")
							.with(Product.PRICE, new BigDecimal("1000"))
							.with(Product.QUANTITY, 10)
							.with(Product.TAX_RATE, new BigDecimal("0.15"))
							.build();

			Entity orderLine = entities.entity(OrderLine.TYPE)
							.with(OrderLine.ID, 1)
							.with(OrderLine.ORDER_FK, order)
							.with(OrderLine.PRODUCT_FK, product)
							.with(OrderLine.QUANTITY, 2)
							.with(OrderLine.UNIT_PRICE, new BigDecimal("1000"))
							.build();

			// Check derived values from foreign keys
			assertEquals("Laptop", orderLine.get(OrderLine.PRODUCT_NAME));
			assertEquals("PENDING", orderLine.get(OrderLine.ORDER_STATUS));
			assertEquals(order.get(Order.ORDER_DATE), orderLine.get(OrderLine.ORDER_DATE));

			// Update foreign key entity values
			order.set(Order.STATUS, "SHIPPED");

			// Create a new orderLine with the updated order to test derived attribute
			Entity updatedOrderLine = entities.entity(OrderLine.TYPE)
							.with(OrderLine.ID, 2)
							.with(OrderLine.ORDER_ID, order.get(Order.ID))
							.with(OrderLine.PRODUCT_ID, product.get(Product.ID))
							.with(OrderLine.QUANTITY, 1)
							.with(OrderLine.UNIT_PRICE, new BigDecimal("1000"))
							.with(OrderLine.ORDER_FK, order)  // Updated order
							.build();

			assertEquals("SHIPPED", updatedOrderLine.get(OrderLine.ORDER_STATUS));

			// Change foreign key reference
			Entity newProduct = entities.entity(Product.TYPE)
							.with(Product.ID, 2)
							.with(Product.NAME, "Desktop")
							.with(Product.PRICE, new BigDecimal("2000"))
							.with(Product.QUANTITY, 5)
							.with(Product.TAX_RATE, new BigDecimal("0.15"))
							.build();

			orderLine.set(OrderLine.PRODUCT_FK, newProduct);
			assertEquals("Desktop", orderLine.get(OrderLine.PRODUCT_NAME));
		}

		@Test
		@DisplayName("derived from null foreign key returns null")
		void derivedFromForeignKey_nullFK_returnsNull() {
			Entity orderLine = entities.entity(OrderLine.TYPE)
							.with(OrderLine.ID, 1)
							.with(OrderLine.QUANTITY, 2)
							.with(OrderLine.UNIT_PRICE, new BigDecimal("1000"))
							.build();

			assertNull(orderLine.get(OrderLine.PRODUCT_NAME));
			assertNull(orderLine.get(OrderLine.ORDER_STATUS));
			assertNull(orderLine.get(OrderLine.ORDER_DATE));
		}

		@Test
		@DisplayName("line total calculation works independently of FK")
		void lineTotal_worksIndependentlyOfFK() {
			Entity orderLine = entities.entity(OrderLine.TYPE)
							.with(OrderLine.ID, 1)
							.with(OrderLine.QUANTITY, 5)
							.with(OrderLine.UNIT_PRICE, new BigDecimal("50.00"))
							.build();

			assertEquals(0, new BigDecimal("250.00").compareTo(orderLine.get(OrderLine.LINE_TOTAL)));
		}
	}

	@Nested
	@DisplayName("Edge Cases and Performance")
	class EdgeCasesAndPerformanceTest {

		@Test
		@DisplayName("derived attribute provider not called unnecessarily")
		void derivedProvider_notCalledUnnecessarily() {
			AtomicInteger callCount = new AtomicInteger(0);

			// Create a custom domain with counting provider
			class CountingDomain extends DomainModel {
				CountingDomain() {
					super(domainType("counting"));
					EntityType type = type().entityType("counting_entity");
					Column<Integer> value = type.integerColumn("value");
					Attribute<Integer> doubled = type.integerAttribute("doubled");

					add(type.define(
									value.define().column(),
									doubled.define()
													.derived()
													.from(value)
													.value(source -> {
														callCount.incrementAndGet();
														Integer val = source.get(value);
														return val != null ? val * 2 : null;
													})
					).build());
				}
			}

			CountingDomain countingDomain = new CountingDomain();
			Entities countingEntities = countingDomain.entities();
			EntityType type = countingDomain.type().entityType("counting_entity");
			Column<Integer> value = type.integerColumn("value");
			Attribute<Integer> doubled = type.integerAttribute("doubled");

			Entity entity = countingEntities.entity(type)
							.with(value, 5)
							.build();

			// First access
			assertEquals(10, entity.get(doubled));
			int firstCount = callCount.get();
			assertTrue(firstCount > 0);

			// Second access - should use cached value
			assertEquals(10, entity.get(doubled));
			assertEquals(firstCount, callCount.get());

			// Change source value
			entity.set(value, 10);

			// Access after change - should recalculate
			assertEquals(20, entity.get(doubled));
			assertTrue(callCount.get() > firstCount);
		}

		@Test
		@DisplayName("circular dependency causes StackOverflowError")
		void circularDependency_causesStackOverflowError() {
			// Circular dependencies should cause StackOverflowError when accessed
			class CircularDomain extends DomainModel {
				CircularDomain() {
					super(domainType("circular"));
					EntityType type = type().entityType("circular_entity");
					Attribute<Integer> attr1 = type.integerAttribute("attr1");
					Attribute<Integer> attr2 = type.integerAttribute("attr2");

					add(type.define(
									attr1.define()
													.derived()
													.from(attr2)
													.value(source -> source.get(attr2)),
									attr2.define()
													.derived()
													.from(attr1)
													.value(source -> source.get(attr1))
					).build());
				}
			}

			Entities circularEntities = new CircularDomain().entities();

			// Create entity successfully
			EntityDefinition entityDef = circularEntities.definitions().iterator().next();
			Entity entity = circularEntities.entity(entityDef.type()).build();

			// Access to circular derived attribute should cause StackOverflowError
			assertThrows(StackOverflowError.class, () -> {
				for (AttributeDefinition<?> attrDef : entity.definition().attributes().definitions()) {
					if (attrDef.derived()) {
						entity.get(attrDef.attribute());
						break;
					}
				}
			});
		}

		@Test
		@DisplayName("derived attribute with exception in provider")
		void derivedProvider_withException_handledGracefully() {
			class ExceptionDomain extends DomainModel {
				ExceptionDomain() {
					super(domainType("exception"));
					EntityType type = type().entityType("exception_entity");
					Column<Integer> value = type.integerColumn("value");
					Attribute<Integer> problematic = type.integerAttribute("problematic");

					add(type.define(
									value.define().column(),
									problematic.define()
													.derived()
													.from(value)
													.value(source -> {
														Integer val = source.get(value);
														if (val != null && val < 0) {
															throw new IllegalArgumentException("Negative values not allowed");
														}
														return val;
													})
					).build());
				}
			}

			ExceptionDomain exceptionDomain = new ExceptionDomain();
			Entities exceptionEntities = exceptionDomain.entities();
			EntityType type = exceptionDomain.type().entityType("exception_entity");
			Column<Integer> value = type.integerColumn("value");
			Attribute<Integer> problematic = type.integerAttribute("problematic");

			Entity entity = exceptionEntities.entity(type)
							.with(value, 5)
							.build();

			// Normal case
			assertEquals(5, entity.get(problematic));

			// Set negative value - provider throws exception
			entity.set(value, -1);
			assertThrows(IllegalArgumentException.class, () -> entity.get(problematic));
		}

		@Test
		@DisplayName("many source attributes performance")
		void manySourceAttributes_performance() {
			// Create entity with many source attributes
			class ManySourceDomain extends DomainModel {
				ManySourceDomain() {
					super(domainType("many_source"));
					EntityType type = type().entityType("many_source_entity");

					List<Column<Integer>> columns = Arrays.asList(
									type.integerColumn("val1"),
									type.integerColumn("val2"),
									type.integerColumn("val3"),
									type.integerColumn("val4"),
									type.integerColumn("val5"),
									type.integerColumn("val6"),
									type.integerColumn("val7"),
									type.integerColumn("val8"),
									type.integerColumn("val9"),
									type.integerColumn("val10")
					);

					Attribute<Integer> sum = type.integerAttribute("sum");


					List<AttributeDefinition.Builder<?, ?>> definitions = new ArrayList<>();
					for (Column<Integer> col : columns) {
						definitions.add(col.define().column());
					}

					definitions.add(sum.define()
									.derived()
									.from(columns.toArray(new Attribute[0]))
									.value(source -> {
										int total = 0;
										for (Column<Integer> col : columns) {
											Integer val = source.get(col);
											if (val != null) {
												total += val;
											}
										}
										return total;
									}));

					add(type.define(definitions.toArray(new AttributeDefinition.Builder[0])).build());
				}
			}

			ManySourceDomain manySourceDomain = new ManySourceDomain();
			Entities manySourceEntities = manySourceDomain.entities();
			EntityType type = manySourceDomain.type().entityType("many_source_entity");
			Attribute<Integer> sum = type.integerAttribute("sum");

			Entity.Builder entityBuilder = manySourceEntities.entity(type);
			for (int i = 1; i <= 10; i++) {
				entityBuilder.with(type.integerColumn("val" + i), i);
			}
			Entity entity = entityBuilder.build();

			// Should sum 1+2+3+...+10 = 55
			assertEquals(55, entity.get(sum));

			// Update one value
			entity.set(type.integerColumn("val5"), 50);
			assertEquals(100, entity.get(sum)); // 55 - 5 + 50 = 100
		}
	}

	@Nested
	@DisplayName("Attribute Definition Constraints")
	class AttributeDefinitionConstraintsTest {

		@Test
		@DisplayName("derived attributes ignore nullable constraint")
		void derivedAttribute_nullableConstraint_ignored() {
			// The AttributeDefinitionTest already covers this, but let's ensure
			// it's clear in our enhanced tests
			assertThrows(UnsupportedOperationException.class, () -> {
				class ConstraintDomain extends DomainModel {
					ConstraintDomain() {
						super(domainType("constraint"));
						EntityType type = type().entityType("constraint_entity");
						Column<Integer> sourceAttribute = type.integerColumn("source");
						Attribute<Integer> derived = type.integerAttribute("derived");

						add(type.define(
										sourceAttribute.define().column(),
										derived.define()
														.derived()
														.from(sourceAttribute)
														.value(source -> source.get(sourceAttribute))
														.nullable(false) // This should throw
						).build());
					}
				}
				new ConstraintDomain();
			});
		}

		@Test
		@DisplayName("derived attributes ignore default value")
		void derivedAttribute_defaultValue_ignored() {
			assertThrows(UnsupportedOperationException.class, () -> {
				class DefaultDomain extends DomainModel {
					DefaultDomain() {
						super(domainType("default"));
						EntityType type = type().entityType("default_entity");
						Column<Integer> sourceAttribute = type.integerColumn("source");
						Attribute<Integer> derived = type.integerAttribute("derived");

						add(type.define(
										sourceAttribute.define().column(),
										derived.define()
														.derived()
														.from(sourceAttribute)
														.value(source -> source.get(sourceAttribute))
														.defaultValue(42) // This should throw
						).build());
					}
				}
				new DefaultDomain();
			});
		}
	}
}