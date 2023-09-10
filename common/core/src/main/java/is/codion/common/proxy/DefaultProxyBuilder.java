/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultProxyBuilder<T> implements ProxyBuilder<T> {

  private final Map<MethodKey, ProxyMethod<T>> methodMap = new HashMap<>();
  private final Class<T> interfaceToProxy;

  private T delegate;

  DefaultProxyBuilder(Class<T> interfaceToProxy) {
    if (!requireNonNull(interfaceToProxy).isInterface()) {
      throw new IllegalArgumentException(interfaceToProxy + " is not an interface");
    }
    this.interfaceToProxy = interfaceToProxy;
  }

  @Override
  public ProxyBuilder<T> delegate(T delegate) {
    this.delegate = requireNonNull(delegate);
    return this;
  }

  @Override
  public ProxyBuilder<T> method(String methodName, ProxyMethod<T> proxyMethod) {
    return method(methodName, emptyList(), proxyMethod);
  }

  @Override
  public ProxyBuilder<T> method(String methodName, Class<?> parameterType, ProxyMethod<T> proxyMethod) {
    return method(methodName, singletonList(requireNonNull(parameterType)), proxyMethod);
  }

  @Override
  public ProxyBuilder<T> method(String methodName, List<Class<?>> parameterTypes, ProxyMethod<T> proxyMethod) {
    requireNonNull(methodName);
    requireNonNull(parameterTypes);
    requireNonNull(proxyMethod);
    try {
      methodMap.put(findMethod(singletonList(interfaceToProxy), methodName, parameterTypes), proxyMethod);

      return this;
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T build() {
    return (T) Proxy.newProxyInstance(interfaceToProxy.getClassLoader(),
            new Class[] {interfaceToProxy}, new DefaultHandler<>(methodMap, delegate));
  }

  private static MethodKey findMethod(List<Class<?>> interfaces, String methodName, List<Class<?>> parameterTypes) throws NoSuchMethodException {
    Method method = null;
    for (Class<?> anInterface : interfaces) {
      try {
        method = anInterface.getMethod(methodName, parameterTypes.toArray(new Class[0]));
      }
      catch (NoSuchMethodException e) {/**/}
    }
    if (method != null) {
      return new MethodKey(method);
    }

    List<Class<?>> superInterfaces = interfaces.stream()
            .flatMap(new GetSuperInterfaces())
            .collect(Collectors.toList());
    if (superInterfaces.isEmpty()) {
      return new MethodKey(Object.class.getMethod(methodName, parameterTypes.toArray(new Class[0])));
    }

    return findMethod(superInterfaces, methodName, parameterTypes);
  }

  private static final class GetSuperInterfaces implements Function<Class<?>, Stream<Class<?>>> {

    @Override
    public Stream<Class<?>> apply(Class<?> anInterface) {
      return Arrays.stream(anInterface.getInterfaces());
    }
  }

  private static final class DefaultHandler<T> implements InvocationHandler {

    private static final String EQUALS = "equals";

    private final Map<MethodKey, ProxyMethod<T>> methodMap;
    private final T delegate;

    private DefaultHandler(Map<MethodKey, ProxyMethod<T>> methodMap, T delegate) {
      this.methodMap = methodMap;
      this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      ProxyMethod<T> proxyMethod = methodMap.get(new MethodKey(method));
      if (proxyMethod != null) {
        return proxyMethod.invoke(new DefaultProxyMethodParameters<>((T) proxy, delegate, args));
      }
      if (isEqualsMethod(method)) {
        return proxy == args[0];
      }
      if (delegate != null) {
        return method.invoke(delegate, args);
      }

      throw new UnsupportedOperationException(method.toString());
    }

    private static boolean isEqualsMethod(Method method) {
      return EQUALS.equals(method.getName()) &&
              method.getParameterCount() == 1 &&
              method.getParameterTypes()[0].equals(Object.class);
    }
  }

  private static final class DefaultProxyMethodParameters<T> implements ProxyMethod.Parameters<T> {

    private final T proxy;
    private final T delegate;
    private final List<?> arguments;

    private DefaultProxyMethodParameters(T proxy, T delegate, Object[] arguments) {
      this.proxy = requireNonNull(proxy);
      this.delegate = delegate;
      this.arguments = arguments == null ? emptyList() : unmodifiableList(Arrays.asList(arguments));
    }

    @Override
    public T proxy() {
      return proxy;
    }

    @Override
    public T delegate() {
      if (delegate == null) {
        throw new IllegalStateException("No delegate specified for proxy");
      }

      return delegate;
    }

    @Override
    public List<?> arguments() {
      return arguments;
    }
  }

  private static final class MethodKey {

    private final String methodName;
    private final Class<?>[] parameterTypes;

    private MethodKey(Method method) {
      this.methodName = method.getName();
      this.parameterTypes = method.getParameterTypes();
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }
      MethodKey methodKey = (MethodKey) object;

      return methodName.equals(methodKey.methodName) &&
              Arrays.equals(parameterTypes, methodKey.parameterTypes);
    }

    @Override
    public int hashCode() {
      int result = methodName.hashCode();
      result = 31 * result + Arrays.hashCode(parameterTypes);

      return result;
    }
  }
}
