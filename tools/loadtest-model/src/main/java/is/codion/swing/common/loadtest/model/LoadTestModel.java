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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.loadtest.model;

import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.LoadTest.Scenario.Result;
import is.codion.common.state.State;
import is.codion.swing.common.loadtest.model.LoadTestModel.ApplicationRow.ColumnId;
import is.codion.swing.common.model.component.table.FilterTableModel;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Collects chart data for a load test.
 * @param <T> the load test application type
 * @see #loadTestModel(LoadTest)
 */
public interface LoadTestModel<T> {

	/**
	 * @return the underlying {@link LoadTest} instance
	 */
	LoadTest<T> loadTest();

	/**
	 * @return a table model for displaying the active application instances
	 */
	FilterTableModel<ApplicationRow, ColumnId> applicationTableModel();

	/**
	 * @return the chart data update interval in milliseconds
	 */
	int getUpdateInterval();

	/**
	 * @param updateInterval the chart data update interval in milliseconds
	 */
	void setUpdateInterval(int updateInterval);

	/**
	 * @return the state controlling whether this load test collects chart data
	 */
	State collectChartData();

	/**
	 * @return the state controlling whether the applications table model is automatically refreshed
	 */
	State autoRefreshApplications();

	/**
	 * Removes the selected applications
	 */
	void removeSelectedApplications();

	/**
	 * Clears the accumulated chart data
	 */
	void clearCharts();

	/**
	 * @param scenarioName the scenario name
	 * @return the total number of runs since the counter was reset
	 */
	int totalRunCount(String scenarioName);

	/**
	 * @param scenarioName the scenario name
	 * @return the total number of successful runs since the counter was reset
	 */
	int successfulRunCount(String scenarioName);

	/**
	 * @param scenarioName the scenario name
	 * @return the total number of unsuccessful runs since the counter was reset
	 */
	int unsuccessfulRunCount(String scenarioName);

	/**
	 * Resets the run counters
	 */
	void resetRunCounter();

	/**
	 * @param scenarioName the scenario name
	 * @return the exceptions collected from running the scenario
	 */
	List<Exception> exceptions(String scenarioName);

	/**
	 * Clears the exceptions collected from running the given scenario
	 * @param scenarioName the scenario name
	 */
	void clearExceptions(String scenarioName);

	/**
	 * @param name the scenario name
	 * @return a dataset plotting the average scenario duration
	 */
	IntervalXYDataset scenarioDurationDataset(String name);

	/**
	 * @return a dataset plotting the think time
	 */
	XYDataset thinkTimeDataset();

	/**
	 * @return a dataset plotting the number of active applications
	 */
	XYDataset numberOfApplicationsDataset();

	/**
	 * @return a dataset plotting the number of runs each usage scenario is being run per second
	 */
	XYDataset scenarioDataset();

	/**
	 * @return a dataset plotting the memory usage of this load test model
	 */
	XYDataset memoryUsageDataset();

	/**
	 * @return a dataset plotting the system load of this load test model
	 */
	XYDataset systemLoadDataset();

	/**
	 * @return a dataset plotting the failure rate of each usage scenario
	 */
	XYDataset scenarioFailureDataset();

	/**
	 * @param loadTest the load test
	 * @param <T> the application type
	 * @return a new {@link LoadTestModel} instance based on the given load test
	 */
	static <T> LoadTestModel<T> loadTestModel(LoadTest<T> loadTest) {
		return new DefaultLoadTestModel<>(loadTest);
	}

	/**
	 * Table model row describing a load test application.
	 */
	interface ApplicationRow {

		/**
		 * Identifies the application table columns
		 */
		enum ColumnId {
			NAME,
			USERNAME,
			SCENARIO,
			SUCCESSFUL,
			DURATION,
			EXCEPTION,
			MESSAGE,
			CREATED
		}

		/**
		 * @return the name of the application
		 */
		String name();

		/**
		 * @return the application username
		 */
		String username();

		/**
		 * @return the application create time
		 */
		LocalDateTime created();

		/**
		 * @return the available run results
		 */
		List<Result> results();
	}
}
