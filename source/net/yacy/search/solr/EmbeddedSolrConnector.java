/**
 *  EmbeddedSolrConnector
 *  Copyright 2012 by Michael Peter Christen
 *  First released 21.06.2012 at http://yacy.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package net.yacy.search.solr;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import net.yacy.cora.services.federated.solr.AbstractSolrConnector;
import net.yacy.cora.services.federated.solr.SolrConnector;
import net.yacy.cora.services.federated.solr.SolrDoc;
import net.yacy.kelondro.logging.Log;
import net.yacy.search.index.SolrField;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.servlet.SolrRequestParsers;
import org.xml.sax.SAXException;

import com.google.common.io.Files;

public class EmbeddedSolrConnector extends AbstractSolrConnector implements SolrConnector {

    public static final String SELECT = "/select";
    public static final String CONTEXT = "/solr";
    private final static String[] confFiles = {"solrconfig.xml", "schema.xml", "stopwords.txt", "synonyms.txt", "protwords.txt", "currency.xml", "elevate.xml", "lang/"};

    private final CoreContainer cores;
    private final String defaultCoreName;
    private final SolrCore defaultCore;
    protected SolrRequestParsers adminRequestParser;
    private final SearchHandler requestHandler;

    public EmbeddedSolrConnector(File storagePath, File solr_config) throws IOException {
        super();
        // copy the solrconfig.xml to the storage path
        File conf = new File(storagePath, "conf");
        conf.mkdirs();
        File source, target;
        for (String cf: confFiles) {
            source = new File(solr_config, cf);
            if (source.isDirectory()) {
                target = new File(conf, cf);
                target.mkdirs();
                for (String cfl: source.list()) {
                    Files.copy(new File(source, cfl), new File(target, cfl));
                }
            } else {
                target = new File(conf, cf);
                target.getParentFile().mkdirs();
                Files.copy(source, target);
            }
        }
        /*
        try {
            CheckIndex.main(new String[]{new File(new File(storagePath, "data"), "index").getAbsolutePath(), "-fix"});
        } catch (InterruptedException e1) {
            Log.logException(e1);
        }
        */
        try {
            this.cores = new CoreContainer(storagePath.getAbsolutePath(), new File(solr_config, "solr.xml"));
        } catch (ParserConfigurationException e) {
            throw new IOException(e.getMessage(), e);
        } catch (SAXException e) {
            throw new IOException(e.getMessage(), e);
        }
        this.defaultCoreName = this.cores.getDefaultCoreName();
        this.defaultCore = this.cores.getCore(this.defaultCoreName); // should be "collection1"
        final NamedList<Object> config = new NamedList<Object>();
        this.requestHandler = new SearchHandler();
        this.requestHandler.init(config);
        this.requestHandler.inform(this.defaultCore);
        super.init(new EmbeddedSolrServer(this.cores, this.defaultCoreName));
    }

    public SolrCore getCore() {
        return this.defaultCore;
    }

    public SolrConfig getConfig() {
        return this.defaultCore.getSolrConfig();
    }

    @Override
    public synchronized void close() {
        super.close();
        this.cores.shutdown();
    }

    public SolrQueryRequest request(final SolrParams params) {
        SolrQueryRequest req = null;
        req = new SolrQueryRequestBase(this.defaultCore, params){};
        req.getContext().put("path", SELECT);
        req.getContext().put("webapp", CONTEXT);
        return req;
    }

    public SolrQueryResponse query(SolrQueryRequest req) {
        final long startTime = System.currentTimeMillis();

        SolrQueryResponse rsp = new SolrQueryResponse();
        NamedList<Object> responseHeader = new SimpleOrderedMap<Object>();
        rsp.add("responseHeader", responseHeader);
        SolrRequestInfo.setRequestInfo(new SolrRequestInfo(req, rsp));

        // send request to solr and create a result
        this.requestHandler.handleRequest(req, rsp);

        // get statistics and add a header with that
        Exception exception = rsp.getException();
        int status = exception == null ? 0 : exception instanceof SolrException ? ((SolrException) exception).code() : 500;
        responseHeader.add("status", status);
        responseHeader.add("QTime",(int) (System.currentTimeMillis() - startTime));

        // return result
        return rsp;
    }

    public static void main(String[] args) {
        File solr_config = new File("defaults/solr");
        File storage = new File("DATA/INDEX/webportal/SEGMENTS/text/solr/");
        storage.mkdirs();
        try {
            EmbeddedSolrConnector solr = new EmbeddedSolrConnector(storage, solr_config);
            SolrDoc solrdoc = new SolrDoc();
            solrdoc.addSolr(SolrField.id, "ABCD0000abcd");
            solrdoc.addSolr(SolrField.title, "Lorem ipsum");
            solrdoc.addSolr(SolrField.text_t, "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
            solr.add(solrdoc);
            SolrServlet.startServer("/solr", 8091, solr);
            SolrDocumentList searchresult = solr.query(SolrField.text_t.name() + ":tempor", 0, 10);
            for (SolrDocument d : searchresult) {
                System.out.println(d.toString());
            }
            // try http://127.0.0.1:8091/solr/select?q=ping
            try {Thread.sleep(1000 * 1000);} catch (InterruptedException e) {}
            solr.close();
        } catch (IOException e) {
            Log.logException(e);
        }

    }
}
