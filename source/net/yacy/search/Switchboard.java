// plasmaSwitchboard.java
// (C) 2004-2007 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 2004 on http://yacy.net
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

/*
   This class holds the run-time environment of the plasma
   Search Engine. It's data forms a blackboard which can be used
   to organize running jobs around the indexing algorithm.
   The blackboard consist of the following entities:
   - storage: one plasmaStore object with the url-based database
   - configuration: initialized by properties once, then by external functions
   - job queues: for parsing, condensing, indexing
 */

package net.yacy.search;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.yacy.cora.date.GenericFormatter;
import net.yacy.cora.document.ASCII;
import net.yacy.cora.document.MultiProtocolURI;
import net.yacy.cora.document.RSSFeed;
import net.yacy.cora.document.RSSMessage;
import net.yacy.cora.document.RSSReader;
import net.yacy.cora.document.UTF8;
import net.yacy.cora.protocol.ClientIdentification;
import net.yacy.cora.protocol.ConnectionInfo;
import net.yacy.cora.protocol.Domains;
import net.yacy.cora.protocol.HeaderFramework;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.protocol.ResponseHeader;
import net.yacy.cora.protocol.TimeoutRequest;
import net.yacy.cora.protocol.http.HTTPClient;
import net.yacy.cora.protocol.http.ProxySettings;
import net.yacy.cora.services.federated.solr.SolrScheme;
import net.yacy.cora.services.federated.solr.SolrShardingConnector;
import net.yacy.cora.services.federated.solr.SolrShardingSelection;
import net.yacy.cora.services.federated.yacy.CacheStrategy;
import net.yacy.document.Condenser;
import net.yacy.document.Document;
import net.yacy.document.LibraryProvider;
import net.yacy.document.Parser;
import net.yacy.document.TextParser;
import net.yacy.document.content.DCEntry;
import net.yacy.document.content.SurrogateReader;
import net.yacy.document.importer.OAIListFriendsLoader;
import net.yacy.document.parser.html.Evaluation;
import net.yacy.gui.Tray;
import net.yacy.kelondro.blob.Tables;
import net.yacy.kelondro.data.meta.DigestURI;
import net.yacy.kelondro.data.meta.URIMetadataRow;
import net.yacy.kelondro.data.word.Word;
import net.yacy.kelondro.index.HandleSet;
import net.yacy.kelondro.index.RowSpaceExceededException;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.order.Base64Order;
import net.yacy.kelondro.order.Digest;
import net.yacy.kelondro.order.NaturalOrder;
import net.yacy.kelondro.rwi.ReferenceContainer;
import net.yacy.kelondro.util.EventTracker;
import net.yacy.kelondro.util.FileUtils;
import net.yacy.kelondro.util.MemoryControl;
import net.yacy.kelondro.util.MemoryTracker;
import net.yacy.kelondro.util.OS;
import net.yacy.kelondro.util.SetTools;
import net.yacy.kelondro.workflow.BusyThread;
import net.yacy.kelondro.workflow.InstantBusyThread;
import net.yacy.kelondro.workflow.WorkflowJob;
import net.yacy.kelondro.workflow.WorkflowProcessor;
import net.yacy.kelondro.workflow.WorkflowThread;
import net.yacy.peers.EventChannel;
import net.yacy.peers.Network;
import net.yacy.peers.NewsPool;
import net.yacy.peers.Protocol;
import net.yacy.peers.Seed;
import net.yacy.peers.SeedDB;
import net.yacy.peers.dht.Dispatcher;
import net.yacy.peers.dht.PeerSelection;
import net.yacy.peers.graphics.WebStructureGraph;
import net.yacy.peers.operation.yacyBuildProperties;
import net.yacy.peers.operation.yacyRelease;
import net.yacy.peers.operation.yacyUpdateLocation;
import net.yacy.repository.Blacklist;
import net.yacy.repository.FilterEngine;
import net.yacy.repository.LoaderDispatcher;
import net.yacy.search.index.Segment;
import net.yacy.search.index.Segments;
import net.yacy.search.query.AccessTracker;
import net.yacy.search.query.QueryParams;
import net.yacy.search.query.SearchEvent;
import net.yacy.search.query.SearchEventCache;
import net.yacy.search.ranking.BlockRank;
import net.yacy.search.ranking.RankingProfile;
import net.yacy.search.snippet.ContentDomain;
import de.anomic.crawler.CrawlProfile;
import de.anomic.crawler.CrawlQueues;
import de.anomic.crawler.CrawlStacker;
import de.anomic.crawler.CrawlSwitchboard;
import de.anomic.crawler.NoticedURL;
import de.anomic.crawler.ResourceObserver;
import de.anomic.crawler.ResultImages;
import de.anomic.crawler.ResultURLs;
import de.anomic.crawler.ResultURLs.EventOrigin;
import de.anomic.crawler.RobotsTxt;
import de.anomic.crawler.ZURL.FailCategory;
import de.anomic.crawler.retrieval.Request;
import de.anomic.crawler.retrieval.Response;
import de.anomic.data.BlogBoard;
import de.anomic.data.BlogBoardComments;
import de.anomic.data.BookmarksDB;
import de.anomic.data.ListManager;
import de.anomic.data.MessageBoard;
import de.anomic.data.URLLicense;
import de.anomic.data.UserDB;
import de.anomic.data.WorkTables;
import de.anomic.data.wiki.WikiBoard;
import de.anomic.data.wiki.WikiCode;
import de.anomic.data.wiki.WikiParser;
import de.anomic.http.client.Cache;
import de.anomic.http.server.RobotsTxtConfig;
import de.anomic.server.serverCore;
import de.anomic.server.serverSwitch;
import de.anomic.tools.CryptoLib;
import de.anomic.tools.UPnP;
import de.anomic.tools.crypt;

public final class Switchboard extends serverSwitch
{

    // load slots
    public static int xstackCrawlSlots = 2000;
    public static long lastPPMUpdate = System.currentTimeMillis() - 30000;
    private static final int dhtMaxContainerCount = 500;
    private int dhtMaxReferenceCount = 1000;

    // colored list management
    public static SortedSet<String> badwords = new TreeSet<String>(NaturalOrder.naturalComparator);
    public static SortedSet<String> stopwords = new TreeSet<String>(NaturalOrder.naturalComparator);
    public static SortedSet<String> blueList = null;
    public static HandleSet badwordHashes = null;
    public static HandleSet blueListHashes = null;
    public static HandleSet stopwordHashes = null;
    public static Blacklist urlBlacklist = null;

    public static WikiParser wikiParser = null;

    // storage management
    public File htCachePath;
    public final File dictionariesPath;
    public File listsPath;
    public File htDocsPath;
    public File workPath;
    public File releasePath;
    public File networkRoot;
    public File queuesRoot;
    public File surrogatesInPath;
    public File surrogatesOutPath;
    public Segments indexSegments;
    public LoaderDispatcher loader;
    public CrawlSwitchboard crawler;
    public CrawlQueues crawlQueues;
    public CrawlStacker crawlStacker;
    public MessageBoard messageDB;
    public WikiBoard wikiDB;
    public BlogBoard blogDB;
    public BlogBoardComments blogCommentDB;
    public RobotsTxt robots;
    public Map<String, Object[]> outgoingCookies, incomingCookies;
    public volatile long proxyLastAccess, localSearchLastAccess, remoteSearchLastAccess;
    public Network yc;
    public ResourceObserver observer;
    public UserDB userDB;
    public BookmarksDB bookmarksDB;
    public WebStructureGraph webStructure;
    public ConcurrentHashMap<String, TreeSet<Long>> localSearchTracker, remoteSearchTracker; // mappings from requesting host to a TreeSet of Long(access time)
    public long indexedPages = 0;
    public int searchQueriesRobinsonFromLocal = 0; // absolute counter of all local queries submitted on this peer from a local or autheticated used
    public int searchQueriesRobinsonFromRemote = 0; // absolute counter of all local queries submitted on this peer from a remote IP without authentication
    public float searchQueriesGlobal = 0f; // partial counter of remote queries (1/number-of-requested-peers)
    public SortedMap<byte[], String> clusterhashes; // map of peerhash(String)/alternative-local-address as ip:port or only ip (String) or null if address in seed should be used
    public URLLicense licensedURLs;
    public List<Pattern> networkWhitelist, networkBlacklist;
    public FilterEngine domainList;
    private Dispatcher dhtDispatcher;
    public LinkedBlockingQueue<String> trail;
    public SeedDB peers;
    public WorkTables tables;
    public Tray tray;

    public WorkflowProcessor<indexingQueueEntry> indexingDocumentProcessor;
    public WorkflowProcessor<indexingQueueEntry> indexingCondensementProcessor;
    public WorkflowProcessor<indexingQueueEntry> indexingAnalysisProcessor;
    public WorkflowProcessor<indexingQueueEntry> indexingStorageProcessor;

    public RobotsTxtConfig robotstxtConfig = null;
    public boolean useTailCache;
    public boolean exceed134217727;

    private final Semaphore shutdownSync = new Semaphore(0);
    private boolean terminate = false;

    //private Object  crawlingPausedSync = new Object();
    //private boolean crawlingIsPaused = false;

    public HashMap<String, Object[]> crawlJobsStatus = new HashMap<String, Object[]>();

    private static Switchboard sb = null;

