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
package is.codion.manual.common;

import is.codion.common.utilities.scheduler.TaskScheduler;

import java.util.concurrent.TimeUnit;

public final class Scheduler {

	void taskSchedulerExample() {
		//tag::taskScheduler[]
		// Build a scheduler that runs a task every 5 seconds
		TaskScheduler scheduler =
						TaskScheduler.builder()
										.task(() -> System.out.println("Running scheduled task"))
										.interval(5, TimeUnit.SECONDS)
										.initialDelay(10) // Wait 10 seconds before first execution
										.name("My Task Scheduler") // Name for debugging
										.build();

		// Start the scheduler
		scheduler.start();

		// Check if it's running
		boolean running = scheduler.running();

		// Stop the scheduler when done
		scheduler.stop();
		//end::taskScheduler[]
	}

	void taskSchedulerAutoStart() {
		//tag::taskSchedulerAutoStart[]
		// Build and start in one step
		TaskScheduler scheduler =
						TaskScheduler.builder()
										.task(this::performMaintenance)
										.interval(30, TimeUnit.SECONDS)
										.name("Maintenance Task")
										.start(); // Builds and starts immediately
		//end::taskSchedulerAutoStart[]
	}

	void taskSchedulerCustomThreadFactory() {
		//tag::taskSchedulerCustomThreadFactory[]
		// Use a custom ThreadFactory for advanced control
		TaskScheduler scheduler =
						TaskScheduler.builder()
										.task(() -> System.out.println("Custom thread task"))
										.interval(1, TimeUnit.MINUTES)
										.threadFactory(runnable -> {
											Thread thread = new Thread(runnable);
											thread.setDaemon(true);
											thread.setPriority(Thread.MIN_PRIORITY);
											thread.setName("Custom Task Thread");
											return thread;
										})
										.start();
		//end::taskSchedulerCustomThreadFactory[]
	}

	private void performMaintenance() {
		// Maintenance logic here
	}
}
