// Segment.java
// (C) 2005-2009 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 2005 on http://yacy.net; full redesign for segments 28.5.2009
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
//
// LICENSE
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package net.yacy.search.index;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import net.yacy.cora.document.encoding.ASCII;
import net.yacy.cora.document.encoding.UTF8;
import net.yacy.cora.document.id.AnchorURL;
import net.yacy.cora.document.id.DigestURL;
import net.yacy.cora.document.id.MultiProtocolURL;
import net.yacy.cora.federate.solr.connector.AbstractSolrConnector;
import net.yacy.cora.federate.solr.connector.SolrConnector;
import net.yacy.cora.federate.yacy.CacheStrategy;
import net.yacy.cora.order.Base64Order;
import net.yacy.cora.order.ByteOrder;
import net.yacy.cora.order.NaturalOrder;
import net.yacy.cora.protocol.ClientIdentification;
import net.yacy.cora.protocol.ResponseHeader;
import net.yacy.cora.storage.HandleSet;
import net.yacy.cora.util.ByteBuffer;
import net.yacy.cora.util.ConcurrentLog;
import net.yacy.cora.util.LookAheadIterator;
import net.yacy.cora.util.SpaceExceededException;
import net.yacy.crawler.retrieval.Response;
import net.yacy.document.Condenser;
import net.yacy.document.Document;
import net.yacy.document.Parser;
import net.yacy.kelondro.data.citation.CitationReference;
import net.yacy.kelondro.data.citation.CitationReferenceFactory;
import net.yacy.kelondro.data.meta.URIMetadataRow;
import net.yacy.kelondro.data.word.Word;
import net.yacy.kelondro.data.word.WordReference;
import net.yacy.kelondro.data.word.WordReferenceFactory;
import net.yacy.kelondro.data.word.WordReferenceRow;
import net.yacy.kelondro.index.RowHandleSet;
import net.yacy.kelondro.rwi.IndexCell;
import net.yacy.kelondro.rwi.ReferenceContainer;
import net.yacy.kelondro.rwi.ReferenceFactory;
import net.yacy.kelondro.util.Bitfield;
import net.yacy.kelondro.util.ISO639;
import net.yacy.kelondro.workflow.WorkflowProcessor;
import net.yacy.repository.LoaderDispatcher;
import net.yacy.search.StorageQueueEntry;
import net.yacy.search.query.SearchEvent;
import net.yacy.search.schema.CollectionConfiguration;
import net.yacy.search.schema.CollectionSchema;
import net.yacy.search.schema.WebgraphConfiguration;
import net.yacy.search.schema.WebgraphSchema;

public class Segment {

    // catchall word
    public final static String catchallString = "yacyall"; // a word that is always in all indexes; can be used for zero-word searches to find ALL documents
    public final static byte[] catchallHash;
    final static Word   catchallWord = new Word(0, 0, 0);
    static {
        catchallHash = Word.word2hash(catchallString); // "KZzU-Vf6h5k-"
        catchallWord.flags = new Bitfield(4);
        for (int i = 0; i < catchallWord.flags.length(); i++) catchallWord.flags.set(i, true);
    }

    // environment constants
    public static final long wCacheMaxAge    = 1000 * 60 * 30; // milliseconds; 30 minutes
    public static final int  wCacheMaxChunk  =  800;           // maximum number of references for each urlhash
    public static final int  lowcachedivisor =  900;
    public static final long targetFileSize  = 64 * 1024 * 1024; // 256 MB
    public static final int  writeBufferSize = 4 * 1024 * 1024;
    public static final String UrlDbName = "text.urlmd";
    public static final String termIndexName = "text.index";
    public static final String citationIndexName = "citation.index";

    // the reference factory
    public static final ReferenceFactory<WordReference> wordReferenceFactory = new WordReferenceFactory();
    public static final ReferenceFactory<CitationReference> citationReferenceFactory = new CitationReferenceFactory();
    public static final ByteOrder wordOrder = Base64Order.enhancedCoder;

    private   final ConcurrentLog                  log;
    private   final File                           segmentPath;
    protected final Fulltext                       fulltext;
    protected       IndexCell<WordReference>       termIndex;
    protected       IndexCell<CitationReference>   urlCitationIndex;
    private   WorkflowProcessor<StorageQueueEntry> indexingPutDocumentProcessor;

