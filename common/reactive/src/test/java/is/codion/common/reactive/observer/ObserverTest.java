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
package is.codion.common.reactive.observer;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Notify;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class ObserverTest {

	@Test
	void observeValue() {
		AtomicInteger oneCounter = new AtomicInteger();
		AtomicInteger twoCounter = new AtomicInteger();
		AtomicInteger nullCounter = new AtomicInteger();
		AtomicReference<Integer> intValue = new AtomicReference<>();
		Value<Integer> value = Value.builder()
						.<Integer>nullable()
						.when(1, oneCounter::incrementAndGet)
						.when(1, intValue::set)
						.when(2, twoCounter::incrementAndGet)
						.when(Objects::isNull, nullCounter::incrementAndGet)
						.when(Objects::isNull, intValue::set)
						.build();

		value.set(1);
		assertEquals(1, intValue.get());
		value.set(2);
		value.clear();
		value.set(null);// no change, no event
		assertNull(intValue.get());
		value.set(1);

		assertEquals(1, intValue.get());
		assertEquals(2, oneCounter.get());
		assertEquals(1, twoCounter.get());
		assertEquals(1, nullCounter.get());

		oneCounter.set(0);
		value = Value.builder()
						.nonNull(0)
						.notify(Notify.SET)
						.build();
		value.when(1).addListener(oneCounter::incrementAndGet);
		value.set(1);
		value.set(1);
		assertEquals(2, oneCounter.get());
	}

	@Test
	void observeState() {
		AtomicInteger trueCounter = new AtomicInteger();
		AtomicInteger falseCounter = new AtomicInteger();

		State state = State.builder()
						.when(true, trueCounter::incrementAndGet)
						.when(false, falseCounter::incrementAndGet)
						.build();

		state.set(true);
		state.set(true);
		state.set(false);

		assertEquals(1, trueCounter.get());
		assertEquals(1, falseCounter.get());

		state = State.builder()
						.notify(Notify.SET)
						.build();

		trueCounter.set(0);
		falseCounter.set(0);
		state.when(true)
						.addListener(trueCounter::incrementAndGet);
		state.when(false)
						.addListener(falseCounter::incrementAndGet);

		state.set(true);
		state.set(true);
		state.set(false);
		state.set(false);

		assertEquals(2, trueCounter.get());
		assertEquals(2, falseCounter.get());
	}
}