    public Switchboard(final File dataPath, final File appPath, final String initPath, final String configPath)
        throws IOException {
        super(dataPath, appPath, initPath, configPath);

        // check if port is already occupied
        final int port = getConfigInt("port", 8090);
        try {
            if ( TimeoutRequest.ping("127.0.0.1", port, 500) ) {
                throw new RuntimeException(
                    "a server is already running on the YaCy port "
                        + port
                        + "; possibly another YaCy process has not terminated yet. Please stop YaCy before running a new instance.");
            }
        } catch ( final ExecutionException e1 ) {
        }

        MemoryTracker.startSystemProfiling();
        sb = this;

        // set loglevel and log
        setLog(new Log("YACY_SEARCH"));

        // set default peer name
        Seed.ANON_PREFIX = getConfig("peernameprefix", "_anon");

        // UPnP port mapping
        if ( getConfigBool(SwitchboardConstants.UPNP_ENABLED, false) ) {
            InstantBusyThread.oneTimeJob(UPnP.class, "addPortMapping", UPnP.log, 0);
        }

        // init TrayIcon if possible
        this.tray = new Tray(this);

        // remote proxy configuration
        initRemoteProxy();

        // memory configuration
        this.useTailCache = getConfigBool("ramcopy", true);
        if ( MemoryControl.available() > 1024 * 1024 * 1024 * 1 ) {
            this.useTailCache = true;
        }
        this.exceed134217727 = getConfigBool("exceed134217727", true);
        if ( MemoryControl.available() > 1024 * 1024 * 1024 * 2 ) {
            this.exceed134217727 = true;
        }

        // load values from configs
        final File indexPath =
            getDataPath(SwitchboardConstants.INDEX_PRIMARY_PATH, SwitchboardConstants.INDEX_PATH_DEFAULT);
        this.log.logConfig("Index Primary Path: " + indexPath.toString());
        this.listsPath =
            getDataPath(SwitchboardConstants.LISTS_PATH, SwitchboardConstants.LISTS_PATH_DEFAULT);
        this.log.logConfig("Lists Path:     " + this.listsPath.toString());
        this.htDocsPath =
            getDataPath(SwitchboardConstants.HTDOCS_PATH, SwitchboardConstants.HTDOCS_PATH_DEFAULT);
        this.log.logConfig("HTDOCS Path:    " + this.htDocsPath.toString());
        this.workPath = getDataPath(SwitchboardConstants.WORK_PATH, SwitchboardConstants.WORK_PATH_DEFAULT);
        this.log.logConfig("Work Path:    " + this.workPath.toString());
        this.dictionariesPath =
            getDataPath(
                SwitchboardConstants.DICTIONARY_SOURCE_PATH,
                SwitchboardConstants.DICTIONARY_SOURCE_PATH_DEFAULT);
        this.log.logConfig("Dictionaries Path:" + this.dictionariesPath.toString());

        // init global host name cache
        this.workPath.mkdirs();
        Domains.init(new File(this.workPath, "globalhosts.list"));

        // init sessionid name file
        final String sessionidNamesFile = getConfig("sessionidNamesFile", "defaults/sessionid.names");
        this.log.logConfig("Loading sessionid file " + sessionidNamesFile);
        MultiProtocolURI.initSessionIDNames(FileUtils.loadList(new File(getAppPath(), sessionidNamesFile)));

        // init tables
        this.tables = new WorkTables(this.workPath);

        // init libraries
        this.log.logConfig("initializing libraries");
        new Thread() {
            @Override
            public void run() {
                LibraryProvider.initialize(Switchboard.this.dictionariesPath);
            }
        }.start();

        // set a high maximum cache size to current size; this is adopted later automatically
        final int wordCacheMaxCount = (int) getConfigLong(SwitchboardConstants.WORDCACHE_MAX_COUNT, 20000);
        setConfig(SwitchboardConstants.WORDCACHE_MAX_COUNT, Integer.toString(wordCacheMaxCount));

        // load the network definition
        overwriteNetworkDefinition();

        // start indexing management
        this.log.logConfig("Starting Indexing Management");
        final String networkName = getConfig(SwitchboardConstants.NETWORK_NAME, "");
        final long fileSizeMax =
            (OS.isWindows) ? sb.getConfigLong("filesize.max.win", Integer.MAX_VALUE) : sb.getConfigLong(
                "filesize.max.other",
                Integer.MAX_VALUE);
        final int redundancy = (int) sb.getConfigLong("network.unit.dhtredundancy.senior", 1);
        final int partitionExponent = (int) sb.getConfigLong("network.unit.dht.partitionExponent", 0);
        this.networkRoot = new File(new File(indexPath, networkName), "NETWORK");
        this.queuesRoot = new File(new File(indexPath, networkName), "QUEUES");
        this.networkRoot.mkdirs();
        this.queuesRoot.mkdirs();
        final File mySeedFile = new File(this.networkRoot, SeedDB.DBFILE_OWN_SEED);
        this.peers =
            new SeedDB(
                this.networkRoot,
                "seed.new.heap",
                "seed.old.heap",
                "seed.pot.heap",
                mySeedFile,
                redundancy,
                partitionExponent,
                this.useTailCache,
                this.exceed134217727);

        // initialize index
        ReferenceContainer.maxReferences = getConfigInt("index.maxReferences", 0);
        final File segmentsPath = new File(new File(indexPath, networkName), "SEGMENTS");
        this.indexSegments =
            new Segments(
                this.log,
                segmentsPath,
                wordCacheMaxCount,
                fileSizeMax,
                this.useTailCache,
                this.exceed134217727);
        // set the default segment names
        setDefaultSegments();

        // load domainList
        try {
            this.domainList = null;
            if ( !getConfig("network.unit.domainlist", "").equals("") ) {
                final Reader r =
                    getConfigFileFromWebOrLocally(getConfig("network.unit.domainlist", ""), getAppPath()
                        .getAbsolutePath(), new File(this.networkRoot, "domainlist.txt"));
                this.domainList = new FilterEngine();
                this.domainList.loadList(new BufferedReader(r), null);
            }
        } catch ( final FileNotFoundException e ) {
            this.log.logSevere("CONFIG: domainlist not found: " + e.getMessage());
        } catch ( final IOException e ) {
            this.log.logSevere("CONFIG: error while retrieving domainlist: " + e.getMessage());
        }

        // create a crawler
        this.crawler = new CrawlSwitchboard(networkName, this.log, this.queuesRoot);

        // start yacy core
        this.log.logConfig("Starting YaCy Protocol Core");
        this.yc = new Network(this);
        InstantBusyThread.oneTimeJob(this, "loadSeedLists", Network.log, 0);
        //final long startedSeedListAquisition = System.currentTimeMillis();

        // init a DHT transmission dispatcher
        this.dhtDispatcher =
            (this.peers.sizeConnected() == 0) ? null : new Dispatcher(
                this.indexSegments.segment(Segments.Process.LOCALCRAWLING),
                this.peers,
                true,
                10000);

        // set up local robots.txt
        this.robotstxtConfig = RobotsTxtConfig.init(this);

        // setting timestamp of last proxy access
        this.proxyLastAccess = System.currentTimeMillis() - 10000;
        this.localSearchLastAccess = System.currentTimeMillis() - 10000;
        this.remoteSearchLastAccess = System.currentTimeMillis() - 10000;
        this.webStructure = new WebStructureGraph(new File(this.queuesRoot, "webStructure.map"));

        // configuring list path
        if ( !(this.listsPath.exists()) ) {
            this.listsPath.mkdirs();
        }

        // load coloured lists
        if ( blueList == null ) {
            // read only once upon first instantiation of this class
            final String f =
                getConfig(SwitchboardConstants.LIST_BLUE, SwitchboardConstants.LIST_BLUE_DEFAULT);
            final File plasmaBlueListFile = new File(f);
            if ( f != null ) {
                blueList = SetTools.loadList(plasmaBlueListFile, NaturalOrder.naturalComparator);
            } else {
                blueList = new TreeSet<String>();
            }
            blueListHashes = Word.words2hashesHandles(blueList);
            this.log.logConfig("loaded blue-list from file "
                + plasmaBlueListFile.getName()
                + ", "
                + blueList.size()
                + " entries, "
                + ppRamString(plasmaBlueListFile.length() / 1024));
        }

        // load blacklist
        this.log.logConfig("Loading blacklist ...");
        final File blacklistsPath =
            getDataPath(SwitchboardConstants.LISTS_PATH, SwitchboardConstants.LISTS_PATH_DEFAULT);
        urlBlacklist = new Blacklist(blacklistsPath);
        ListManager.switchboard = this;
        ListManager.listsPath = blacklistsPath;
        ListManager.reloadBlacklists();

        // load badwords (to filter the topwords)
        if ( badwords == null || badwords.isEmpty() ) {
            final File badwordsFile = new File(appPath, SwitchboardConstants.LIST_BADWORDS_DEFAULT);
            badwords = SetTools.loadList(badwordsFile, NaturalOrder.naturalComparator);
            badwordHashes = Word.words2hashesHandles(badwords);
            this.log.logConfig("loaded badwords from file "
                + badwordsFile.getName()
                + ", "
                + badwords.size()
                + " entries, "
                + ppRamString(badwordsFile.length() / 1024));
        }

        // load stopwords
        if ( stopwords == null || stopwords.isEmpty() ) {
            final File stopwordsFile = new File(appPath, SwitchboardConstants.LIST_STOPWORDS_DEFAULT);
            stopwords = SetTools.loadList(stopwordsFile, NaturalOrder.naturalComparator);
            stopwordHashes = Word.words2hashesHandles(stopwords);
            this.log.logConfig("loaded stopwords from file "
                + stopwordsFile.getName()
                + ", "
                + stopwords.size()
                + " entries, "
                + ppRamString(stopwordsFile.length() / 1024));
        }

        // load ranking from distribution
        final File rankingPath = new File(this.appPath, "ranking/YBR".replace('/', File.separatorChar));
        BlockRank.loadBlockRankTable(rankingPath, 16);

        // load distributed ranking
        // very large memory configurations allow to re-compute a ranking table
        /*
        final File hostIndexFile = new File(this.queuesRoot, "hostIndex.blob");
        if (MemoryControl.available() > 1024 * 1024 * 1024) new Thread() {
            public void run() {
                ReferenceContainerCache<HostReference> hostIndex; // this will get large, more than 0.5 million entries by now
                if (!hostIndexFile.exists()) {
                    hostIndex = BlockRank.collect(Switchboard.this.peers, Switchboard.this.webStructure, Integer.MAX_VALUE);
                    BlockRank.saveHostIndex(hostIndex, hostIndexFile);
                } else {
                    hostIndex = BlockRank.loadHostIndex(hostIndexFile);
                }

                // use an index segment to find hosts for given host hashes
                final String segmentName = getConfig(SwitchboardConstants.SEGMENT_PUBLIC, "default");
                final Segment segment = Switchboard.this.indexSegments.segment(segmentName);
                final MetadataRepository metadata = segment.urlMetadata();
                Map<String,HostStat> hostHashResolver;
                try {
                    hostHashResolver = metadata.domainHashResolver(metadata.domainSampleCollector());
                } catch (final IOException e) {
                    hostHashResolver = new HashMap<String, HostStat>();
                }

                // recursively compute a new ranking table
                Switchboard.this.log.logInfo("BLOCK RANK: computing new ranking tables...");
                BlockRank.ybrTables = BlockRank.evaluate(hostIndex, hostHashResolver, null, 0);
                hostIndex = null; // we don't need that here any more, so free the memory

                // use the web structure and the hostHash resolver to analyse the ranking table
                Switchboard.this.log.logInfo("BLOCK RANK: analysis of " + BlockRank.ybrTables.length + " tables...");
                BlockRank.analyse(Switchboard.this.webStructure, hostHashResolver);
                // store the new table
                Switchboard.this.log.logInfo("BLOCK RANK: storing fresh table...");
                BlockRank.storeBlockRankTable(rankingPath);
            }
        }.start();
        */

        // load the robots.txt db
        this.log.logConfig("Initializing robots.txt DB");
        this.robots = new RobotsTxt(this.tables);
        this.log.logConfig("Loaded robots.txt DB: " + this.robots.size() + " entries");

        // start a cache manager
        this.log.logConfig("Starting HT Cache Manager");

        // create the cache directory
        this.htCachePath =
            getDataPath(SwitchboardConstants.HTCACHE_PATH, SwitchboardConstants.HTCACHE_PATH_DEFAULT);
        this.log.logInfo("HTCACHE Path = " + this.htCachePath.getAbsolutePath());
        final long maxCacheSize =
            1024 * 1024 * Long.parseLong(getConfig(SwitchboardConstants.PROXY_CACHE_SIZE, "2")); // this is megabyte
        Cache.init(this.htCachePath, this.peers.mySeed().hash, maxCacheSize);

        // create the surrogates directories
        this.surrogatesInPath =
            getDataPath(
                SwitchboardConstants.SURROGATES_IN_PATH,
                SwitchboardConstants.SURROGATES_IN_PATH_DEFAULT);
        this.log.logInfo("surrogates.in Path = " + this.surrogatesInPath.getAbsolutePath());
        this.surrogatesInPath.mkdirs();
        this.surrogatesOutPath =
            getDataPath(
                SwitchboardConstants.SURROGATES_OUT_PATH,
                SwitchboardConstants.SURROGATES_OUT_PATH_DEFAULT);
        this.log.logInfo("surrogates.out Path = " + this.surrogatesOutPath.getAbsolutePath());
        this.surrogatesOutPath.mkdirs();

        // create the release download directory
        this.releasePath =
            getDataPath(SwitchboardConstants.RELEASE_PATH, SwitchboardConstants.RELEASE_PATH_DEFAULT);
        this.releasePath.mkdirs();
        this.log.logInfo("RELEASE Path = " + this.releasePath.getAbsolutePath());

        // starting message board
        initMessages();

        // starting wiki
        initWiki();

        //starting blog
        initBlog();

        // init User DB
        this.log.logConfig("Loading User DB");
        final File userDbFile = new File(getDataPath(), "DATA/SETTINGS/user.heap");
        this.userDB = new UserDB(userDbFile);
        this.log.logConfig("Loaded User DB from file "
            + userDbFile.getName()
            + ", "
            + this.userDB.size()
            + " entries"
            + ", "
            + ppRamString(userDbFile.length() / 1024));

        // init html parser evaluation scheme
        File parserPropertiesPath = new File("defaults/");
        String[] settingsList = parserPropertiesPath.list();
        for ( final String l : settingsList ) {
            if ( l.startsWith("parser.") && l.endsWith(".properties") ) {
                Evaluation.add(new File(parserPropertiesPath, l));
            }
        }
        parserPropertiesPath = new File(getDataPath(), "DATA/SETTINGS/");
        settingsList = parserPropertiesPath.list();
        for ( final String l : settingsList ) {
            if ( l.startsWith("parser.") && l.endsWith(".properties") ) {
                Evaluation.add(new File(parserPropertiesPath, l));
            }
        }

        // init bookmarks DB: needs more time since this does a DNS lookup for each Bookmark.
        // Can be started concurrently
        new Thread() {
            @Override
            public void run() {
                try {
                    initBookmarks();
                } catch ( final IOException e ) {
                    Log.logException(e);
                }
            }
        }.start();

        // define a realtime parsable mimetype list
        this.log.logConfig("Parser: Initializing Mime Type deny list");
        TextParser.setDenyMime(getConfig(SwitchboardConstants.PARSER_MIME_DENY, ""));
        TextParser.setDenyExtension(getConfig(SwitchboardConstants.PARSER_EXTENSIONS_DENY, ""));

        // prepare a solr index profile switch list
        final File solrBackupProfile = new File("defaults/solr.keys.list");
        final String schemename =
            getConfig("federated.service.solr.indexing.schemefile", "solr.keys.default.list");
        final File solrWorkProfile = new File(getDataPath(), "DATA/SETTINGS/" + schemename);
        if ( !solrWorkProfile.exists() ) {
            FileUtils.copy(solrBackupProfile, solrWorkProfile);
        }
        final SolrScheme backupScheme = new SolrScheme(solrBackupProfile);
        final SolrScheme workingScheme = new SolrScheme(solrWorkProfile);

        // update the working scheme with the backup scheme. This is necessary to include new features.
        // new features are always activated by default
        workingScheme.fill(backupScheme, false);

        // set up the solr interface
        final String solrurls =
            getConfig("federated.service.solr.indexing.url", "http://127.0.0.1:8983/solr");
        final boolean usesolr =
            getConfigBool("federated.service.solr.indexing.enabled", false) & solrurls.length() > 0;
        try {
            this.indexSegments.segment(Segments.Process.LOCALCRAWLING).connectSolr(
                (usesolr) ? new SolrShardingConnector(
                    solrurls,
                    workingScheme,
                    SolrShardingSelection.Method.MODULO_HOST_MD5,
                    10000) : null);
        } catch ( final IOException e ) {
            Log.logException(e);
            this.indexSegments.segment(Segments.Process.LOCALCRAWLING).connectSolr(null);
        }

        // start a loader
        this.log.logConfig("Starting Crawl Loader");
        this.loader = new LoaderDispatcher(this);
        final Map<String, File> oaiFriends =
            OAIListFriendsLoader.loadListFriendsSources(
                new File("defaults/oaiListFriendsSource.xml"),
                getDataPath());
        OAIListFriendsLoader.init(this.loader, oaiFriends);
        this.crawlQueues = new CrawlQueues(this, this.queuesRoot);
        this.crawlQueues.noticeURL.setMinimumDelta(
            getConfigLong("minimumLocalDelta", this.crawlQueues.noticeURL.getMinimumLocalDelta()),
            getConfigLong("minimumGlobalDelta", this.crawlQueues.noticeURL.getMinimumGlobalDelta()));

        /*
         * Creating sync objects and loading status for the crawl jobs
         * a) local crawl
         * b) remote triggered crawl
         * c) global crawl trigger
         */
        this.crawlJobsStatus.put(
            SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL,
            new Object[] {
                new Object(),
                Boolean.valueOf(getConfig(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL + "_isPaused", "false"))
            });
        this.crawlJobsStatus.put(
            SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL,
            new Object[] {
                new Object(),
                Boolean.valueOf(getConfig(
                    SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL + "_isPaused",
                    "false"))
            });
        this.crawlJobsStatus.put(
            SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER,
            new Object[] {
                new Object(),
                Boolean.valueOf(getConfig(
                    SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER + "_isPaused",
                    "false"))
            });

        // init cookie-Monitor
        this.log.logConfig("Starting Cookie Monitor");
        this.outgoingCookies = new ConcurrentHashMap<String, Object[]>();
        this.incomingCookies = new ConcurrentHashMap<String, Object[]>();

        // init search history trackers
        this.localSearchTracker = new ConcurrentHashMap<String, TreeSet<Long>>(); // String:TreeSet - IP:set of Long(accessTime)
        this.remoteSearchTracker = new ConcurrentHashMap<String, TreeSet<Long>>();

        // init messages: clean up message symbol
        final File notifierSource =
            new File(getAppPath(), getConfig(
                SwitchboardConstants.HTROOT_PATH,
                SwitchboardConstants.HTROOT_PATH_DEFAULT) + "/env/grafics/empty.gif");
        final File notifierDest =
            new File(
                getDataPath(SwitchboardConstants.HTDOCS_PATH, SwitchboardConstants.HTDOCS_PATH_DEFAULT),
                "notifier.gif");
        try {
            FileUtils.copy(notifierSource, notifierDest);
        } catch ( final IOException e ) {
        }

        // init nameCacheNoCachingList
        Domains
            .setNoCachingPatterns(getConfig(SwitchboardConstants.HTTPC_NAME_CACHE_CACHING_PATTERNS_NO, ""));

        // generate snippets cache
        this.log.logConfig("Initializing Snippet Cache");

        // init the wiki
        wikiParser = new WikiCode();

        // initializing the resourceObserver
        InstantBusyThread.oneTimeJob(ResourceObserver.class, "initThread", ResourceObserver.log, 0);

        // initializing the stackCrawlThread
        this.crawlStacker =
            new CrawlStacker(
                this.crawlQueues,
                this.crawler,
                this.indexSegments.segment(Segments.Process.LOCALCRAWLING),
                this.peers,
                isIntranetMode(),
                isGlobalMode(),
                this.domainList); // Intranet and Global mode may be both true!

        // possibly switch off localIP check
        Domains.setNoLocalCheck(isAllIPMode());

        // check status of account configuration: when local url crawling is allowed, it is not allowed
        // that an automatic authorization of localhost is done, because in this case crawls from local
        // addresses are blocked to prevent attack szenarios where remote pages contain links to localhost
        // addresses that can steer a YaCy peer
        if ( (getConfigBool("adminAccountForLocalhost", false)) ) {
            if ( getConfig(SwitchboardConstants.ADMIN_ACCOUNT_B64MD5, "").startsWith("0000") ) {
                // the password was set automatically with a random value.
                // We must remove that here to prevent that a user cannot log in any more
                setConfig(SwitchboardConstants.ADMIN_ACCOUNT_B64MD5, "");
                // after this a message must be generated to alert the user to set a new password
                this.log.logInfo("RANDOM PASSWORD REMOVED! User must set a new password");
            }
        }

        // initializing dht chunk generation
        this.dhtMaxReferenceCount = (int) getConfigLong(SwitchboardConstants.INDEX_DIST_CHUNK_SIZE_START, 50);

        // init robinson cluster
        // before we do that, we wait some time until the seed list is loaded.
        this.clusterhashes = this.peers.clusterHashes(getConfig("cluster.peers.yacydomain", ""));

        // deploy blocking threads
        this.indexingStorageProcessor =
            new WorkflowProcessor<indexingQueueEntry>(
                "storeDocumentIndex",
                "This is the sequencing step of the indexing queue. Files are written as streams, too much councurrency would destroy IO performance. In this process the words are written to the RWI cache, which flushes if it is full.",
                new String[] {
                    "RWI/Cache/Collections"
                },
                this,
                "storeDocumentIndex",
                2 * WorkflowProcessor.availableCPU,
                null,
                1 /*Math.max(1, WorkflowProcessor.availableCPU / 2)*/);
        this.indexingAnalysisProcessor =
            new WorkflowProcessor<indexingQueueEntry>(
                "webStructureAnalysis",
                "This just stores the link structure of the document into a web structure database.",
                new String[] {
                    "storeDocumentIndex"
                },
                this,
                "webStructureAnalysis",
                2 * WorkflowProcessor.availableCPU,
                this.indexingStorageProcessor,
                WorkflowProcessor.availableCPU);
        this.indexingCondensementProcessor =
            new WorkflowProcessor<indexingQueueEntry>(
                "condenseDocument",
                "This does a structural analysis of plain texts: markup of headlines, slicing into phrases (i.e. sentences), markup with position, counting of words, calculation of term frequency.",
                new String[] {
                    "webStructureAnalysis"
                },
                this,
                "condenseDocument",
                4 * WorkflowProcessor.availableCPU,
                this.indexingAnalysisProcessor,
                WorkflowProcessor.availableCPU);
        this.indexingDocumentProcessor =
            new WorkflowProcessor<indexingQueueEntry>(
                "parseDocument",
                "This does the parsing of the newly loaded documents from the web. The result is not only a plain text document, but also a list of URLs that are embedded into the document. The urls are handed over to the CrawlStacker. This process has two child process queues!",
                new String[] {
                    "condenseDocument", "CrawlStacker"
                },
                this,
                "parseDocument",
                4 * WorkflowProcessor.availableCPU,
                this.indexingCondensementProcessor,
                WorkflowProcessor.availableCPU);

        // deploy busy threads
        this.log.logConfig("Starting Threads");
        MemoryControl.gc(10000, "plasmaSwitchboard, help for profiler"); // help for profiler - thq

        deployThread(
            SwitchboardConstants.CLEANUP,
            "Cleanup",
            "simple cleaning process for monitoring information",
            null,
            new InstantBusyThread(
                this,
                SwitchboardConstants.CLEANUP_METHOD_START,
                SwitchboardConstants.CLEANUP_METHOD_JOBCOUNT,
                SwitchboardConstants.CLEANUP_METHOD_FREEMEM,
                60000,
                Long.MAX_VALUE,
                10000,
                Long.MAX_VALUE),
            60000); // all 5 Minutes, wait 1 minute until first run
        deployThread(
            SwitchboardConstants.SURROGATES,
            "Surrogates",
            "A thread that polls the SURROGATES path and puts all Documents in one surroagte file into the indexing queue.",
            null,
            new InstantBusyThread(
                this,
                SwitchboardConstants.SURROGATES_METHOD_START,
                SwitchboardConstants.SURROGATES_METHOD_JOBCOUNT,
                SwitchboardConstants.SURROGATES_METHOD_FREEMEM,
                20000,
                Long.MAX_VALUE,
                0,
                Long.MAX_VALUE),
            10000);
        deployThread(
            SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL,
            "Remote Crawl Job",
            "thread that performes a single crawl/indexing step triggered by a remote peer",
            "/IndexCreateWWWRemoteQueue_p.html",
            new InstantBusyThread(
                this.crawlQueues,
                SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL_METHOD_START,
                SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL_METHOD_JOBCOUNT,
                SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL_METHOD_FREEMEM,
                0,
                Long.MAX_VALUE,
                0,
                Long.MAX_VALUE),
            10000);
        deployThread(
            SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER,
            "Remote Crawl URL Loader",
            "thread that loads remote crawl lists from other peers",
            null,
            new InstantBusyThread(
                this.crawlQueues,
                SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER_METHOD_START,
                SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER_METHOD_JOBCOUNT,
                SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER_METHOD_FREEMEM,
                10000,
                Long.MAX_VALUE,
                10000,
                Long.MAX_VALUE),
            10000); // error here?
        deployThread(
            SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL,
            "Local Crawl",
            "thread that performes a single crawl step from the local crawl queue",
            "/IndexCreateWWWLocalQueue_p.html",
            new InstantBusyThread(
                this.crawlQueues,
                SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL_METHOD_START,
                SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL_METHOD_JOBCOUNT,
                SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL_METHOD_FREEMEM,
                0,
                Long.MAX_VALUE,
                0,
                Long.MAX_VALUE),
            10000);
        deployThread(
            SwitchboardConstants.SEED_UPLOAD,
            "Seed-List Upload",
            "task that a principal peer performes to generate and upload a seed-list to a ftp account",
            null,
            new InstantBusyThread(
                this.yc,
                SwitchboardConstants.SEED_UPLOAD_METHOD_START,
                SwitchboardConstants.SEED_UPLOAD_METHOD_JOBCOUNT,
                SwitchboardConstants.SEED_UPLOAD_METHOD_FREEMEM,
                600000,
                Long.MAX_VALUE,
                300000,
                Long.MAX_VALUE),
            180000);
        deployThread(
            SwitchboardConstants.PEER_PING,
            "YaCy Core",
            "this is the p2p-control and peer-ping task",
            null,
            new InstantBusyThread(
                this.yc,
                SwitchboardConstants.PEER_PING_METHOD_START,
                SwitchboardConstants.PEER_PING_METHOD_JOBCOUNT,
                SwitchboardConstants.PEER_PING_METHOD_FREEMEM,
                30000,
                Long.MAX_VALUE,
                30000,
                Long.MAX_VALUE),
            2000);
        deployThread(
            SwitchboardConstants.INDEX_DIST,
            "DHT Distribution",
            "selection, transfer and deletion of index entries that are not searched on your peer, but on others",
            null,
            new InstantBusyThread(
                this,
                SwitchboardConstants.INDEX_DIST_METHOD_START,
                SwitchboardConstants.INDEX_DIST_METHOD_JOBCOUNT,
                SwitchboardConstants.INDEX_DIST_METHOD_FREEMEM,
                10000,
                Long.MAX_VALUE,
                1000,
                Long.MAX_VALUE),
            5000,
            Long.parseLong(getConfig(SwitchboardConstants.INDEX_DIST_IDLESLEEP, "5000")),
            Long.parseLong(getConfig(SwitchboardConstants.INDEX_DIST_BUSYSLEEP, "0")),
            Long.parseLong(getConfig(SwitchboardConstants.INDEX_DIST_MEMPREREQ, "1000000")));

        // set network-specific performance attributes
        if ( this.firstInit ) {
            setRemotecrawlPPM(Math.max(1, (int) getConfigLong("network.unit.remotecrawl.speed", 60)));
        }

        // test routine for snippet fetch
        //Set query = new HashSet();
        //query.add(CrawlSwitchboardEntry.word2hash("Weitergabe"));
        //query.add(CrawlSwitchboardEntry.word2hash("Zahl"));
        //plasmaSnippetCache.result scr = snippetCache.retrieve(new URL("http://www.heise.de/mobil/newsticker/meldung/mail/54980"), query, true);
        //plasmaSnippetCache.result scr = snippetCache.retrieve(new URL("http://www.heise.de/security/news/foren/go.shtml?read=1&msg_id=7301419&forum_id=72721"), query, true);
        //plasmaSnippetCache.result scr = snippetCache.retrieve(new URL("http://www.heise.de/kiosk/archiv/ct/2003/4/20"), query, true, 260);

        this.trail = new LinkedBlockingQueue<String>();

        this.log.logConfig("Finished Switchboard Initialization");
        sb = this;
    }

