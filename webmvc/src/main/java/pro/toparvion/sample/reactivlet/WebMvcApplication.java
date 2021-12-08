package pro.toparvion.sample.reactivlet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * A sample WebMVC application and REST controller to showcase the way servlet-based programs utilize threads
 * @author Toparvion
 */
@Slf4j
@RestController
@SpringBootApplication
public class WebMvcApplication {

  private final RestTemplate restTemplate;

  @Autowired
  public WebMvcApplication(RestTemplateBuilder builder,
                           @Value("${wiremock.base-url}") String wiremockBaseUrl) {
    this.restTemplate = builder
            .rootUri(wiremockBaseUrl)
            .defaultHeader("User-Agent", "DemoMvcApp")
            .build();
  }

  /**
   * {@code GET /sync/{template}} proxies the {@code template} query to the target by means of servlet {@link RestTemplate}
   * @param template the query to redirect, e.g. {@code fast} or {@code slow}
   * @return the proxied reply from the target
   */
  @GetMapping("/{template}")
  Object proxy(@PathVariable("template") String template) {
    log.info("Proxying the query to /{}", template);
    Object responseObject = restTemplate.getForObject("/{template}", Object.class, template);
    log.info("The proxy target responded with: {}", responseObject);
    return responseObject;
  }

  public static void main(String[] args) {
    SpringApplication.run(WebMvcApplication.class, args);
  }

}
