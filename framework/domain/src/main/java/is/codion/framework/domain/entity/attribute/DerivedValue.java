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

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Responsible for providing values derived from other attribute values.
 * <p>
 * Derived attributes compute their values from other attributes within the same entity
 * or from related entities. They provide calculated fields, formatting, concatenation,
 * and other derived values without being stored in the database.
 * <p>
 * Derived attributes are defined using value providers that receive source values
 * and compute the derived result:
 * {@snippet class = "is.codion.demos.chinook.javadoc.DerivedValueSnippets" region = "usage" :
 * // Computes the invoice line total from the quantity and unit price // @start region=usage
 * class InvoiceLineTotal implements DerivedValue<BigDecimal> {
 *
 *   @Override
 *   public BigDecimal from(SourceValues values) {
 *     Integer quantity = values.get(InvoiceLine.QUANTITY);
 *     BigDecimal unitPrice = values.get(InvoiceLine.UNITPRICE);
 *     if (unitPrice == null || quantity == null) {
 *       return null;
 *     }
 *
 *     return unitPrice.multiply(BigDecimal.valueOf(quantity));
 *   }
 * }
 *
 * // In the entity definition
 * InvoiceLine.TOTAL.as()
 *         .derived()
 *         .from(InvoiceLine.QUANTITY, InvoiceLine.UNITPRICE)
 *         .with(new InvoiceLineTotal())
 *         .caption("Total");
 *
 * // Usage
 * Entity invoiceLine = entities.entity(InvoiceLine.TYPE)
 *         .with(InvoiceLine.UNITPRICE, BigDecimal.valueOf(0.99))
 *         .with(InvoiceLine.QUANTITY, 2)
 *         .build();
 *
 * // Derived values are computed automatically
 * BigDecimal total = invoiceLine.get(InvoiceLine.TOTAL); // 1.98 // @end}
 * @param <T> the value type
 * @see #sourceValues(Attribute, Map)
 * @see DerivedValue.SourceValues
 */
@FunctionalInterface
public interface DerivedValue<T> extends Serializable {

	/**
	 * <p>Must be total, that is, return a value or null for every combination of source values, including
	 * all null, which a new entity presents. A cached derived value is computed for every attribute of an
	 * entity being made immutable, not only for the ones a caller happens to read, so a provider throwing
	 * on some combination throws from {@link is.codion.framework.domain.entity.Entity#immutable()} rather
	 * than from the read it would once have failed.
	 * @param values the source values, mapped to their respective attributes
	 * @return the derived value
	 */
	@Nullable
	T from(SourceValues values);

	/**
	 * Provides the source values from which to derive a value.
	 */
	sealed interface SourceValues permits DefaultSourceValues {

		/**
		 * Returns the value associated with the given source attribute.
		 * @param attribute the source attribute which value to retrieve
		 * @param <T> the value type
		 * @return the value associated with the given source attribute
		 * @throws IllegalArgumentException in case the given attribute is not a source attribute
		 */
		@Nullable
		<T> T get(Attribute<T> attribute);

		/**
		 * Returns the source value associated with the given attribute or an empty {@link Optional}
		 * if the associated value is null or if the given attribute is not a source attribute
		 * @param attribute the attribute which value to retrieve
		 * @param <T> the value type
		 * @return the value associated with attribute
		 */
		<T> Optional<T> optional(Attribute<T> attribute);
	}

	/**
	 * @param derivedAttribute the derived attribute
	 * @param values the values
	 * @return a new {@link SourceValues} instance
	 */
	static SourceValues sourceValues(Attribute<?> derivedAttribute, Map<Attribute<?>, Object> values) {
		return new DefaultSourceValues(requireNonNull(derivedAttribute), requireNonNull(values));
	}
}
