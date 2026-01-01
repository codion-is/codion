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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.domain.api.Chinook.ArtistRevenue;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.*;

public final class AnalyticsModel {

	private static final ResourceBundle BUNDLE = getBundle(AnalyticsModel.class.getName());

	private final EntityConnectionProvider connectionProvider;
	private final SalesComparison salesComparison;
	private final TopArtists topArtists;
	private final TopArtistRevenue topArtistRevenue;

	public AnalyticsModel(EntityConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
		this.salesComparison = new SalesComparison();
		this.topArtists = new TopArtists();
		this.topArtistRevenue = new TopArtistRevenue();
	}

	public EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	public void clear() {
		salesComparison.dataset.clear();
		topArtists.dataset.clear();
		topArtistRevenue.dataset.clear();
	}

	public void refresh() {
		salesComparison.refresh();
		topArtists.refresh();
		topArtistRevenue.refresh();
	}

	public SalesComparison salesComparison() {
		return salesComparison;
	}

	public TopArtists topArtists() {
		return topArtists;
	}

	public TopArtistRevenue topArtistRevenue() {
		return topArtistRevenue;
	}

	public final class SalesComparison {

		private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		private SalesComparison() {}

		public CategoryDataset dataset() {
			return dataset;
		}

		private void refresh() {
			List<Entity> invoices = connectionProvider.connection().select(all(Invoice.TYPE));
			Map<Integer, Map<Month, BigDecimal>> revenueByYearAndMonth = invoices.stream()
							.collect(groupingBy(invoice -> invoice.get(Invoice.DATE).getYear(),
											groupingBy(invoice -> invoice.get(Invoice.DATE).getMonth(),
															reducing(BigDecimal.ZERO,
																			invoice -> invoice.get(Invoice.TOTAL),
																			BigDecimal::add))));

			dataset.clear();

			revenueByYearAndMonth.forEach((year, monthlyRevenue) -> {
				for (Month month : Month.values()) {
					BigDecimal revenue = monthlyRevenue.getOrDefault(month, BigDecimal.ZERO);
					String monthLabel = month.getDisplayName(TextStyle.SHORT, Locale.getDefault());
					dataset.addValue(revenue, String.valueOf(year), monthLabel);
				}
			});
		}
	}

	public final class TopArtists {

		private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		private final EntityComboBoxModel genreComboBoxModel;

		private TopArtists() {
			this.genreComboBoxModel = EntityComboBoxModel.builder()
							.entityType(Chinook.Genre.TYPE)
							.connectionProvider(connectionProvider)
							.onSelection(this::refresh)
							.nullCaption(BUNDLE.getString("all_genres"))
							.refresh(true)
							.build();
		}

		public EntityComboBoxModel genreComboBoxModel() {
			return genreComboBoxModel;
		}

		public CategoryDataset dataset() {
			return dataset;
		}

		private void refresh() {
			refresh(genreComboBoxModel.selection().item().get());
		}

		private void refresh(Entity genre) {
			Condition condition = genre == null ? all(Track.TYPE) : Track.GENRE_FK.equalTo(genre);
			List<Entity> tracks = connectionProvider.connection().select(condition);

			Map<String, Double> avgRatingByArtist = tracks.stream()
							.collect(groupingBy(track -> track.get(Track.ARTIST_NAME),
											averagingDouble(track -> track.get(Track.RATING))));

			dataset.clear();

			avgRatingByArtist.entrySet().stream()
							.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
							.limit(15)
							.forEach(entry ->
											dataset.addValue(entry.getValue(), BUNDLE.getString("rating"), entry.getKey()));
		}
	}

	public final class TopArtistRevenue {

		private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		private TopArtistRevenue() {}

		public CategoryDataset dataset() {
			return dataset;
		}

		private void refresh() {
			List<Entity> artistRevenues = connectionProvider.connection().select(Select.all(ArtistRevenue.TYPE)
							.limit(15)
							.build());

			dataset.clear();

			artistRevenues.forEach(artistRevenue ->
							dataset.addValue(artistRevenue.get(ArtistRevenue.TOTAL_REVENUE),
											BUNDLE.getString("revenue"), artistRevenue.get(ArtistRevenue.NAME)));
		}
	}
}
