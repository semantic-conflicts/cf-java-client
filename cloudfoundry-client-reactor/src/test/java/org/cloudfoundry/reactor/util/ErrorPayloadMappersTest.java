package org.cloudfoundry.reactor.util;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.UnknownCloudFoundryException;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v3.ClientV3Exception;
import org.cloudfoundry.reactor.HttpClientResponseWithConnection;
import org.cloudfoundry.uaa.UaaException;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.http.client.HttpClientResponse;
import reactor.test.StepVerifier;
import java.io.IOException;
import java.time.Duration;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
public final class ErrorPayloadMappersTest {
  public Connection connection=mock(Connection.class,RETURNS_SMART_NULLS);
  public NettyInbound inbound=mock(NettyInbound.class,RETURNS_SMART_NULLS);
  public ObjectMapper objectMapper=new ObjectMapper();
  public HttpClientResponse response=mock(HttpClientResponse.class,RETURNS_SMART_NULLS);
  @Test public void clientV2BadPayload() throws IOException {
    when(this.connection.inbound()).thenReturn(this.inbound);
    when(this.inbound.receive()).thenReturn(ByteBufFlux.fromPath(new ClassPathResource("fixtures/invalid_error_response.json").getFile().toPath()));
    when(this.response.status()).thenReturn(BAD_REQUEST);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.clientV2(this.objectMapper)).as(StepVerifier::create).consumeErrorWith(t -> assertThat(t).isInstanceOf(UnknownCloudFoundryException.class).hasMessage("Unknown Cloud Foundry Exception").extracting("statusCode","payload").containsExactly(BAD_REQUEST.code(),"Invalid Error Response")).verify(Duration.ofSeconds(1));
  }
  @Test public void clientV2ClientError() throws IOException {
    when(this.connection.inbound()).thenReturn(this.inbound);
    when(this.inbound.receive()).thenReturn(ByteBufFlux.fromPath(new ClassPathResource("fixtures/client/v2/error_response.json").getFile().toPath()));
    when(this.response.status()).thenReturn(BAD_REQUEST);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.clientV2(this.objectMapper)).as(StepVerifier::create).consumeErrorWith(t -> assertThat(t).isInstanceOf(ClientV2Exception.class).hasMessage("CF-UnprocessableEntity(10008): The request is semantically invalid: space_guid and name unique").extracting("statusCode","code","description","errorCode").containsExactly(BAD_REQUEST.code(),10008,"The request is semantically invalid: space_guid and name unique","CF-UnprocessableEntity")).verify(Duration.ofSeconds(1));
  }
  @Test public void clientV2NoError(){
    when(this.response.status()).thenReturn(OK);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.clientV2(this.objectMapper)).as(StepVerifier::create).expectNext(responseWithConnection).expectComplete().verify(Duration.ofSeconds(1));
  }
  @Test public void clientV2ServerError() throws IOException {
    when(this.connection.inbound()).thenReturn(this.inbound);
    when(this.inbound.receive()).thenReturn(ByteBufFlux.fromPath(new ClassPathResource("fixtures/client/v2/error_response.json").getFile().toPath()));
    when(this.response.status()).thenReturn(INTERNAL_SERVER_ERROR);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.clientV2(this.objectMapper)).as(StepVerifier::create).consumeErrorWith(t -> assertThat(t).isInstanceOf(ClientV2Exception.class).hasMessage("CF-UnprocessableEntity(10008): The request is semantically invalid: space_guid and name unique").extracting("statusCode","code","description","errorCode").containsExactly(INTERNAL_SERVER_ERROR.code(),10008,"The request is semantically invalid: space_guid and name unique","CF-UnprocessableEntity")).verify(Duration.ofSeconds(1));
  }
  @Test public void clientV3BadPayload() throws IOException {
    when(this.connection.inbound()).thenReturn(this.inbound);
    when(this.inbound.receive()).thenReturn(ByteBufFlux.fromPath(new ClassPathResource("fixtures/invalid_error_response.json").getFile().toPath()));
    when(this.response.status()).thenReturn(BAD_REQUEST);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.clientV3(this.objectMapper)).as(StepVerifier::create).consumeErrorWith(t -> assertThat(t).isInstanceOf(UnknownCloudFoundryException.class).hasMessage("Unknown Cloud Foundry Exception").extracting("statusCode","payload").containsExactly(BAD_REQUEST.code(),"Invalid Error Response")).verify(Duration.ofSeconds(1));
  }
  @Test public void clientV3ClientError() throws IOException {
    when(this.connection.inbound()).thenReturn(this.inbound);
    when(this.inbound.receive()).thenReturn(ByteBufFlux.fromPath(new ClassPathResource("fixtures/client/v3/error_response.json").getFile().toPath()));
    when(this.response.status()).thenReturn(BAD_REQUEST);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.clientV3(this.objectMapper)).as(StepVerifier::create).consumeErrorWith(t -> {
      assertThat(t).isInstanceOf(ClientV3Exception.class).hasMessage("CF-UnprocessableEntity(10008): something went wrong").extracting("statusCode").isEqualTo(BAD_REQUEST.code());
      assertThat(((ClientV3Exception)t).getErrors()).flatExtracting(org.cloudfoundry.client.v3.Error::getCode,org.cloudfoundry.client.v3.Error::getDetail,org.cloudfoundry.client.v3.Error::getTitle).containsExactly(10008,"something went wrong","CF-UnprocessableEntity");
    }
).verify(Duration.ofSeconds(1));
  }
  @Test public void clientV3NoError(){
    when(this.response.status()).thenReturn(OK);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.clientV3(this.objectMapper)).as(StepVerifier::create).expectNext(responseWithConnection).expectComplete().verify(Duration.ofSeconds(1));
  }
  @Test public void clientV3ServerError() throws IOException {
    when(this.connection.inbound()).thenReturn(this.inbound);
    when(this.inbound.receive()).thenReturn(ByteBufFlux.fromPath(new ClassPathResource("fixtures/client/v3/error_response.json").getFile().toPath()));
    when(this.response.status()).thenReturn(INTERNAL_SERVER_ERROR);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.clientV3(this.objectMapper)).as(StepVerifier::create).consumeErrorWith(t -> {
      assertThat(t).isInstanceOf(ClientV3Exception.class).hasMessage("CF-UnprocessableEntity(10008): something went wrong").extracting("statusCode").isEqualTo(INTERNAL_SERVER_ERROR.code());
      assertThat(((ClientV3Exception)t).getErrors()).flatExtracting(org.cloudfoundry.client.v3.Error::getCode,org.cloudfoundry.client.v3.Error::getDetail,org.cloudfoundry.client.v3.Error::getTitle).containsExactly(10008,"something went wrong","CF-UnprocessableEntity");
    }
).verify(Duration.ofSeconds(1));
  }
  @Test public void uaaBadPayload() throws IOException {
    when(this.connection.inbound()).thenReturn(this.inbound);
    when(this.inbound.receive()).thenReturn(ByteBufFlux.fromPath(new ClassPathResource("fixtures/invalid_error_response.json").getFile().toPath()));
    when(this.response.status()).thenReturn(BAD_REQUEST);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.uaa(this.objectMapper)).as(StepVerifier::create).consumeErrorWith(t -> assertThat(t).isInstanceOf(UnknownCloudFoundryException.class).hasMessage("Unknown Cloud Foundry Exception").extracting("statusCode","payload").containsExactly(BAD_REQUEST.code(),"Invalid Error Response")).verify(Duration.ofSeconds(1));
  }
  @Test public void uaaClientError() throws IOException {
    when(this.connection.inbound()).thenReturn(this.inbound);
    when(this.inbound.receive()).thenReturn(ByteBufFlux.fromPath(new ClassPathResource("fixtures/uaa/error_response.json").getFile().toPath()));
    when(this.response.status()).thenReturn(BAD_REQUEST);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.uaa(this.objectMapper)).as(StepVerifier::create).consumeErrorWith(t -> assertThat(t).isInstanceOf(UaaException.class).hasMessage("unauthorized: Bad credentials").extracting("statusCode","error","errorDescription").containsExactly(BAD_REQUEST.code(),"unauthorized","Bad credentials")).verify(Duration.ofSeconds(1));
  }
  @Test public void uaaNoError(){
    when(this.response.status()).thenReturn(OK);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.uaa(this.objectMapper)).as(StepVerifier::create).expectNext(responseWithConnection).expectComplete().verify(Duration.ofSeconds(1));
  }
  @Test public void uaaServerError() throws IOException {
    when(this.connection.inbound()).thenReturn(this.inbound);
    when(this.inbound.receive()).thenReturn(ByteBufFlux.fromPath(new ClassPathResource("fixtures/uaa/error_response.json").getFile().toPath()));
    when(this.response.status()).thenReturn(INTERNAL_SERVER_ERROR);
    HttpClientResponseWithConnection responseWithConnection=buildResponseWithConnection(this.connection);
    Flux.just(responseWithConnection).transform(ErrorPayloadMappers.uaa(this.objectMapper)).as(StepVerifier::create).consumeErrorWith(t -> assertThat(t).isInstanceOf(UaaException.class).hasMessage("unauthorized: Bad credentials").extracting("statusCode","error","errorDescription").containsExactly(INTERNAL_SERVER_ERROR.code(),"unauthorized","Bad credentials")).verify(Duration.ofSeconds(1));
  }
  public HttpClientResponseWithConnection buildResponseWithConnection(  Connection connection){
    return HttpClientResponseWithConnection.of(connection,this.response);
  }
  public ErrorPayloadMappersTest(){
  }
}