    /**
     * create a new Segment
     * @param log
     * @param segmentPath that should be the path ponting to the directory "SEGMENT"
     * @param collectionSchema
     */
    public Segment(final ConcurrentLog log, final File segmentPath,
            final CollectionConfiguration collectionConfiguration, final WebgraphConfiguration webgraphConfiguration) {
        log.info("Initializing Segment '" + segmentPath + ".");
        this.log = log;
        this.segmentPath = segmentPath;

        // create LURL-db
        this.fulltext = new Fulltext(segmentPath, collectionConfiguration, webgraphConfiguration);
        this.termIndex = null;
        this.urlCitationIndex = null;

        this.indexingPutDocumentProcessor = new WorkflowProcessor<StorageQueueEntry>(
                "putDocument",
                "solr document put queueing",
                new String[] {},
                this,
                "putDocument",
                10,
                null,
                1);
    }
    
    public boolean connectedRWI() {
        return this.termIndex != null;
    }

    public void connectRWI(final int entityCacheMaxSize, final long maxFileSize) throws IOException {
        if (this.termIndex != null) return;
        this.termIndex = new IndexCell<WordReference>(
                        new File(this.segmentPath, "default"),
                        termIndexName,
                        wordReferenceFactory,
                        wordOrder,
                        Word.commonHashLength,
                        entityCacheMaxSize,
                        targetFileSize,
                        maxFileSize,
                        writeBufferSize);
    }

    public void disconnectRWI() {
        if (this.termIndex == null) return;
        this.termIndex.close();
        this.termIndex = null;
    }

    public boolean connectedCitation() {
        return this.urlCitationIndex != null;
    }

    public void connectCitation(final int entityCacheMaxSize, final long maxFileSize) throws IOException {
        if (this.urlCitationIndex != null) return;
        this.urlCitationIndex = new IndexCell<CitationReference>(
                        new File(this.segmentPath, "default"),
                        citationIndexName,
                        citationReferenceFactory,
                        wordOrder,
                        Word.commonHashLength,
                        entityCacheMaxSize,
                        targetFileSize,
                        maxFileSize,
                        writeBufferSize);
    }

    public void disconnectCitation() {
        if (this.urlCitationIndex == null) return;
        this.urlCitationIndex.close();
        this.urlCitationIndex = null;
    }

    public int citationCount() {
        return this.urlCitationIndex == null ? 0 : this.urlCitationIndex.sizesMax();
    }
    
    public long citationSegmentCount() {
        return this.urlCitationIndex == null ? 0 : this.urlCitationIndex.getSegmentCount();
    }
    
    public void connectUrlDb(final boolean useTailCache, final boolean exceed134217727) {
        this.fulltext.connectUrlDb(UrlDbName, useTailCache, exceed134217727);
    }

    public Fulltext fulltext() {
        return this.fulltext;
    }

    public IndexCell<WordReference> termIndex() {
        return this.termIndex;
    }

    public IndexCell<CitationReference> urlCitation() {
        return this.urlCitationIndex;
    }

