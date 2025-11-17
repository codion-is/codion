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
 * Copyright (c) 2011 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.utilities.Serializer;
import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.common.utilities.item.Item;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public final class AttributeDefinitionTest {

	private static final DomainType DOMAIN_TYPE = DomainType.domainType("domainType");
	private static final EntityType ENTITY_TYPE = DOMAIN_TYPE.entityType("entityType", AttributeDefinitionTest.class.getName());
	private static final EntityType ENTITY_TYPE2 = DOMAIN_TYPE.entityType("entityType2");

	@Test
	void foreignKeyNonUniqueReferencedAttribute() {
		Column<Integer> attribute = ENTITY_TYPE.integerColumn("attr");
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute, attribute));
	}

	@Test
	void foreignKeyDifferentReferenceEntities() {
		Column<Integer> integerAttribute = ENTITY_TYPE2.integerColumn("test");
		Column<Integer> attribute1 = ENTITY_TYPE.integerColumn("attribute1");
		Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, attribute2, integerAttribute));
	}

	@Test
	void foreignKeyDifferentReferenceEntities2() {
		Column<Integer> integerAttribute = ENTITY_TYPE2.integerColumn("test");
		Column<Integer> attribute1 = ENTITY_TYPE.integerColumn("attribute1");
		Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, integerAttribute, attribute2));
	}

	@Test
	void foreignKeyAttributeFromOtherEntity() {
		Column<Integer> attribute1 = ENTITY_TYPE2.integerColumn("attribute1");
		Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute2));
	}

	@Test
	void foreignKeyDuplicateAttribute() {
		Column<Integer> attribute1 = ENTITY_TYPE.integerColumn("attribute1");
		Column<Integer> attribute2 = ENTITY_TYPE.integerColumn("attribute2");
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute2, attribute1, attribute2));
	}

	@Test
	void foreignKeyWithoutReferencedAttribute() {
		Column<Integer> attribute = ENTITY_TYPE.integerColumn("attribute");
		assertThrows(NullPointerException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute, null));
	}

	@Test
	void foreignKeyWithoutReference() {
		Column<Integer> attribute = ENTITY_TYPE.integerColumn("attribute");
		assertThrows(NullPointerException.class, () -> ENTITY_TYPE.foreignKey("attribute", null, attribute));
	}

	@Test
	void intColumnWithDateFormat() {
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.integerColumn("attribute").as().column()
						.format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().datePattern())));
	}

	@Test
	void doubleColumnWithDateFormat() {
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.doubleColumn("attribute").as().column()
						.format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().datePattern())));
	}

	@Test
	void nonTemporalColumnWithFormatPatter() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.integerColumn("attribute").as().column().dateTimePattern("dd-MM-yy"));
	}

	@Test
	void nonDecimalWithRoundingMode() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.integerColumn("attribute")
						.as()
						.column()
						.roundingMode(RoundingMode.CEILING));
	}

	@Test
	void dateColumnWithNumberFormat() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateColumn("attribute")
						.as()
						.column()
						.format(NumberFormat.getIntegerInstance()));
	}

	@Test
	void timestampColumnWithNumberFormat() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateTimeColumn("attribute")
						.as()
						.column()
						.format(NumberFormat.getIntegerInstance()));
	}

	@Test
	void setFractionDigitsNotNumerical() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateColumn("attribute").as().column().fractionDigits(5));
	}

	@Test
	void fractionDigitsNotNumerical() {
		assertEquals(-1, ENTITY_TYPE.localDateColumn("attribute").as().column().build().fractionDigits());
	}

	@Test
	void setNumberGroupingNotNumerical() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateColumn("attribute").as().column().numberGrouping(false));
	}

	@Test
	void setRangeNonNumerical() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.localDateColumn("attribute").as().column().range(5, 6));
	}

	@Test
	void setMaximumLengthNonString() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.stringColumn("attribute").as().column().fractionDigits(5));
	}

	@Test
	void characterMaximumLength() {
		assertEquals(1, ENTITY_TYPE.characterColumn("attribute").as().column().build().maximumLength());
	}

	@Test
	void minimumMaximum() {
		ColumnDefinition.Builder<Double, ?> builder = ENTITY_TYPE.doubleColumn("attribute").as().column();
		assertThrows(IllegalArgumentException.class, () -> builder.range(5, 4));
	}

	@Test
	void setColumnName() {
		assertEquals("hello", ((ColumnDefinition<?>) ENTITY_TYPE.integerColumn("attribute").as().column().name("hello").build()).name());
	}

	@Test
	void setColumnNameNull() {
		assertThrows(NullPointerException.class, () -> ENTITY_TYPE.integerColumn("attribute").as().column().name(null));
	}

	@Test
	void description() {
		final String description = "Here is a description";
		AttributeDefinition<Integer> attributeDefinition = ENTITY_TYPE.integerColumn("attribute")
						.as()
						.column()
						.description(description)
						.build();
		assertEquals(description, attributeDefinition.description().orElse(""));
	}

	@Test
	void mnemonic() {
		final char mnemonic = 'M';
		AttributeDefinition<Integer> attributeDefinition = ENTITY_TYPE.integerColumn("attribute").as().column().mnemonic(mnemonic).build();
		assertEquals(mnemonic, attributeDefinition.mnemonic());
		attributeDefinition = ENTITY_TYPE.integerColumn("attribute").as().column().build();
		assertEquals(0, attributeDefinition.mnemonic());
	}

	@Test
	void subqueryColumns() {
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").as().subquery("select").readOnly(true));
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").as().subquery("select").readOnly(false));
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").as().subquery("select").updatable(false));
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test")
						.as()
						.subquery("select")
						.insertable(false));
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test")
						.as()
						.subquery("select")
						.expression("expression"));
	}

	@Test
	void stringColumnNegativeMaxLength() {
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.stringColumn("attribute").as().column().maximumLength(-4));
	}

	@Test
	void searchableNonVarchar() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.integerColumn("attribute").as().column().searchable(true));
	}

	@Test
	void i18n() throws IOException, ClassNotFoundException {
		AttributeDefinition<Integer> attributeDefinition =
						ENTITY_TYPE.integerColumn("test").as().column().build();

		Locale.setDefault(new Locale("en", "EN"));
		assertEquals("Test", attributeDefinition.caption());
		assertEquals('T', attributeDefinition.mnemonic());
		assertEquals("Description", attributeDefinition.description().orElse(null));

		attributeDefinition = Serializer.deserialize(Serializer.serialize(attributeDefinition));

		Locale.setDefault(new Locale("is", "IS"));
		assertEquals("Prufa", attributeDefinition.caption());
		assertEquals('P', attributeDefinition.mnemonic());
		assertEquals("Lysing", attributeDefinition.description().orElse(null));

		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE2.integerColumn("i18n").as().column()
						.captionResource("key"));
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE2.integerColumn("i18n").as().column()
						.mnemonicResource("key.mnemonic"));

		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.integerColumn("i18n").as().column()
						.captionResource("invalid_key"));
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.integerColumn("i18n").as().column()
						.mnemonicResource("invalid_key.mnemonic"));
	}

	@Test
	void itemAttribute() {
		List<Item<Integer>> itemsDuplicate = Arrays.asList(Item.item(null), Item.item(1), Item.item(2), Item.item(1));
		assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.integerColumn("item").as().column().items(itemsDuplicate));

		List<Item<Integer>> items = Arrays.asList(Item.item(null), Item.item(1), Item.item(2), Item.item(3));
		ValueAttributeDefinition<Integer> attribute = ENTITY_TYPE.integerAttribute("item").as().attribute().items(items).build();
		assertFalse(attribute.validItem(4));
		assertTrue(attribute.validItem(null));
		assertTrue(attribute.validItem(2));
	}
}
