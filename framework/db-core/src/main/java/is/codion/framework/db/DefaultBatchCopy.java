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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.BatchCopy;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultBatchCopy implements BatchCopy {

	private final EntityConnection source;
	private final EntityConnection destination;
	private final Map<EntityType, Condition> entityTypeConditions = new LinkedHashMap<>();
	private final int batchSize;
	private final boolean includePrimaryKeys;

	DefaultBatchCopy(DefaultBuilder builder) {
		this.source = builder.source;
		this.destination = builder.destination;
		this.entityTypeConditions.putAll(builder.entityTypeConditions);
		this.batchSize = builder.batchSize;
		this.includePrimaryKeys = builder.includePrimaryKeys;
	}

	@Override
	public void execute() {
		for (Map.Entry<EntityType, Condition> entityTypeCondition : entityTypeConditions.entrySet()) {
			Select.Builder conditionBuilder = entityTypeCondition.getValue() == null ?
							Select.all(entityTypeCondition.getKey()) :
							Select.where(entityTypeCondition.getValue());
			List<Entity> entities = source.select(conditionBuilder
											.fetchDepth(0)
											.build())
							.stream()
							.map(entity -> includePrimaryKeys ? entity : entity.copy().builder()
											.clearPrimaryKey()
											.build())
							.collect(toList());
			new DefaultBatchInsert.DefaultBuilder(destination, entities.iterator())
							.batchSize(batchSize)
							.execute();
		}
	}

	static final class DefaultBuilder implements Builder {

		private final EntityConnection source;
		private final EntityConnection destination;
		private final Map<EntityType, Condition> entityTypeConditions = new LinkedHashMap<>();

		private boolean includePrimaryKeys = true;
		private int batchSize = 100;

		DefaultBuilder(EntityConnection source, EntityConnection destination) {
			this.source = requireNonNull(source);
			this.destination = requireNonNull(destination);
		}

		@Override
		public Builder entityTypes(EntityType... entityTypes) {
			requireNonNull(entityTypes);
			Arrays.stream(entityTypes).forEach(entityType ->
							entityTypeConditions.put(requireNonNull(entityType), null));
			return this;
		}

		@Override
		public Builder conditions(Condition... conditions) {
			requireNonNull(conditions);
			Arrays.stream(conditions).forEach(condition ->
							entityTypeConditions.put(requireNonNull(condition.entityType()), condition));
			return this;
		}

		@Override
		public Builder batchSize(int batchSize) {
			if (batchSize <= 0) {
				throw new IllegalArgumentException("Batch size must be a positive integer: " + batchSize);
			}
			this.batchSize = batchSize;
			return this;
		}

		@Override
		public Builder includePrimaryKeys(boolean includePrimaryKeys) {
			this.includePrimaryKeys = includePrimaryKeys;
			return this;
		}

		@Override
		public void execute() {
			build().execute();
		}

		@Override
		public BatchCopy build() {
			return new DefaultBatchCopy(this);
		}
	}
}
