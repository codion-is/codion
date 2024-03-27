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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.plugin.jasperreports.JasperReports;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;

public final class Store extends DefaultDomain {

	public static final DomainType DOMAIN = domainType(Store.class);

	public interface Address {
		EntityType TYPE = DOMAIN.entityType("store.address");

		Column<Long> ID = TYPE.longColumn("id");
		Column<String> STREET = TYPE.stringColumn("street");
		Column<String> CITY = TYPE.stringColumn("city");
		Column<Boolean> VALID = TYPE.booleanColumn("valid");
	}

	public interface Customer {
		EntityType TYPE = DOMAIN.entityType("store.customer");

		Column<String> ID = TYPE.stringColumn("id");
		Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
		Column<String> LAST_NAME = TYPE.stringColumn("last_name");
		Column<String> EMAIL = TYPE.stringColumn("email");
		Column<Boolean> ACTIVE = TYPE.booleanColumn("active");

		ReportType<JasperReport, JasperPrint, Map<String, Object>> REPORT =
						JasperReports.reportType("customer_report");
	}

	public interface CustomerAddress {
		EntityType TYPE = DOMAIN.entityType("store.customer_address");

		Column<Long> ID = TYPE.longColumn("id");
		Column<String> CUSTOMER_ID = TYPE.stringColumn("customer_id");
		Column<Long> ADDRESS_ID = TYPE.longColumn("address_id");

		ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
		ForeignKey ADDRESS_FK = TYPE.foreignKey("address_fk", ADDRESS_ID, Address.ID);
	}

	public Store() {
		super(DOMAIN);
		customer();
		address();
		customerAddress();
	}

	private void customer() {
		// tag::customer[]
		add(Customer.TYPE.define(
										Customer.ID.define()
														.primaryKey(),
										Customer.FIRST_NAME.define()
														.column()
														.caption("First name")
														.nullable(false)
														.maximumLength(40),
										Customer.LAST_NAME.define()
														.column()
														.caption("Last name")
														.nullable(false)
														.maximumLength(40),
										Customer.EMAIL.define()
														.column()
														.caption("Email"),
										Customer.ACTIVE.define()
														.column()
														.caption("Active")
														.columnHasDefaultValue(true)
														.defaultValue(true))
						.keyGenerator(new UUIDKeyGenerator())
						// tag::customerStringFactory[]
						.stringFactory(new CustomerToString())
						// end::customerStringFactory[]
						.caption("Customer"));
		// end::customer[]
	}

	private void address() {
		// tag::address[]
		add(Address.TYPE.define(
										Address.ID.define()
														.primaryKey(),
										Address.STREET.define()
														.column()
														.caption("Street")
														.nullable(false)
														.maximumLength(120),
										Address.CITY.define()
														.column()
														.caption("City")
														.nullable(false)
														.maximumLength(50),
										Address.VALID.define()
														.column()
														.caption("Valid")
														.columnHasDefaultValue(true)
														.nullable(false))
						.stringFactory(StringFactory.builder()
										.value(Address.STREET)
										.text(", ")
										.value(Address.CITY)
										.build())
						.keyGenerator(identity())
						.smallDataset(true)
						.caption("Address"));
		// end::address[]
	}

	private void customerAddress() {
		// tag::customerAddress[]
		add(CustomerAddress.TYPE.define(
										CustomerAddress.ID.define()
														.primaryKey(),
										CustomerAddress.CUSTOMER_ID.define()
														.column()
														.nullable(false),
										CustomerAddress.CUSTOMER_FK.define()
														.foreignKey()
														.caption("Customer"),
										CustomerAddress.ADDRESS_ID.define()
														.column()
														.nullable(false),
										CustomerAddress.ADDRESS_FK.define()
														.foreignKey()
														.caption("Address"))
						.keyGenerator(identity())
						.caption("Customer address"));
		// end::customerAddress[]
	}

	// tag::toString[]
	private static final class CustomerToString implements Function<Entity, String>, Serializable {

		private static final long serialVersionUID = 1;

		@Override
		public String apply(Entity customer) {
			StringBuilder builder =
							new StringBuilder(customer.get(Customer.LAST_NAME))
											.append(", ")
											.append(customer.get(Customer.FIRST_NAME));
			if (customer.isNotNull(Customer.EMAIL)) {
				builder.append(" <")
								.append(customer.get(Customer.EMAIL))
								.append(">");
			}

			return builder.toString();
		}
	}
	// end::toString[]

	// tag::keyGenerator[]
	private static final class UUIDKeyGenerator implements KeyGenerator {

		@Override
		public void beforeInsert(Entity entity, DatabaseConnection connection) {
			entity.put(Customer.ID, UUID.randomUUID().toString());
		}
	}
	// end::keyGenerator[]
}
