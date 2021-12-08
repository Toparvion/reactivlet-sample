package pro.toparvion.sample.reactivlet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A sample application and a REST controller to showcase various HTTP clients in action:<ul>
 *   <li>{@code GET /reactive/{template}} proxies the {@code template} query to the target by means of reactive
 *   {@link WebClient}</li>
 *   <li>{@code GET /sync/{template}} proxies the {@code template} query to the target by means of servlet {@link RestTemplate}</li>
 *   <li>{@code GET /feign/{template}} proxies the {@code template} query to the target by means of OpenFeign
 *   {@link WiremockFeignClient client}</li>
 * </ul>
 * @author Toparvion
 */
@Slf4j
@RestController
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class WebFluxApplication {

  private final WebClient webClient;
  private final RestTemplate restTemplate;
  private final WiremockFeignClient feignClient;

  @Autowired
  public WebFluxApplication(WebClient.Builder webClientBuilder,
                            WiremockFeignClient feignClient,
                            @Value("${wiremock.base-url}") String wiremockBaseUrl) {
    this.feignClient = feignClient;
    webClient = webClientBuilder
      .baseUrl(wiremockBaseUrl)
      .defaultHeader("User-Agent", "DemoFluxApp")
      .build();
    restTemplate = new RestTemplateBuilder()
      .rootUri(wiremockBaseUrl)
      .defaultHeader("User-Agent", "DemoFluxApp")
      .build();
  }

  @GetMapping("/reactive/{template}")
  Mono<Object> reactiveProxy(@PathVariable("template") String template) {
    return webClient.get()
      .uri("/{template}", template)
      .retrieve()
      .bodyToMono(Object.class)
      .doOnSubscribe(sub -> log.info("Reactive mode: proxying the query to /{}", template))
      .doOnSuccess(responseObject -> log.info("Proxy target responded with: {}", responseObject));
  }

  @GetMapping("/sync/{template}")
  Mono<Object> syncProxy(@PathVariable("template") String template) {
    return Mono.fromCallable(
        () -> restTemplate.getForObject("/{template}", Object.class, template))
      .subscribeOn(Schedulers.boundedElastic())
      .doOnSubscribe(sub -> log.info("Sync mode: proxying the query to /{}", template))
      .doOnSuccess(responseObject -> log.info("Proxy target responded with: {}", responseObject));
  }

  @GetMapping("/feign/{template}")
  Mono<Object> feignProxy(@PathVariable("template") String template) {
    return Mono.fromCallable(
        () -> feignClient.call(template))
//      .subscribeOn(Schedulers.boundedElastic()) // commented in order to let the FeignClientConfig.blockingLoadBalancerClient act
      .doOnSubscribe(sub -> log.info("Feign mode: proxying the query to /{}", template))
      .doOnSuccess(responseObject -> log.info("Proxy target responded with: {}", responseObject));
  }

  public static void main(String[] args) {
    SpringApplication.run(WebFluxApplication.class, args);
  }

}
