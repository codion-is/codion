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

import is.codion.common.utilities.Text;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ItemValidationException;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.framework.domain.DomainType.domainType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for entity validation functionality
 */
@DisplayName("EntityValidationEnhancement")
public final class EntityValidationEnhancementTest {

	private static final DomainType DOMAIN_TYPE = domainType("validation_test");

	private Entities entities;

	interface Customer {
		EntityType TYPE = DOMAIN_TYPE.entityType("customer");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> EMAIL = TYPE.stringColumn("email");
		Column<String> PHONE = TYPE.stringColumn("phone");
		Column<LocalDate> BIRTH_DATE = TYPE.localDateColumn("birth_date");
		Column<BigDecimal> CREDIT_LIMIT = TYPE.bigDecimalColumn("credit_limit");
		Column<String> STATUS = TYPE.stringColumn("status");
		Column<String> COUNTRY_CODE = TYPE.stringColumn("country_code");
		Column<Integer> LOYALTY_POINTS = TYPE.integerColumn("loyalty_points");

		// Derived attribute for validation
		Attribute<Integer> AGE = TYPE.integerAttribute("age");
	}

	interface Product {
		EntityType TYPE = DOMAIN_TYPE.entityType("product");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> CODE = TYPE.stringColumn("code");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<BigDecimal> PRICE = TYPE.bigDecimalColumn("price");
		Column<Integer> STOCK_QUANTITY = TYPE.integerColumn("stock_quantity");
		Column<String> CATEGORY = TYPE.stringColumn("category");
		Column<LocalDateTime> EXPIRY_DATE = TYPE.localDateTimeColumn("expiry_date");
		Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
		Column<Double> WEIGHT = TYPE.doubleColumn("weight");
		Column<String> BARCODE = TYPE.stringColumn("barcode");
	}

	interface Order {
		EntityType TYPE = DOMAIN_TYPE.entityType("order");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> CUSTOMER_ID = TYPE.integerColumn("customer_id");
		Column<LocalDateTime> ORDER_DATE = TYPE.localDateTimeColumn("order_date");
		Column<String> STATUS = TYPE.stringColumn("status");
		Column<BigDecimal> TOTAL = TYPE.bigDecimalColumn("total");
		Column<String> NOTES = TYPE.stringColumn("notes");

		ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
	}

	interface OrderItem {
		EntityType TYPE = DOMAIN_TYPE.entityType("order_item");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> ORDER_ID = TYPE.integerColumn("order_id");
		Column<Integer> PRODUCT_ID = TYPE.integerColumn("product_id");
		Column<Integer> QUANTITY = TYPE.integerColumn("quantity");
		Column<BigDecimal> UNIT_PRICE = TYPE.bigDecimalColumn("unit_price");
		Column<BigDecimal> DISCOUNT = TYPE.bigDecimalColumn("discount");

		ForeignKey ORDER_FK = TYPE.foreignKey("order_fk", ORDER_ID, Order.ID);
		ForeignKey PRODUCT_FK = TYPE.foreignKey("product_fk", PRODUCT_ID, Product.ID);

		// Derived for cross-entity validation
		Attribute<BigDecimal> LINE_TOTAL = TYPE.bigDecimalAttribute("line_total");
	}

	private static final class TestDomain extends DomainModel {

		private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
		private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
		private static final Pattern BARCODE_PATTERN = Pattern.compile("^[0-9]{12,13}$");

