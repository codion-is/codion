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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

/**
 * A column definition representing an audit column that tracks data modification events.
 * <p>
 * Audit columns automatically capture information about when and by whom data was
 * inserted or updated. They are typically used for compliance, debugging, and
 * data lineage tracking.
 * <p>
 * Audit columns are assumed to be automatically populated during insert and update operations by the underlying database:
 * {@snippet :
 * public class Store extends DefaultDomain {
 *     
 *     interface Product {
 *         EntityType TYPE = DOMAIN.entityType("store.product");
 *         
 *         // Regular columns
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<String> NAME = TYPE.stringColumn("name");
 *         Column<BigDecimal> PRICE = TYPE.bigDecimalColumn("price");
 *         
 *         // Audit columns
 *         Column<String> CREATED_BY = TYPE.stringColumn("created_by");
 *         Column<LocalDateTime> CREATED_DATE = TYPE.localDateTimeColumn("created_date");
 *         Column<String> UPDATED_BY = TYPE.stringColumn("updated_by");
 *         Column<LocalDateTime> UPDATED_DATE = TYPE.localDateTimeColumn("updated_date");
 *     }
 *     
 *     void defineProduct() {
 *         Product.TYPE.define(
 *                 Product.ID.define()
 *                     .primaryKey(),
 *                 Product.NAME.define()
 *                     .column(),
 *                 Product.PRICE.define()
 *                     .column(),
 *                 
 *                 // Audit columns for INSERT operations
 *                 Product.CREATED_BY.define()
 *                     .auditColumn()
 *                     .auditAction(AuditAction.INSERT)
 *                     .caption("Created By"),
 *                 Product.CREATED_DATE.define()
 *                     .auditColumn()
 *                     .auditAction(AuditAction.INSERT)
 *                     .caption("Created Date"),
 *                 
 *                 // Audit columns for UPDATE operations
 *                 Product.UPDATED_BY.define()
 *                     .auditColumn()
 *                     .auditAction(AuditAction.UPDATE)
 *                     .caption("Updated By"),
 *                 Product.UPDATED_DATE.define()
 *                     .auditColumn()
 *                     .auditAction(AuditAction.UPDATE)
 *                     .caption("Updated Date"))
 *             .build();
 *     }
 * }
 * 
 * // Usage - audit columns are populated automatically
 * Entity product = entities.builder(Product.TYPE)
 *     .with(Product.NAME, "Laptop")
 *     .with(Product.PRICE, new BigDecimal("999.99"))
 *     .build();
 * 
 * // Insert - CREATED_BY and CREATED_DATE are populated automatically
 * connection.insert(product);
 * 
 * String createdBy = product.get(Product.CREATED_BY);         // Current user
 * LocalDateTime createdDate = product.get(Product.CREATED_DATE); // Current timestamp
 * 
 * // Update - UPDATED_BY and UPDATED_DATE are populated automatically
 * product.set(Product.PRICE, new BigDecimal("899.99"));
 * connection.update(product);
 * 
 * String updatedBy = product.get(Product.UPDATED_BY);         // Current user
 * LocalDateTime updatedDate = product.get(Product.UPDATED_DATE); // Current timestamp
 * 
 * // Audit columns provide full modification history
 * System.out.println("Product created by " + createdBy + " on " + createdDate);
 * System.out.println("Product updated by " + updatedBy + " on " + updatedDate);
 * }
 * @param <T> the underlying type
 * @see AuditAction
 * @see #auditAction()
 */
public interface AuditColumnDefinition<T> extends ColumnDefinition<T> {

	/**
	 * @return the audit action this column represents
	 */
	AuditAction auditAction();
}
