/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.apachedubbo.v2_7

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.apachedubbo.v2_7.api.HelloService
import io.opentelemetry.instrumentation.apachedubbo.v2_7.impl.HelloServiceImpl
import io.opentelemetry.instrumentation.test.InstrumentationSpecification
import io.opentelemetry.instrumentation.test.utils.PortUtils
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import org.apache.dubbo.common.utils.NetUtils
import org.apache.dubbo.config.ApplicationConfig
import org.apache.dubbo.config.ProtocolConfig
import org.apache.dubbo.config.ReferenceConfig
import org.apache.dubbo.config.RegistryConfig
import org.apache.dubbo.config.ServiceConfig
import org.apache.dubbo.config.bootstrap.DubboBootstrap
import org.apache.dubbo.rpc.service.GenericService
import spock.lang.Shared
import spock.lang.Unroll

import static io.opentelemetry.api.trace.SpanKind.CLIENT
import static io.opentelemetry.api.trace.SpanKind.SERVER
import static io.opentelemetry.instrumentation.apachedubbo.v2_7.DubboTestUtil.newDubboBootstrap
import static io.opentelemetry.instrumentation.apachedubbo.v2_7.DubboTestUtil.newFrameworkModel

@Unroll
abstract class AbstractDubboTest extends InstrumentationSpecification {

  @Shared
  def protocolConfig = new ProtocolConfig()

  def setupSpec() {
    NetUtils.LOCAL_ADDRESS = InetAddress.getLoopbackAddress()
  }

  ReferenceConfig<HelloService> configureClient(int port) {
    ReferenceConfig<HelloService> reference = new ReferenceConfig<>()
    reference.setInterface(HelloService)
    reference.setGeneric("true")
    reference.setUrl("dubbo://localhost:" + port + "/?timeout=30000")
    return reference
  }

  ServiceConfig configureServer() {
    def registerConfig = new RegistryConfig()
    registerConfig.setAddress("N/A")
    ServiceConfig<HelloServiceImpl> service = new ServiceConfig<>()
    service.setInterface(HelloService)
    service.setRef(new HelloServiceImpl())
    service.setRegistry(registerConfig)
    return service
  }

  def "test apache dubbo base #dubbo"() {
    setup:
    def port = PortUtils.findOpenPort()
    protocolConfig.setPort(port)

    def frameworkModel = newFrameworkModel()
    DubboBootstrap bootstrap = newDubboBootstrap(frameworkModel)
    bootstrap.application(new ApplicationConfig("dubbo-test-provider"))
      .service(configureServer())
      .protocol(protocolConfig)
      .start()

    def consumerProtocolConfig = new ProtocolConfig()
    consumerProtocolConfig.setRegister(false)

    def reference = configureClient(port)
    DubboBootstrap consumerBootstrap = newDubboBootstrap(frameworkModel)
    consumerBootstrap.application(new ApplicationConfig("dubbo-demo-api-consumer"))
      .reference(reference)
      .protocol(consumerProtocolConfig)
      .start()

    when:
    GenericService genericService = reference.get()
    def o = new Object[1]
    o[0] = "hello"
    def response = runWithSpan("parent") {
      genericService.$invoke("hello", [String.getName()] as String[], o)
    }

    then:
    response == "hello"
    assertTraces(1) {
      trace(0, 3) {
        span(0) {
          name "parent"
          kind SpanKind.INTERNAL
          hasNoParent()
        }
        span(1) {
          name "org.apache.dubbo.rpc.service.GenericService/\$invoke"
          kind CLIENT
          childOf span(0)
          attributes {
            "$SemanticAttributes.RPC_SYSTEM" "apache_dubbo"
            "$SemanticAttributes.RPC_SERVICE" "org.apache.dubbo.rpc.service.GenericService"
            "$SemanticAttributes.RPC_METHOD" "\$invoke"
            "$SemanticAttributes.NET_PEER_NAME" "localhost"
            "$SemanticAttributes.NET_PEER_PORT" Long
            "$SemanticAttributes.NET_SOCK_PEER_ADDR" { it == null || it instanceof String}
          }
        }
        span(2) {
          name "io.opentelemetry.instrumentation.apachedubbo.v2_7.api.HelloService/hello"
          kind SERVER
          childOf span(1)
          attributes {
            "$SemanticAttributes.RPC_SYSTEM" "apache_dubbo"
            "$SemanticAttributes.RPC_SERVICE" "io.opentelemetry.instrumentation.apachedubbo.v2_7.api.HelloService"
            "$SemanticAttributes.RPC_METHOD" "hello"
            "$SemanticAttributes.NET_SOCK_PEER_ADDR" String
            "$SemanticAttributes.NET_SOCK_PEER_PORT" Long
            "$SemanticAttributes.NET_SOCK_HOST_PORT" Long
            "$SemanticAttributes.NET_SOCK_FAMILY" { it == SemanticAttributes.NetSockFamilyValues.INET6 || it == null }
          }
        }
      }
    }

    cleanup:
    bootstrap.destroy()
    consumerBootstrap.destroy()
    frameworkModel?.destroy()
  }

