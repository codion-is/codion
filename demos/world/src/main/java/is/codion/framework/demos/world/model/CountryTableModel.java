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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.world.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.EntitySearchConditionModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.framework.model.SwingEntityTableModel;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static is.codion.plugin.jasperreports.JasperReports.classPathReport;
import static is.codion.plugin.jasperreports.JasperReports.fillReport;
import static java.util.Collections.singletonMap;

public final class CountryTableModel extends SwingEntityTableModel {

	private static final String CITY_SUBREPORT_PARAMETER = "CITY_SUBREPORT";
	private static final String COUNTRY_REPORT = "country_report.jasper";
	private static final String CITY_REPORT = "city_report.jasper";

	CountryTableModel(EntityConnectionProvider connectionProvider) {
		super(new CountryEditModel(connectionProvider));
		configureCapitalConditionModel();
	}

	public JasperPrint fillCountryReport(ProgressReporter<String> progressReporter) throws ReportException {
		CountryReportDataSource dataSource = new CountryReportDataSource(selectionModel().getSelectedItems(),
						connection(), progressReporter);

		return fillReport(classPathReport(CountryTableModel.class, COUNTRY_REPORT), dataSource, reportParameters());
	}

	private static Map<String, Object> reportParameters() throws ReportException {
		return new HashMap<>(singletonMap(CITY_SUBREPORT_PARAMETER,
						classPathReport(CityTableModel.class, CITY_REPORT).load()));
	}

	private void configureCapitalConditionModel() {
		((EntitySearchConditionModel) conditionModel()
						.attributeModel(Country.CAPITAL_FK))
						.searchModel()
						.condition().set(new CapitalConditionSupplier());
	}

	private final class CapitalConditionSupplier implements Supplier<Condition> {
		@Override
		public Condition get() {
			try {
				return City.ID.in(connection().select(Country.CAPITAL));
			}
			catch (DatabaseException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
