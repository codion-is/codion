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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.common.reactive.value.Value.Validator;
import is.codion.demos.chinook.domain.api.Chinook.Track.RaisePriceParameters;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.model.worker.ProgressWorker.ResultTaskHandler;
import is.codion.swing.framework.model.SwingEntityConditions;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;

import java.math.BigDecimal;
import java.util.Collection;

import static is.codion.demos.chinook.domain.api.Chinook.Track;
import static is.codion.framework.model.EntityQueryModel.entityQueryModel;

public final class TrackTableModel extends SwingEntityTableModel {

	private static final int DEFAULT_LIMIT = 1_000;
	private static final int MAXIMUM_LIMIT = 10_000;

	public TrackTableModel(EntityConnectionProvider connectionProvider) {
		super(new TrackEditModel(connectionProvider),
						entityQueryModel(EntityConditionModel.builder()
										.entityType(Track.TYPE)
										.connectionProvider(connectionProvider)
										.conditions(new TrackConditions(connectionProvider))
										.build()));
		editor().enabled().set(true);
		configureLimit();
	}

	public RaisePriceTask raisePriceOfSelected(BigDecimal increase) {
		return new RaisePriceTask(increase);
	}

	private void configureLimit() {
		query().limit().set(DEFAULT_LIMIT);
		query().limit().addListener(items()::refresh);
		query().limit().addValidator(new LimitValidator());
	}

	// tag::raisePrice[]
	public final class RaisePriceTask implements ResultTaskHandler<Collection<Entity>> {

		private final BigDecimal increase;

		private RaisePriceTask(BigDecimal increase) {
			this.increase = increase;
		}

		@Override
		public Collection<Entity> execute() throws Exception {
			Collection<Long> trackIds = Entity.values(Track.ID, selection().items().get());

			return connection().execute(Track.RAISE_PRICE, new RaisePriceParameters(trackIds, increase));
		}

		@Override
		public void onResult(Collection<Entity> result) {
			replace(result);
		}
	}
	// end::raisePrice[]

	private static final class LimitValidator implements Validator<Integer> {

		@Override
		public void validate(Integer limit) {
			if (limit != null && limit > MAXIMUM_LIMIT) {
				// The error message is never displayed, so not required
				throw new IllegalArgumentException();
			}
		}
	}

	private static class TrackConditions extends SwingEntityConditions {

		private TrackConditions(EntityConnectionProvider connectionProvider) {
			super(Track.TYPE, connectionProvider);
		}

		@Override
		protected ForeignKeyConditionModel condition(ForeignKey foreignKey) {
			if (foreignKey.equals(Track.MEDIATYPE_FK)) {
				return SwingForeignKeyConditionModel.builder(foreignKey)
								.equalComboBoxModel(createEqualComboBoxModel(Track.MEDIATYPE_FK))
								.build();
			}

			return super.condition(foreignKey);
		}
	}
}
