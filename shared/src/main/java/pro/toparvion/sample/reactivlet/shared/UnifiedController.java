package pro.toparvion.sample.reactivlet.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * A sample controller showcasing the ability to work both on servlet and reactive stack.
 * Note that while works as expected, the {@link #inspect()} method doesn't use ractive-style declaration and
 * thus doesn't benefit from its features.
 * @author Toparvion
 */
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UnifiedController {

  private final HttpRequestAccessor httpRequestAccessor;

  /**
   * Extracts various data from the current request without explicitly declaring it as an argument.
   * Works the same way on both servlet and reactive stack.
   * @return an aggregated collection of all the extracted data
   */
  @GetMapping("/inspect")
  Map<String, Object> inspect() {
    HttpRequest currentRequest = httpRequestAccessor.fetchCurrentRequest();
    MultiValueMap<String, String> parameters = httpRequestAccessor.getParameters(currentRequest);
    List<HttpCookie> cookies = httpRequestAccessor.getCookies(currentRequest);
    Map<String, String> attributes = httpRequestAccessor.getAttributes(currentRequest);
    return Map.of(
      "parameters", parameters,
      "headers", currentRequest.getHeaders(),
      "cookies", cookies,
      "attributes", attributes
    );
  }

}
