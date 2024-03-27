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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.Memory;
import is.codion.common.scheduler.TaskScheduler;

import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.util.concurrent.TimeUnit;

/**
 * A text field containing information about the memory usage in KB.
 */
public final class MemoryUsageField extends JTextField {

	/**
	 * @param updateIntervalMilliseconds the update interval
	 */
	public MemoryUsageField(int updateIntervalMilliseconds) {
		super(8);
		setEditable(false);
		setHorizontalAlignment(SwingConstants.CENTER);
		TaskScheduler.builder(() -> SwingUtilities.invokeLater(() -> setText(Memory.memoryUsage())))
						.interval(updateIntervalMilliseconds, TimeUnit.MILLISECONDS)
						.start();
	}
}
