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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.javadoc;

import is.codion.common.utilities.scheduler.TaskScheduler;

import java.util.concurrent.TimeUnit;

import static is.codion.common.utilities.scheduler.TaskScheduler.builder;

/**
 * The {@link TaskScheduler} javadoc snippets, each the region of the same name.
 */
final class TaskSchedulerSnippets {

  void usage() {
    TaskScheduler scheduler = builder() // @start region=usage
            .task(() -> System.out.println("Running wild..."))
            .interval(2, TimeUnit.SECONDS)
            .build();

    scheduler.start();
    // ...
    scheduler.interval().set(1); // task restarted using the new interval
    // ...
    scheduler.stop(); // @end
  }
}