		public TestDomain() {
			super(DOMAIN_TYPE);

			// Customer entity
			add(Customer.TYPE.define(
							Customer.ID.define().primaryKey(),
							Customer.NAME.define()
											.column()
											.nullable(false)
											.maximumLength(100),
							Customer.EMAIL.define()
											.column()
											.nullable(false)
											.maximumLength(255),
							Customer.PHONE.define()
											.column()
											.maximumLength(20),
							Customer.BIRTH_DATE.define()
											.column(),
							Customer.CREDIT_LIMIT.define()
											.column()
											.nullable(false)
											.range(BigDecimal.ZERO, new BigDecimal("1000000")),
							Customer.STATUS.define()
											.column()
											.nullable(false)
											.items(Arrays.asList(item("ACTIVE"), item("INACTIVE"), item("SUSPENDED"))),
							Customer.COUNTRY_CODE.define()
											.column()
											.nullable(false)
											.maximumLength(2),
							Customer.LOYALTY_POINTS.define()
											.column()
											.nullable(false)
											.defaultValue(0)
											.minimum(0),
							Customer.AGE.define()
											.derived()
											.from(Customer.BIRTH_DATE)
											.with(source -> {
												LocalDate birthDate = source.get(Customer.BIRTH_DATE);
												if (birthDate != null) {
													return LocalDate.now().getYear() - birthDate.getYear();
												}
												return null;
											})
			).validator(new CustomerValidator()).build());

			// Product entity
			add(Product.TYPE.define(
							Product.ID.define().primaryKey(),
							Product.CODE.define()
											.column()
											.nullable(false)
											.maximumLength(20),
							Product.NAME.define()
											.column()
											.nullable(false)
											.maximumLength(200),
							Product.PRICE.define()
											.column()
											.nullable(false)
											.minimum(BigDecimal.ZERO),
							Product.STOCK_QUANTITY.define()
											.column()
											.nullable(false)
											.minimum(0),
							Product.CATEGORY.define()
											.column()
											.nullable(false)
											.items(Arrays.asList(item("ELECTRONICS"), item("CLOTHING"), item("FOOD"), item("BOOKS"), item("OTHER"))),
							Product.EXPIRY_DATE.define()
											.column(),
							Product.ACTIVE.define()
											.column()
											.nullable(false)
											.defaultValue(true),
							Product.WEIGHT.define()
											.column()
											.range(0.0, 1000.0),
							Product.BARCODE.define()
											.column()
											.maximumLength(13)
			).validator(new ProductValidator()).build());

			// Order entity
			add(Order.TYPE.define(
							Order.ID.define().primaryKey(),
							Order.CUSTOMER_ID.define()
											.column()
											.nullable(false),
							Order.ORDER_DATE.define()
											.column()
											.nullable(false),
							Order.STATUS.define()
											.column()
											.nullable(false)
											.items(Arrays.asList(item("PENDING"), item("PROCESSING"), item("SHIPPED"), item("DELIVERED"), item("CANCELLED"))),
							Order.TOTAL.define()
											.column()
											.nullable(false)
											.minimum(BigDecimal.ZERO),
							Order.NOTES.define()
											.column()
											.maximumLength(1000),
							Order.CUSTOMER_FK.define()
											.foreignKey()
			).validator(new OrderValidator()).build());

			// OrderItem entity
			add(OrderItem.TYPE.define(
											OrderItem.ID.define().primaryKey(),
											OrderItem.ORDER_ID.define()
															.column()
															.nullable(false),
											OrderItem.PRODUCT_ID.define()
															.column()
															.nullable(false),
											OrderItem.QUANTITY.define()
															.column()
															.nullable(false)
															.range(1, 1000),
											OrderItem.UNIT_PRICE.define()
															.column()
															.nullable(false)
															.minimum(BigDecimal.ZERO),
											OrderItem.DISCOUNT.define()
															.column()
															.nullable(false)
															.defaultValue(BigDecimal.ZERO)
															.range(BigDecimal.ZERO, new BigDecimal("100")),
											OrderItem.ORDER_FK.define()
															.foreignKey(),
											OrderItem.PRODUCT_FK.define()
															.foreignKey()
															.include(Product.STOCK_QUANTITY, Product.ACTIVE),
											OrderItem.LINE_TOTAL.define()
															.derived()
															.from(OrderItem.QUANTITY, OrderItem.UNIT_PRICE, OrderItem.DISCOUNT)
															.with(source -> {
																Integer quantity = source.get(OrderItem.QUANTITY);
																BigDecimal unitPrice = source.get(OrderItem.UNIT_PRICE);
																BigDecimal discount = source.get(OrderItem.DISCOUNT);
																if (quantity != null && unitPrice != null && discount != null) {
																	BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
																	BigDecimal discountAmount = subtotal.multiply(discount.divide(new BigDecimal("100")));
																	return subtotal.subtract(discountAmount);
																}
																return null;
															}))
							.validator(new OrderItemValidator()).build());
		}

		private static class CustomerValidator implements EntityValidator {
			@Override
			public void validate(Entity entity, Attribute<?> attribute) throws ValidationException {
				EntityValidator.super.validate(entity, attribute);

				if (attribute.equals(Customer.EMAIL)) {
					String email = entity.get(Customer.EMAIL);
					if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
						throw new ValidationException(Customer.EMAIL, email, "Invalid email format");
					}
				}