    private void setDefaultSegments() {
        this.indexSegments.setSegment(
            Segments.Process.RECEIPTS,
            getConfig(SwitchboardConstants.SEGMENT_RECEIPTS, "default"));
        this.indexSegments.setSegment(
            Segments.Process.QUERIES,
            getConfig(SwitchboardConstants.SEGMENT_QUERIES, "default"));
        this.indexSegments.setSegment(
            Segments.Process.DHTIN,
            getConfig(SwitchboardConstants.SEGMENT_DHTIN, "default"));
        this.indexSegments.setSegment(
            Segments.Process.DHTOUT,
            getConfig(SwitchboardConstants.SEGMENT_DHTOUT, "default"));
        this.indexSegments.setSegment(
            Segments.Process.PROXY,
            getConfig(SwitchboardConstants.SEGMENT_PROXY, "default"));
        this.indexSegments.setSegment(
            Segments.Process.LOCALCRAWLING,
            getConfig(SwitchboardConstants.SEGMENT_LOCALCRAWLING, "default"));
        this.indexSegments.setSegment(
            Segments.Process.REMOTECRAWLING,
            getConfig(SwitchboardConstants.SEGMENT_REMOTECRAWLING, "default"));
        this.indexSegments.setSegment(
            Segments.Process.PUBLIC,
            getConfig(SwitchboardConstants.SEGMENT_PUBLIC, "default"));
    }

    public int getIndexingProcessorsQueueSize() {
        return this.indexingDocumentProcessor.queueSize()
            + this.indexingCondensementProcessor.queueSize()
            + this.indexingAnalysisProcessor.queueSize()
            + this.indexingStorageProcessor.queueSize();
    }

    public void overwriteNetworkDefinition() throws FileNotFoundException, IOException {

        // load network configuration into settings
        String networkUnitDefinition =
            getConfig("network.unit.definition", "defaults/yacy.network.freeworld.unit");

        // patch old values
        if ( networkUnitDefinition.equals("yacy.network.unit") ) {
            networkUnitDefinition = "defaults/yacy.network.freeworld.unit";
            setConfig("network.unit.definition", networkUnitDefinition);
        }

        // remove old release and bootstrap locations
        final Iterator<String> ki = configKeys();
        final ArrayList<String> d = new ArrayList<String>();
        String k;
        while ( ki.hasNext() ) {
            k = ki.next();
            if ( k.startsWith("network.unit.update.location") || k.startsWith("network.unit.bootstrap") ) {
                d.add(k);
            }
        }
        for ( final String s : d ) {
            removeConfig(s); // must be removed afterwards otherwise a ki.remove() would not remove the property on file
        }

        // include additional network definition properties into our settings
        // note that these properties cannot be set in the application because they are
        // _always_ overwritten each time with the default values. This is done so on purpose.
        // the network definition should be made either consistent for all peers,
        // or independently using a bootstrap URL
        Map<String, String> initProps;
        final Reader netDefReader =
            getConfigFileFromWebOrLocally(networkUnitDefinition, getAppPath().getAbsolutePath(), new File(
                this.workPath,
                "network.definition.backup"));
        initProps = FileUtils.table(netDefReader);
        setConfig(initProps);

        // set release locations
        int i = 0;
        CryptoLib cryptoLib;
        try {
            cryptoLib = new CryptoLib();
            while ( true ) {
                final String location = getConfig("network.unit.update.location" + i, "");
                if ( location.isEmpty() ) {
                    break;
                }
                DigestURI locationURL;
                try {
                    // try to parse url
                    locationURL = new DigestURI(location);
                } catch ( final MalformedURLException e ) {
                    break;
                }
                PublicKey publicKey = null;
                // get public key if it's in config
                try {
                    final String publicKeyString =
                        getConfig("network.unit.update.location" + i + ".key", null);
                    if ( publicKeyString != null ) {
                        final byte[] publicKeyBytes =
                            Base64Order.standardCoder.decode(publicKeyString.trim());
                        publicKey = cryptoLib.getPublicKeyFromBytes(publicKeyBytes);
                    }
                } catch ( final InvalidKeySpecException e ) {
                    Log.logException(e);
                }
                final yacyUpdateLocation updateLocation = new yacyUpdateLocation(locationURL, publicKey);
                yacyRelease.latestReleaseLocations.add(updateLocation);
                i++;
            }
        } catch ( final NoSuchAlgorithmException e1 ) {
            // TODO Auto-generated catch block
            Log.logException(e1);
        }

        // initiate url license object
        this.licensedURLs = new URLLicense(8);

        // set white/blacklists
        this.networkWhitelist = Domains.makePatterns(getConfig(SwitchboardConstants.NETWORK_WHITELIST, ""));
        this.networkBlacklist = Domains.makePatterns(getConfig(SwitchboardConstants.NETWORK_BLACKLIST, ""));

        /*
        // in intranet and portal network set robinson mode
        if (networkUnitDefinition.equals("defaults/yacy.network.webportal.unit") ||
            networkUnitDefinition.equals("defaults/yacy.network.intranet.unit")) {
            // switch to robinson mode
            setConfig("crawlResponse", "false");
            setConfig(plasmaSwitchboardConstants.INDEX_DIST_ALLOW, false);
            setConfig(plasmaSwitchboardConstants.INDEX_RECEIVE_ALLOW, false);
        }

        // in freeworld network set full p2p mode
        if (networkUnitDefinition.equals("defaults/yacy.network.freeworld.unit")) {
            // switch to robinson mode
            setConfig("crawlResponse", "true");
            setConfig(plasmaSwitchboardConstants.INDEX_DIST_ALLOW, true);
            setConfig(plasmaSwitchboardConstants.INDEX_RECEIVE_ALLOW, true);
        }
        */
        // write the YaCy network identification inside the yacybot client user agent to distinguish networks
        String newagent =
            ClientIdentification.generateYaCyBot(getConfig(SwitchboardConstants.NETWORK_NAME, "")
                + (isRobinsonMode() ? "-" : "/")
                + getConfig(SwitchboardConstants.NETWORK_DOMAIN, "global"));
        if ( !getConfigBool("network.unit.dht", false)
            && getConfig("network.unit.tenant.agent", "").length() > 0 ) {
            newagent = getConfig("network.unit.tenant.agent", "").trim();
            this.log.logInfo("new user agent: '" + newagent + "'");
        }
        ClientIdentification.setUserAgent(newagent);
    }

    public void switchNetwork(final String networkDefinition) throws FileNotFoundException, IOException {
        this.log.logInfo("SWITCH NETWORK: switching to '" + networkDefinition + "'");
        // pause crawls
        final boolean lcp = crawlJobIsPaused(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL);
        if ( !lcp ) {
            pauseCrawlJob(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL);
        }
        final boolean rcp = crawlJobIsPaused(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL);
        if ( !rcp ) {
            pauseCrawlJob(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL);
        }
        // trigger online caution
        this.proxyLastAccess = System.currentTimeMillis() + 3000; // at least 3 seconds online caution to prevent unnecessary action on database meanwhile
        this.log.logInfo("SWITCH NETWORK: SHUT DOWN OF OLD INDEX DATABASE...");
        // clean search events which have cached relations to the old index
        SearchEventCache.cleanupEvents(true);

        // switch the networks
        synchronized ( this ) {

            // shut down
            this.crawler.close();
            if ( this.dhtDispatcher != null ) {
                this.dhtDispatcher.close();
            }
            synchronized ( this.indexSegments ) {
                this.indexSegments.close();
            }
            this.crawlStacker.announceClose();
            this.crawlStacker.close();
            this.webStructure.close();

            this.log.logInfo("SWITCH NETWORK: START UP OF NEW INDEX DATABASE...");

            // new properties
            setConfig("network.unit.definition", networkDefinition);
            overwriteNetworkDefinition();
            final File indexPrimaryPath =
                getDataPath(SwitchboardConstants.INDEX_PRIMARY_PATH, SwitchboardConstants.INDEX_PATH_DEFAULT);
            final int wordCacheMaxCount =
                (int) getConfigLong(SwitchboardConstants.WORDCACHE_MAX_COUNT, 20000);
            final long fileSizeMax =
                (OS.isWindows) ? sb.getConfigLong("filesize.max.win", (long) Integer.MAX_VALUE) : sb
                    .getConfigLong("filesize.max.other", (long) Integer.MAX_VALUE);
            final int redundancy = (int) sb.getConfigLong("network.unit.dhtredundancy.senior", 1);
            final int partitionExponent = (int) sb.getConfigLong("network.unit.dht.partitionExponent", 0);
            final String networkName = getConfig(SwitchboardConstants.NETWORK_NAME, "");
            this.networkRoot = new File(new File(indexPrimaryPath, networkName), "NETWORK");
            this.queuesRoot = new File(new File(indexPrimaryPath, networkName), "QUEUES");
            this.networkRoot.mkdirs();
            this.queuesRoot.mkdirs();

            // clear statistic data
            ResultURLs.clearStacks();

            // remove heuristics
            setConfig("heuristic.site", false);
            setConfig("heuristic.scroogle", false);
            setConfig("heuristic.blekko", false);

            // relocate
            this.peers.relocate(
                this.networkRoot,
                redundancy,
                partitionExponent,
                this.useTailCache,
                this.exceed134217727);
            this.indexSegments =
                new Segments(
                    this.log,
                    new File(new File(indexPrimaryPath, networkName), "SEGMENTS"),
                    wordCacheMaxCount,
                    fileSizeMax,
                    this.useTailCache,
                    this.exceed134217727);
            // set the default segment names
            setDefaultSegments();
            this.crawlQueues.relocate(this.queuesRoot); // cannot be closed because the busy threads are working with that object

            // create a crawler
            this.crawler = new CrawlSwitchboard(networkName, this.log, this.queuesRoot);

            // init a DHT transmission dispatcher
            this.dhtDispatcher =
                (this.peers.sizeConnected() == 0) ? null : new Dispatcher(
                    this.indexSegments.segment(Segments.Process.LOCALCRAWLING),
                    this.peers,
                    true,
                    10000);

            // create new web structure
            this.webStructure = new WebStructureGraph(new File(this.queuesRoot, "webStructure.map"));

            // load domainList
            try {
                this.domainList = null;
                if ( !getConfig("network.unit.domainlist", "").equals("") ) {
                    final Reader r =
                        getConfigFileFromWebOrLocally(getConfig("network.unit.domainlist", ""), getAppPath()
                            .getAbsolutePath(), new File(this.networkRoot, "domainlist.txt"));
                    this.domainList = new FilterEngine();
                    this.domainList.loadList(new BufferedReader(r), null);
                }
            } catch ( final FileNotFoundException e ) {
                this.log.logSevere("CONFIG: domainlist not found: " + e.getMessage());
            } catch ( final IOException e ) {
                this.log.logSevere("CONFIG: error while retrieving domainlist: " + e.getMessage());
            }

            this.crawlStacker =
                new CrawlStacker(
                    this.crawlQueues,
                    this.crawler,
                    this.indexSegments.segment(Segments.Process.LOCALCRAWLING),
                    this.peers,
                    "local.any".indexOf(getConfig(SwitchboardConstants.NETWORK_DOMAIN, "global")) >= 0,
                    "global.any".indexOf(getConfig(SwitchboardConstants.NETWORK_DOMAIN, "global")) >= 0,
                    this.domainList);

        }
        Domains.setNoLocalCheck(isAllIPMode()); // possibly switch off localIP check

        // start up crawl jobs
        continueCrawlJob(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL);
        continueCrawlJob(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL);
        this.log
            .logInfo("SWITCH NETWORK: FINISHED START UP, new network is now '" + networkDefinition + "'.");

        // set the network-specific remote crawl ppm
        setRemotecrawlPPM(Math.max(1, (int) getConfigLong("network.unit.remotecrawl.speed", 60)));
    }

    public void setRemotecrawlPPM(final int ppm) {
        final long newBusySleep = Math.max(100, 60000 / ppm);

        // propagate to crawler
        final BusyThread rct = getThread(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL);
        setConfig(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL_BUSYSLEEP, newBusySleep);
        setConfig(
            SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL_IDLESLEEP,
            Math.min(10000, newBusySleep * 10));
        rct.setBusySleep(getConfigLong(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL_BUSYSLEEP, 1000));
        rct
            .setIdleSleep(getConfigLong(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL_IDLESLEEP, 10000));

        // propagate to loader
        final BusyThread rcl = getThread(SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER);
        setConfig(SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER_BUSYSLEEP, newBusySleep * 4);
        setConfig(
            SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER_IDLESLEEP,
            Math.min(10000, newBusySleep * 20));
        rcl.setBusySleep(getConfigLong(SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER_BUSYSLEEP, 1000));
        rcl.setIdleSleep(getConfigLong(SwitchboardConstants.CRAWLJOB_REMOTE_CRAWL_LOADER_IDLESLEEP, 10000));
    }

