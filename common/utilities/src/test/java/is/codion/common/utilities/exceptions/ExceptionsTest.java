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
 * Copyright (c) 2014 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.exceptions;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class ExceptionsTest {

	@Test
	void unwrap() {
		Exception rootException = new Exception();
		Exception wrapper = new RuntimeException(rootException);
		List<Class<? extends Throwable>> toUnwrap = new ArrayList<>();
		toUnwrap.add(RuntimeException.class);
		Throwable unwrapped = Exceptions.unwrap(wrapper, toUnwrap);
		assertSame(rootException, unwrapped);

		rootException = new Exception();
		wrapper = new RuntimeException(new RuntimeException(rootException));
		unwrapped = Exceptions.unwrap(wrapper, toUnwrap);
		assertSame(rootException, unwrapped);

		rootException = new IllegalArgumentException();
		wrapper = new RuntimeException(rootException);
		unwrapped = Exceptions.unwrap(wrapper, toUnwrap);
		assertSame(rootException, unwrapped);

		rootException = new Exception();
		unwrapped = Exceptions.unwrap(rootException, toUnwrap);
		assertSame(rootException, unwrapped);

		rootException = new RuntimeException();
		unwrapped = Exceptions.unwrap(rootException, toUnwrap);
		assertSame(rootException, unwrapped);

		rootException = new TestRuntimeException();
		toUnwrap.add(InvocationTargetException.class);

		unwrapped = Exceptions.unwrap(new RuntimeException(new InvocationTargetException(rootException)), toUnwrap);
		assertSame(rootException, unwrapped);

		rootException = new TestRuntimeException();

		unwrapped = Exceptions.unwrap(new InvocationTargetException(new InvocationTargetException(new RuntimeException(rootException))), toUnwrap);
		assertSame(rootException, unwrapped);
	}

	@Test
	void runtime() {
		Exception exception = new Exception("Testing");
		RuntimeException runtime = Exceptions.runtime(exception);
		assertSame(exception, runtime.getCause());
		assertEquals(exception.getMessage(), runtime.getMessage());
		assertTrue(Objects.deepEquals(exception.getStackTrace(), runtime.getStackTrace()));
	}

	private static final class TestRuntimeException extends RuntimeException {
		private TestRuntimeException() {}
	}
}
