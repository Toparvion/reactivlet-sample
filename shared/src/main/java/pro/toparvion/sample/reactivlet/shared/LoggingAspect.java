package pro.toparvion.sample.reactivlet.shared;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * This aspect wraps every {@link GetMapping}-annotated method with logging of both arguments and the result.
 * The aspect does not intercept exceptions.
 * @author Toparvion
 */
@Slf4j
@Aspect
@Component
class LoggingAspect {

  @Around("within(pro.toparvion.sample.reactivlet..*) " +
      " && execution(@org.springframework.web.bind.annotation.GetMapping * *.*(..))")
  public Object logArgsAndResult(ProceedingJoinPoint pjp) throws Throwable {
    Object[] args = pjp.getArgs();
    String targetMethodName = pjp.getSignature().getName();
    log.debug("Method '{}' is being invoked with arguments: {}", targetMethodName, args);
    Object resultTail = pjp.proceed(args);
    return assignResultLoggingBehavior(targetMethodName, resultTail);
  }

  @SuppressWarnings("ReactiveStreamsUnusedPublisher") // publishers are eventually used in case of reactive app
  private Object assignResultLoggingBehavior(String targetMethodName, Object tail) {
    if (tail instanceof Mono<?> monoResult) {
      tail = monoResult.doOnSuccess(result -> logResult(targetMethodName, result));
    } else if (tail instanceof Flux<?> fluxResult) {
      tail = fluxResult.doOnNext(result -> logResult(targetMethodName, result));
    } else {
      logResult(targetMethodName, tail);
    }
    return tail;
  }

  private void logResult(String targetMethodName, Object result) {
    log.debug("Method '{}' resulted in: {}", targetMethodName, result);
  }

}
