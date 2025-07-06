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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.proxy;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Builds a dynamic proxy for a single interface.
 * <p>
 * This builder is reusable and thread-safe. Each call to {@link #build()} creates a new proxy instance
 * based on the current builder state. Subsequent changes to the builder do not affect previously built proxies.
 * <p>
 * Methods can be replaced by calling {@link #method(String, ProxyMethod)} again with the same method signature,
 * but once added, methods cannot be removed from the builder.
 * <p>
 * Note that if the {@link Object#equals(Object)} method is not proxied the resulting proxy is equal only to itself.
 * {@snippet :
 * List<String> list = new ArrayList<>();
 *
 * ProxyBuilder<List> builder = ProxyBuilder.of(List.class)
 *     .delegate(list)
 *     .method("add", Object.class, parameters -> {
 *       Object item = parameters.arguments().get(0);
 *       System.out.println("Adding: " + item);
 *
 *       return parameters.delegate().add(item);
 *     })
 *     .method("size", parameters -> {
 *       System.out.println("Size");
 *
 *       return parameters.delegate().size();
 *     });
 *
 * List<String> proxy1 = builder.build();
 * 
 * // Builder can be reused and modified
 * builder.method("remove", Object.class, parameters -> {
 *   Object item = parameters.arguments().get(0);
 *   System.out.println("Removing: " + item);
 *
 *   return parameters.delegate().remove(item);
 * });
 *
 * List<String> proxy2 = builder.build(); // Has all three methods
 * // proxy1 still has only add() and size() methods proxied
 *}
 * @param <T> the proxy type
 * @see #of(Class)
 */
public interface ProxyBuilder<T> {

	/**
	 * Sets the delegate instance to forward non-proxied method calls to.
	 * If not specified, all non-proxied methods throw {@link UnsupportedOperationException}.
	 * <p>
	 * The delegate is captured at build time. Changing the delegate after building a proxy
	 * does not affect previously built proxies.
	 * @param delegate the delegate instance to receive all non-proxied method calls
	 * @return this proxy builder
	 */
	ProxyBuilder<T> delegate(T delegate);

	/**
	 * Proxy the given no-argument method with the given proxy method.
	 * <p>
	 * If a proxy method has already been defined for this method signature,
	 * it will be replaced with the new one.
	 * @param name the method name
	 * @param method the proxy replacement method
	 * @return this proxy builder
	 */
	ProxyBuilder<T> method(String name, ProxyMethod<T> method);

	/**
	 * Proxy the given single-argument method with the given proxy method.
	 * <p>
	 * If a proxy method has already been defined for this method signature,
	 * it will be replaced with the new one.
	 * @param name the method name
	 * @param parameterType the method parameter type
	 * @param method the proxy method
	 * @return this proxy builder
	 */
	ProxyBuilder<T> method(String name, Class<?> parameterType, ProxyMethod<T> method);

	/**
	 * Proxy the given method with the given proxy method.
	 * <p>
	 * If a proxy method has already been defined for this method signature,
	 * it will be replaced with the new one.
	 * @param name the method name
	 * @param parameterTypes the method parameter types
	 * @param method the proxy method
	 * @return this proxy builder
	 */
	ProxyBuilder<T> method(String name, List<Class<?>> parameterTypes, ProxyMethod<T> method);

	/**
	 * Builds the Proxy instance.
	 * <p>
	 * Each call to this method returns a new proxy instance with the current builder configuration.
	 * The returned proxy is independent of the builder and will not be affected by subsequent
	 * changes to the builder.
	 * @return a new proxy instance
	 */
	T build();

	/**
	 * A proxy method.
	 * @param <T> the proxy type
	 */
	interface ProxyMethod<T> {

		/**
		 * Invokes this proxy method.
		 * @param parameters the parameters
		 * @return the result, may be null
		 * @throws Throwable in case of an exception
		 */
		@Nullable Object invoke(Parameters<T> parameters) throws Throwable;

		/**
		 * Parameters available to the invocation handler when calling a proxy method.
		 * @param <T> the proxy type
		 */
		interface Parameters<T> {

			/**
			 * @return the proxy instance
			 */
			T proxy();

			/**
			 * @return the delegate instance
			 * @throws IllegalStateException in case no delagate is available
			 */
			T delegate();

			/**
			 * @return the method arguments or an empty list in case of no arguments
			 */
			List<?> arguments();
		}
	}

	/**
	 * Returns a new {@link ProxyBuilder} instance.
	 * <p>Note: Unlike other builders in the framework, ProxyBuilder serves as both the
	 * builder interface and the factory, as it builds dynamic proxy instances rather
	 * than framework objects with separate builder interfaces.
	 * @param interfaceToProxy the interface to proxy
	 * @param <T> the proxy type
	 * @return a new {@link ProxyBuilder} instance.
	 * @throws IllegalArgumentException in case {@code interfaceToProxy} is not an interface
	 */
	static <T> ProxyBuilder<T> of(Class<T> interfaceToProxy) {
		return new DefaultProxyBuilder<>(interfaceToProxy);
	}
}
