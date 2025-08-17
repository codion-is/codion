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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.scaler;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

/**
 * Provides a way to scale UIs.
 */
public interface Scaler {

	/**
	 * Specifies the global UI scaling ratio in percentages, with 100 meaning no scaling.<br>
	 * 85 = decrease the default size by 15%<br>
	 * 100 = use the default size<br>
	 * 125 = increase the default size by 25%<br>
	 */
	PropertyValue<Integer> SCALING = Configuration.integerValue(Scaler.class.getName() + ".scaling", 100);

	/**
	 * Applies the scale ratio specified by {@link #SCALING}
	 */
	void apply();

	/**
	 * @param lookAndFeelClassName the look and feel classname
	 * @return true if this {@link Scaler} supports the given look and feel
	 */
	boolean supports(String lookAndFeelClassName);

	/**
	 * @param size the size to scale
	 * @return the size scaled according to {@link #SCALING}
	 */
	static int scale(int size) {
		int scaling = Scaler.SCALING.getOrThrow();
		if (scaling != 100) {
			return Math.round(size * (scaling / 100f));
		}

		return size;
	}

	/**
	 * @param lookAndFeelClassName the look and feel classname
	 * @return the first available {@link Scaler} supporting the given look and feel
	 */
	static Optional<Scaler> instance(String lookAndFeelClassName) {
		requireNonNull(lookAndFeelClassName);
		try {
			return stream(ServiceLoader.load(Scaler.class).spliterator(), false)
							.filter(scaler -> scaler.supports(lookAndFeelClassName))
							.findFirst();
		}
		catch (ServiceConfigurationError e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
	}
}