    public void initMessages() throws IOException {
        this.log.logConfig("Starting Message Board");
        final File messageDbFile = new File(this.workPath, "message.heap");
        this.messageDB = new MessageBoard(messageDbFile);
        this.log.logConfig("Loaded Message Board DB from file "
            + messageDbFile.getName()
            + ", "
            + this.messageDB.size()
            + " entries"
            + ", "
            + ppRamString(messageDbFile.length() / 1024));
    }

    public void initWiki() throws IOException {
        this.log.logConfig("Starting Wiki Board");
        final File wikiDbFile = new File(this.workPath, "wiki.heap");
        this.wikiDB = new WikiBoard(wikiDbFile, new File(this.workPath, "wiki-bkp.heap"));
        this.log.logConfig("Loaded Wiki Board DB from file "
            + wikiDbFile.getName()
            + ", "
            + this.wikiDB.size()
            + " entries"
            + ", "
            + ppRamString(wikiDbFile.length() / 1024));
    }

    public void initBlog() throws IOException {
        this.log.logConfig("Starting Blog");
        final File blogDbFile = new File(this.workPath, "blog.heap");
        this.blogDB = new BlogBoard(blogDbFile);
        this.log.logConfig("Loaded Blog DB from file "
            + blogDbFile.getName()
            + ", "
            + this.blogDB.size()
            + " entries"
            + ", "
            + ppRamString(blogDbFile.length() / 1024));

        final File blogCommentDbFile = new File(this.workPath, "blogComment.heap");
        this.blogCommentDB = new BlogBoardComments(blogCommentDbFile);
        this.log.logConfig("Loaded Blog-Comment DB from file "
            + blogCommentDbFile.getName()
            + ", "
            + this.blogCommentDB.size()
            + " entries"
            + ", "
            + ppRamString(blogCommentDbFile.length() / 1024));
    }

    public void initBookmarks() throws IOException {
        this.log.logConfig("Loading Bookmarks DB");
        final File bookmarksFile = new File(this.workPath, "bookmarks.heap");
        final File tagsFile = new File(this.workPath, "bookmarkTags.heap");
        final File datesFile = new File(this.workPath, "bookmarkDates.heap");
        tagsFile.delete();
        this.bookmarksDB = new BookmarksDB(bookmarksFile, datesFile);
        this.log.logConfig("Loaded Bookmarks DB from files "
            + bookmarksFile.getName()
            + ", "
            + tagsFile.getName());
        this.log.logConfig(this.bookmarksDB.tagsSize()
            + " Tag, "
            + this.bookmarksDB.bookmarksSize()
            + " Bookmarks");
    }

    public static Switchboard getSwitchboard() {
        return sb;
    }

    public boolean isIntranetMode() {
        return "local.any".indexOf(getConfig(SwitchboardConstants.NETWORK_DOMAIN, "global")) >= 0;
    }

    public boolean isGlobalMode() {
        return "global.any".indexOf(getConfig(SwitchboardConstants.NETWORK_DOMAIN, "global")) >= 0;
    }

    public boolean isAllIPMode() {
        return "any".indexOf(getConfig(SwitchboardConstants.NETWORK_DOMAIN, "global")) >= 0;
    }

    /**
     * in nocheck mode the isLocal property is not checked to omit DNS lookup. Can only be done in allip mode
     *
     * @return
     */
    public boolean isIPNoCheckMode() {
        return isAllIPMode() && getConfigBool(SwitchboardConstants.NETWORK_DOMAIN_NOCHECK, false);
    }

    public boolean isRobinsonMode() {
        // we are in robinson mode, if we do not exchange index by dht distribution
        // we need to take care that search requests and remote indexing requests go only
        // to the peers in the same cluster, if we run a robinson cluster.
        return (this.peers != null && this.peers.sizeConnected() == 0)
            || (!getConfigBool(SwitchboardConstants.INDEX_DIST_ALLOW, false) && !getConfigBool(
                SwitchboardConstants.INDEX_RECEIVE_ALLOW,
                false));
    }

    public boolean isPublicRobinson() {
        // robinson peers may be member of robinson clusters, which can be public or private
        // this does not check the robinson attribute, only the specific subtype of the cluster
        final String clustermode =
            getConfig(SwitchboardConstants.CLUSTER_MODE, SwitchboardConstants.CLUSTER_MODE_PUBLIC_PEER);
        return (clustermode.equals(SwitchboardConstants.CLUSTER_MODE_PUBLIC_CLUSTER))
            || (clustermode.equals(SwitchboardConstants.CLUSTER_MODE_PUBLIC_PEER));
    }

    public boolean isInMyCluster(final String peer) {
        // check if the given peer is in the own network, if this is a robinson cluster
        // depending on the robinson cluster type, the peer String may be a peerhash (b64-hash)
        // or a ip:port String or simply a ip String
        // if this robinson mode does not define a cluster membership, false is returned
        if ( peer == null ) {
            return false;
        }
        if ( !isRobinsonMode() ) {
            return false;
        }
        final String clustermode =
            getConfig(SwitchboardConstants.CLUSTER_MODE, SwitchboardConstants.CLUSTER_MODE_PUBLIC_PEER);
        if ( clustermode.equals(SwitchboardConstants.CLUSTER_MODE_PUBLIC_CLUSTER) ) {
            // check if we got the request from a peer in the public cluster
            return this.clusterhashes.containsKey(ASCII.getBytes(peer));
        } else {
            return false;
        }
    }

    public boolean isInMyCluster(final Seed seed) {
        // check if the given peer is in the own network, if this is a robinson cluster
        // if this robinson mode does not define a cluster membership, false is returned
        if ( seed == null ) {
            return false;
        }
        if ( !isRobinsonMode() ) {
            return false;
        }
        final String clustermode =
            getConfig(SwitchboardConstants.CLUSTER_MODE, SwitchboardConstants.CLUSTER_MODE_PUBLIC_PEER);
        if ( clustermode.equals(SwitchboardConstants.CLUSTER_MODE_PUBLIC_CLUSTER) ) {
            // check if we got the request from a peer in the public cluster
            return this.clusterhashes.containsKey(ASCII.getBytes(seed.hash));
        } else {
            return false;
        }
    }

    public String urlExists(final Segments.Process process, final byte[] hash) {
        // tests if hash occurrs in any database
        // if it exists, the name of the database is returned,
        // if it not exists, null is returned
        if ( this.indexSegments.urlMetadata(process).exists(hash) ) {
            return "loaded";
        }
        return this.crawlQueues.urlExists(hash);
    }

    public void urlRemove(final Segment segment, final byte[] hash) {
        segment.urlMetadata().remove(hash);
        ResultURLs.remove(ASCII.String(hash));
        this.crawlQueues.urlRemove(hash);
    }

    public DigestURI getURL(final Segments.Process process, final byte[] urlhash) {
        if ( urlhash == null ) {
            return null;
        }
        if ( urlhash.length == 0 ) {
            return null;
        }
        final URIMetadataRow le = this.indexSegments.urlMetadata(process).load(urlhash);
        if ( le != null ) {
            return le.url();
        }
        return this.crawlQueues.getURL(urlhash);
    }

    public RankingProfile getRanking() {
        return (getConfig("rankingProfile", "").length() == 0)
            ? new RankingProfile(ContentDomain.TEXT)
            : new RankingProfile("", crypt.simpleDecode(sb.getConfig("rankingProfile", ""), null));
    }

    /**
     * checks if the proxy, the local search or remote search was accessed some time before If no limit is
     * exceeded, null is returned. If a limit is exceeded, then the name of the service that caused the
     * caution is returned
     *
     * @return
     */
    public String onlineCaution() {
        if ( System.currentTimeMillis() - this.proxyLastAccess < Integer.parseInt(getConfig(
            SwitchboardConstants.PROXY_ONLINE_CAUTION_DELAY,
            "100")) ) {
            return "proxy";
        }
        if ( System.currentTimeMillis() - this.localSearchLastAccess < Integer.parseInt(getConfig(
            SwitchboardConstants.LOCALSEACH_ONLINE_CAUTION_DELAY,
            "1000")) ) {
            return "localsearch";
        }
        if ( System.currentTimeMillis() - this.remoteSearchLastAccess < Integer.parseInt(getConfig(
            SwitchboardConstants.REMOTESEARCH_ONLINE_CAUTION_DELAY,
            "500")) ) {
            return "remotesearch";
        }
        return null;
    }

    /**
     * Creates a human readable string from a number which represents the size of a file. The method has a
     * side effect: It changes the input. Since the method is private and only used in this class for values
     * which are not used later, this should be OK in this case, but one should never use this method without
     * thinking about the side effect. [MN]
     *
     * @param bytes the length of a file
     * @return the length of a file in human readable form
     */
    private static String ppRamString(long bytes) {
        if ( bytes < 1024 ) {
            return bytes + " KByte";
        }
        bytes = bytes / 1024;
        if ( bytes < 1024 ) {
            return bytes + " MByte";
        }
        bytes = bytes / 1024;
        if ( bytes < 1024 ) {
            return bytes + " GByte";
        }
        return (bytes / 1024) + "TByte";
    }

    /**
     * {@link CrawlProfiles Crawl Profiles} are saved independently from the queues themselves and therefore
     * have to be cleaned up from time to time. This method only performs the clean-up if - and only if - the
     * {@link IndexingStack switchboard}, {@link LoaderDispatcher loader} and {@link plasmaCrawlNURL local
     * crawl} queues are all empty.
     * <p>
     * Then it iterates through all existing {@link CrawlProfiles crawl profiles} and removes all profiles
     * which are not hard-coded.
     * </p>
     * <p>
     * <i>If this method encounters DB-failures, the profile DB will be reseted and</i> <code>true</code><i>
     * will be returned</i>
     * </p>
     *
     * @see #CRAWL_PROFILE_PROXY hardcoded
     * @see #CRAWL_PROFILE_REMOTE hardcoded
     * @see #CRAWL_PROFILE_SNIPPET_TEXT hardcoded
     * @see #CRAWL_PROFILE_SNIPPET_MEDIA hardcoded
     * @return whether this method has done something or not (i.e. because the queues have been filled or
     *         there are no profiles left to clean up)
     * @throws <b>InterruptedException</b> if the current thread has been interrupted, i.e. by the shutdown
     *         procedure
     */
    public boolean cleanProfiles() throws InterruptedException {
        if (getIndexingProcessorsQueueSize() > 0 ||
            this.crawlQueues.workerSize() > 0 ||
            this.crawlQueues.coreCrawlJobSize() > 0 ||
            this.crawlQueues.limitCrawlJobSize() > 0 ||
            this.crawlQueues.remoteTriggeredCrawlJobSize() > 0 ||
            this.crawlQueues.noloadCrawlJobSize() > 0 ||
            (this.crawlStacker != null && !this.crawlStacker.isEmpty()) ||
            this.crawlQueues.noticeURL.notEmpty()) {
            return false;
        }
        return this.crawler.clear();
    }

    public void close() {
        this.log.logConfig("SWITCHBOARD SHUTDOWN STEP 1: sending termination signal to managed threads:");
        MemoryTracker.stopSystemProfiling();
        terminateAllThreads(true);
        net.yacy.gui.framework.Switchboard.shutdown();
        this.log.logConfig("SWITCHBOARD SHUTDOWN STEP 2: sending termination signal to threaded indexing");
        // closing all still running db importer jobs
        this.indexingDocumentProcessor.announceShutdown();
        this.indexingDocumentProcessor.awaitShutdown(12000);
        this.crawlStacker.announceClose();
        this.indexingCondensementProcessor.announceShutdown();
        this.indexingAnalysisProcessor.announceShutdown();
        this.indexingStorageProcessor.announceShutdown();
        if ( this.dhtDispatcher != null ) {
            this.dhtDispatcher.close();
        }
        this.indexingCondensementProcessor.awaitShutdown(12000);
        this.indexingAnalysisProcessor.awaitShutdown(12000);
        this.indexingStorageProcessor.awaitShutdown(12000);
        this.crawlStacker.close();
//        de.anomic.http.client.Client.closeAllConnections();
        this.wikiDB.close();
        this.blogDB.close();
        this.blogCommentDB.close();
        this.userDB.close();
        if ( this.bookmarksDB != null ) {
            this.bookmarksDB.close(); // may null if concurrent initialization was not finished
        }
        this.messageDB.close();
        this.webStructure.close();
        this.crawlQueues.close();
        this.crawler.close();
        this.log
            .logConfig("SWITCHBOARD SHUTDOWN STEP 3: sending termination signal to database manager (stand by...)");
        this.indexSegments.close();
        this.peers.close();
        Cache.close();
        this.tables.close();
        Domains.close();
        AccessTracker.dumpLog(new File("DATA/LOG/queries.log"));
        UPnP.deletePortMapping();
        this.tray.remove();
        try {
            HTTPClient.closeConnectionManager();
        } catch ( final InterruptedException e ) {
            Log.logException(e);
        }
        this.log.logConfig("SWITCHBOARD SHUTDOWN TERMINATED");
    }

    /**
     * pass a response to the indexer
     *
     * @param response
     * @return null if successful, an error message otherwise
     */
    public String toIndexer(final Response response) {
        assert response != null;

        // get next queue entry and start a queue processing
        if ( response == null ) {
            if ( this.log.isFine() ) {
                this.log.logFine("deQueue: queue entry is null");
            }
            return "queue entry is null";
        }
        if ( response.profile() == null ) {
            if ( this.log.isFine() ) {
                this.log.logFine("deQueue: profile is null");
            }
            return "profile is null";
        }

        // check if the document should be indexed based on proxy/crawler rules
        String noIndexReason = "unspecified indexing error";
        if ( response.processCase(this.peers.mySeed().hash) == EventOrigin.PROXY_LOAD ) {
            // proxy-load
            noIndexReason = response.shallIndexCacheForProxy();
        } else {
            // normal crawling
            noIndexReason = response.shallIndexCacheForCrawler();
        }

        // check if the parser supports the mime type
        if ( noIndexReason == null ) {
            noIndexReason = TextParser.supports(response.url(), response.getMimeType());
        }

        // check X-YACY-Index-Control
        // With the X-YACY-Index-Control header set to "no-index" a client could disallow
        // yacy to index the response returned as answer to a request
        if ( noIndexReason == null && response.requestProhibitsIndexing() ) {
            noIndexReason = "X-YACY-Index-Control header prohibits indexing";
        }

        // check accepted domain / localhost accesses
        if ( noIndexReason == null ) {
            noIndexReason = this.crawlStacker.urlInAcceptedDomain(response.url());
        }

        // in the noIndexReason is set, indexing is not allowed
        if ( noIndexReason != null ) {
            // log cause and close queue
            final DigestURI referrerURL = response.referrerURL();
            //if (log.isFine()) log.logFine("deQueue: not indexed any word in URL " + response.url() + "; cause: " + noIndexReason);
            addURLtoErrorDB(
                response.url(),
                (referrerURL == null) ? null : referrerURL.hash(),
                response.initiator(),
                response.name(),
                FailCategory.FINAL_PROCESS_CONTEXT,
                noIndexReason);
            // finish this entry
            return "not allowed: " + noIndexReason;
        }

        // put document into the concurrent processing queue
        if ( this.log.isFinest() ) {
            this.log.logFinest("deQueue: passing to indexing queue: "
                + response.url().toNormalform(true, false));
        }
        try {
            this.indexingDocumentProcessor.enQueue(new indexingQueueEntry(
                Segments.Process.LOCALCRAWLING,
                response,
                null,
                null));
            return null;
        } catch ( final InterruptedException e ) {
            Log.logException(e);
            return "interrupted: " + e.getMessage();
        }
    }

