package pro.toparvion.sample.reactivlet.shared;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ServerWebExchange;
import reactor.util.context.Context;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static java.util.stream.Collectors.toMap;

/**
 * A front-side class of cross-stack facilities. Serves as a fetcher of the current HTTP request as well as a
 * provider of methods to access various request data.
 */
@Component
public class HttpRequestAccessor {

  /**
   * Thread-local storage of the current reactive exchange.
   * Project Reactor has no appropriate facility as its logic should stay thread-independent. The closest analogy -
   * a {@link Context context} - is not suitable because it requries the client logic to be written in reactive
   * style.<p>
   * The field is not {@code private} in order to be accesible from {@link Reactor2ServletBridge}.
   */
  static final ThreadLocal<ServerWebExchange> CURRENT_EXCHANGE_HOLDER = new InheritableThreadLocal<>();

  /**
   * @return current HTTP-request
   * @throws IllegalStateException on reactive stack - if the current thread was not prepended with {@code
   * Reactor2ServletBridge}, on servlet stack - if the current thread is not operated by Spring WebMVC
   * @apiNote The result's {@linkplain HttpRequest class} has a quite general interface and thus lacks some
   * frequently used methods for getting request data. To access such data, you should use corresponding methods of
   * this class: <ul>
   * <li>for URI parameters &mdash; {@link #getParameters}</li>
   * <li>for the attributes &mdash; {@link #getAttributes}</li>
   * <li>for the cookies &mdash; {@link #getCookies}</li>
   * </ul>
   */
  public HttpRequest fetchCurrentRequest() throws IllegalStateException {
    ServerWebExchange currentReactiveExchange = CURRENT_EXCHANGE_HOLDER.get();
    if (currentReactiveExchange != null) {
      return currentReactiveExchange.getRequest();
    }
    var servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpServletRequest servletRequest = servletRequestAttributes.getRequest();
    return new ServletServerHttpRequest(servletRequest);
  }

  /**
   * Depending on the given request, returns either the request's attributes of the current exchange's attributes.
   * @implNote Please note that actual set of attributes heavily depends on the current stack and thus cannot be
   * guaranteed beforehand. Moreover, semantically the same internal attributes in Spring WebMVC and WebFlux has
   * different names as the names start with the fully qualified name of {@code HandlerMapping} class (which has its
   * own version in every framework).
   * @apiNote For the sake of simplicity, the map's entries values represented with String class
   * @param httpRequest current request (usually returned by {@link #fetchCurrentRequest()})
   * @return the map of all the attributes
   */
  public Map<String, String> getAttributes(HttpRequest httpRequest) {
    // servlet request
    if (httpRequest instanceof ServletServerHttpRequest wrapper) {
      HttpServletRequest httpServletRequest = wrapper.getServletRequest();
      Map<String, String> attributes = new LinkedHashMap<>();
      Enumeration<String> attributeNames = httpServletRequest.getAttributeNames();
      while (attributeNames.hasMoreElements()) {
        String attributeName = attributeNames.nextElement();
        Object attributeValue = httpServletRequest.getAttribute(attributeName);
        attributes.put(attributeName, String.valueOf(attributeValue));
      }
      return attributes;
    }
    // reactive request
    if (httpRequest instanceof ServerHttpRequest) {
      ServerWebExchange currentReactiveExchange = CURRENT_EXCHANGE_HOLDER.get();
      Assert.notNull(currentReactiveExchange, "In reactive mode the current exchange must be stored " +
        "in HttpRequestAccessor#CURRENT_EXCHANGE_HOLDER field");
      return currentReactiveExchange.getAttributes()
        .entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), String.valueOf(entry.getValue())))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    // unknown request
    throw new IllegalArgumentException("Unknown request class: " + httpRequest.getClass());
  }

  /**
   * @param httpRequest current request (usually returned by {@link #fetchCurrentRequest()})
   * @return a multivalue map with all the parameters of given request
   * @apiNote Note that in case of servlet stack the returned collection may contain request body parameters (usually
   * POSTed with a form) while reactive stack assumes parameters from request URI only
   */
  public MultiValueMap<String, String> getParameters(HttpRequest httpRequest) {
    // servlet request
    if (httpRequest instanceof ServletServerHttpRequest wrapper) {
      HttpServletRequest servletRequest = wrapper.getServletRequest();
      Map<String, String[]> parameterMap = servletRequest.getParameterMap();
      Map<String, List<String>> rawMap = parameterMap.entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), Arrays.asList(entry.getValue())))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
      return CollectionUtils.toMultiValueMap(rawMap);
    }
    // reactive request
    if (httpRequest instanceof ServerHttpRequest serverHttpRequest) {
      return serverHttpRequest.getQueryParams();
    }
    // unknown request
    throw new IllegalArgumentException("Unknown request class: " + httpRequest.getClass());
  }

  /**
   * @param httpRequest current request (usually returned by {@link #fetchCurrentRequest()})
   * @return a list of all the cookies of given request
   */
  public List<HttpCookie> getCookies(HttpRequest httpRequest) {
    // servlet request
    if (httpRequest instanceof ServletServerHttpRequest servletRequest) {
      Cookie[] cookies = servletRequest.getServletRequest().getCookies();
      if (cookies == null) {
        return List.of();
      }
      return Arrays.stream(cookies)
        .map(servletCookie -> new HttpCookie(servletCookie.getName(), servletCookie.getValue()))
        .toList();
    }
    // reactive request
    if (httpRequest instanceof ServerHttpRequest reactiveRequest) {
      return reactiveRequest.getCookies()
        .values().stream()
        .flatMap(List::stream)
        .toList();
    }
    // unknown request
    throw new IllegalArgumentException("Unknown request class: " + httpRequest.getClass());
  }
}
