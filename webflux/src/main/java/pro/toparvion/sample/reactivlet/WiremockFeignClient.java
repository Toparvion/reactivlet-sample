package pro.toparvion.sample.reactivlet;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * A sample OpenFeign client to Wiremock.
 * @author Toparvion
 */
@FeignClient(value = "wiremock")
public interface WiremockFeignClient {

  @RequestMapping(method = GET, value = "/{template}")
  Object call(@PathVariable String template);
}
