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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.Serial;
import java.util.Collection;
import java.util.Map;

final class SelectSerializer extends StdSerializer<Select> {

	@Serial
	private static final long serialVersionUID = 1;

	private final EntityObjectMapper entityObjectMapper;

	SelectSerializer(EntityObjectMapper entityObjectMapper) {
		super(Select.class);
		this.entityObjectMapper = entityObjectMapper;
	}

	@Override
	public void serialize(Select select, JsonGenerator generator,
												SerializerProvider provider) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("entityType", select.where().entityType().name());
		generator.writeFieldName("where");
		entityObjectMapper.serializeCondition(select.where(), generator);
		generator.writeFieldName("having");
		entityObjectMapper.serializeCondition(select.having(), generator);
		OrderBy orderBy = select.orderBy().orElse(null);
		if (orderBy != null) {
			generator.writeFieldName("orderBy");
			generator.writeStartArray();
			for (OrderBy.OrderByColumn orderByColumn : orderBy.orderByColumns()) {
				generator.writeString(orderByColumn.column().name() +
								":" + (orderByColumn.ascending() ? "asc" : "desc") +
								":" + orderByColumn.nullOrder().name() +
								":" + orderByColumn.ignoreCase());
			}
			generator.writeEndArray();
		}
		int limit = select.limit().orElse(-1);
		if (limit != -1) {
			generator.writeNumberField("limit", limit);
		}
		int offset = select.offset().orElse(-1);
		if (offset != -1) {
			generator.writeNumberField("offset", offset);
		}
		if (select.forUpdate()) {
			generator.writeBooleanField("forUpdate", select.forUpdate());
		}
		if (select.timeout() != 0) {
			generator.writeNumberField("timeout", select.timeout());
		}
		int conditionReferenceDepth = select.referenceDepth().orElse(-1);
		if (conditionReferenceDepth != -1) {
			generator.writeNumberField("referenceDepth", conditionReferenceDepth);
		}
		Map<ForeignKey, Integer> foreignKeyReferenceDepths = select.foreignKeyReferenceDepths();
		if (!foreignKeyReferenceDepths.isEmpty()) {
			generator.writeFieldName("fkReferenceDepth");
			generator.writeStartObject();
			for (Map.Entry<ForeignKey, Integer> entry : foreignKeyReferenceDepths.entrySet()) {
				generator.writeNumberField(entry.getKey().name(), entry.getValue());
			}
			generator.writeEndObject();
		}
		writeAttributeArray(generator, "attributes", select.attributes());
		writeAttributeArray(generator, "include", select.include());
		writeAttributeArray(generator, "exclude", select.exclude());
		generator.writeEndObject();
	}

	private static void writeAttributeArray(JsonGenerator generator, String fieldName,
																					Collection<Attribute<?>> attributes) throws IOException {
		if (!attributes.isEmpty()) {
			generator.writeFieldName(fieldName);
			generator.writeStartArray();
			for (Attribute<?> attribute : attributes) {
				generator.writeString(attribute.name());
			}
			generator.writeEndArray();
		}
	}
}
