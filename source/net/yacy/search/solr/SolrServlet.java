package net.yacy.search.solr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.yacy.cora.document.UTF8;
import net.yacy.kelondro.logging.Log;

import org.apache.lucene.document.Document;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.FastWriter;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.ServletSolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.response.XMLResponseWriter;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.servlet.cache.HttpCacheHeaderUtil;
import org.apache.solr.servlet.cache.Method;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;


public class SolrServlet implements Filter {

    private static final QueryResponseWriter responseWriter = new XMLResponseWriter();
    private static EmbeddedSolrConnector connector;

    public SolrServlet() {
    }

    public static void initCore(EmbeddedSolrConnector c) {
        connector = c;
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)) {
            if (chain != null) chain.doFilter(request, response);
            return;
        }

        HttpServletRequest hrequest = (HttpServletRequest) request;
        HttpServletResponse hresponse = (HttpServletResponse) response;
        SolrQueryRequest req = null;

        // check if this servlet was called correctly
        String pathInfo = hrequest.getPathInfo();
        String path = pathInfo == null ? hrequest.getServletPath() : hrequest.getServletPath() + pathInfo; // should be "/select" after this

        if (!EmbeddedSolrConnector.SELECT.equals(path)) {
            // this is not for this servlet
            if (chain != null) chain.doFilter(request, response);
            return;
        }
        if (!EmbeddedSolrConnector.CONTEXT.equals(hrequest.getContextPath())) {
            // this is not for this servlet
            if (chain != null) chain.doFilter(request, response);
            return;
        }

        // reject POST which is not supported here
        final Method reqMethod = Method.getMethod(hrequest.getMethod());
        if (reqMethod == null || (reqMethod != Method.GET && reqMethod != Method.HEAD)) {
            throw new ServletException("Unsupported method: " + hrequest.getMethod());
        }

        try {
            SolrCore core = connector.getCore();
            if (core == null) {
                throw new UnsupportedOperationException("core not initialized");
            }

            // prepare request to solr
            hrequest.setAttribute("org.apache.solr.CoreContainer", core);
            req = connector.request(new ServletSolrParams(hrequest));

            SolrQueryResponse rsp = connector.query(req);

            // prepare response
            hresponse.setHeader("Cache-Control", "no-cache");
            HttpCacheHeaderUtil.checkHttpCachingVeto(rsp, hresponse, reqMethod);

            // check error
            if (rsp.getException() != null) {
                sendError(hresponse, rsp.getException());
                return;
            }

            // write response header
            final String contentType = responseWriter.getContentType(req, rsp);
            if (null != contentType) response.setContentType(contentType);

            if (Method.HEAD == reqMethod) {
                return;
            }

            // write response body
            Writer out = new FastWriter(new OutputStreamWriter(response.getOutputStream(), UTF8.charset));

            //debug
            @SuppressWarnings("unchecked")
            Iterator<Map.Entry<String, Object>> ie = rsp.getValues().iterator();
            Map.Entry<String, Object> e;
            while (ie.hasNext()) {
                e = ie.next();
                System.out.println("Field: " + e.getKey() + ", value: " + e.getValue().getClass().getName());
                //Field: responseHeader, value: org.apache.solr.common.util.SimpleOrderedMap
                //Field: response, value: org.apache.solr.search.DocSlice
                if (e.getValue() instanceof DocList) {
                    DocList ids = (DocList) e.getValue();
                    SolrIndexSearcher searcher = req.getSearcher();
                    DocIterator iterator = ids.iterator();
                    int sz = ids.size();
                    for (int i = 0; i < sz; i++) {
                        int id = iterator.nextDoc();
                        Document doc = searcher.doc(id);
                    }
                }
            }

            responseWriter.write(out, req, rsp);
            out.flush();
            return;

        } catch (Throwable ex) {
            sendError(hresponse, ex);
            return;
        } finally {
            if (req != null) {
                req.close();
            }
            SolrRequestInfo.clearRequestInfo();
        }
    }

    private static void sendError(HttpServletResponse hresponse, Throwable ex) throws IOException {
        int code = (ex instanceof SolrException) ? ((SolrException) ex).code() : 500;
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        hresponse.sendError((code < 100) ? 500 : code, ex.getMessage() + "\n\n" + sw.toString());
    }

    /**
     * from org.apache.solr.client.solrj.embedded.JettySolrRunner
     */
    public static Server startServer(String context, int port, EmbeddedSolrConnector c) {
        //this.context = context;
        Server server = new Server(port);
        /*
            SocketConnector connector = new SocketConnector();
            connector.setPort(port);
            connector.setReuseAddress(true);
            this.server.setConnectors(new Connector[] { connector });
            this.server.setSessionIdManager(new HashSessionIdManager(new Random()));
        */
        server.setStopAtShutdown(true);
        Context root = new Context(server, context, Context.SESSIONS);
        root.addServlet(Servlet404.class, "/*");

        // attach org.apache.solr.response.XMLWriter to search requests
        SolrServlet.initCore(c);
        FilterHolder dispatchFilter = root.addFilter(SolrServlet.class, "*", Handler.REQUEST);

        if (!server.isRunning()) {
            try {
                server.start();
                waitForSolr(context, port);
            } catch (Exception e) {
                Log.logException(e);
            }
        }
        return server;
    }

    public static void waitForSolr(String context, int port) throws Exception {
        // A raw term query type doesn't check the schema
        URL url = new URL("http://127.0.0.1:" + port + context + "/select?q={!raw+f=test_query}ping");

        Exception ex=null;
        // Wait for a total of 20 seconds: 100 tries, 200 milliseconds each
        for (int i = 0; i < 600; i++) {
            try {
                InputStream stream = url.openStream();
                stream.close();
            } catch (IOException e) {
                ex=e;
                Thread.sleep(200);
                continue;
            }
            return;
        }
        throw new RuntimeException("Jetty/Solr unresponsive", ex);
    }

    public static class Servlet404 extends HttpServlet {
        private static final long serialVersionUID=-4497069674942245148L;
        @Override
        public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
            res.sendError(404, "Can not find: " + req.getRequestURI());
        }
    }

}
