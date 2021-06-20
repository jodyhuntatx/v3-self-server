import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;
//import java.util.Base64;

import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

public class PASBasicAuthFilter implements Filter {
    /** Logger */
    private static final Logger logger = Logger.getLogger(PASBasicAuthFilter.class.getName());
    private String realm = "Protected";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String paramRealm = filterConfig.getInitParameter("realm");
        if (paramRealm != null && paramRealm.length() > 0) {
            realm = paramRealm;
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();

                if (basic.equalsIgnoreCase("Basic")) {
                    try {
                        String credentials = new String(Base64.getDecoder().decode(st.nextToken()));
                        int p = credentials.indexOf(":");
                        if (p != -1) {
                            String _username = credentials.substring(0, p).trim();
                            String _password = credentials.substring(p + 1).trim();

                            logger.log(Level.INFO, "Logging in user: " + _username);
                            String pasAuthnResponse=PASJava.logon(_username, _password);
                            if (pasAuthnResponse == null) {
                              logger.log(Level.INFO, "Login failed.");
                            }
                            response.getOutputStream().println("{"
                                                                + "\"pasSessionToken\": "
                                                                + "\"" + pasAuthnResponse + "\""
                                                                + "}"
                                                                );
                            filterChain.doFilter(servletRequest, servletResponse);
                        } else {
                            unauthorized(response, "Basic auth credentials must be in <username>:<password> format.");
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new Error("Couldn't retrieve authentication", e);
                    }
                }
            }
        } else {
            unauthorized(response);
        }

    }

    @Override
    public void destroy() {
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
        response.sendError(401, message);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        unauthorized(response, "Unauthorized");
    }

}