    /**
     * compute the click level using the citation reference database
     * @param citations the citation database
     * @param searchhash the hash of the url to be checked
     * @return the clickdepth level or 999 if the root url cannot be found or a recursion limit is reached
     * @throws IOException
     */
    private int getClickDepth(ReferenceReportCache rrc, final DigestURL url, int maxtime) throws IOException {

        final byte[] searchhash = url.hash();
        RowHandleSet rootCandidates = getPossibleRootHashes(url);
        
        Set<byte[]> ignore = new TreeSet<byte[]>(NaturalOrder.naturalOrder); // a set of urlhashes to be ignored. This is generated from all hashes that are seen during recursion to prevent enless loops
        Set<byte[]> levelhashes = new TreeSet<byte[]>(NaturalOrder.naturalOrder); // all hashes of a clickdepth. The first call contains the target hash only and therefore just one entry
        levelhashes.add(searchhash);
        int leveldepth = 0; // the recursion depth and therefore the result depth-1. Shall be 0 for the first call
        final byte[] hosthash = new byte[6]; // the host of the url to be checked
        System.arraycopy(searchhash, 6, hosthash, 0, 6);
        
        long timeout = System.currentTimeMillis() + maxtime;
        mainloop: for (int maxdepth = 0; maxdepth < 6 && System.currentTimeMillis() < timeout; maxdepth++) {
            
            Set<byte[]> checknext = new TreeSet<byte[]>(NaturalOrder.naturalOrder);
            
            // loop over all hashes at this clickdepth; the first call to this loop should contain only one hash and a leveldepth = 0
            checkloop: for (byte[] urlhash: levelhashes) {
    
                // get all the citations for this url and iterate
                ReferenceReport rr = rrc.getReferenceReport(urlhash, false);
                //ReferenceContainer<CitationReference> references = this.urlCitationIndex.get(urlhash, null);
                if (rr == null || rr.getInternalCount() == 0) continue checkloop; // don't know
                Iterator<byte[]> i = rr.getInternallIDs().iterator();
                nextloop: while (i.hasNext()) {
                    byte[] u = i.next();
                    if (u == null) continue nextloop;
                    
                    // check if this is from the same host
                    assert (ByteBuffer.equals(u, 6, hosthash, 0, 6));
                    
                    // check ignore
                    if (ignore.contains(u)) continue nextloop;
                    
                    // check if the url is a root url
                    if (rootCandidates.has(u)) {
                        return leveldepth + 1;
                    }
                    
                    checknext.add(u);
                    ignore.add(u);
                }
                if (System.currentTimeMillis() > timeout) break mainloop;
            }
            leveldepth++;
            levelhashes = checknext;
        }
        return 999;
    }
    
    private static RowHandleSet getPossibleRootHashes(DigestURL url) {
        RowHandleSet rootCandidates = new RowHandleSet(URIMetadataRow.rowdef.primaryKeyLength, URIMetadataRow.rowdef.objectOrder, 10);
        String rootStub = url.getProtocol() + "://" + url.getHost();
        try {
            rootCandidates.put(new DigestURL(rootStub).hash());
            rootCandidates.put(new DigestURL(rootStub + "/").hash());
            rootCandidates.put(new DigestURL(rootStub + "/index.htm").hash());
            rootCandidates.put(new DigestURL(rootStub + "/index.html").hash());
            rootCandidates.put(new DigestURL(rootStub + "/index.php").hash());
            rootCandidates.put(new DigestURL(rootStub + "/home.htm").hash());
            rootCandidates.put(new DigestURL(rootStub + "/home.html").hash());
            rootCandidates.put(new DigestURL(rootStub + "/home.php").hash());
            rootCandidates.put(new DigestURL(rootStub + "/default.htm").hash());
            rootCandidates.put(new DigestURL(rootStub + "/default.html").hash());
            rootCandidates.put(new DigestURL(rootStub + "/default.php").hash());
            rootCandidates.optimize();
        } catch (final Throwable e) {}
        return rootCandidates;
    }

    public ReferenceReportCache getReferenceReportCache()  {
        return new ReferenceReportCache();
    }
    
    public class ReferenceReportCache {
        Map<byte[], ReferenceReport> cache;
        public ReferenceReportCache() {
            this.cache = new TreeMap<byte[], ReferenceReport>(Base64Order.enhancedCoder);
        }
        public ReferenceReport getReferenceReport(final byte[] id, final boolean acceptSelfReference) throws IOException {
            ReferenceReport rr = cache.get(id);
            if (rr != null) return rr;
            try {
                rr = new ReferenceReport(id, acceptSelfReference);
                cache.put(id, rr);
                return rr;
            } catch (final SpaceExceededException e) {
                ConcurrentLog.logException(e);
                throw new IOException(e.getMessage());
            }
        }
    }
    
    public ClickdepthCache getClickdepthCache(ReferenceReportCache rrc)  {
        return new ClickdepthCache(rrc);
    }
    
    public class ClickdepthCache {
        ReferenceReportCache rrc;
        Map<byte[], Integer> cache;
        public ClickdepthCache(ReferenceReportCache rrc) {
            this.rrc = rrc;
            this.cache = new TreeMap<byte[], Integer>(Base64Order.enhancedCoder);
        }
        public int getClickdepth(final DigestURL url, int maxtime) throws IOException {
            Integer clickdepth = cache.get(url.hash());
            if (clickdepth != null) {
                //ConcurrentLog.info("Segment", "get clickdepth of url " + url.toNormalform(true) + ": " + clickdepth + " CACHE HIT");
                return clickdepth.intValue();
            }
            clickdepth = Segment.this.getClickDepth(this.rrc, url, maxtime);
            //ConcurrentLog.info("Segment", "get clickdepth of url " + url.toNormalform(true) + ": " + clickdepth);
            this.cache.put(url.hash(), clickdepth);
            return clickdepth.intValue();
        }
    }
    
