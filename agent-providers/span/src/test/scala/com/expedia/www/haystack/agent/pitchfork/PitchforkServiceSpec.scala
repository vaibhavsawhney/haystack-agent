/*
 *  Copyright 2019 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.expedia.www.haystack.agent.pitchfork

import java.util
import java.util.Collections

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.agent.core.Dispatcher
import com.expedia.www.haystack.agent.pitchfork.processors.{SpanValidator, ZipkinSpanProcessorFactory}
import com.expedia.www.haystack.agent.pitchfork.service.PitchforkService
import com.squareup.okhttp.{MediaType, OkHttpClient, Request, RequestBody}
import com.typesafe.config.ConfigFactory
import org.easymock.EasyMock
import org.scalatest.easymock.EasyMockSugar
import org.scalatest.{FunSpec, Matchers}
import zipkin2.Endpoint
import zipkin2.codec.SpanBytesEncoder

import scala.collection.JavaConverters._

class PitchforkServiceSpec extends FunSpec with Matchers with EasyMockSugar {

  private val client = new OkHttpClient()

  private def zipkinSpan(traceId: String): zipkin2.Span = {
    zipkin2.Span.newBuilder()
      .traceId(traceId)
      .id(1)
      .parentId(2)
      .name("/foo")
      .localEndpoint(Endpoint.newBuilder().serviceName("foo").build())
      .remoteEndpoint(Endpoint.newBuilder().serviceName("bar").port(8080).ip("10.10.10.10").build())
      .timestamp(System.currentTimeMillis() * 1000)
      .duration(100000l)
      .putTag("error", "true")
      .putTag("pos", "1").build()
  }

  describe("Pitchfork Agent Http service") {
    it("should dispatch the zipkinv2 span successfully") {
      val mockDispatcher = mock[Dispatcher]
      val config = ConfigFactory.parseMap(Map("port" -> 9115, "http.threads.min" -> 2, "http.threads.max" -> 4).asJava)

      val keyCapture = EasyMock.newCapture[Array[Byte]]()
      val haystackSpanCapture = EasyMock.newCapture[Array[Byte]]()

      expecting {
        mockDispatcher.getName.andReturn("mock")
        mockDispatcher.dispatch(EasyMock.capture(keyCapture), EasyMock.capture(haystackSpanCapture))
      }

      whenExecuting(mockDispatcher) {
        val service = new PitchforkService(config, new ZipkinSpanProcessorFactory(new SpanValidator(config),
          Collections.singletonList(mockDispatcher), Collections.emptyList()))

        service.start()

        // let the server start
        Thread.sleep(5000)

        val body = RequestBody.create(
          MediaType.parse("application/json"), SpanBytesEncoder.JSON_V2.encode(zipkinSpan("0000000000000064")))
        val request = new Request.Builder()
          .url("http://localhost:9115" + "/api/v2/spans")
          .post(body)
          .build()

        val response = client.newCall(request).execute()
//        response.code() shouldBe 200
//
//        new String(keyCapture.getValue) shouldEqual "0000000000000064"
//        val haystackSpan = Span.parseFrom(haystackSpanCapture.getValue)
//        haystackSpan.getTraceId shouldEqual "0000000000000064"
//        haystackSpan.getSpanId shouldEqual "0000000000000001"
//        haystackSpan.getParentSpanId shouldEqual "0000000000000002"
//        haystackSpan.getOperationName shouldEqual "/foo"
//        haystackSpan.getServiceName shouldEqual "foo"
//        haystackSpan.getDuration shouldBe 100000l
//        haystackSpan.getStartTime should be >((System.currentTimeMillis() - 20000) * 1000)
//        haystackSpan.getTagsCount shouldBe 6
//        haystackSpan.getTagsList.asScala.find(t => t.getKey == "pos").get.getVStr shouldEqual "1"
//        haystackSpan.getTagsList.asScala.find(t => t.getKey == "error").get.getVBool shouldBe true
//        haystackSpan.getTagsList.asScala.find(t => t.getKey == "remote.service.name").get.getVStr shouldBe "bar"
//        haystackSpan.getTagsList.asScala.find(t => t.getKey == "remote.service.port").get.getVLong shouldBe 8080
        service.stop()
      }
    }

    it("should dispatch the proto spans successfully") {
      val mockDispatcher = mock[Dispatcher]
      val config = ConfigFactory.parseMap(Map("port" -> 9112, "http.threads.min" -> 2, "http.threads.max" -> 4).asJava)

      val keyCapture_1 = EasyMock.newCapture[Array[Byte]]()
      val haystackSpanCapture_1 = EasyMock.newCapture[Array[Byte]]()

      val keyCapture_2 = EasyMock.newCapture[Array[Byte]]()
      val haystackSpanCapture_2 = EasyMock.newCapture[Array[Byte]]()

      expecting {
        mockDispatcher.getName.andReturn("mock").times(2)
        mockDispatcher.dispatch(EasyMock.capture(keyCapture_1), EasyMock.capture(haystackSpanCapture_1))
        mockDispatcher.dispatch(EasyMock.capture(keyCapture_2), EasyMock.capture(haystackSpanCapture_2))
      }

      whenExecuting(mockDispatcher) {
        val service = new PitchforkService(config, new ZipkinSpanProcessorFactory(new SpanValidator(config),
          Collections.singletonList(mockDispatcher), Collections.emptyList()))

        service.start()

        // let the server start
        Thread.sleep(5000)

        val body = RequestBody.create(
          MediaType.parse("application/x-protobuf"), SpanBytesEncoder.PROTO3.encodeList(util.Arrays.asList(
          zipkinSpan("0000000000000065"),
          zipkinSpan("0000000000000066"))))

        val request = new Request.Builder()
          .url("http://localhost:9112" + "/api/v2/spans")
          .post(body)
          .build()

        val response = client.newCall(request).execute()
//        response.code() shouldBe 200
//
//        new String(keyCapture_1.getValue) shouldEqual "0000000000000065"
//        val haystackSpan_1 = Span.parseFrom(haystackSpanCapture_1.getValue)
//        haystackSpan_1.getTraceId shouldEqual "0000000000000065"
//
//
//        new String(keyCapture_2.getValue) shouldEqual "0000000000000066"
//        val haystackSpan_2 = Span.parseFrom(haystackSpanCapture_2.getValue)
//        haystackSpan_2.getTraceId shouldEqual "0000000000000066"

        service.stop()
      }
    }
  }
}
