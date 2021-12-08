package pro.toparvion.sample.reactivlet.shared;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

/**
 * A kind of logical brigde from reactive stack to servlet one. Allows tradinitional servlet logic to work in
 * reactive enviroment. Should be used instead of {@link RequestContextHolder} class.
 * @apiNote The class is not intended to be used directly and thus is not declared {@code public}.
 * @author Toparvion
 * @see <a href="http://ttddyy.github.io/mdc-with-webclient-in-webmvc/">Source of the prototype</a>
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = REACTIVE)
class Reactor2ServletBridge implements WebFilter, Ordered {

  @Override
  public int getOrder() {
    return (Ordered.LOWEST_PRECEDENCE - 100);
  }

  /**
   * Provides the worker thread with current exchange by means of thread-local variable
   * @param exchange current exchange
   * @param chain the rest of the filters to delegate to
   * @return reactive chain tail
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return chain.filter(exchange)
        .doOnSubscribe(sub -> {
          HttpRequestAccessor.CURRENT_EXCHANGE_HOLDER.set(exchange);
          log.trace("Exchange has been bound to current HTTP worker thread");
        })
        .doFinally(whatever -> {
          HttpRequestAccessor.CURRENT_EXCHANGE_HOLDER.remove();
          log.trace("Exchange has been unbound from current HTTP worker thread");
        });
  }

  @PostConstruct
  void setupReactorThreadsDecorator() {
    Schedulers.onScheduleHook("REQUEST_HOLDER", runnable -> {
      //region Actions in parent thread:
      ServerWebExchange currentExchange = HttpRequestAccessor.CURRENT_EXCHANGE_HOLDER.get();
      //endregion

      return () -> {
        //region Actions in child thread:
        if (currentExchange != null) {
          HttpRequestAccessor.CURRENT_EXCHANGE_HOLDER.set(currentExchange);
          log.trace("Exchange has been stored to application thread");
        }
        // Now that we've set the necessary thread-local environment, the task can run as usual
        try {
          runnable.run();

        } finally {
          HttpRequestAccessor.CURRENT_EXCHANGE_HOLDER.remove();
          log.trace("Exchange has been removed from application thread");
        }
        //endregion
      };
    });
  }

  @PreDestroy
  void shutdownReactorThreadsDecorator() {
    Schedulers.resetOnScheduleHook("REQUEST_HOLDER");
  }

}