    /**
     * A ReferenceReport object is a container for all referenced to a specific url.
     * The class stores the number of links from domain-internal and domain-external backlinks,
     * and the host hashes of all externally linking documents,
     * all IDs from external hosts and all IDs from the same domain.
     */
    public final class ReferenceReport {
        private int internal, external;
        private HandleSet externalHosts, externalIDs, internalIDs;
        public ReferenceReport(final byte[] id, final boolean acceptSelfReference) throws IOException, SpaceExceededException {
            this.internal = 0;
            this.external = 0;
            this.externalHosts = new RowHandleSet(6, Base64Order.enhancedCoder, 0);
            this.internalIDs = new RowHandleSet(12, Base64Order.enhancedCoder, 0);
            this.externalIDs = new RowHandleSet(12, Base64Order.enhancedCoder, 0);
            if (connectedCitation()) {
                // read the references from the citation index
                ReferenceContainer<CitationReference> references;
                references = urlCitation().get(id, null);
                if (references == null) return; // no references at all
                Iterator<CitationReference> ri = references.entries();
                while (ri.hasNext()) {
                    CitationReference ref = ri.next();
                    byte[] hh = ref.hosthash(); // host hash
                    if (ByteBuffer.equals(hh, 0, id, 6, 6)) {
                        internalIDs.put(ref.urlhash());
                        internal++;
                    } else {
                        externalHosts.put(hh);
                        externalIDs.put(ref.urlhash());
                        external++;
                    }
                }
            }
            if ((internalIDs.size() == 0 || !connectedCitation()) && Segment.this.fulltext.writeToWebgraph()) {
                // reqd the references from the webgraph
                SolrConnector webgraph = Segment.this.fulltext.getWebgraphConnector();
                BlockingQueue<SolrDocument> docs = webgraph.concurrentDocumentsByQuery("{!raw f=" + WebgraphSchema.target_id_s.getSolrFieldName() + "}" + ASCII.String(id), 0, 10000000, 1000, 100, WebgraphSchema.source_id_s.getSolrFieldName());
                SolrDocument doc;
                try {
                    while ((doc = docs.take()) != AbstractSolrConnector.POISON_DOCUMENT) {
                        String refid = (String) doc.getFieldValue(WebgraphSchema.source_id_s.getSolrFieldName());
                        if (refid == null) continue;
                        byte[] refidh = ASCII.getBytes(refid);
                        byte[] hh = new byte[6]; // host hash
                        System.arraycopy(refidh, 6, hh, 0, 6);
                        if (ByteBuffer.equals(hh, 0, id, 6, 6)) {
                            if (acceptSelfReference || !ByteBuffer.equals(refidh, id)) {
                                internalIDs.put(refidh);
                                internal++;
                            }
                        } else {
                            externalHosts.put(hh);
                            externalIDs.put(refidh);
                            external++;
                        }
                    }
                } catch (final InterruptedException e) {
                    ConcurrentLog.logException(e);
                }
            }
        }
        public int getInternalCount() {
            return this.internal;
        }
        public int getExternalCount() {
            return this.external;
        }
        public HandleSet getExternalHostIDs() {
            return this.externalHosts;
        }
        public HandleSet getExternalIDs() {
            return this.externalIDs;
        }
        public HandleSet getInternallIDs() {
            return this.internalIDs;
        }
    }
    
    public long RWICount() {
        if (this.termIndex == null) return 0;
        return this.termIndex.sizesMax();
    }
    
    public long RWISegmentCount() {
        if (this.termIndex == null) return 0;
        return this.termIndex.getSegmentCount();
    }

    public int RWIBufferCount() {
        if (this.termIndex == null) return 0;
        return this.termIndex.getBufferSize();
    }

