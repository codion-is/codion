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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.proxy;

import is.codion.common.utilities.TypeReference;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public final class ProxyBuilderTest {

	@Test
	void test() {
		//not an interface
		assertThrows(IllegalArgumentException.class, () -> ProxyBuilder.of(String.class));
		try {
			//non-existing method
			ProxyBuilder.of(List.class)
							.method("nonexistingMethod", parameters -> null);
			fail();
		}
		catch (Exception e) {
			assertInstanceOf(NoSuchMethodException.class, e.getCause());
		}

		List<String> proxyInstance = ProxyBuilder.of(new TypeReference<List<String>>() {})
						.delegate(emptyList())
						.method("toString", parameters -> "toStringResult")
						.method("equals", Object.class, parameters ->
										parameters.arguments().get(0) == emptyList())
						.build();
		assertEquals("toStringResult", proxyInstance.toString());
		assertNotEquals(proxyInstance, proxyInstance);
		assertEquals(proxyInstance, emptyList());
		proxyInstance.hashCode();
		assertTrue(proxyInstance::isEmpty);

		//delegate is an immutable list
		assertThrows(UnsupportedOperationException.class, () -> proxyInstance.add("test"));

		List<Object> proxyInstance2 = ProxyBuilder.of(List.class)
						.delegate(emptyList())
						.build();
		assertEquals(proxyInstance2, proxyInstance2);

		List<String> proxyInstance3 = ProxyBuilder.of(new TypeReference<List<String>>() {})
						.method("size", parameters -> parameters.delegate().size())
						.build();
		assertThrows(IllegalStateException.class, proxyInstance3::size);//no delegate
		assertThrows(UnsupportedOperationException.class, () -> proxyInstance3.add("testing"));
	}

	@Test
	void example() {
		List<String> list = new ArrayList<>();

		List<String> listProxy = ProxyBuilder.of(new TypeReference<List<String>>() {})
						.delegate(list)
						.method("add", Object.class, parameters -> {
							String item = (String) parameters.arguments().get(0);
							System.out.println("Adding: " + item);

							return parameters.delegate().add(item);
						})
						.method("remove", Object.class, parameters -> {
							Object item = parameters.arguments().get(0);
							System.out.println("Removing: " + item);

							return parameters.delegate().remove(item);
						})
						.method("equals", Object.class, parameters -> {
							Object object = parameters.arguments().get(0);
							System.out.println("Equals: " + object);

							return parameters.delegate().equals(object);
						})
						.method("size", parameters -> {
							System.out.println("Size");

							return parameters.delegate().size();
						})
						.build();

		listProxy.add("hey");
		listProxy.add("whatsup");
		listProxy.remove("hey");
		listProxy.size();
		listProxy.equals(list);
		assertThrows(IndexOutOfBoundsException.class, () -> listProxy.get(10));
	}

	interface Counter {
		int count();

		default int doubled() {
			return count() * 2;
		}

		default int tripled() {
			return count() * 3;
		}
	}

	@Test
	void defaultMethod_noDelegate_runsDefaultBody() {
		Counter proxy = ProxyBuilder.of(Counter.class)
						.method("count", parameters -> 5)
						.build();
		assertEquals(10, proxy.doubled());
		assertEquals(15, proxy.tripled());
	}

	@Test
	void defaultMethod_noDelegate_defaultCallsBackThroughProxy() {
		// doubled() calls count() — that call must re-enter the proxy
		// and hit the proxyMethod, NOT escape to some other implementation
		int[] callCount = {0};
		Counter proxy = ProxyBuilder.of(Counter.class)
						.method("count", parameters -> {
							callCount[0]++;
							return 7;
						})
						.build();
		assertEquals(14, proxy.doubled());
		assertEquals(1, callCount[0]);
	}

	@Test
	void defaultMethod_noDelegate_noProxyForBaseMethod_throws() {
		// doubled() calls count(), which has no proxy method and no delegate;
		// the abstract method bottoms out in UnsupportedOperationException.
		Counter proxy = ProxyBuilder.of(Counter.class).build();
		assertThrows(UnsupportedOperationException.class, proxy::doubled);
	}

	@Test
	void defaultMethod_withDelegate_inheritedDefault() {
		Counter delegate = () -> 4;
		Counter proxy = ProxyBuilder.of(Counter.class)
						.delegate(delegate)
						.build();
		assertEquals(8, proxy.doubled());
	}

	@Test
	void defaultMethod_withDelegate_delegateOverridesDefault() {
		Counter delegate = new Counter() {
			@Override
			public int count() {
				return 10;
			}

			@Override
			public int doubled() {
				return 999;
			}
		};
		Counter proxy = ProxyBuilder.of(Counter.class)
						.delegate(delegate)
						.build();
		assertEquals(999, proxy.doubled());
	}

	@Test
	void defaultMethod_proxyMethodOverridesDefault() {
		Counter delegate = () -> 3;
		Counter proxy = ProxyBuilder.of(Counter.class)
						.delegate(delegate)
						.method("doubled", parameters -> 100)
						.build();
		assertEquals(100, proxy.doubled());
		assertEquals(9, proxy.tripled());// not overridden — still uses default × delegate
	}
}