				if (attribute.equals(Customer.PHONE)) {
					String phone = entity.get(Customer.PHONE);
					if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
						throw new ValidationException(Customer.PHONE, phone, "Invalid phone format");
					}
				}

				if (attribute.equals(Customer.BIRTH_DATE)) {
					LocalDate birthDate = entity.get(Customer.BIRTH_DATE);
					if (birthDate != null) {
						if (birthDate.isAfter(LocalDate.now())) {
							throw new ValidationException(Customer.BIRTH_DATE, birthDate, "Birth date cannot be in the future");
						}
						if (birthDate.isBefore(LocalDate.now().minusYears(150))) {
							throw new ValidationException(Customer.BIRTH_DATE, birthDate, "Birth date too far in the past");
						}
					}
				}

				if (attribute.equals(Customer.COUNTRY_CODE)) {
					String countryCode = entity.get(Customer.COUNTRY_CODE);
					if (countryCode != null && !countryCode.matches("^[A-Z]{2}$")) {
						throw new ValidationException(Customer.COUNTRY_CODE, countryCode, "Country code must be 2 uppercase letters");
					}
				}
			}
		}

		private static class ProductValidator implements EntityValidator {
			@Override
			public void validate(Entity entity, Attribute<?> attribute) throws ValidationException {
				EntityValidator.super.validate(entity, attribute);

				if (attribute.equals(Product.CODE)) {
					String code = entity.get(Product.CODE);
					if (code != null && !code.matches("^[A-Z0-9-]+$")) {
						throw new ValidationException(Product.CODE, code, "Product code must contain only uppercase letters, numbers, and hyphens");
					}
				}

				if (attribute.equals(Product.EXPIRY_DATE)) {
					LocalDateTime expiryDate = entity.get(Product.EXPIRY_DATE);
					String category = entity.get(Product.CATEGORY);
					if ("FOOD".equals(category) && expiryDate == null) {
						throw new NullValidationException(Product.EXPIRY_DATE, "Food products must have an expiry date");
					}
				}

				if (attribute.equals(Product.BARCODE)) {
					String barcode = entity.get(Product.BARCODE);
					if (barcode != null && !BARCODE_PATTERN.matcher(barcode).matches()) {
						throw new ValidationException(Product.BARCODE, barcode, "Barcode must be 12 or 13 digits");
					}
				}
			}
		}

		private static class OrderValidator implements EntityValidator {
			@Override
			public void validate(Entity entity, Attribute<?> attribute) throws ValidationException {
				EntityValidator.super.validate(entity, attribute);

				if (attribute.equals(Order.ORDER_DATE)) {
					LocalDateTime orderDate = entity.get(Order.ORDER_DATE);
					if (orderDate != null && orderDate.isAfter(LocalDateTime.now())) {
						throw new ValidationException(Order.ORDER_DATE, orderDate, "Order date cannot be in the future");
					}
				}

				if (attribute.equals(Order.STATUS)) {
					String status = entity.get(Order.STATUS);
					String previousStatus = entity.original(Order.STATUS);

					// State transition validation
					if (previousStatus != null && status != null && !previousStatus.equals(status)) {
						if ("DELIVERED".equals(previousStatus) && !"CANCELLED".equals(status)) {
							throw new ValidationException(Order.STATUS, status, "Delivered orders can only be cancelled");
						}
						if ("CANCELLED".equals(previousStatus)) {
							throw new ValidationException(Order.STATUS, status, "Cancelled orders cannot be modified");
						}
					}
				}
			}
		}

		private static class OrderItemValidator implements EntityValidator {
			@Override
			public void validate(Entity entity, Attribute<?> attribute) throws ValidationException {
				EntityValidator.super.validate(entity, attribute);

				if (attribute.equals(OrderItem.QUANTITY)) {
					Integer quantity = entity.get(OrderItem.QUANTITY);
					Entity product = entity.get(OrderItem.PRODUCT_FK);

					if (quantity != null && product != null) {
						Integer stockQuantity = product.get(Product.STOCK_QUANTITY);
						if (stockQuantity != null && quantity > stockQuantity) {
							throw new ValidationException(OrderItem.QUANTITY, quantity,
											"Quantity exceeds available stock (" + stockQuantity + ")");
						}

						Boolean active = product.get(Product.ACTIVE);
						if (Boolean.FALSE.equals(active)) {
							throw new ValidationException(OrderItem.PRODUCT_FK, product,
											"Cannot order inactive product");
						}
					}
				}

				if (attribute.equals(OrderItem.DISCOUNT)) {
					BigDecimal discount = entity.get(OrderItem.DISCOUNT);
					BigDecimal unitPrice = entity.get(OrderItem.UNIT_PRICE);

					if (discount != null && unitPrice != null &&
									discount.compareTo(new BigDecimal("50")) > 0 &&
									unitPrice.compareTo(new BigDecimal("100")) < 0) {
						throw new ValidationException(OrderItem.DISCOUNT, discount,
										"Discount cannot exceed 50% for items under $100");
					}
				}
			}
		}
	}

	@BeforeEach
	void setUp() {
		entities = new TestDomain().entities();
	}

	@Nested
	@DisplayName("Basic Validation")
	class BasicValidationTest {

		@Test
		@DisplayName("null validation catches missing required values")
		void nullValidation_missingRequiredValues_throwsException() {
			// First create a valid entity
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "John Doe")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			EntityValidator validator = new EntityValidator() {};
			assertDoesNotThrow(() -> validator.validate(customer));

			// Now remove required field and expect validation to fail
			customer.set(Customer.NAME, null);
			ValidationException exception = assertThrows(NullValidationException.class,
							() -> validator.validate(customer));
			assertEquals(Customer.NAME, exception.attribute());
		}

		@Test
		@DisplayName("length validation enforces maximum length")
		void lengthValidation_exceedsMaxLength_throwsException() {
			// First create a valid entity
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Valid Name")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			EntityValidator validator = new EntityValidator() {};
			assertDoesNotThrow(() -> validator.validate(customer));

			// Now modify to violate length constraint and test validation
			customer.set(Customer.NAME, Text.leftPad("", 101, 'A')); // Exceeds 100 char limit
			ValidationException exception = assertThrows(LengthValidationException.class,
							() -> validator.validate(customer));
			assertEquals(Customer.NAME, exception.attribute());
		}

		@Test
		@DisplayName("range validation enforces min and max values")
		void rangeValidation_outsideRange_throwsException() {
			// First create a valid entity
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test Customer")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			EntityValidator validator = new EntityValidator() {};
			assertDoesNotThrow(() -> validator.validate(customer));

			// Now modify to violate range constraint and test validation
			customer.set(Customer.CREDIT_LIMIT, new BigDecimal("2000000")); // Exceeds max
			ValidationException exception = assertThrows(RangeValidationException.class,
							() -> validator.validate(customer));
			assertEquals(Customer.CREDIT_LIMIT, exception.attribute());
		}

		@Test
		@DisplayName("item validation ensures value is in allowed list")
		void itemValidation_invalidItem_throwsException() {
			// Create entity with invalid item using entity(Map) to bypass immediate validation
			Map<Attribute<?>, Object> values = new HashMap<>();
			values.put(Customer.ID, 1);
			values.put(Customer.NAME, "Test Customer");
			values.put(Customer.EMAIL, "test@example.com");
			values.put(Customer.CREDIT_LIMIT, new BigDecimal("1000"));
			values.put(Customer.STATUS, "BLOCKED"); // Invalid status
			values.put(Customer.COUNTRY_CODE, "US");

			Entity customer = entities.definition(Customer.TYPE).entity(values);
			EntityValidator validator = new EntityValidator() {};
			ValidationException exception = assertThrows(ItemValidationException.class,
							() -> validator.validate(customer, Customer.STATUS));
			assertEquals(Customer.STATUS, exception.attribute());
		}
	}

	@Nested
	@DisplayName("Custom Validation")
	class CustomValidationTest {

		@Test
		@DisplayName("email validation enforces format")
		void emailValidation_invalidFormat_throwsException() {
			// First create a valid entity
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test Customer")
							.with(Customer.EMAIL, "valid@example.com")
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			TestDomain.CustomerValidator validator = new TestDomain.CustomerValidator();
			assertDoesNotThrow(() -> validator.validate(customer));

			// Now modify to violate email format constraint and test validation
			customer.set(Customer.EMAIL, "invalid-email"); // Invalid format
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(customer));
			assertEquals(Customer.EMAIL, exception.attribute());
			assertTrue(exception.getMessage().contains("Invalid email format"));
		}

		@ParameterizedTest
		@DisplayName("phone validation with various formats")
		@ValueSource(strings = {"+1234567890", "1234567890", "+123456789012345"})
		void phoneValidation_validFormats_passes(String phone) {
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test Customer")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.PHONE, phone)
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			TestDomain.CustomerValidator validator = new TestDomain.CustomerValidator();
			assertDoesNotThrow(() -> validator.validate(customer));
		}

		@ParameterizedTest
		@DisplayName("phone validation with invalid formats")
		@ValueSource(strings = {"123", "abcd1234567890", "+12345678901234567890"})
		void phoneValidation_invalidFormats_throwsException(String phone) {
			// First create a valid entity
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test Customer")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.PHONE, "+1234567890") // Valid format initially
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			TestDomain.CustomerValidator validator = new TestDomain.CustomerValidator();
			assertDoesNotThrow(() -> validator.validate(customer));

			// Now modify to violate phone format constraint and test validation
			customer.set(Customer.PHONE, phone);
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(customer));
			assertEquals(Customer.PHONE, exception.attribute());
		}

		@Test
		@DisplayName("birth date validation prevents future dates")
		void birthDateValidation_futureDate_throwsException() {
			// First create a valid entity
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test Customer")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.BIRTH_DATE, LocalDate.now().minusYears(25))
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			TestDomain.CustomerValidator validator = new TestDomain.CustomerValidator();
			assertDoesNotThrow(() -> validator.validate(customer));

			// Now modify to violate birth date constraint and test validation
			customer.set(Customer.BIRTH_DATE, LocalDate.now().plusDays(1));
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(customer));
			assertEquals(Customer.BIRTH_DATE, exception.attribute());
			assertTrue(exception.getMessage().contains("future"));
		}

		@Test
		@DisplayName("country code validation enforces format")
		void countryCodeValidation_invalidFormat_throwsException() {
			// First create a valid entity
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test Customer")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			TestDomain.CustomerValidator validator = new TestDomain.CustomerValidator();
			assertDoesNotThrow(() -> validator.validate(customer));

			// Now modify to violate country code format constraint and test validation
			customer.set(Customer.COUNTRY_CODE, "usa"); // Should be uppercase
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(customer));
			assertEquals(Customer.COUNTRY_CODE, exception.attribute());
		}
	}

	@Nested
	@DisplayName("Cross-Entity Validation")
	class CrossEntityValidationTest {

		@Test
		@DisplayName("stock quantity validation prevents over-ordering")
		void stockQuantityValidation_exceedsStock_throwsException() {
			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Test Product")
							.with(Product.CODE, "PROD-001")
							.with(Product.PRICE, new BigDecimal("50"))
							.with(Product.STOCK_QUANTITY, 5)
							.with(Product.CATEGORY, "OTHER")
							.with(Product.ACTIVE, true)
							.build();

			Entity order = entities.entity(Order.TYPE)
							.with(Order.ID, 1)
							.with(Order.CUSTOMER_ID, 1)
							.with(Order.ORDER_DATE, LocalDateTime.now())
							.with(Order.STATUS, "PENDING")
							.with(Order.TOTAL, new BigDecimal("500"))
							.build();

			Entity orderItem = entities.entity(OrderItem.TYPE)
							.with(OrderItem.ID, 1)
							.with(OrderItem.ORDER_FK, order)
							.with(OrderItem.PRODUCT_FK, product)
							.with(OrderItem.QUANTITY, 10) // Exceeds stock
							.with(OrderItem.UNIT_PRICE, new BigDecimal("50"))
							.with(OrderItem.DISCOUNT, BigDecimal.ZERO)
							.build();

			TestDomain.OrderItemValidator validator = new TestDomain.OrderItemValidator();
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(orderItem, OrderItem.QUANTITY));
			assertEquals(OrderItem.QUANTITY, exception.attribute());
			assertTrue(exception.getMessage().contains("exceeds available stock"));
		}

		@Test
		@DisplayName("inactive product validation prevents ordering")
		void inactiveProductValidation_throwsException() {
			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Test Product")
							.with(Product.CODE, "PROD-001")
							.with(Product.PRICE, new BigDecimal("50"))
							.with(Product.STOCK_QUANTITY, 100)
							.with(Product.CATEGORY, "OTHER")
							.with(Product.ACTIVE, false) // Inactive
							.build();

			Entity orderItem = entities.entity(OrderItem.TYPE)
							.with(OrderItem.ID, 1)
							.with(OrderItem.ORDER_ID, 1)
							.with(OrderItem.PRODUCT_FK, product)
							.with(OrderItem.QUANTITY, 1)
							.with(OrderItem.UNIT_PRICE, new BigDecimal("50"))
							.with(OrderItem.DISCOUNT, BigDecimal.ZERO)
							.build();

			TestDomain.OrderItemValidator validator = new TestDomain.OrderItemValidator();
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(orderItem, OrderItem.QUANTITY));
			assertEquals(OrderItem.PRODUCT_FK, exception.attribute());
			assertTrue(exception.getMessage().contains("inactive product"));
		}

		@Test
		@DisplayName("discount validation based on unit price")
		void discountValidation_exceedsLimit_throwsException() {
			Entity orderItem = entities.entity(OrderItem.TYPE)
							.with(OrderItem.ID, 1)
							.with(OrderItem.ORDER_ID, 1)
							.with(OrderItem.PRODUCT_ID, 1)
							.with(OrderItem.QUANTITY, 1)
							.with(OrderItem.UNIT_PRICE, new BigDecimal("50")) // Under $100
							.with(OrderItem.DISCOUNT, new BigDecimal("60")) // Over 50%
							.build();

			TestDomain.OrderItemValidator validator = new TestDomain.OrderItemValidator();
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(orderItem, OrderItem.DISCOUNT));
			assertEquals(OrderItem.DISCOUNT, exception.attribute());
			assertTrue(exception.getMessage().contains("cannot exceed 50%"));
		}

		@Test
		@DisplayName("discount validation allows high discount for expensive items")
		void discountValidation_highPriceHighDiscount_passes() {
			Entity orderItem = entities.entity(OrderItem.TYPE)
							.with(OrderItem.ID, 1)
							.with(OrderItem.ORDER_ID, 1)
							.with(OrderItem.PRODUCT_ID, 1)
							.with(OrderItem.QUANTITY, 1)
							.with(OrderItem.UNIT_PRICE, new BigDecimal("200")) // Over $100
							.with(OrderItem.DISCOUNT, new BigDecimal("75")) // High discount OK
							.build();

			TestDomain.OrderItemValidator validator = new TestDomain.OrderItemValidator();
			assertDoesNotThrow(() -> validator.validate(orderItem));
		}
	}

	@Nested
	@DisplayName("Conditional Validation")
	class ConditionalValidationTest {

		@Test
		@DisplayName("food products require expiry date")
		void foodProductValidation_missingExpiryDate_throwsException() {
			// First create a valid entity with non-FOOD category
			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Test Food")
							.with(Product.CODE, "FOOD-001")
							.with(Product.PRICE, new BigDecimal("10"))
							.with(Product.STOCK_QUANTITY, 100)
							.with(Product.CATEGORY, "OTHER")
							.with(Product.ACTIVE, true)
							.build();

			TestDomain.ProductValidator validator = new TestDomain.ProductValidator();
			assertDoesNotThrow(() -> validator.validate(product));

			// Now change to FOOD category without expiry date and test validation
			product.set(Product.CATEGORY, "FOOD");
			ValidationException exception = assertThrows(NullValidationException.class,
							() -> validator.validate(product, Product.EXPIRY_DATE));
			assertEquals(Product.EXPIRY_DATE, exception.attribute());
			assertTrue(exception.getMessage().contains("Food products must have an expiry date"));
		}

		@Test
		@DisplayName("non-food products can omit expiry date")
		void nonFoodProductValidation_noExpiryDate_passes() {
			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Test Electronics")
							.with(Product.CODE, "ELEC-001")
							.with(Product.PRICE, new BigDecimal("100"))
							.with(Product.STOCK_QUANTITY, 50)
							.with(Product.CATEGORY, "ELECTRONICS")
							// No expiry date is OK
							.with(Product.ACTIVE, true)
							.build();

			TestDomain.ProductValidator validator = new TestDomain.ProductValidator();
			assertDoesNotThrow(() -> validator.validate(product));
		}
	}

	@Nested
	@DisplayName("State Transition Validation")
	class StateTransitionValidationTest {

		@Test
		@DisplayName("delivered orders can only be cancelled")
		void orderStatusValidation_invalidTransition_throwsException() {
			Entity order = entities.entity(Order.TYPE)
							.with(Order.ID, 1)
							.with(Order.CUSTOMER_ID, 1)
							.with(Order.ORDER_DATE, LocalDateTime.now())
							.with(Order.STATUS, "DELIVERED")
							.with(Order.TOTAL, new BigDecimal("100"))
							.build();

			// Save to establish original state
			order.save();

			// Try to change to PROCESSING
			order.set(Order.STATUS, "PROCESSING");

			TestDomain.OrderValidator validator = new TestDomain.OrderValidator();
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(order));
			assertEquals(Order.STATUS, exception.attribute());
			assertTrue(exception.getMessage().contains("Delivered orders can only be cancelled"));
		}

		@Test
		@DisplayName("cancelled orders cannot be modified")
		void cancelledOrderValidation_anyChange_throwsException() {
			Entity order = entities.entity(Order.TYPE)
							.with(Order.ID, 1)
							.with(Order.CUSTOMER_ID, 1)
							.with(Order.ORDER_DATE, LocalDateTime.now())
							.with(Order.STATUS, "CANCELLED")
							.with(Order.TOTAL, new BigDecimal("100"))
							.build();

			// Save to establish original state
			order.save();

			// Try to change to any other status
			order.set(Order.STATUS, "PENDING");

			TestDomain.OrderValidator validator = new TestDomain.OrderValidator();
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(order));
			assertEquals(Order.STATUS, exception.attribute());
			assertTrue(exception.getMessage().contains("Cancelled orders cannot be modified"));
		}

		@Test
		@DisplayName("valid state transitions are allowed")
		void orderStatusValidation_validTransitions_pass() {
			Entity order = entities.entity(Order.TYPE)
							.with(Order.ID, 1)
							.with(Order.CUSTOMER_ID, 1)
							.with(Order.ORDER_DATE, LocalDateTime.now())
							.with(Order.STATUS, "PENDING")
							.with(Order.TOTAL, new BigDecimal("100"))
							.build();

			TestDomain.OrderValidator validator = new TestDomain.OrderValidator();

			// PENDING -> PROCESSING
			order.save();
			order.set(Order.STATUS, "PROCESSING");
			assertDoesNotThrow(() -> validator.validate(order));

			// PROCESSING -> SHIPPED
			order.save();
			order.set(Order.STATUS, "SHIPPED");
			assertDoesNotThrow(() -> validator.validate(order));

			// SHIPPED -> DELIVERED
			order.save();
			order.set(Order.STATUS, "DELIVERED");
			assertDoesNotThrow(() -> validator.validate(order));

			// DELIVERED -> CANCELLED
			order.save();
			order.set(Order.STATUS, "CANCELLED");
			assertDoesNotThrow(() -> validator.validate(order));
		}
	}

	@Nested
	@DisplayName("Validation Performance")
	class ValidationPerformanceTest {

		@Test
		@DisplayName("validation is not called for unmodified entities")
		void validation_unmodifiedEntity_notCalled() {
			AtomicInteger validationCount = new AtomicInteger(0);

			class CountingValidator implements EntityValidator {
				@Override
				public void validate(Entity entity, Attribute<?> attribute) throws ValidationException {
					validationCount.incrementAndGet();
					EntityValidator.super.validate(entity, attribute);
				}
			}

			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.with(Customer.LOYALTY_POINTS, 0)
							.build();

			// Simulate existing entity
			customer.save();
			customer.set(Customer.ID, 1); // Primary key indicates existing

			CountingValidator validator = new CountingValidator();
			assertDoesNotThrow(() -> validator.validate(customer));
			assertEquals(0, validationCount.get()); // No validation for unmodified
		}

		@Test
		@DisplayName("strict validation validates all values")
		void strictValidation_validatesAllValues() {
			AtomicInteger validationCount = new AtomicInteger(0);

			class CountingValidator implements EntityValidator {

				@Override
				public void validate(Entity entity, Attribute<?> attribute) throws ValidationException {
					validationCount.incrementAndGet();
					EntityValidator.super.validate(entity, attribute);
				}

				@Override
				public boolean strict() {
					return true;
				}
			}

			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.with(Customer.LOYALTY_POINTS, 0)
							.build();

			// Simulate existing entity
			customer.save();
			customer.set(Customer.ID, 1);

			CountingValidator validator = new CountingValidator();
			assertDoesNotThrow(() -> validator.validate(customer));
			assertTrue(validationCount.get() > 0); // Validates even unmodified
		}
	}

	@Nested
	@DisplayName("Null Value Handling")
	class NullValueHandlingTest {

		@ParameterizedTest
		@NullSource
		@DisplayName("optional fields accept null values")
		void optionalFields_nullValues_pass(String nullValue) {
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.PHONE, nullValue) // Optional
							.with(Customer.BIRTH_DATE, null) // Optional
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			EntityValidator validator = new EntityValidator() {};
			assertDoesNotThrow(() -> validator.validate(customer));
		}

		@Test
		@DisplayName("nullable false rejects null values")
		void nullableFalse_nullValue_throwsException() {
			// First create a valid entity
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test Customer")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.build();

			EntityValidator validator = new EntityValidator() {};
			assertDoesNotThrow(() -> validator.validate(customer));

			// Now modify to violate null constraint and test validation
			customer.set(Customer.NAME, null); // Required field
			assertThrows(NullValidationException.class,
							() -> validator.validate(customer));
		}
	}

	@Nested
	@DisplayName("Complex Validation Scenarios")
	class ComplexValidationScenariosTest {

		@Test
		@DisplayName("multiple validation errors on different attributes")
		void multipleValidationErrors_differentAttributes() {
			// Create entity with multiple invalid values using entity(Map) to bypass immediate validation
			Map<Attribute<?>, Object> values = new HashMap<>();
			values.put(Customer.ID, 1);
			values.put(Customer.NAME, Text.leftPad("", 101, 'A')); // Too long
			values.put(Customer.EMAIL, "invalid-email"); // Invalid format
			values.put(Customer.CREDIT_LIMIT, new BigDecimal("-1000")); // Negative
			values.put(Customer.STATUS, "INVALID"); // Invalid item
			values.put(Customer.COUNTRY_CODE, "usa"); // Lowercase

			Entity customer = entities.definition(Customer.TYPE).entity(values);
			TestDomain.CustomerValidator validator = new TestDomain.CustomerValidator();

			// Will throw on first validation error encountered
			// Test the email validation which should fail due to invalid format
			assertThrows(ValidationException.class,
							() -> validator.validate(customer, Customer.EMAIL));
		}

		@Test
		@DisplayName("validation with derived attributes")
		void derivedAttributeValidation_worksCorrectly() {
			Entity customer = entities.entity(Customer.TYPE)
							.with(Customer.ID, 1)
							.with(Customer.NAME, "Test")
							.with(Customer.EMAIL, "test@example.com")
							.with(Customer.BIRTH_DATE, LocalDate.now().minusYears(25))
							.with(Customer.CREDIT_LIMIT, new BigDecimal("1000"))
							.with(Customer.STATUS, "ACTIVE")
							.with(Customer.COUNTRY_CODE, "US")
							.with(Customer.LOYALTY_POINTS, 0)
							.build();

			// Age is derived from birth date
			assertEquals(25, customer.get(Customer.AGE));

			// Validation should pass
			TestDomain.CustomerValidator validator = new TestDomain.CustomerValidator();
			assertDoesNotThrow(() -> validator.validate(customer));
		}

		@Test
		@DisplayName("complex business rule validation")
		void complexBusinessRule_validation() {
			// Create a product with low price
			Entity product = entities.entity(Product.TYPE)
							.with(Product.ID, 1)
							.with(Product.NAME, "Cheap Product")
							.with(Product.CODE, "CHEAP-001")
							.with(Product.PRICE, new BigDecimal("50")) // Under $100
							.with(Product.STOCK_QUANTITY, 100)
							.with(Product.CATEGORY, "OTHER")
							.with(Product.ACTIVE, true)
							.build();

			// Try to apply high discount
			Entity orderItem = entities.entity(OrderItem.TYPE)
							.with(OrderItem.ID, 1)
							.with(OrderItem.ORDER_ID, 1)
							.with(OrderItem.PRODUCT_FK, product)
							.with(OrderItem.QUANTITY, 1)
							.with(OrderItem.UNIT_PRICE, new BigDecimal("50"))
							.with(OrderItem.DISCOUNT, new BigDecimal("60")) // Over 50% for cheap item
							.build();

			TestDomain.OrderItemValidator validator = new TestDomain.OrderItemValidator();
			ValidationException exception = assertThrows(ValidationException.class,
							() -> validator.validate(orderItem, OrderItem.DISCOUNT));
			assertTrue(exception.getMessage().contains("cannot exceed 50%"));

			// Now with expensive product
			product.set(Product.PRICE, new BigDecimal("200"));
			orderItem.set(OrderItem.UNIT_PRICE, new BigDecimal("200"));

			// Same high discount now allowed
			assertDoesNotThrow(() -> validator.validate(orderItem, OrderItem.DISCOUNT));
		}
	}
}