  def "test apache dubbo test #dubbo"() {
    setup:
    def port = PortUtils.findOpenPort()
    protocolConfig.setPort(port)

    def frameworkModel = newFrameworkModel()
    DubboBootstrap bootstrap = newDubboBootstrap(frameworkModel)
    bootstrap.application(new ApplicationConfig("dubbo-test-async-provider"))
      .service(configureServer())
      .protocol(protocolConfig)
      .start()

    def consumerProtocolConfig = new ProtocolConfig()
    consumerProtocolConfig.setRegister(false)

    def reference = configureClient(port)
    DubboBootstrap consumerBootstrap = newDubboBootstrap(frameworkModel)
    consumerBootstrap.application(new ApplicationConfig("dubbo-demo-async-api-consumer"))
      .reference(reference)
      .protocol(consumerProtocolConfig)
      .start()

    when:
    GenericService genericService = reference.get()
    def o = new Object[1]
    o[0] = "hello"
    def responseAsync = runWithSpan("parent") {
      genericService.$invokeAsync("hello", [String.getName()] as String[], o)
    }

    then:
    responseAsync.get() == "hello"
    assertTraces(1) {
      trace(0, 3) {
        span(0) {
          name "parent"
          kind SpanKind.INTERNAL
          hasNoParent()
        }
        span(1) {
          name "org.apache.dubbo.rpc.service.GenericService/\$invokeAsync"
          kind CLIENT
          childOf span(0)
          attributes {
            "$SemanticAttributes.RPC_SYSTEM" "apache_dubbo"
            "$SemanticAttributes.RPC_SERVICE" "org.apache.dubbo.rpc.service.GenericService"
            "$SemanticAttributes.RPC_METHOD" "\$invokeAsync"
            "$SemanticAttributes.NET_PEER_NAME" "localhost"
            "$SemanticAttributes.NET_PEER_PORT" Long
            "$SemanticAttributes.NET_SOCK_PEER_ADDR" { it == null || it instanceof String}
          }
        }
        span(2) {
          name "io.opentelemetry.instrumentation.apachedubbo.v2_7.api.HelloService/hello"
          kind SERVER
          childOf span(1)
          attributes {
            "$SemanticAttributes.RPC_SYSTEM" "apache_dubbo"
            "$SemanticAttributes.RPC_SERVICE" "io.opentelemetry.instrumentation.apachedubbo.v2_7.api.HelloService"
            "$SemanticAttributes.RPC_METHOD" "hello"
            "$SemanticAttributes.NET_SOCK_PEER_ADDR" String
            "$SemanticAttributes.NET_SOCK_PEER_PORT" Long
            "$SemanticAttributes.NET_SOCK_HOST_PORT" Long
            "$SemanticAttributes.NET_SOCK_FAMILY" { it == SemanticAttributes.NetSockFamilyValues.INET6 || it == null }
          }
        }
      }
    }

    cleanup:
    bootstrap.destroy()
    consumerBootstrap.destroy()
    frameworkModel?.destroy()
  }
}