    /**
     * get a guess about the word count. This is only a guess because it uses the term index if present and this index may be
     * influenced by index transmission processes in its statistic word distribution. However, it can be a hint for heuristics
     * which use the word count. Please do NOT use this if the termIndex is not present because it otherwise uses the solr index
     * which makes it painfull slow.
     * @param word
     * @return the number of references for this word.
     */
    public int getWordCountGuess(String word) {
        if (this.fulltext.getDefaultConnector() == null) return 0;
        if (word == null || word.indexOf(':') >= 0 || word.indexOf(' ') >= 0 || word.indexOf('/') >= 0) return 0;
        if (this.termIndex != null) {
            int count = this.termIndex.count(Word.word2hash(word));
            if (count > 0) return count;
        }
        try {
            return (int) this.fulltext.getDefaultConnector().getCountByQuery(CollectionSchema.text_t.getSolrFieldName() + ":\"" + word + "\"");
        } catch (final Throwable e) {
            ConcurrentLog.warn("Segment", "problem with word guess for word: " + word);
            ConcurrentLog.logException(e);
            return 0;
        }
    }

    @Deprecated
    public boolean exists(final String urlhash) {
        return this.fulltext.exists(urlhash);
    }

    /**
     * Multiple-test for existing url hashes in the search index.
     * All given ids are tested and a subset of the given ids are returned.
     * @param ids
     * @return a set of ids which exist in the database
     */
    public Set<String> exists(final Set<String> ids) {
        return this.fulltext.exists(ids);
    }

    /**
     * discover all urls that start with a given url stub
     * @param stub
     * @return an iterator for all matching urls
     */
    public Iterator<DigestURL> urlSelector(final MultiProtocolURL stub, final long maxtime, final int maxcount) {
        final BlockingQueue<SolrDocument> docQueue;
        final String urlstub;
        if (stub == null) {
            docQueue = this.fulltext.getDefaultConnector().concurrentDocumentsByQuery(AbstractSolrConnector.CATCHALL_TERM, 0, Integer.MAX_VALUE, maxtime, maxcount, CollectionSchema.id.getSolrFieldName(), CollectionSchema.sku.getSolrFieldName());
            urlstub = null;
        } else {
            final String host = stub.getHost();
            String hh = DigestURL.hosthash(host);
            docQueue = this.fulltext.getDefaultConnector().concurrentDocumentsByQuery(CollectionSchema.host_id_s + ":\"" + hh + "\"", 0, Integer.MAX_VALUE, maxtime, maxcount, CollectionSchema.id.getSolrFieldName(), CollectionSchema.sku.getSolrFieldName());
            urlstub = stub.toNormalform(true);
        }

        // now filter the stub from the iterated urls
        return new LookAheadIterator<DigestURL>() {
            @Override
            protected DigestURL next0() {
                while (true) {
                    SolrDocument doc;
                    try {
                        doc = docQueue.take();
                    } catch (final InterruptedException e) {
                        ConcurrentLog.logException(e);
                        return null;
                    }
                    if (doc == null || doc == AbstractSolrConnector.POISON_DOCUMENT) return null;
                    String u = (String) doc.getFieldValue(CollectionSchema.sku.getSolrFieldName());
                    String id =  (String) doc.getFieldValue(CollectionSchema.id.getSolrFieldName());
                    DigestURL url;
                    try {
                        url = new DigestURL(u, ASCII.getBytes(id));
                    } catch (final MalformedURLException e) {
                        continue;
                    }
                    if (urlstub == null || u.startsWith(urlstub)) return url;
                }
            }
        };
    }

    public void clear() {
        try {
            if (this.termIndex != null) this.termIndex.clear();
            if (this.fulltext != null) this.fulltext.clearURLIndex();
            if (this.fulltext != null) this.fulltext.clearLocalSolr();
            if (this.fulltext != null) this.fulltext.clearRemoteSolr();
            if (this.urlCitationIndex != null) this.urlCitationIndex.clear();
        } catch (final IOException e) {
            ConcurrentLog.logException(e);
        }
    }
    
    public void clearCaches() {
        if (this.urlCitationIndex != null) this.urlCitationIndex.clearCache();
        if (this.termIndex != null) this.termIndex.clearCache();
        this.fulltext.clearCaches();
    }

    public File getLocation() {
        return this.segmentPath;
    }

