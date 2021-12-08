package pro.toparvion.sample.reactivlet.shared;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

/**
 * The filter fills MDC context with RID - the value of {@code rid} URI parameter.
 * @author Toparvion
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = SERVLET)
class ServletMdcFilter implements Filter {
  private static final String RID = "rid";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    String rid = httpServletRequest.getParameter(RID);
    MDC.put(RID, rid);
    log.trace("RID mark has been bound to the current thread.");
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(RID);
      log.trace("RID mark has been removed from the current thread.");
    }
  }
}
