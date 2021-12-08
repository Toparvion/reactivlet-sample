package pro.toparvion.sample.reactivlet.shared;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.util.Map;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

/**
 * The filter fills MDC context with RID - the value of {@code rid} URI parameter.
 * @author Toparvion
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = REACTIVE)
class ReactiveMdcFilter implements WebFilter, Ordered {
  private static final String RID = "rid";

  @Override
  public int getOrder() {
    return (Ordered.LOWEST_PRECEDENCE - 150);
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String rid = exchange.getRequest().getQueryParams().getFirst(RID);
    return chain.filter(exchange)
      .doOnSubscribe(subscription -> {
        MDC.put(RID, rid);
        log.trace("RID mark has been set: {}", rid);
      })
      .doFinally(whatever -> {
        MDC.remove(RID);
        log.trace("RID mark has been removed: {}", rid);
      });
  }

  @PostConstruct
  void setupReactorThreadsDecorator() {
    Schedulers.onScheduleHook("mdc", runnable -> {
      Map<String, String> mdc = MDC.getCopyOfContextMap(); // can be narrowed down to RID only if necessary
      return () -> {
        MDC.setContextMap(mdc);
        log.trace("MDC map has been copied to the current thread");
        try {
          runnable.run();
        } finally {
          MDC.clear();
          log.trace("MDC map has been cleared for the current thread");
        }
      };
    });
  }

  @PreDestroy
  void shutdownThreadsDecorator() {
    Schedulers.resetOnScheduleHook("mdc");
  }
}