    public synchronized void close() {
        this.indexingPutDocumentProcessor.shutdown();
    	if (this.termIndex != null) this.termIndex.close();
        if (this.fulltext != null) this.fulltext.close();
        if (this.urlCitationIndex != null) this.urlCitationIndex.close();
    }

    private static String votedLanguage(
                    final DigestURL url,
                    final String urlNormalform,
                    final Document document,
                    final Condenser condenser) {
     // do a identification of the language
        String language = condenser.language(); // this is a statistical analysation of the content: will be compared with other attributes
        final String bymetadata = document.dc_language(); // the languageByMetadata may return null if there was no declaration
        if (language == null) {
            // no statistics available, we take either the metadata (if given) or the TLD
            language = (bymetadata == null) ? url.language() : bymetadata;
        } else {
            if (bymetadata == null) {
                // two possible results: compare and report conflicts
                if (!language.equals(url.language())) {
                    // see if we have a hint in the url that the statistic was right
                    final String u = urlNormalform.toLowerCase();
                    if (!u.contains("/" + language + "/") && !u.contains("/" + ISO639.country(language).toLowerCase() + "/")) {
                        // no confirmation using the url, use the TLD
                        language = url.language();
                    } else {
                        // this is a strong hint that the statistics was in fact correct
                    }
                }
            } else {
                // here we have three results: we can do a voting
                if (language.equals(bymetadata)) {
                    //if (log.isFine()) log.logFine("LANGUAGE-BY-STATISTICS: " + entry.url() + " CONFIRMED - METADATA IDENTICAL: " + language);
                } else if (language.equals(url.language())) {
                    //if (log.isFine()) log.logFine("LANGUAGE-BY-STATISTICS: " + entry.url() + " CONFIRMED - TLD IS IDENTICAL: " + language);
                } else if (bymetadata.equals(url.language())) {
                    //if (log.isFine()) log.logFine("LANGUAGE-BY-STATISTICS: " + entry.url() + " CONFLICTING: " + language + " BUT METADATA AND TLD ARE IDENTICAL: " + bymetadata + ")");
                    language = bymetadata;
                } else {
                    //if (log.isFine()) log.logFine("LANGUAGE-BY-STATISTICS: " + entry.url() + " CONFLICTING: ALL DIFFERENT! statistic: " + language + ", metadata: " + bymetadata + ", TLD: + " + entry.url().language() + ". taking metadata.");
                    language = bymetadata;
                }
            }
        }
        return language;
    }

    public void storeRWI(final ReferenceContainer<WordReference> wordContainer) throws IOException, SpaceExceededException {
        if (this.termIndex != null) this.termIndex.add(wordContainer);
    }

    public void storeRWI(final byte[] termHash, final WordReference entry) throws IOException, SpaceExceededException {
        if (this.termIndex != null) this.termIndex.add(termHash, entry);
    }

    /**
     * putDocument should not be called directly; instead, put queueEntries to
     * indexingPutDocumentProcessor
     * (this must be public, otherwise the WorkflowProcessor - calling by reflection - does not work)
     * ATTENTION: do not remove! profiling tools will show that this is not called, which is not true (using reflection)
     * @param queueEntry
     * @throws IOException
     */
    public void putDocument(final StorageQueueEntry queueEntry) {
        try {
            this.fulltext().putDocument(queueEntry.queueEntry);
        } catch (final IOException e) {
            ConcurrentLog.logException(e);
        }
    }
    
    /**
     * put a solr document into the index. This is the right point to call when enqueueing of solr document is wanted.
     * This method exist to prevent that solr is filled concurrently with data which makes it fail or throw strange exceptions.
     * @param queueEntry
     */
    public void putDocumentInQueue(final SolrInputDocument queueEntry) {
        this.indexingPutDocumentProcessor.enQueue(new StorageQueueEntry(queueEntry));
    }