    public boolean processSurrogate(final String s) {
        final File infile = new File(this.surrogatesInPath, s);
        if ( !infile.exists() || !infile.canWrite() || !infile.canRead() ) {
            return false;
        }
        final File outfile = new File(this.surrogatesOutPath, s);
        //if (outfile.exists()) return false;
        boolean moved = false;
        if ( s.endsWith("xml.zip") ) {
            // open the zip file with all the xml files in it
            try {
                final InputStream is = new BufferedInputStream(new FileInputStream(infile));
                final ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;
                while ( (entry = zis.getNextEntry()) != null ) {
                    int size;
                    final byte[] buffer = new byte[2048];
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ( (size = zis.read(buffer, 0, buffer.length)) != -1 ) {
                        baos.write(buffer, 0, size);
                    }
                    baos.flush();
                    processSurrogate(new ByteArrayInputStream(baos.toByteArray()), entry.getName());
                    baos.close();
                }
            } catch ( final IOException e ) {
                Log.logException(e);
            } finally {
                moved = infile.renameTo(outfile);
            }
            return moved;
        } else {
            try {
                InputStream is = new BufferedInputStream(new FileInputStream(infile));
                if ( s.endsWith(".gz") ) {
                    is = new GZIPInputStream(is);
                }
                processSurrogate(is, infile.getName());
            } catch ( final IOException e ) {
                Log.logException(e);
            } finally {
                moved = infile.renameTo(outfile);
                if ( moved ) {
                    // check if this file is already compressed, if not, compress now
                    if ( !outfile.getName().endsWith(".gz") ) {
                        final String gzname = outfile.getName() + ".gz";
                        final File gzfile = new File(outfile.getParentFile(), gzname);
                        try {
                            final OutputStream os =
                                new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(gzfile)));
                            FileUtils.copy(new BufferedInputStream(new FileInputStream(outfile)), os);
                            os.close();
                            if ( gzfile.exists() ) {
                                FileUtils.deletedelete(outfile);
                            }
                        } catch ( final FileNotFoundException e ) {
                            Log.logException(e);
                        } catch ( final IOException e ) {
                            Log.logException(e);
                        }
                    }
                }
            }
            return moved;
        }
    }

    public void processSurrogate(final InputStream is, final String name) throws IOException {
        final SurrogateReader reader = new SurrogateReader(is, 100);
        final Thread readerThread = new Thread(reader, name);
        readerThread.start();
        DCEntry surrogate;
        Response response;
        while ( (surrogate = reader.take()) != DCEntry.poison ) {
            // check if url is in accepted domain
            assert surrogate != null;
            assert this.crawlStacker != null;
            final String urlRejectReason =
                this.crawlStacker.urlInAcceptedDomain(surrogate.getIdentifier(true));
            if ( urlRejectReason != null ) {
                this.log.logWarning("Rejected URL '"
                    + surrogate.getIdentifier(true)
                    + "': "
                    + urlRejectReason);
                continue;
            }

            // create a queue entry
            final Document document = surrogate.document();
            final Request request =
                new Request(
                    ASCII.getBytes(this.peers.mySeed().hash),
                    surrogate.getIdentifier(true),
                    null,
                    "",
                    surrogate.getDate(),
                    this.crawler.defaultSurrogateProfile.handle(),
                    0,
                    0,
                    0,
                    0);
            response = new Response(request, null, null, "200", this.crawler.defaultSurrogateProfile);
            final indexingQueueEntry queueEntry =
                new indexingQueueEntry(Segments.Process.SURROGATES, response, new Document[] {
                    document
                }, null);

            // place the queue entry into the concurrent process of the condenser (document analysis)
            try {
                this.indexingCondensementProcessor.enQueue(queueEntry);
            } catch ( final InterruptedException e ) {
                Log.logException(e);
                break;
            }
        }
    }

    public int surrogateQueueSize() {
        // count surrogates
        final String[] surrogatelist = this.surrogatesInPath.list();
        if ( surrogatelist.length > 100 ) {
            return 100;
        }
        int count = 0;
        for ( final String s : surrogatelist ) {
            if ( s.endsWith(".xml") ) {
                count++;
            }
            if ( count >= 100 ) {
                break;
            }
        }
        return count;
    }

    public void surrogateFreeMem() {
        // do nothing
    }

    public boolean surrogateProcess() {
        // work off fresh entries from the proxy or from the crawler
        final String cautionCause = onlineCaution();
        if ( cautionCause != null ) {
            if ( this.log.isFine() ) {
                this.log.logFine("deQueue: online caution for "
                    + cautionCause
                    + ", omitting resource stack processing");
            }
            return false;
        }

        try {
            // check surrogates
            final String[] surrogatelist = this.surrogatesInPath.list();
            if ( surrogatelist.length > 0 ) {
                // look if the is any xml inside
                for ( final String surrogate : surrogatelist ) {

                    // check for interruption
                    checkInterruption();

                    if ( surrogate.endsWith(".xml")
                        || surrogate.endsWith(".xml.gz")
                        || surrogate.endsWith(".xml.zip") ) {
                        // read the surrogate file and store entry in index
                        if ( processSurrogate(surrogate) ) {
                            return true;
                        }
                    }
                }
            }

        } catch ( final InterruptedException e ) {
            return false;
        }
        return false;
    }

    public static class indexingQueueEntry extends WorkflowJob
    {
        public Segments.Process process;
        public Response queueEntry;
        public Document[] documents;
        public Condenser[] condenser;

        public indexingQueueEntry(
            final Segments.Process process,
            final Response queueEntry,
            final Document[] documents,
            final Condenser[] condenser) {
            super();
            this.process = process;
            this.queueEntry = queueEntry;
            this.documents = documents;
            this.condenser = condenser;
        }
    }

    public int cleanupJobSize() {
        int c = 0;
        if ( (this.crawlQueues.delegatedURL.stackSize() > 1000) ) {
            c++;
        }
        if ( (this.crawlQueues.errorURL.stackSize() > 1000) ) {
            c++;
        }
        for ( final EventOrigin origin : EventOrigin.values() ) {
            if ( ResultURLs.getStackSize(origin) > 1000 ) {
                c++;
            }
        }
        return c;
    }

    public boolean cleanupJob() {
        try {
            // clear caches if necessary
            if ( !MemoryControl.request(8000000L, false) ) {
                for ( final Segment indexSegment : this.indexSegments ) {
                    indexSegment.urlMetadata().clearCache();
                }
                SearchEventCache.cleanupEvents(true);
                this.trail.clear();
            }

            // set a random password if no password is configured
            if ( getConfigBool("adminAccountForLocalhost", false)
                && getConfig(SwitchboardConstants.ADMIN_ACCOUNT_B64MD5, "").length() == 0 ) {
                // make a 'random' password
                setConfig(SwitchboardConstants.ADMIN_ACCOUNT_B64MD5, "0000" + this.genRandomPassword());
                setConfig("adminAccount", "");
            }

            // refresh recrawl dates
            try {
                CrawlProfile selentry;
                for ( final byte[] handle : this.crawler.getActive() ) {
                    selentry = this.crawler.getActive(handle);
                    assert selentry.handle() != null : "profile.name = " + selentry.name();
                    if ( selentry.handle() == null ) {
                        this.crawler.removeActive(handle);
                        continue;
                    }
                    boolean insert = false;
                    if ( selentry.name().equals(CrawlSwitchboard.CRAWL_PROFILE_PROXY) ) {
                        selentry.put(CrawlProfile.RECRAWL_IF_OLDER, Long.toString(CrawlProfile
                            .getRecrawlDate(CrawlSwitchboard.CRAWL_PROFILE_PROXY_RECRAWL_CYCLE)));
                        insert = true;
                    }
                    if ( selentry.name().equals(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_LOCAL_TEXT) ) {
                        selentry
                            .put(
                                CrawlProfile.RECRAWL_IF_OLDER,
                                Long.toString(CrawlProfile
                                    .getRecrawlDate(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_LOCAL_TEXT_RECRAWL_CYCLE)));
                        insert = true;
                    }
                    if ( selentry.name().equals(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_GLOBAL_TEXT) ) {
                        selentry
                            .put(
                                CrawlProfile.RECRAWL_IF_OLDER,
                                Long.toString(CrawlProfile
                                    .getRecrawlDate(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_GLOBAL_TEXT_RECRAWL_CYCLE)));
                        insert = true;
                    }
                    if ( selentry.name().equals(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_LOCAL_MEDIA) ) {
                        selentry
                            .put(
                                CrawlProfile.RECRAWL_IF_OLDER,
                                Long.toString(CrawlProfile
                                    .getRecrawlDate(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_LOCAL_MEDIA_RECRAWL_CYCLE)));
                        insert = true;
                    }
                    if ( selentry.name().equals(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_GLOBAL_MEDIA) ) {
                        selentry
                            .put(
                                CrawlProfile.RECRAWL_IF_OLDER,
                                Long.toString(CrawlProfile
                                    .getRecrawlDate(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_GLOBAL_MEDIA_RECRAWL_CYCLE)));
                        insert = true;
                    }
                    if ( selentry.name().equals(CrawlSwitchboard.CRAWL_PROFILE_SURROGATE) ) {
                        selentry.put(CrawlProfile.RECRAWL_IF_OLDER, Long.toString(CrawlProfile
                            .getRecrawlDate(CrawlSwitchboard.CRAWL_PROFILE_SURROGATE_RECRAWL_CYCLE)));
                        insert = true;
                    }
                    if ( insert ) {
                        this.crawler.putActive(UTF8.getBytes(selentry.handle()), selentry);
                    }
                }
            } catch ( final Exception e ) {
                Log.logException(e);
            }

            // execute scheduled API actions
            Tables.Row row;
            final List<String> pks = new ArrayList<String>();
            final Date now = new Date();
            try {
                final Iterator<Tables.Row> plainIterator = this.tables.iterator(WorkTables.TABLE_API_NAME);
                final Iterator<Tables.Row> mapIterator =
                    this.tables
                        .orderBy(plainIterator, -1, WorkTables.TABLE_API_COL_DATE_RECORDING)
                        .iterator();
                while ( mapIterator.hasNext() ) {
                    row = mapIterator.next();
                    if ( row == null ) {
                        continue;
                    }
                    final Date date_next_exec = row.get(WorkTables.TABLE_API_COL_DATE_NEXT_EXEC, (Date) null);
                    if ( date_next_exec == null ) {
                        continue;
                    }
                    if ( date_next_exec.after(now) ) {
                        continue;
                    }
                    pks.add(UTF8.String(row.getPK()));
                }
            } catch ( final IOException e ) {
                Log.logException(e);
            }
            for ( final String pk : pks ) {
                try {
                    row = this.tables.select(WorkTables.TABLE_API_NAME, UTF8.getBytes(pk));
                    WorkTables.calculateAPIScheduler(row, true); // calculate next update time
                    this.tables.update(WorkTables.TABLE_API_NAME, row);
                } catch ( final IOException e ) {
                    Log.logException(e);
                    continue;
                } catch ( final RowSpaceExceededException e ) {
                    Log.logException(e);
                    continue;
                }
            }
            final Map<String, Integer> callResult =
                this.tables.execAPICalls(
                    "localhost",
                    (int) getConfigLong("port", 8090),
                    getConfig(SwitchboardConstants.ADMIN_ACCOUNT_B64MD5, ""),
                    pks);
            for ( final Map.Entry<String, Integer> call : callResult.entrySet() ) {
                this.log.logInfo("Scheduler executed api call, response "
                    + call.getValue()
                    + ": "
                    + call.getKey());
            }

            // close unused connections
            ConnectionInfo.cleanUp();

            // clean up delegated stack
            checkInterruption();
            if ( (this.crawlQueues.delegatedURL.stackSize() > 1000) ) {
                if ( this.log.isFine() ) {
                    this.log.logFine("Cleaning Delegated-URLs report stack, "
                        + this.crawlQueues.delegatedURL.stackSize()
                        + " entries on stack");
                }
                this.crawlQueues.delegatedURL.clearStack();
            }

            // clean up error stack
            checkInterruption();
            if ( (this.crawlQueues.errorURL.stackSize() > 1000) ) {
                if ( this.log.isFine() ) {
                    this.log.logFine("Cleaning Error-URLs report stack, "
                        + this.crawlQueues.errorURL.stackSize()
                        + " entries on stack");
                }
                this.crawlQueues.errorURL.clearStack();
            }

            // clean up loadedURL stack
            for ( final EventOrigin origin : EventOrigin.values() ) {
                checkInterruption();
                if ( ResultURLs.getStackSize(origin) > 1000 ) {
                    if ( this.log.isFine() ) {
                        this.log.logFine("Cleaning Loaded-URLs report stack, "
                            + ResultURLs.getStackSize(origin)
                            + " entries on stack "
                            + origin.getCode());
                    }
                    ResultURLs.clearStack(origin);
                }
            }

            // clean up image stack
            ResultImages.clearQueues();

            // clean up profiles
            checkInterruption();
            cleanProfiles();

            // clean up news
            checkInterruption();
            try {
                if ( this.log.isFine() ) {
                    this.log.logFine("Cleaning Incoming News, "
                        + this.peers.newsPool.size(NewsPool.INCOMING_DB)
                        + " entries on stack");
                }
                this.peers.newsPool.automaticProcess(this.peers);
            } catch ( final Exception e ) {
                Log.logException(e);
            }
            if ( getConfigBool("cleanup.deletionProcessedNews", true) ) {
                this.peers.newsPool.clear(NewsPool.PROCESSED_DB);
            }
            if ( getConfigBool("cleanup.deletionPublishedNews", true) ) {
                this.peers.newsPool.clear(NewsPool.PUBLISHED_DB);
            }

            // clean up seed-dbs
            if ( getConfigBool("routing.deleteOldSeeds.permission", true) ) {
                final long deleteOldSeedsTime =
                    getConfigLong("routing.deleteOldSeeds.time", 7) * 24 * 3600000;
                Iterator<Seed> e = this.peers.seedsSortedDisconnected(true, Seed.LASTSEEN);
                Seed seed = null;
                final List<String> deleteQueue = new ArrayList<String>();
                checkInterruption();
                // clean passive seeds
                while ( e.hasNext() ) {
                    seed = e.next();
                    if ( seed != null ) {
                        //list is sorted -> break when peers are too young to delete
                        if ( !seed.isLastSeenTimeout(deleteOldSeedsTime) ) {
                            break;
                        }
                        deleteQueue.add(seed.hash);
                    }
                }
                for ( int i = 0; i < deleteQueue.size(); ++i ) {
                    this.peers.removeDisconnected(deleteQueue.get(i));
                }
                deleteQueue.clear();
                e = this.peers.seedsSortedPotential(true, Seed.LASTSEEN);
                checkInterruption();
                // clean potential seeds
                while ( e.hasNext() ) {
                    seed = e.next();
                    if ( seed != null ) {
                        //list is sorted -> break when peers are too young to delete
                        if ( !seed.isLastSeenTimeout(deleteOldSeedsTime) ) {
                            break;
                        }
                        deleteQueue.add(seed.hash);
                    }
                }
                for ( int i = 0; i < deleteQueue.size(); ++i ) {
                    this.peers.removePotential(deleteQueue.get(i));
                }
            }

            // check if update is available and
            // if auto-update is activated perform an automatic installation and restart
            final yacyRelease updateVersion = yacyRelease.rulebasedUpdateInfo(false);
            if ( updateVersion != null ) {
                // there is a version that is more recent. Load it and re-start with it
                this.log.logInfo("AUTO-UPDATE: downloading more recent release " + updateVersion.getUrl());
                final File downloaded = updateVersion.downloadRelease();
                final boolean devenvironment = new File(this.getAppPath(), ".git").exists();
                if ( devenvironment ) {
                    this.log
                        .logInfo("AUTO-UPDATE: omitting update because this is a development environment");
                } else if ( (downloaded == null) || (!downloaded.exists()) || (downloaded.length() == 0) ) {
                    this.log
                        .logInfo("AUTO-UPDATE: omitting update because download failed (file cannot be found, is too small or signature is bad)");
                } else {
                    yacyRelease.deployRelease(downloaded);
                    terminate(10, "auto-update to install " + downloaded.getName());
                    this.log.logInfo("AUTO-UPDATE: deploy and restart initiated");
                }
            }

            // initiate broadcast about peer startup to spread supporter url
            if ( !isRobinsonMode() && this.peers.newsPool.size(NewsPool.OUTGOING_DB) == 0 ) {
                // read profile
                final Properties profile = new Properties();
                FileInputStream fileIn = null;
                try {
                    fileIn = new FileInputStream(new File("DATA/SETTINGS/profile.txt"));
                    profile.load(fileIn);
                } catch ( final IOException e ) {
                } finally {
                    if ( fileIn != null ) {
                        try {
                            fileIn.close();
                        } catch ( final Exception e ) {
                        }
                    }
                }
                final String homepage = (String) profile.get("homepage");
                if ( (homepage != null) && (homepage.length() > 10) ) {
                    final Properties news = new Properties();
                    news.put("homepage", profile.get("homepage"));
                    this.peers.newsPool.publishMyNews(
                        this.peers.mySeed(),
                        NewsPool.CATEGORY_PROFILE_BROADCAST,
                        news);
                }
            }

            // update the cluster set
            this.clusterhashes = this.peers.clusterHashes(getConfig("cluster.peers.yacydomain", ""));

            // check if we are reachable and try to map port again if not (e.g. when router rebooted)
            if ( getConfigBool(SwitchboardConstants.UPNP_ENABLED, false) && sb.peers.mySeed().isJunior() ) {
                UPnP.addPortMapping();
            }

            // after all clean up is done, check the resource usage
            this.observer.resourceObserverJob();

            // cleanup cached search failures
            if ( getConfigBool(SwitchboardConstants.NETWORK_SEARCHVERIFY, false)
                && this.peers.mySeed().getFlagAcceptRemoteIndex() ) {
                this.tables.cleanFailURLS(getConfigLong("cleanup.failedSearchURLtimeout", -1));
            }

            return true;
        } catch ( final InterruptedException e ) {
            this.log.logInfo("cleanupJob: Shutdown detected");
            return false;
        }
    }

    /**
     * With this function the crawling process can be paused
     *
     * @param jobType
     */
    public void pauseCrawlJob(final String jobType) {
        final Object[] status = this.crawlJobsStatus.get(jobType);
        synchronized ( status[SwitchboardConstants.CRAWLJOB_SYNC] ) {
            status[SwitchboardConstants.CRAWLJOB_STATUS] = Boolean.TRUE;
        }
        setConfig(jobType + "_isPaused", "true");
    }

    /**
     * Continue the previously paused crawling
     *
     * @param jobType
     */
    public void continueCrawlJob(final String jobType) {
        final Object[] status = this.crawlJobsStatus.get(jobType);
        synchronized ( status[SwitchboardConstants.CRAWLJOB_SYNC] ) {
            if ( ((Boolean) status[SwitchboardConstants.CRAWLJOB_STATUS]).booleanValue() ) {
                status[SwitchboardConstants.CRAWLJOB_STATUS] = Boolean.FALSE;
                status[SwitchboardConstants.CRAWLJOB_SYNC].notifyAll();
            }
        }
        setConfig(jobType + "_isPaused", "false");
    }

    /**
     * @param jobType
     * @return <code>true</code> if crawling was paused or <code>false</code> otherwise
     */
    public boolean crawlJobIsPaused(final String jobType) {
        final Object[] status = this.crawlJobsStatus.get(jobType);
        synchronized ( status[SwitchboardConstants.CRAWLJOB_SYNC] ) {
            return ((Boolean) status[SwitchboardConstants.CRAWLJOB_STATUS]).booleanValue();
        }
    }

    public indexingQueueEntry parseDocument(final indexingQueueEntry in) {
        in.queueEntry.updateStatus(Response.QUEUE_STATE_PARSING);

        // debug
        if ( this.log.isFinest() ) {
            this.log.logFinest("PARSE " + in.queueEntry);
        }

        Document[] documents = null;
        try {
            documents = parseDocument(in.queueEntry);
        } catch ( final InterruptedException e ) {
            documents = null;
        } catch ( final Exception e ) {
            documents = null;
        }
        if ( documents == null ) {
            return null;
        }
        return new indexingQueueEntry(in.process, in.queueEntry, documents, null);
    }

    private Document[] parseDocument(final Response response) throws InterruptedException {
        Document[] documents = null;
        final EventOrigin processCase = response.processCase(this.peers.mySeed().hash);

        if ( this.log.isFine() ) {
            this.log.logFine("processResourceStack processCase="
                + processCase
                + ", depth="
                + response.depth()
                + ", maxDepth="
                + ((response.profile() == null) ? "null" : Integer.toString(response.profile().depth()))
                + ", must-match="
                + ((response.profile() == null) ? "null" : response
                    .profile()
                    .urlMustMatchPattern()
                    .toString())
                + ", must-not-match="
                + ((response.profile() == null) ? "null" : response
                    .profile()
                    .urlMustNotMatchPattern()
                    .toString())
                + ", initiatorHash="
                + ((response.initiator() == null) ? "null" : ASCII.String(response.initiator()))
                +
                //", responseHeader=" + ((entry.responseHeader() == null) ? "null" : entry.responseHeader().toString()) +
                ", url="
                + response.url()); // DEBUG
        }

        // PARSE CONTENT
        final long parsingStartTime = System.currentTimeMillis();
        if ( response.getContent() == null ) {
            // fetch the document from cache
            response.setContent(Cache.getContent(response.url().hash()));
            if ( response.getContent() == null ) {
                this.log.logWarning("the resource '" + response.url() + "' is missing in the cache.");
                addURLtoErrorDB(
                    response.url(),
                    response.referrerHash(),
                    response.initiator(),
                    response.name(),
                    FailCategory.FINAL_LOAD_CONTEXT,
                    "missing in cache");
                return null;
            }
        }
        assert response.getContent() != null;
        try {
            // parse the document
            documents =
                TextParser.parseSource(
                    response.url(),
                    response.getMimeType(),
                    response.getCharacterEncoding(),
                    response.getContent(),
                    response.profile().directDocByURL());
            if ( documents == null ) {
                throw new Parser.Failure("Parser returned null.", response.url());
            }
        } catch ( final Parser.Failure e ) {
            this.log.logWarning("Unable to parse the resource '" + response.url() + "'. " + e.getMessage());
            addURLtoErrorDB(
                response.url(),
                response.referrerHash(),
                response.initiator(),
                response.name(),
                FailCategory.FINAL_PROCESS_CONTEXT,
                e.getMessage());
            return null;
        }

        final long parsingEndTime = System.currentTimeMillis();

        // put anchors on crawl stack
        final long stackStartTime = System.currentTimeMillis();
        if ( ((processCase == EventOrigin.PROXY_LOAD) || (processCase == EventOrigin.LOCAL_CRAWLING))
            && ((response.profile() == null) || (response.depth() < response.profile().depth())) ) {
            // get the hyperlinks
            final Map<MultiProtocolURI, String> hl = Document.getHyperlinks(documents);

            // add all images also to the crawl stack
            hl.putAll(Document.getImagelinks(documents));

            // insert those hyperlinks to the crawler
            MultiProtocolURI nextUrl;
            for ( final Map.Entry<MultiProtocolURI, String> nextEntry : hl.entrySet() ) {
                // check for interruption
                checkInterruption();

                // process the next hyperlink
                nextUrl = nextEntry.getKey();
                final String u = nextUrl.toNormalform(true, true, false, true);
                if ( !(u.startsWith("http://")
                    || u.startsWith("https://")
                    || u.startsWith("ftp://")
                    || u.startsWith("smb://") || u.startsWith("file://")) ) {
                    continue;
                }
                // enqueue the hyperlink into the pre-notice-url db
                try {
                    this.crawlStacker.enqueueEntry(new Request(
                        response.initiator(),
                        new DigestURI(u),
                        response.url().hash(),
                        nextEntry.getValue(),
                        new Date(),
                        response.profile().handle(),
                        response.depth() + 1,
                        0,
                        0,
                        response.size() < 0 ? 0 : response.size()));
                } catch ( final MalformedURLException e ) {
                    Log.logException(e);
                }
            }
            final long stackEndTime = System.currentTimeMillis();
            if ( this.log.isInfo() ) {
                this.log.logInfo("CRAWL: ADDED "
                    + hl.size()
                    + " LINKS FROM "
                    + response.url().toNormalform(false, true)
                    + ", STACKING TIME = "
                    + (stackEndTime - stackStartTime)
                    + ", PARSING TIME = "
                    + (parsingEndTime - parsingStartTime));
            }
        }
        return documents;
    }

    public indexingQueueEntry condenseDocument(final indexingQueueEntry in) {
        in.queueEntry.updateStatus(Response.QUEUE_STATE_CONDENSING);
        if ( this.indexSegments.segment(Segments.Process.LOCALCRAWLING).getSolr() != null
            && getConfigBool("federated.service.solr.indexing.enabled", false)/*in.queueEntry.profile().pushSolr()*/) {
            // send the documents to solr
            for ( final Document doc : in.documents ) {
                try {
                    final String id = UTF8.String(new DigestURI(doc.dc_identifier(), null).hash());
                    final String iquh = UTF8.String(in.queueEntry.url().hash());
                    if ( !id.equals(iquh) ) {
                        this.log.logWarning("condenseDocument consistency check doc="
                            + id
                            + ":"
                            + doc.dc_identifier()
                            + ", query="
                            + iquh
                            + ":"
                            + in.queueEntry.url());
                        // in case that this happens it appears that the doc id is the right one
                    }
                    try {
                        this.indexSegments
                            .segment(Segments.Process.LOCALCRAWLING)
                            .getSolr()
                            .add(id, in.queueEntry.getResponseHeader(), doc);
                    } catch ( final IOException e ) {
                        Log.logWarning(
                            "SOLR",
                            "failed to send "
                                + in.queueEntry.url().toNormalform(true, false)
                                + " to solr: "
                                + e.getMessage());
                    }
                } catch ( final MalformedURLException e ) {
                    Log.logException(e);
                    continue;
                }
            }
        }

        // check if we should accept the document for our index
        if ( !getConfigBool("federated.service.yacy.indexing.enabled", false) ) {
            if ( this.log.isInfo() ) {
                this.log.logInfo("Not Condensed Resource '"
                    + in.queueEntry.url().toNormalform(false, true)
                    + "': indexing not wanted by federated rule for YaCy");
            }
            return new indexingQueueEntry(in.process, in.queueEntry, in.documents, null);
        }
        if ( !in.queueEntry.profile().indexText() && !in.queueEntry.profile().indexMedia() ) {
            if ( this.log.isInfo() ) {
                this.log.logInfo("Not Condensed Resource '"
                    + in.queueEntry.url().toNormalform(false, true)
                    + "': indexing not wanted by crawl profile");
            }
            return new indexingQueueEntry(in.process, in.queueEntry, in.documents, null);
        }
        final List<Document> doclist = new ArrayList<Document>();

        // check which files may take part in the indexing process
        for ( final Document document : in.documents ) {
            if ( document.indexingDenied() ) {
                if ( this.log.isInfo() ) {
                    this.log.logInfo("Not Condensed Resource '"
                        + in.queueEntry.url().toNormalform(false, true)
                        + "': denied by document-attached noindexing rule");
                }
                addURLtoErrorDB(
                    in.queueEntry.url(),
                    in.queueEntry.referrerHash(),
                    in.queueEntry.initiator(),
                    in.queueEntry.name(),
                    FailCategory.FINAL_PROCESS_CONTEXT,
                    "denied by document-attached noindexing rule");
                continue;
            }
            doclist.add(document);
        }

        if ( doclist.isEmpty() ) {
            return new indexingQueueEntry(in.process, in.queueEntry, in.documents, null);
        }
        in.documents = doclist.toArray(new Document[doclist.size()]);
        final Condenser[] condenser = new Condenser[in.documents.length];
        if ( this.log.isFine() ) {
            this.log.logFine("Condensing for '" + in.queueEntry.url().toNormalform(false, true) + "'");
        }
        for ( int i = 0; i < in.documents.length; i++ ) {
            condenser[i] =
                new Condenser(in.documents[i], in.queueEntry.profile().indexText(), in.queueEntry
                    .profile()
                    .indexMedia(), LibraryProvider.dymLib);

            // update image result list statistics
            // its good to do this concurrently here, because it needs a DNS lookup
            // to compute a URL hash which is necessary for a double-check
            final CrawlProfile profile = in.queueEntry.profile();
            ResultImages.registerImages(in.queueEntry.url(), in.documents[i], (profile == null)
                ? true
                : !profile.remoteIndexing());
        }
        return new indexingQueueEntry(in.process, in.queueEntry, in.documents, condenser);
    }

    public indexingQueueEntry webStructureAnalysis(final indexingQueueEntry in) {
        in.queueEntry.updateStatus(Response.QUEUE_STATE_STRUCTUREANALYSIS);
        for ( int i = 0; i < in.documents.length; i++ ) {
            assert this.webStructure != null;
            assert in != null;
            assert in.queueEntry != null;
            assert in.documents != null;
            assert in.queueEntry != null;
            this.webStructure.generateCitationReference(
                in.queueEntry.url(),
                in.documents[i],
                (in.condenser == null) ? null : in.condenser[i]); // [outlinksSame, outlinksOther]
        }
        return in;
    }

    public void storeDocumentIndex(final indexingQueueEntry in) {
        in.queueEntry.updateStatus(Response.QUEUE_STATE_INDEXSTORAGE);
        // the condenser may be null in case that an indexing is not wanted (there may be a no-indexing flag in the file)
        if ( in.condenser != null ) {
            for ( int i = 0; i < in.documents.length; i++ ) {
                storeDocumentIndex(
                    in.process,
                    in.queueEntry,
                    in.documents[i],
                    in.condenser[i],
                    null,
                    "crawler/indexing queue");
            }
        }
        in.queueEntry.updateStatus(Response.QUEUE_STATE_FINISHED);
    }

    private void storeDocumentIndex(
        final Segments.Process process,
        final Response queueEntry,
        final Document document,
        final Condenser condenser,
        final SearchEvent searchEvent,
        final String sourceName) {

        //TODO: document must carry referer, size and last modified

        // CREATE INDEX
        final String dc_title = document.dc_title();
        final DigestURI url = new DigestURI(document.dc_source());
        final DigestURI referrerURL = queueEntry.referrerURL();
        EventOrigin processCase = queueEntry.processCase(this.peers.mySeed().hash);
        if ( process == Segments.Process.SURROGATES ) {
            processCase = EventOrigin.SURROGATES;
        }

        if ( condenser == null || document.indexingDenied() ) {
            //if (this.log.isInfo()) log.logInfo("Not Indexed Resource '" + queueEntry.url().toNormalform(false, true) + "': denied by rule in document, process case=" + processCase);
            addURLtoErrorDB(
                url,
                (referrerURL == null) ? null : referrerURL.hash(),
                queueEntry.initiator(),
                dc_title,
                FailCategory.FINAL_PROCESS_CONTEXT,
                "denied by rule in document, process case=" + processCase);
            return;
        }

        if ( !queueEntry.profile().indexText() && !queueEntry.profile().indexMedia() ) {
            //if (this.log.isInfo()) log.logInfo("Not Indexed Resource '" + queueEntry.url().toNormalform(false, true) + "': denied by profile rule, process case=" + processCase + ", profile name = " + queueEntry.profile().name());
            addURLtoErrorDB(
                url,
                (referrerURL == null) ? null : referrerURL.hash(),
                queueEntry.initiator(),
                dc_title,
                FailCategory.FINAL_LOAD_CONTEXT,
                "denied by profile rule, process case="
                    + processCase
                    + ", profile name = "
                    + queueEntry.profile().name());
            return;
        }

        // remove stopwords
        this.log.logInfo("Excluded " + condenser.excludeWords(stopwords) + " words in URL " + url);

        // STORE WORD INDEX
        URIMetadataRow newEntry = null;
        try {
            newEntry =
                this.indexSegments.segment(process).storeDocument(
                    url,
                    referrerURL,
                    queueEntry.lastModified(),
                    new Date(),
                    queueEntry.size(),
                    document,
                    condenser,
                    searchEvent,
                    sourceName);
            final RSSFeed feed =
                EventChannel.channels(queueEntry.initiator() == null
                    ? EventChannel.PROXY
                    : Base64Order.enhancedCoder.equal(
                        queueEntry.initiator(),
                        ASCII.getBytes(this.peers.mySeed().hash))
                        ? EventChannel.LOCALINDEXING
                        : EventChannel.REMOTEINDEXING);
            feed.addMessage(new RSSMessage("Indexed web page", dc_title, queueEntry.url().toNormalform(
                true,
                false)));
        } catch ( final IOException e ) {
            //if (this.log.isFine()) log.logFine("Not Indexed Resource '" + queueEntry.url().toNormalform(false, true) + "': process case=" + processCase);
            addURLtoErrorDB(
                url,
                (referrerURL == null) ? null : referrerURL.hash(),
                queueEntry.initiator(),
                dc_title,
                FailCategory.FINAL_LOAD_CONTEXT,
                "error storing url: "
                    + url.toNormalform(false, true)
                    + "': process case="
                    + processCase
                    + ", error = "
                    + e.getMessage());
            return;
        }

        // store rss feeds in document into rss table
        for ( final Map.Entry<MultiProtocolURI, String> rssEntry : document.getRSS().entrySet() ) {
            final Tables.Data rssRow = new Tables.Data();
            rssRow.put("referrer", url.hash());
            rssRow.put("url", UTF8.getBytes(rssEntry.getKey().toNormalform(true, false)));
            rssRow.put("title", UTF8.getBytes(rssEntry.getValue()));
            rssRow.put("recording_date", new Date());
            try {
                this.tables.update("rss", new DigestURI(rssEntry.getKey()).hash(), rssRow);
            } catch ( final IOException e ) {
                Log.logException(e);
            }
        }

        // update url result list statistics
        ResultURLs.stack(newEntry, // loaded url db entry
            queueEntry.initiator(), // initiator peer hash
            UTF8.getBytes(this.peers.mySeed().hash), // executor peer hash
            processCase // process case
            );

        // increment number of indexed urls
        this.indexedPages++;

        // update profiling info
        if ( System.currentTimeMillis() - lastPPMUpdate > 20000 ) {
            // we don't want to do this too often
            updateMySeed();
            EventTracker.update(EventTracker.EClass.PPM, Long.valueOf(currentPPM()), true);
            lastPPMUpdate = System.currentTimeMillis();
        }
        EventTracker.update(EventTracker.EClass.INDEX, url.toNormalform(true, false), false);

        // if this was performed for a remote crawl request, notify requester
        if ( (processCase == EventOrigin.GLOBAL_CRAWLING) && (queueEntry.initiator() != null) ) {
            final Seed initiatorPeer = this.peers.get(ASCII.String(queueEntry.initiator()));
            if ( initiatorPeer != null ) {
                if ( this.clusterhashes != null ) {
                    initiatorPeer.setAlternativeAddress(this.clusterhashes.get(queueEntry.initiator()));
                }
                // start a thread for receipt sending to avoid a blocking here
                new Thread(new receiptSending(initiatorPeer, newEntry), "sending receipt to "
                    + ASCII.String(queueEntry.initiator())).start();
            }
        }
    }

    public final void addAllToIndex(
        final DigestURI url,
        final Map<MultiProtocolURI, String> links,
        final SearchEvent searchEvent,
        final String heuristicName) {

        // add the landing page to the index. should not load that again since it should be in the cache
        if ( url != null ) {
            try {
                addToIndex(url, searchEvent, heuristicName);
            } catch ( final IOException e ) {
            } catch ( final Parser.Failure e ) {
            }

        }

        // check if some of the links match with the query
        final Map<MultiProtocolURI, String> matcher = searchEvent.getQuery().separateMatches(links);

        // take the matcher and load them all
        for ( final Map.Entry<MultiProtocolURI, String> entry : matcher.entrySet() ) {
            try {
                addToIndex(new DigestURI(entry.getKey(), (byte[]) null), searchEvent, heuristicName);
            } catch ( final IOException e ) {
            } catch ( final Parser.Failure e ) {
            }
        }

        // take then the no-matcher and load them also
        for ( final Map.Entry<MultiProtocolURI, String> entry : links.entrySet() ) {
            try {
                addToIndex(new DigestURI(entry.getKey(), (byte[]) null), searchEvent, heuristicName);
            } catch ( final IOException e ) {
            } catch ( final Parser.Failure e ) {
            }
        }
    }

    /**
     * load the content of a URL, parse the content and add the content to the index This process is started
     * concurrently. The method returns immediately after the call.
     *
     * @param url the url that shall be indexed
     * @param searchEvent (optional) a search event that shall get results from the indexed pages directly
     *        feeded. If object is null then it is ignored
     * @throws IOException
     * @throws Parser.Failure
     */
    public void addToIndex(final DigestURI url, final SearchEvent searchEvent, final String heuristicName)
        throws IOException,
        Parser.Failure {
        final Segments.Process process = Segments.Process.LOCALCRAWLING;
        if ( searchEvent != null ) {
            searchEvent.addHeuristic(url.hash(), heuristicName, true);
        }
        if ( this.indexSegments.segment(process).exists(url.hash()) ) {
            return; // don't do double-work
        }
        final Request request = this.loader.request(url, true, true);
        final CrawlProfile profile = sb.crawler.getActive(ASCII.getBytes(request.profileHandle()));
        final String acceptedError = this.crawlStacker.checkAcceptance(url, profile, 0);
        if ( acceptedError != null ) {
            this.log.logWarning("addToIndex: cannot load "
                + url.toNormalform(false, false)
                + ": "
                + acceptedError);
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    final Response response =
                        Switchboard.this.loader.load(request, CacheStrategy.IFFRESH, true);
                    if ( response == null ) {
                        throw new IOException("response == null");
                    }
                    if ( response.getContent() == null ) {
                        throw new IOException("content == null");
                    }
                    if ( response.getResponseHeader() == null ) {
                        throw new IOException("header == null");
                    }
                    final Document[] documents = response.parse();
                    if ( documents != null ) {
                        for ( final Document document : documents ) {
                            if ( document.indexingDenied() ) {
                                throw new Parser.Failure("indexing is denied", url);
                            }
                            final Condenser condenser =
                                new Condenser(document, true, true, LibraryProvider.dymLib);
                            ResultImages.registerImages(url, document, true);
                            Switchboard.this.webStructure.generateCitationReference(url, document, condenser);
                            storeDocumentIndex(
                                process,
                                response,
                                document,
                                condenser,
                                searchEvent,
                                "heuristic:" + heuristicName);
                            Switchboard.this.log.logInfo("addToIndex fill of url "
                                + url.toNormalform(true, true)
                                + " finished");
                        }
                    }
                } catch ( final IOException e ) {
                    Switchboard.this.log.logWarning("addToIndex: failed loading "
                        + url.toNormalform(false, false)
                        + ": "
                        + e.getMessage());
                } catch ( final Parser.Failure e ) {
                    Switchboard.this.log.logWarning("addToIndex: failed parsing "
                        + url.toNormalform(false, false)
                        + ": "
                        + e.getMessage());
                }
            }
        }.start();
    }

    public class receiptSending implements Runnable
    {
        private final Seed initiatorPeer;
        private final URIMetadataRow reference;

        public receiptSending(final Seed initiatorPeer, final URIMetadataRow reference) {
            this.initiatorPeer = initiatorPeer;
            this.reference = reference;
        }

        public void run() {
            final long t = System.currentTimeMillis();
            final Map<String, String> response =
                Protocol.crawlReceipt(
                    Switchboard.this.peers.mySeed(),
                    this.initiatorPeer,
                    "crawl",
                    "fill",
                    "indexed",
                    this.reference,
                    "");
            if ( response == null ) {
                Switchboard.this.log.logInfo("Sending crawl receipt for '"
                    + this.reference.url().toNormalform(false, true)
                    + "' to "
                    + this.initiatorPeer.getName()
                    + " FAILED, send time = "
                    + (System.currentTimeMillis() - t));
                return;
            }
            final String delay = response.get("delay");
            Switchboard.this.log.logInfo("Sending crawl receipt for '"
                + this.reference.url().toNormalform(false, true)
                + "' to "
                + this.initiatorPeer.getName()
                + " success, delay = "
                + delay
                + ", send time = "
                + (System.currentTimeMillis() - t));
        }
    }

    public boolean accessFromLocalhost(final RequestHeader requestHeader) {

        // authorization for localhost, only if flag is set to grant localhost access as admin
        final String clientIP = requestHeader.get(HeaderFramework.CONNECTION_PROP_CLIENTIP, "");
        if ( !Domains.isLocalhost(clientIP) ) {
            return false;
        }
        final String refererHost = requestHeader.refererHost();
        return refererHost == null || refererHost.length() == 0 || Domains.isLocalhost(refererHost);
    }

    /**
     * check authentication status for request access shall be granted if return value >= 2; these are the
     * cases where an access is granted to protected pages: - a password is not configured: auth-level 2 -
     * access from localhost is granted and access comes from localhost: auth-level 3 - a password is
     * configured and access comes from localhost and the realm-value of a http-authentify String is equal to
     * the stored base64MD5: auth-level 3 - a password is configured and access comes with matching
     * http-authentify: auth-level 4
     *
     * @param requestHeader
     * @return the auth-level as described above or 1 which means 'not authorized'. a 0 is returned in case of
     *         fraud attempts
     */
    public int adminAuthenticated(final RequestHeader requestHeader) {

        // authorization in case that there is no account stored
        final String adminAccountBase64MD5 = getConfig(SwitchboardConstants.ADMIN_ACCOUNT_B64MD5, "");
        if ( adminAccountBase64MD5.length() == 0 ) {
            return 2; // no password stored; this should not happen for older peers
        }

        // authorization for localhost, only if flag is set to grant localhost access as admin
        final boolean accessFromLocalhost = accessFromLocalhost(requestHeader);
        if ( getConfigBool("adminAccountForLocalhost", false) && accessFromLocalhost ) {
            return 3; // soft-authenticated for localhost
        }

        // get the authorization string from the header
        final String realmProp = (requestHeader.get(RequestHeader.AUTHORIZATION, "xxxxxx")).trim();
        final String realmValue = realmProp.substring(6);

        // security check against too long authorization strings
        if ( realmValue.length() > 256 ) {
            return 0;
        }

        // authorization by encoded password, only for localhost access
        if ( accessFromLocalhost && (adminAccountBase64MD5.equals(realmValue)) ) {
            return 3; // soft-authenticated for localhost
        }

        // authorization by hit in userDB
        if ( this.userDB.hasAdminRight(realmProp, requestHeader.getHeaderCookies()) ) {
            return 4; //return, because 4=max
        }

        // authorization with admin keyword in configuration
        if ( realmValue == null || realmValue.length() == 0 ) {
            return 1;
        }
        if ( adminAccountBase64MD5.equals(Digest.encodeMD5Hex(realmValue)) ) {
            return 4; // hard-authenticated, all ok
        }
        return 1;
    }

    public boolean verifyAuthentication(final RequestHeader header) {
        // handle access rights
        switch ( adminAuthenticated(header) ) {
            case 0: // wrong password given
                //try { Thread.sleep(3000); } catch (final InterruptedException e) { } // prevent brute-force
                return false;
            case 1: // no password given
                return false;
            case 2: // no password stored
                return true;
            case 3: // soft-authenticated for localhost only
                return true;
            case 4: // hard-authenticated, all ok
                return true;
            default:
                return false;
        }
    }

    public void setPerformance(final int wantedPPM) {
        int wPPM = wantedPPM;
        // we consider 3 cases here
        //         wantedPPM <=   10: low performance
        // 10   <  wantedPPM <  30000: custom performance
        // 30000 <= wantedPPM        : maximum performance
        if ( wPPM <= 0 ) {
            wPPM = 1;
        }
        if ( wPPM >= 30000 ) {
            wPPM = 30000;
        }
        final int newBusySleep = 60000 / wPPM; // for wantedPPM = 10: 6000; for wantedPPM = 1000: 60

        BusyThread thread;

        thread = getThread(SwitchboardConstants.INDEX_DIST);
        if ( thread != null ) {
            setConfig(
                SwitchboardConstants.INDEX_DIST_BUSYSLEEP,
                thread.setBusySleep(Math.max(2000, thread.setBusySleep(newBusySleep * 2))));
            thread.setIdleSleep(30000);
        }

        thread = getThread(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL);
        if ( thread != null ) {
            setConfig(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL_BUSYSLEEP, thread.setBusySleep(newBusySleep));
            thread.setIdleSleep(2000);
        }
    }

    public static int accessFrequency(final Map<String, SortedSet<Long>> tracker, final String host) {
        // returns the access frequency in queries per hour for a given host and a specific tracker
        final long timeInterval = 1000 * 60 * 60;
        final SortedSet<Long> accessSet = tracker.get(host);
        if ( accessSet == null ) {
            return 0;
        }
        return accessSet.tailSet(Long.valueOf(System.currentTimeMillis() - timeInterval)).size();
    }

    public String dhtShallTransfer(final String segment) {
        final String cautionCause = onlineCaution();
        if ( cautionCause != null ) {
            return "online caution for " + cautionCause + ", dht transmission";
        }
        if ( this.peers == null ) {
            return "no DHT distribution: seedDB == null";
        }
        if ( this.peers.mySeed() == null ) {
            return "no DHT distribution: mySeed == null";
        }
        if ( this.peers.mySeed().isVirgin() ) {
            return "no DHT distribution: status is virgin";
        }
        if ( this.peers.noDHTActivity() ) {
            return "no DHT distribution: network too small";
        }
        if ( !getConfigBool("network.unit.dht", true) ) {
            return "no DHT distribution: disabled by network.unit.dht";
        }
        if ( getConfig(SwitchboardConstants.INDEX_DIST_ALLOW, "false").equalsIgnoreCase("false") ) {
            return "no DHT distribution: not enabled (per setting)";
        }
        final Segment indexSegment = this.indexSegments.segment(segment);
        if ( indexSegment.urlMetadata().size() < 10 ) {
            return "no DHT distribution: loadedURL.size() = " + indexSegment.urlMetadata().size();
        }
        if ( indexSegment.termIndex().sizesMax() < 100 ) {
            return "no DHT distribution: not enough words - wordIndex.size() = "
                + indexSegment.termIndex().sizesMax();
        }
        if ( (getConfig(SwitchboardConstants.INDEX_DIST_ALLOW_WHILE_CRAWLING, "false")
            .equalsIgnoreCase("false")) && (this.crawlQueues.noticeURL.notEmptyLocal()) ) {
            return "no DHT distribution: crawl in progress: noticeURL.stackSize() = "
                + this.crawlQueues.noticeURL.size()
                + ", sbQueue.size() = "
                + getIndexingProcessorsQueueSize();
        }
        if ( (getConfig(SwitchboardConstants.INDEX_DIST_ALLOW_WHILE_INDEXING, "false")
            .equalsIgnoreCase("false")) && (getIndexingProcessorsQueueSize() > 1) ) {
            return "no DHT distribution: indexing in progress: noticeURL.stackSize() = "
                + this.crawlQueues.noticeURL.size()
                + ", sbQueue.size() = "
                + getIndexingProcessorsQueueSize();
        }
        return null; // this means; yes, please do dht transfer
    }

    public boolean dhtTransferJob() {
        return dhtTransferJob(getConfig(SwitchboardConstants.SEGMENT_DHTOUT, "default"));
    }

    public boolean dhtTransferJob(final String segment) {
        if ( this.dhtDispatcher == null ) {
            return false;
        }
        final String rejectReason = dhtShallTransfer(segment);
        if ( rejectReason != null ) {
            if ( this.log.isFine() ) {
                this.log.logFine(rejectReason);
            }
            return false;
        }
        boolean hasDoneSomething = false;
        final long kbytesUp = ConnectionInfo.getActiveUpbytes() / 1024;
        // accumulate RWIs to transmission cloud
        if ( this.dhtDispatcher.cloudSize() > this.peers.scheme.verticalPartitions() * 2 ) {
            this.log.logInfo("dhtTransferJob: no selection, too many entries in transmission cloud: "
                + this.dhtDispatcher.cloudSize());
        } else if ( MemoryControl.available() < 1024 * 1024 * 25 ) {
            this.log.logInfo("dhtTransferJob: no selection, too less memory available : "
                + (MemoryControl.available() / 1024 / 1024)
                + " MB");
        } else if ( ConnectionInfo.getLoadPercent() > 50 ) {
            this.log.logInfo("dhtTransferJob: too many connections in httpc pool : "
                + ConnectionInfo.getCount());
            // close unused connections
//            Client.cleanup();
        } else if ( kbytesUp > 128 ) {
            this.log.logInfo("dhtTransferJob: too much upload(1), currently uploading: " + kbytesUp + " Kb");
        } else {
            byte[] startHash = null, limitHash = null;
            int tries = 10;
            while ( tries-- > 0 ) {
                startHash = PeerSelection.selectTransferStart();
                assert startHash != null;
                limitHash = PeerSelection.limitOver(this.peers, startHash);
                if ( limitHash != null ) {
                    break;
                }
            }
            if ( limitHash == null || startHash == null ) {
                this.log.logInfo("dhtTransferJob: approaching full DHT dispersion.");
                return false;
            }
            this.log.logInfo("dhtTransferJob: selected " + ASCII.String(startHash) + " as start hash");
            this.log.logInfo("dhtTransferJob: selected " + ASCII.String(limitHash) + " as limit hash");
            final boolean enqueued =
                this.dhtDispatcher.selectContainersEnqueueToCloud(
                    startHash,
                    limitHash,
                    dhtMaxContainerCount,
                    this.dhtMaxReferenceCount,
                    5000);
            hasDoneSomething = hasDoneSomething | enqueued;
            this.log.logInfo("dhtTransferJob: result from enqueueing: " + ((enqueued) ? "true" : "false"));
        }

        // check if we can deliver entries to other peers
        if ( this.dhtDispatcher.transmissionSize() >= 10 ) {
            this.log
                .logInfo("dhtTransferJob: no dequeueing from cloud to transmission: too many concurrent sessions: "
                    + this.dhtDispatcher.transmissionSize());
        } else if ( ConnectionInfo.getLoadPercent() > 75 ) {
            this.log.logInfo("dhtTransferJob: too many connections in httpc pool : "
                + ConnectionInfo.getCount());
            // close unused connections
//            Client.cleanup();
        } else if ( kbytesUp > 256 ) {
            this.log.logInfo("dhtTransferJob: too much upload(2), currently uploading: " + kbytesUp + " Kb");
        } else {
            final boolean dequeued = this.dhtDispatcher.dequeueContainer();
            hasDoneSomething = hasDoneSomething | dequeued;
            this.log.logInfo("dhtTransferJob: result from dequeueing: " + ((dequeued) ? "true" : "false"));
        }
        return hasDoneSomething;
    }

    private void addURLtoErrorDB(
        final DigestURI url,
        final byte[] referrerHash,
        final byte[] initiator,
        final String name,
        final FailCategory failCategory,
        final String failreason) {
        // assert initiator != null; // null == proxy
        // create a new errorURL DB entry
        final Request bentry =
            new Request(
                initiator,
                url,
                referrerHash,
                (name == null) ? "" : name,
                new Date(),
                null,
                0,
                0,
                0,
                0);
        this.crawlQueues.errorURL.push(bentry, initiator, new Date(), 0, failCategory, failreason, -1);
    }

    public final void heuristicSite(final SearchEvent searchEvent, final String host) {
        new Thread() {
            @Override
            public void run() {
                String r = host;
                if ( r.indexOf("//", 0) < 0 ) {
                    r = "http://" + r;
                }

                // get the links for a specific site
                DigestURI url;
                try {
                    url = new DigestURI(r);
                } catch ( final MalformedURLException e ) {
                    Log.logException(e);
                    return;
                }

                final Map<MultiProtocolURI, String> links;
                searchEvent.getRankingResult().oneFeederStarted();
                try {
                    links = Switchboard.this.loader.loadLinks(url, CacheStrategy.NOCACHE);
                    if ( links != null ) {
                        final Iterator<MultiProtocolURI> i = links.keySet().iterator();
                        while ( i.hasNext() ) {
                            if ( !i.next().getHost().endsWith(host) ) {
                                i.remove();
                            }
                        }

                        // add all pages to the index
                        addAllToIndex(url, links, searchEvent, "site");
                    }
                } catch ( final Throwable e ) {
                    Log.logException(e);
                } finally {
                    searchEvent.getRankingResult().oneFeederTerminated();
                }
            }
        }.start();
    }

    public final void heuristicScroogle(final SearchEvent searchEvent) {
        new Thread() {
            @Override
            public void run() {
                QueryParams query = searchEvent.getQuery();
                String queryString = query.queryString(true);
                final int meta = queryString.indexOf("heuristic:", 0);
                if ( meta >= 0 ) {
                    final int q = queryString.indexOf(' ', meta);
                    queryString =
                        (q >= 0)
                            ? queryString.substring(0, meta) + queryString.substring(q + 1)
                            : queryString.substring(0, meta);
                }
                final String urlString =
                    "http://www.scroogle.org/cgi-bin/nbbw.cgi?Gw="
                        + queryString.trim().replaceAll(" ", "+")
                        + "&n=2";
                final DigestURI url;
                try {
                    url = new DigestURI(MultiProtocolURI.unescape(urlString));
                } catch ( final MalformedURLException e1 ) {
                    Log.logWarning("heuristicScroogle", "url not well-formed: '" + urlString + "'");
                    return;
                }

                Map<MultiProtocolURI, String> links = null;
                searchEvent.getRankingResult().oneFeederStarted();
                try {
                    links = Switchboard.this.loader.loadLinks(url, CacheStrategy.NOCACHE);
                    if ( links != null ) {
                        final Iterator<MultiProtocolURI> i = links.keySet().iterator();
                        while ( i.hasNext() ) {
                            if ( i.next().toNormalform(false, false).indexOf("scroogle", 0) >= 0 ) {
                                i.remove();
                            }
                        }
                        Switchboard.this.log.logInfo("Heuristic: adding "
                            + links.size()
                            + " links from scroogle");
                        // add all pages to the index
                        addAllToIndex(null, links, searchEvent, "scroogle");
                    }
                } catch ( final Throwable e ) {
                    //Log.logException(e);
                } finally {
                    searchEvent.getRankingResult().oneFeederTerminated();
                }
            }
        }.start();
    }

    // blekko pattern: http://blekko.com/ws/$+/rss
    public final void heuristicRSS(
        final String urlpattern,
        final SearchEvent searchEvent,
        final String feedName) {
        final int p = urlpattern.indexOf('$');
        if ( p < 0 ) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                QueryParams query = searchEvent.getQuery();
                String queryString = query.queryString(true);
                final int meta = queryString.indexOf("heuristic:", 0);
                if ( meta >= 0 ) {
                    final int q = queryString.indexOf(' ', meta);
                    if ( q >= 0 ) {
                        queryString = queryString.substring(0, meta) + queryString.substring(q + 1);
                    } else {
                        queryString = queryString.substring(0, meta);
                    }
                }

                final String urlString =
                    urlpattern.substring(0, p)
                        + queryString.trim().replaceAll(" ", "+")
                        + urlpattern.substring(p + 1);
                final DigestURI url;
                try {
                    url = new DigestURI(MultiProtocolURI.unescape(urlString));
                } catch ( final MalformedURLException e1 ) {
                    Log.logWarning("heuristicRSS", "url not well-formed: '" + urlString + "'");
                    return;
                }

                // if we have an url then try to load the rss
                RSSReader rss = null;
                searchEvent.getRankingResult().oneFeederStarted();
                try {
                    final Response response =
                        sb.loader.load(sb.loader.request(url, true, false), CacheStrategy.NOCACHE, true);
                    final byte[] resource = (response == null) ? null : response.getContent();
                    //System.out.println("BLEKKO: " + UTF8.String(resource));
                    rss = resource == null ? null : RSSReader.parse(RSSFeed.DEFAULT_MAXSIZE, resource);
                    if ( rss != null ) {
                        final Map<MultiProtocolURI, String> links = new TreeMap<MultiProtocolURI, String>();
                        MultiProtocolURI uri;
                        for ( final RSSMessage message : rss.getFeed() ) {
                            try {
                                uri = new MultiProtocolURI(message.getLink());
                                links.put(uri, message.getTitle());
                            } catch ( final MalformedURLException e ) {
                            }
                        }

                        Log.logInfo("heuristicRSS", "Heuristic: adding "
                            + links.size()
                            + " links from '"
                            + feedName
                            + "' rss feed");
                        // add all pages to the index
                        addAllToIndex(null, links, searchEvent, feedName);
                    }
                } catch ( final Throwable e ) {
                    //Log.logException(e);
                } finally {
                    searchEvent.getRankingResult().oneFeederTerminated();
                }
            }
        }.start();
    }

    public int currentPPM() {
        return EventTracker.countEvents(EventTracker.EClass.INDEX, 20000) * 3;
    }

    public float averageQPM() {
        final long uptime = (System.currentTimeMillis() - serverCore.startupTime) / 1000;
        return (this.searchQueriesRobinsonFromRemote + this.searchQueriesGlobal) * 60f / Math.max(uptime, 1f);
    }

    public float averageQPMGlobal() {
        final long uptime = (System.currentTimeMillis() - serverCore.startupTime) / 1000;
        return (this.searchQueriesGlobal) * 60f / Math.max(uptime, 1f);
    }

    public float averageQPMPrivateLocal() {
        final long uptime = (System.currentTimeMillis() - serverCore.startupTime) / 1000;
        return (this.searchQueriesRobinsonFromLocal) * 60f / Math.max(uptime, 1f);
    }

    public float averageQPMPublicLocal() {
        final long uptime = (System.currentTimeMillis() - serverCore.startupTime) / 1000;
        return (this.searchQueriesRobinsonFromRemote) * 60f / Math.max(uptime, 1f);
    }

    public void updateMySeed() {
        this.peers.mySeed().put(Seed.PORT, Integer.toString(serverCore.getPortNr(getConfig("port", "8090"))));

        //the speed of indexing (pages/minute) of the peer
        final long uptime = (System.currentTimeMillis() - serverCore.startupTime) / 1000;
        this.peers.mySeed().put(Seed.ISPEED, Integer.toString(currentPPM()));
        this.peers.mySeed().put(Seed.RSPEED, Float.toString(averageQPM()));
        this.peers.mySeed().put(Seed.UPTIME, Long.toString(uptime / 60)); // the number of minutes that the peer is up in minutes/day (moving average MA30)
        this.peers.mySeed().put(Seed.LCOUNT, Long.toString(this.indexSegments.URLCount())); // the number of links that the peer has stored (LURL's)
        this.peers.mySeed().put(Seed.NCOUNT, Integer.toString(this.crawlQueues.noticeURL.size())); // the number of links that the peer has noticed, but not loaded (NURL's)
        this.peers.mySeed().put(
            Seed.RCOUNT,
            Integer.toString(this.crawlQueues.noticeURL.stackSize(NoticedURL.StackType.GLOBAL))); // the number of links that the peer provides for remote crawling (ZURL's)
        this.peers.mySeed().put(Seed.ICOUNT, Long.toString(this.indexSegments.RWICount())); // the minimum number of words that the peer has indexed (as it says)
        this.peers.mySeed().put(Seed.SCOUNT, Integer.toString(this.peers.sizeConnected())); // the number of seeds that the peer has stored
        this.peers.mySeed().put(
            Seed.CCOUNT,
            Float.toString(((int) ((this.peers.sizeConnected() + this.peers.sizeDisconnected() + this.peers
                .sizePotential()) * 60.0f / (uptime + 1.01f)) * 100.0f) / 100.0f)); // the number of clients that the peer connects (as connects/hour)
        this.peers.mySeed().put(Seed.VERSION, yacyBuildProperties.getLongVersion());
        this.peers.mySeed().setFlagDirectConnect(true);
        this.peers.mySeed().setLastSeenUTC();
        this.peers.mySeed().put(Seed.UTC, GenericFormatter.UTCDiffString());
        this.peers.mySeed().setFlagAcceptRemoteCrawl(getConfig("crawlResponse", "").equals("true"));
        this.peers.mySeed().setFlagAcceptRemoteIndex(getConfig("allowReceiveIndex", "").equals("true"));
        //mySeed.setFlagAcceptRemoteIndex(true);
    }

    public void loadSeedLists() {
        // uses the superseed to initialize the database with known seeds

        Seed ys;
        String seedListFileURL;
        DigestURI url;
        Iterator<String> enu;
        int lc;
        final int sc = this.peers.sizeConnected();
        ResponseHeader header;

        final RequestHeader reqHeader = new RequestHeader();
        reqHeader.put(HeaderFramework.PRAGMA, "no-cache");
        reqHeader.put(HeaderFramework.CACHE_CONTROL, "no-cache");
        reqHeader.put(HeaderFramework.USER_AGENT, ClientIdentification.getUserAgent());
        final HTTPClient client = new HTTPClient();
        client.setHeader(reqHeader.entrySet());
        client.setTimout((int) getConfigLong("bootstrapLoadTimeout", 20000));

        Network.log.logInfo("BOOTSTRAP: " + sc + " seeds known from previous run");

        // - use the superseed to further fill up the seedDB
        int ssc = 0, c = 0;
        while ( true ) {
            if ( Thread.currentThread().isInterrupted() ) {
                break;
            }
            seedListFileURL = sb.getConfig("network.unit.bootstrap.seedlist" + c, "");
            if ( seedListFileURL.length() == 0 ) {
                break;
            }
            c++;
            if ( seedListFileURL.startsWith("http://") || seedListFileURL.startsWith("https://") ) {
                // load the seed list
                try {

                    url = new DigestURI(seedListFileURL);
                    //final long start = System.currentTimeMillis();
                    client.HEADResponse(url.toString());
                    header = new ResponseHeader(client.getHttpResponse().getAllHeaders());
                    //final long loadtime = System.currentTimeMillis() - start;
                    /*if (header == null) {
                        if (loadtime > getConfigLong("bootstrapLoadTimeout", 6000)) {
                            yacyCore.log.logWarning("BOOTSTRAP: seed-list URL " + seedListFileURL + " not available, time-out after " + loadtime + " milliseconds");
                        } else {
                            yacyCore.log.logWarning("BOOTSTRAP: seed-list URL " + seedListFileURL + " not available, no content");
                        }
                    } else*/if ( header.lastModified() == null ) {
                        Network.log.logWarning("BOOTSTRAP: seed-list URL "
                            + seedListFileURL
                            + " not usable, last-modified is missing");
                    } else if ( (header.age() > 86400000) && (ssc > 0) ) {
                        Network.log.logInfo("BOOTSTRAP: seed-list URL "
                            + seedListFileURL
                            + " too old ("
                            + (header.age() / 86400000)
                            + " days)");
                    } else {
                        ssc++;
                        final byte[] content = client.GETbytes(url);
                        enu = FileUtils.strings(content);
                        lc = 0;
                        while ( enu.hasNext() ) {
                            try {
                                ys = Seed.genRemoteSeed(enu.next(), null, false, null);
                                if ( (ys != null)
                                    && (!this.peers.mySeedIsDefined() || !this.peers.mySeed().hash
                                        .equals(ys.hash)) ) {
                                    final long lastseen =
                                        Math
                                            .abs((System.currentTimeMillis() - ys.getLastSeenUTC()) / 1000 / 60);
                                    if ( lastseen < 240 ) {
                                        if ( this.peers.peerActions.connectPeer(ys, false) ) {
                                            lc++;
                                        }
                                    }
                                }
                            } catch ( final IOException e ) {
                                Network.log.logInfo("BOOTSTRAP: bad seed: " + e.getMessage());
                            }
                        }
                        Network.log.logInfo("BOOTSTRAP: "
                            + lc
                            + " seeds from seed-list URL "
                            + seedListFileURL
                            + ", AGE="
                            + (header.age() / 3600000)
                            + "h");
                    }

                } catch ( final IOException e ) {
                    // this is when wget fails, commonly because of timeout
                    Network.log.logWarning("BOOTSTRAP: failed (1) to load seeds from seed-list URL "
                        + seedListFileURL
                        + ": "
                        + e.getMessage());
                } catch ( final Exception e ) {
                    // this is when wget fails; may be because of missing internet connection
                    Network.log.logSevere("BOOTSTRAP: failed (2) to load seeds from seed-list URL "
                        + seedListFileURL
                        + ": "
                        + e.getMessage(), e);
                }
            }
        }
        Network.log.logInfo("BOOTSTRAP: "
            + (this.peers.sizeConnected() - sc)
            + " new seeds while bootstraping.");
    }

    public void initRemoteProxy() {
        // reading the proxy host name
        final String host = getConfig("remoteProxyHost", "").trim();
        // reading the proxy host port
        int port;
        try {
            port = Integer.parseInt(getConfig("remoteProxyPort", "3128"));
        } catch ( final NumberFormatException e ) {
            port = 3128;
        }
        // create new config
        ProxySettings.use4ssl = true;
        ProxySettings.use4YaCy = true;
        ProxySettings.port = port;
        ProxySettings.host = host;
        ProxySettings.use = ((ProxySettings.host != null) && (ProxySettings.host.length() > 0));

        // determining if remote proxy usage is enabled
        ProxySettings.use = getConfigBool("remoteProxyUse", false);

        // determining if remote proxy should be used for yacy -> yacy communication
        ProxySettings.use4YaCy = getConfig("remoteProxyUse4Yacy", "true").equalsIgnoreCase("true");

        // determining if remote proxy should be used for ssl connections
        ProxySettings.use4ssl = getConfig("remoteProxyUse4SSL", "true").equalsIgnoreCase("true");

        ProxySettings.user = getConfig("remoteProxyUser", "").trim();
        ProxySettings.password = getConfig("remoteProxyPwd", "").trim();

        // determining addresses for which the remote proxy should not be used
        final String remoteProxyNoProxy = getConfig("remoteProxyNoProxy", "").trim();
        ProxySettings.noProxy = remoteProxyNoProxy.split(",");
        // trim split entries
        int i = 0;
        for ( final String pattern : ProxySettings.noProxy ) {
            ProxySettings.noProxy[i] = pattern.trim();
            i++;
        }
    }

    public void checkInterruption() throws InterruptedException {
        final Thread curThread = Thread.currentThread();
        if ( (curThread instanceof WorkflowThread) && ((WorkflowThread) curThread).shutdownInProgress() ) {
            throw new InterruptedException("Shutdown in progress ...");
        } else if ( this.terminate || curThread.isInterrupted() ) {
            throw new InterruptedException("Shutdown in progress ...");
        }
    }

    public void terminate(final long delay, final String reason) {
        if ( delay <= 0 ) {
            throw new IllegalArgumentException("The shutdown delay must be greater than 0.");
        }
        this.log.logInfo("caught delayed terminate request: " + reason);
        (new delayedShutdown(this, delay, reason)).start();
    }

    public boolean shallTerminate() {
        return this.terminate;
    }

    public void terminate(final String reason) {
        this.terminate = true;
        this.log.logInfo("caught terminate request: " + reason);
        this.shutdownSync.release();
    }

    public boolean isTerminated() {
        return this.terminate;
    }

    public boolean waitForShutdown() throws InterruptedException {
        this.shutdownSync.acquire();
        return this.terminate;
    }

    /**
     * loads the url as Map Strings like abc=123 are parsed as pair: abc => 123
     *
     * @param url
     * @return
     */
    /**
     * @param url
     * @return
     */
    public static Map<String, String> loadFileAsMap(final DigestURI url) {
        final RequestHeader reqHeader = new RequestHeader();
        reqHeader.put(HeaderFramework.USER_AGENT, ClientIdentification.getUserAgent());
        final HTTPClient client = new HTTPClient();
        client.setHeader(reqHeader.entrySet());
        try {
            // sending request
            final Map<String, String> result = FileUtils.table(client.GETbytes(url));
            return (result == null) ? new HashMap<String, String>() : result;
        } catch ( final Exception e ) {
            Log.logException(e);
            return new HashMap<String, String>();
        }
    }
}

class delayedShutdown extends Thread
{
    private final Switchboard sb;
    private final long delay;
    private final String reason;

    public delayedShutdown(final Switchboard sb, final long delay, final String reason) {
        this.sb = sb;
        this.delay = delay;
        this.reason = reason;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(this.delay);
        } catch ( final InterruptedException e ) {
            this.sb.getLog().logInfo("interrupted delayed shutdown");
        } catch ( final Exception e ) {
            Log.logException(e);
        }
        this.sb.terminate(this.reason);
    }
}
