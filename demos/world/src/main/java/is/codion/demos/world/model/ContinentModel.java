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
package is.codion.demos.world.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Wildcard;
import is.codion.demos.world.domain.api.World.Continent;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.ModelLink;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.util.Collection;
import java.util.function.Consumer;

public final class ContinentModel extends SwingEntityModel {

	private final DefaultPieDataset<String> surfaceAreaDataset = new DefaultPieDataset<>();
	private final DefaultPieDataset<String> populationDataset = new DefaultPieDataset<>();
	private final DefaultPieDataset<String> gnpDataset = new DefaultPieDataset<>();
	private final DefaultCategoryDataset lifeExpectancyDataset = new DefaultCategoryDataset();

	ContinentModel(EntityConnectionProvider connectionProvider) {
		super(Continent.TYPE, connectionProvider);
		tableModel().items().refresher().result().addConsumer(this::refreshChartDatasets);
		detailModels().add(ModelLink.builder(new CountryModel(connectionProvider))
						.onSelection(new OnContinentsSelected())
						.active(true)
						.build());
	}

	public PieDataset<String> populationDataset() {
		return populationDataset;
	}

	public PieDataset<String> surfaceAreaDataset() {
		return surfaceAreaDataset;
	}

	public PieDataset<String> gnpDataset() {
		return gnpDataset;
	}

	public CategoryDataset lifeExpectancyDataset() {
		return lifeExpectancyDataset;
	}

	private void refreshChartDatasets(Collection<Entity> continents) {
		populationDataset.clear();
		surfaceAreaDataset.clear();
		gnpDataset.clear();
		lifeExpectancyDataset.clear();
		continents.forEach(continent -> {
			String contientName = continent.get(Continent.NAME);
			populationDataset.setValue(contientName, continent.get(Continent.POPULATION));
			surfaceAreaDataset.setValue(contientName, continent.get(Continent.SURFACE_AREA));
			gnpDataset.setValue(contientName, continent.get(Continent.GNP));
			lifeExpectancyDataset.addValue(continent.get(Continent.MIN_LIFE_EXPECTANCY), "Lowest", contientName);
			lifeExpectancyDataset.addValue(continent.get(Continent.MAX_LIFE_EXPECTANCY), "Highest", contientName);
		});
	}

	private static final class CountryModel extends SwingEntityModel {

		private CountryModel(EntityConnectionProvider connectionProvider) {
			super(Country.TYPE, connectionProvider);
			editModel().readOnly().set(true);
			ConditionModel<?> continentCondition =
							tableModel().queryModel().conditions().get(Country.CONTINENT);
			continentCondition.operands().wildcard().set(Wildcard.NONE);
			continentCondition.caseSensitive().set(true);
		}
	}

	private final class OnContinentsSelected implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> continents) {
			SwingEntityTableModel countryTableModel = detailModels().get(Country.TYPE).tableModel();
			Collection<String> continentNames = Entity.values(Continent.NAME, continents);
			if (countryTableModel.queryModel().conditions().get(Country.CONTINENT).set().in(continentNames)) {
				countryTableModel.items().refresh();
			}
		}
	}
}
