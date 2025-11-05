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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.common.reactive.value.Value.Validator;
import is.codion.demos.chinook.domain.api.Chinook.Track.RaisePriceParameters;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.framework.model.SwingEntityConditionModelFactory;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;

import java.math.BigDecimal;
import java.util.Collection;

import static is.codion.demos.chinook.domain.api.Chinook.Track;
import static is.codion.framework.model.EntityQueryModel.entityQueryModel;
import static is.codion.framework.model.EntityTableConditionModel.entityTableConditionModel;

public final class TrackTableModel extends SwingEntityTableModel {

	private static final int DEFAULT_LIMIT = 1_000;
	private static final int MAXIMUM_LIMIT = 10_000;

	public TrackTableModel(EntityConnectionProvider connectionProvider) {
		super(new TrackEditModel(connectionProvider),
						entityQueryModel(entityTableConditionModel(Track.TYPE, connectionProvider,
										new TrackColumnConditionFactory(connectionProvider))));
		editable().set(true);
		configureLimit();
	}

	// tag::raisePrice[]
	public void raisePriceOfSelected(BigDecimal increase) {
		if (selection().empty().not().is()) {
			Collection<Long> trackIds = Entity.values(Track.ID, selection().items().get());
			Collection<Entity> result = connection()
							.execute(Track.RAISE_PRICE, new RaisePriceParameters(trackIds, increase));
			replace(result);
		}
	}
	// end::raisePrice[]

	private void configureLimit() {
		queryModel().limit().set(DEFAULT_LIMIT);
		queryModel().limit().addListener(items()::refresh);
		queryModel().limit().addValidator(new LimitValidator());
	}

	private static final class LimitValidator implements Validator<Integer> {

		@Override
		public void validate(Integer limit) {
			if (limit != null && limit > MAXIMUM_LIMIT) {
				// The error message is never displayed, so not required
				throw new IllegalArgumentException();
			}
		}
	}

	private static class TrackColumnConditionFactory extends SwingEntityConditionModelFactory {

		private TrackColumnConditionFactory(EntityConnectionProvider connectionProvider) {
			super(Track.TYPE, connectionProvider);
		}

		@Override
		protected ForeignKeyConditionModel conditionModel(ForeignKey foreignKey) {
			if (foreignKey.equals(Track.MEDIATYPE_FK)) {
				return SwingForeignKeyConditionModel.builder()
								.equalComboBoxModel(createEqualComboBoxModel(Track.MEDIATYPE_FK))
								.build();
			}

			return super.conditionModel(foreignKey);
		}
	}
}
