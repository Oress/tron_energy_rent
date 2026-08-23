package org.ipan.nrgyrent.ui;

import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * SPA fallback implemented as a 404 handler (not a catch-all GET mapping), so it can
 * never shadow an existing or future endpoint: requests only reach this code when no
 * controller matched and the response is 404.
 * <p>
 * Existing endpoints — e.g. the itrx/trxx webhook callbacks ({@code POST
 * /api/itrx/callback}, {@code POST /api/trxx/callback}) and actuator — are unaffected:
 * they match their own controller mappings before any 404 is produced.
 * <p>
 * For browser-like requests (Accept: text/html) to non-API, non-file paths we forward to
 * the Angular dashboard's index.html; everything else (including unmapped {@code /api/**}
 * paths, which keep returning a JSON 404) uses the default Spring error handling.
 */
@Controller
public class SpaErrorController extends BasicErrorController {

    public SpaErrorController(ErrorAttributes errorAttributes,
                              ErrorProperties errorProperties,
                              List<ErrorViewResolver> errorViewResolvers) {
        super(errorAttributes, errorProperties, errorViewResolvers);
    }

    @Override
    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        if (HttpStatus.NOT_FOUND.equals(getStatus(request)) && isSpaRoute(request)) {
            // index.html is served with 200 so client-side routing receives a clean response;
            // if the UI bundle is absent the forward 404s and default handling takes over.
            response.setStatus(HttpStatus.OK.value());
            return new ModelAndView("forward:/index.html");
        }
        return super.errorHtml(request, response);
    }

    private boolean isSpaRoute(HttpServletRequest request) {
        Object rawPath = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (!(rawPath instanceof String path) || path.isEmpty()) {
            return false;
        }
        // Never serve the SPA for API or actuator paths.
        if (path.equals("/api") || path.startsWith("/api/")
                || path.equals("/actuator") || path.startsWith("/actuator/")) {
            return false;
        }
        // Only router-style paths: the last segment must not look like a file.
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return !lastSegment.contains(".");
    }
}
