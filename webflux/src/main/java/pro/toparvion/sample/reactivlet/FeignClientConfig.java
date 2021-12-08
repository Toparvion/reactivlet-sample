package pro.toparvion.sample.reactivlet;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import reactor.core.publisher.Mono;

import static java.util.stream.Collectors.toList;

/**
 * A bunch of helpful beans for OpenFeign clients
 * @author Toparvion
 */
@Slf4j
@Configuration
public class FeignClientConfig {

  @Bean
  public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
    return new HttpMessageConverters(converters.orderedStream().collect(toList()));
  }

  @Bean // the name of the bean is aimed to override the like-named bean in Spring
  public SimpleDiscoveryProperties simpleDiscoveryProperties() {
    return new SimpleDiscoveryProperties();
  }

  @Bean // the name of the bean is aimed to override the like-named bean in Spring
  public LoadBalancerClient blockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory,
                                                       LoadBalancerProperties properties) {
    return new BlockingLoadBalancerClient(loadBalancerClientFactory, properties) {
      @Override
      public <T> ServiceInstance choose(String serviceId, Request<T> request) {
        ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerClientFactory.getInstance(serviceId);
        if (loadBalancer == null) {
          return null;
        }
        Publisher<Response<ServiceInstance>> serverInstancePub = loadBalancer.choose(request);
        ServiceInstance[] serviceInstance = new ServiceInstance[1];
        Mono.from(serverInstancePub)
          .subscribe(
            serviceInstanceResponse -> serviceInstance[0] = serviceInstanceResponse.getServer(),
            oops -> log.error("Failed to choose an instance", oops));
        return serviceInstance[0];
      }
    };
  }
}