    public SolrInputDocument storeDocument(
            final DigestURL url,
            final DigestURL referrerURL,
            final Map<String, Pattern> collections,
            final ResponseHeader responseHeader,
            final Document document,
            final Condenser condenser,
            final SearchEvent searchEvent,
            final String sourceName, // contains the crawl profile hash if this comes from a web crawl
            final boolean storeToRWI
            ) {
        final long startTime = System.currentTimeMillis();
        
        // CREATE INDEX

        // load some document metadata
        final Date loadDate = new Date();
        final String id = ASCII.String(url.hash());
        final String dc_title = document.dc_title();
        final String urlNormalform = url.toNormalform(true);
        final String language = votedLanguage(url, urlNormalform, document, condenser); // identification of the language

        // STORE URL TO LOADED-URL-DB
        Date modDate = responseHeader == null ? new Date() : responseHeader.lastModified();
        if (modDate.getTime() > loadDate.getTime()) modDate = loadDate;
        char docType = Response.docType(document.dc_format());
        
        // CREATE SOLR DOCUMENT
        final CollectionConfiguration.SolrVector vector = this.fulltext.getDefaultConfiguration().yacy2solr(collections, responseHeader, document, condenser, referrerURL, language, this.fulltext.getWebgraphConfiguration(), sourceName);
        
        // ENRICH DOCUMENT WITH RANKING INFORMATION
        if (this.connectedCitation()) {
            this.fulltext.getDefaultConfiguration().postprocessing_references(this.getReferenceReportCache(), vector, url, null);
        }
        // STORE TO SOLR
        String error = null;
        this.putDocumentInQueue(vector);
        List<SolrInputDocument> webgraph = vector.getWebgraphDocuments();
        if (webgraph != null && webgraph.size() > 0) {
            
            // write the edges to the webgraph solr index
            if (this.fulltext.writeToWebgraph()) {
                tryloop: for (int i = 0; i < 20; i++) {
                    try {
                        error = null;
                        this.fulltext.putEdges(webgraph);
                        break tryloop;
                    } catch (final IOException e ) {
                        error = "failed to send " + urlNormalform + " to solr: " + e.getMessage();
                        ConcurrentLog.warn("SOLR", error);
                        if (i == 10) this.fulltext.commit(true);
                        try {Thread.sleep(1000);} catch (final InterruptedException e1) {}
                        continue tryloop;
                    }
                }
            }
        
            // write the edges to the citation reference index
            if (this.connectedCitation()) try {
                // normal links
                for (SolrInputDocument edge: webgraph) {
                    String referrerhash = (String) edge.getFieldValue(WebgraphSchema.source_id_s.getSolrFieldName());
                    String anchorhash = (String) edge.getFieldValue(WebgraphSchema.target_id_s.getSolrFieldName());
                    if (referrerhash != null && anchorhash != null) {
                        urlCitationIndex.add(ASCII.getBytes(anchorhash), new CitationReference(ASCII.getBytes(referrerhash), loadDate.getTime()));
                    }
                }
                // media links as well!
                for (AnchorURL image: document.getImages().keySet()) urlCitationIndex.add(image.hash(), new CitationReference(url.hash(), loadDate.getTime()));
                for (AnchorURL audio: document.getAudiolinks().keySet()) urlCitationIndex.add(audio.hash(), new CitationReference(url.hash(), loadDate.getTime()));
                for (AnchorURL video: document.getVideolinks().keySet()) urlCitationIndex.add(video.hash(), new CitationReference(url.hash(), loadDate.getTime()));
            } catch (Throwable e) {
                ConcurrentLog.logException(e);
            }
        }
        
        if (error != null) {
            ConcurrentLog.severe("SOLR", error + ", PLEASE REPORT TO bugs.yacy.net");
            //Switchboard.getSwitchboard().pauseCrawlJob(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL, error);
            //Switchboard.getSwitchboard().pauseCrawlJob(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL, error);
        }
        final long storageEndTime = System.currentTimeMillis();

        // STORE PAGE INDEX INTO WORD INDEX DB
        int outlinksSame = document.inboundLinks().size();
        int outlinksOther = document.outboundLinks().size();
        final int urlLength = urlNormalform.length();
        final int urlComps = MultiProtocolURL.urlComps(url.toString()).length;

        // create a word prototype which is re-used for all entries
        if ((this.termIndex != null && storeToRWI) || searchEvent != null) {
            final int len = (document == null) ? urlLength : document.dc_title().length();
            final WordReferenceRow ientry = new WordReferenceRow(
                            url.hash(),
                            urlLength, urlComps, len,
                            condenser.RESULT_NUMB_WORDS,
                            condenser.RESULT_NUMB_SENTENCES,
                            modDate.getTime(),
                            System.currentTimeMillis(),
                            UTF8.getBytes(language),
                            docType,
                            outlinksSame, outlinksOther);
    
            // iterate over all words of content text
            Word wprop = null;
            byte[] wordhash;
            String word;
            for (Map.Entry<String, Word> wentry: condenser.words().entrySet()) {
                word = wentry.getKey();
                wprop = wentry.getValue();
                assert (wprop.flags != null);
                ientry.setWord(wprop);
                wordhash = Word.word2hash(word);
                if (this.termIndex != null && storeToRWI) try {
                    this.termIndex.add(wordhash, ientry);
                } catch (final Exception e) {
                    ConcurrentLog.logException(e);
                }
    
                // during a search event it is possible that a heuristic is used which aquires index
                // data during search-time. To transfer indexed data directly to the search process
                // the following lines push the index data additionally to the search process
                // this is done only for searched words
                if (searchEvent != null && !searchEvent.query.getQueryGoal().getExcludeHashes().has(wordhash) && searchEvent.query.getQueryGoal().getIncludeHashes().has(wordhash)) {
                    // if the page was added in the context of a heuristic this shall ensure that findings will fire directly into the search result
                    ReferenceContainer<WordReference> container;
                    try {
                        container = ReferenceContainer.emptyContainer(Segment.wordReferenceFactory, wordhash, 1);
                        container.add(ientry);
                        searchEvent.addRWIs(container, true, sourceName, 1, 5000);
                    } catch (final SpaceExceededException e) {
                        continue;
                    }
                }
            }
            if (searchEvent != null) searchEvent.addFinalize();
    
            // assign the catchall word
            ientry.setWord(wprop == null ? catchallWord : wprop); // we use one of the word properties as template to get the document characteristics
            if (this.termIndex != null) try {
                this.termIndex.add(catchallHash, ientry);
            } catch (final Exception e) {
                ConcurrentLog.logException(e);
            }
        }

        // finish index time
        final long indexingEndTime = System.currentTimeMillis();

        if (this.log.isInfo()) {
            this.log.info("*Indexed " + condenser.words().size() + " words in URL " + url +
                    " [" + id + "]" +
                    "\n\tDescription:  " + dc_title +
                    "\n\tMimeType: "  + document.dc_format() + " | Charset: " + document.getCharset() + " | " +
                    "Size: " + document.getTextLength() + " bytes | " +
                    //"Anchors: " + refs +
                    "\n\tLinkStorageTime: " + (storageEndTime - startTime) + " ms | " +
                    "indexStorageTime: " + (indexingEndTime - storageEndTime) + " ms");
        }

        // finished
        return vector;
    }

    public void removeAllUrlReferences(final HandleSet urls, final LoaderDispatcher loader, final ClientIdentification.Agent agent, final CacheStrategy cacheStrategy) {
        for (final byte[] urlhash: urls) removeAllUrlReferences(urlhash, loader, agent, cacheStrategy);
    }

    /**
     * find all the words in a specific resource and remove the url reference from every word index
     * finally, delete the url entry
     * @param urlhash the hash of the url that shall be removed
     * @param loader
     * @param cacheStrategy
     * @return number of removed words
     */
    public int removeAllUrlReferences(final byte[] urlhash, final LoaderDispatcher loader, final ClientIdentification.Agent agent, final CacheStrategy cacheStrategy) {

        if (urlhash == null) return 0;
        // determine the url string
        final DigestURL url = fulltext().getURL(urlhash);
        if (url == null) return 0;

        try {
            // parse the resource
            final Document document = Document.mergeDocuments(url, null, loader.loadDocuments(loader.request(url, true, false), cacheStrategy, Integer.MAX_VALUE, null, agent));
            if (document == null) {
                // delete just the url entry
                fulltext().remove(urlhash);
                return 0;
            }
            // get the word set
            Set<String> words = null;
            words = new Condenser(document, true, true, null, null, false).words().keySet();

            // delete all word references
            int count = 0;
            if (words != null && termIndex() != null) count = termIndex().remove(Word.words2hashesHandles(words), urlhash);

            // finally delete the url entry itself
            fulltext().remove(urlhash);
            return count;
        } catch (final Parser.Failure e) {
            return 0;
        } catch (final IOException e) {
            ConcurrentLog.logException(e);
            return 0;
        }
    }

}
