// yacy.java
// -----------------------
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.yacy.net
// Frankfurt, Germany, 2004, 2005
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
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

package net.yacy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import net.yacy.cora.date.GenericFormatter;
import net.yacy.cora.lod.JenaTripleStore;
import net.yacy.cora.protocol.ClientIdentification;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.protocol.http.HTTPClient;
import net.yacy.cora.sorting.Array;
import net.yacy.gui.YaCyApp;
import net.yacy.gui.framework.Browser;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.util.FileUtils;
import net.yacy.kelondro.util.Formatter;
import net.yacy.kelondro.util.MemoryControl;
import net.yacy.kelondro.util.OS;
import net.yacy.peers.operation.yacyBuildProperties;
import net.yacy.peers.operation.yacyRelease;
import net.yacy.peers.operation.yacyVersion;
import net.yacy.search.Switchboard;
import net.yacy.search.SwitchboardConstants;

import com.google.common.io.Files;

import de.anomic.data.Translator;
import de.anomic.http.server.HTTPDemon;
import de.anomic.server.serverCore;

/**
* This is the main class of YaCy. Several threads are started from here:
* <ul>
* <li>one single instance of the plasmaSwitchboard is generated, which itself
* starts a thread with a plasmaHTMLCache object. This object simply counts
* files sizes in the cache and terminates them. It also generates a
* plasmaCrawlerLoader object, which may itself start some more httpc-calling
* threads to load web pages. They terminate automatically when a page has
* loaded.
* <li>one serverCore - thread is started, which implements a multi-threaded
* server. The process may start itself many more processes that handle
* connections.lo
* <li>finally, all idle-dependent processes are written in a queue in
* plasmaSwitchboard which are worked off inside an idle-sensitive loop of the
* main process. (here)
* </ul>
*
* On termination, the following must be done:
* <ul>
* <li>stop feeding of the crawling process because it otherwise fills the
* indexing queue.
* <li>say goodbye to connected peers and disable new connections. Don't wait for
* success.
* <li>first terminate the serverCore thread. This prevents that new cache
* objects are queued.
* <li>wait that the plasmaHTMLCache terminates (it should be normal that this
* process already has terminated).
* <li>then wait for termination of all loader process of the
* plasmaCrawlerLoader.
* <li>work off the indexing and cache storage queue. These values are inside a
* RAM cache and would be lost otherwise.
* <li>write all settings.
* <li>terminate.
* </ul>
*/

public final class yacy {

    // static objects
    public static final String vString = yacyBuildProperties.getVersion();
    public static float version = 0.1f;

    public static final String vDATE   = yacyBuildProperties.getBuildDate();
    public static final String copyright = "[ YaCy v" + vString + ", build " + vDATE + " by Michael Christen / www.yacy.net ]";
    public static final String hline = "-------------------------------------------------------------------------------";
    public static final Semaphore shutdownSemaphore = new Semaphore(0);

    /**
     * a reference to the {@link Switchboard} created by the
     * {@link yacy#startup(String, long, long)} method.
     */
    private static Switchboard sb = null;
	public static String homedir;
	public static File dataHome_g;

    /**
    * Starts up the whole application. Sets up all datastructures and starts
    * the main threads.
    *
    * @param homePath Root-path where all information is to be found.
    * @param startupFree free memory at startup time, to be used later for statistics
    */
    private static void startup(final File dataHome, final File appHome, final long startupMemFree, final long startupMemTotal, final boolean gui) {
        try {
            // start up
            System.out.println(copyright);
            System.out.println(hline);

            // check java version
            try {
                "a".codePointAt(0); // needs at least Java 1.5
            } catch (final NoSuchMethodError e) {
                System.err.println("STARTUP: Java Version too low. You need at least Java 1.5 to run YaCy"); // TODO: is 1.6 now
                Thread.sleep(3000);
                System.exit(-1);
            }

            // ensure that there is a DATA directory, if not, create one and if that fails warn and die
            mkdirsIfNeseccary(dataHome);
            dataHome_g = dataHome;
            mkdirsIfNeseccary(appHome);
            File f = new File(dataHome, "DATA/");
            mkdirsIfNeseccary(f);
			if (!(f.exists())) {
				System.err.println("Error creating DATA-directory in " + dataHome.toString() + " . Please check your write-permission for this folder. YaCy will now terminate.");
				System.exit(-1);
			}

			homedir = appHome.toString();

            // setting up logging
			f = new File(dataHome, "DATA/LOG/");
            mkdirsIfNeseccary(f);
			f = new File(dataHome, "DATA/LOG/yacy.logging");
			final File f0 = new File(appHome, "defaults/yacy.logging");
			if (!f.exists() || f0.lastModified() > f.lastModified()) try {
			    Files.copy(f0, f);
            } catch (final IOException e){
                System.out.println("could not copy yacy.logging");
            }
            try{
                Log.configureLogging(dataHome, appHome, new File(dataHome, "DATA/LOG/yacy.logging"));
            } catch (final IOException e) {
                System.out.println("could not find logging properties in homePath=" + dataHome);
                Log.logException(e);
            }
            Log.logConfig("STARTUP", "YaCy version: " + yacyBuildProperties.getVersion() + "/" + yacyBuildProperties.getSVNRevision());
            Log.logConfig("STARTUP", "Java version: " + System.getProperty("java.version", "no-java-version"));
            Log.logConfig("STARTUP", "Operation system: " + System.getProperty("os.name","unknown"));
            Log.logConfig("STARTUP", "Application root-path: " + appHome);
            Log.logConfig("STARTUP", "Data root-path: " + dataHome);
            Log.logConfig("STARTUP", "Time zone: UTC" + GenericFormatter.UTCDiffString() + "; UTC+0000 is " + System.currentTimeMillis());
            Log.logConfig("STARTUP", "Maximum file system path length: " + OS.maxPathLength);

            f = new File(dataHome, "DATA/yacy.running");
            if (f.exists()) {                // another instance running? VM crash? User will have to care about this
                Log.logSevere("STARTUP", "WARNING: the file " + f + " exists, this usually means that a YaCy instance is still running");
                delete(f);
            }
            if(!f.createNewFile())
                Log.logSevere("STARTUP", "WARNING: the file " + f + " can not be created!");
            try { new FileOutputStream(f).write(Integer.toString(OS.getPID()).getBytes()); } catch (final Exception e) { } // write PID
            f.deleteOnExit();
            FileChannel channel = null;
            FileLock lock = null;
            try {
            	channel = new RandomAccessFile(f,"rw").getChannel();
            	lock = channel.tryLock(); // lock yacy.running
            } catch (final Exception e) { }

            final String oldconf = "DATA/SETTINGS/httpProxy.conf".replace("/", File.separator);
            final String newconf = "DATA/SETTINGS/yacy.conf".replace("/", File.separator);
            final File oldconffile = new File(dataHome, oldconf);
            if (oldconffile.exists()) {
            	final File newconfFile = new File(dataHome, newconf);
                if(!oldconffile.renameTo(newconfFile))
                    Log.logSevere("STARTUP", "WARNING: the file " + oldconffile + " can not be renamed to "+ newconfFile +"!");
            }
            try {
                sb = new Switchboard(dataHome, appHome, "defaults/yacy.init".replace("/", File.separator), newconf);
            } catch (final RuntimeException e) {
                Log.logSevere("STARTUP", "YaCy cannot start: " + e.getMessage(), e);
                System.exit(-1);
            }
            //sbSync.V(); // signal that the sb reference was set

            // switch the memory strategy
            MemoryControl.setStandardStrategy(sb.getConfigBool("memory.standardStrategy", true));

            // save information about available memory at startup time
            sb.setConfig("memoryFreeAfterStartup", startupMemFree);
            sb.setConfig("memoryTotalAfterStartup", startupMemTotal);

            // start gui if wanted
            if (gui) YaCyApp.start("localhost", (int) sb.getConfigLong("port", 8090));

            // hardcoded, forced, temporary value-migration
            sb.setConfig("htTemplatePath", "htroot/env/templates");

            int oldRev;
    	    try {
                oldRev = Integer.parseInt(sb.getConfig("svnRevision", "0"));
            } catch (final NumberFormatException e) {
                oldRev = 0;
    	    }
            final int newRev = Integer.parseInt(yacyBuildProperties.getSVNRevision());
            sb.setConfig("svnRevision", yacyBuildProperties.getSVNRevision());
            sb.setConfig("applicationRoot", appHome.toString());
            sb.setConfig("dataRoot", dataHome.toString());
            yacyVersion.latestRelease = version;

            // read environment
            final int timeout = Math.max(5000, Integer.parseInt(sb.getConfig("httpdTimeout", "5000")));

            // create some directories
            final File htRootPath = new File(appHome, sb.getConfig("htRootPath", "htroot"));
            final File htDocsPath = sb.getDataPath(SwitchboardConstants.HTDOCS_PATH, SwitchboardConstants.HTDOCS_PATH_DEFAULT);
            mkdirIfNeseccary(htDocsPath);
            //final File htTemplatePath = new File(homePath, sb.getConfig("htTemplatePath","htdocs"));

            // create default notifier picture
            //TODO: Use templates instead of copying images ...
            if (!((new File(htDocsPath, "notifier.gif")).exists())) try {
                Files.copy(new File(htRootPath, "env/grafics/empty.gif"),
                                     new File(htDocsPath, "notifier.gif"));
            } catch (final IOException e) {}

            final File htdocsReadme = new File(htDocsPath, "readme.txt");
            if (!(htdocsReadme.exists())) try {FileUtils.copy((
                    "This is your root directory for individual Web Content\r\n" +
                    "\r\n" +
                    "Please place your html files into the www subdirectory.\r\n" +
                    "The URL of that path is either\r\n" +
                    "http://www.<your-peer-name>.yacy    or\r\n" +
                    "http://<your-ip>:<your-port>/www\r\n" +
                    "\r\n" +
                    "Other subdirectories may be created; they map to corresponding sub-domains.\r\n" +
                    "This directory shares it's content with the applications htroot path, so you\r\n" +
                    "may access your yacy search page with\r\n" +
                    "http://<your-peer-name>.yacy/\r\n" +
                    "\r\n").getBytes(), htdocsReadme);} catch (final IOException e) {
                        System.out.println("Error creating htdocs readme: " + e.getMessage());
                    }

            final File wwwDefaultPath = new File(htDocsPath, "www");
            mkdirIfNeseccary(wwwDefaultPath);


            final File shareDefaultPath = new File(htDocsPath, "share");
            mkdirIfNeseccary(shareDefaultPath);

            migration.migrate(sb, oldRev, newRev);

            // delete old release files
            final int deleteOldDownloadsAfterDays = (int) sb.getConfigLong("update.deleteOld", 30);
            yacyRelease.deleteOldDownloads(sb.releasePath, deleteOldDownloadsAfterDays );

            // set user-agent
            HTTPClient.setDefaultUserAgent(ClientIdentification.getUserAgent());

            // initial fill of the triplestore
            File triplestore = new File(sb.getConfig("triplestore", new File(dataHome, "DATA/TRIPLESTORE").getAbsolutePath()));
            mkdirIfNeseccary(triplestore);
            for (String s: triplestore.list()) {
            	if ((s.endsWith(".rdf") || s.endsWith(".nt")) && !s.equals("local.rdf") && !s.endsWith("_triplestore.rdf") && !s.startsWith("private_store_")) {
                    try {
                        JenaTripleStore.load(new File(triplestore, s).getAbsolutePath());
                    } catch (IOException e) {
                        Log.logException(e);
                    }
            	}
            }
            if (sb.getConfigBool("triplestore.persistent", false)) {
                File local = new File(triplestore, "local.rdf");
                if (local.exists()) {
                    try {
                        JenaTripleStore.load(local.getAbsolutePath());
                    } catch (IOException e) {
                        Log.logException(e);
                    }
                }
            }

            // start main threads
            final String port = sb.getConfig("port", "8090");
            try {
                final HTTPDemon protocolHandler = new HTTPDemon(sb);
                final serverCore server = new serverCore(
                        timeout /*control socket timeout in milliseconds*/,
                        true /* block attacks (wrong protocol) */,
                        protocolHandler /*command class*/,
                        sb,
                        30000 /*command max length incl. GET args*/);
                server.setName("httpd:"+port);
                server.setPriority(Thread.MAX_PRIORITY);
                server.setObeyIntermission(false);

                // start the server
                sb.deployThread("10_httpd", "HTTPD Server/Proxy", "the HTTPD, used as web server and proxy", null, server, 0, 0, 0, 0);
                //server.start();

                // open the browser window
                final boolean browserPopUpTrigger = sb.getConfig(SwitchboardConstants.BROWSER_POP_UP_TRIGGER, "true").equals("true");
                if (browserPopUpTrigger) try {
                    final String  browserPopUpPage = sb.getConfig(SwitchboardConstants.BROWSER_POP_UP_PAGE, "ConfigBasic.html");
                    //boolean properPW = (sb.getConfig("adminAccount", "").isEmpty()) && (sb.getConfig(httpd.ADMIN_ACCOUNT_B64MD5, "").length() > 0);
                    //if (!properPW) browserPopUpPage = "ConfigBasic.html";
                    Browser.openBrowser((server.withSSL()?"https":"http") + "://localhost:" + serverCore.getPortNr(port) + "/" + browserPopUpPage);
                } catch (final Throwable e) {
                    // cannot open browser. This may be normal in headless environments
                    //Log.logException(e);
                }

                // enable browser popup, http server is ready now
                sb.tray.setReady();

                //regenerate Locales from Translationlist, if needed
                final File locale_source = sb.getAppPath("locale.source", "locales");
                final String lang = sb.getConfig("locale.language", "");
                if (!lang.equals("") && !lang.equals("default")) { //locale is used
                    String currentRev = "";
                    try{
                        final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(sb.getDataPath("locale.translated_html", "DATA/LOCALE/htroot"), lang+"/version" ))));
                        currentRev = br.readLine();
                        br.close();
                    }catch(final IOException e){
                        //Error
                    }

                    if (!currentRev.equals(sb.getConfig("svnRevision", ""))) try { //is this another version?!
                        final File sourceDir = new File(sb.getConfig("htRootPath", "htroot"));
                        final File destDir = new File(sb.getDataPath("locale.translated_html", "DATA/LOCALE/htroot"), lang);
                        if (Translator.translateFilesRecursive(sourceDir, destDir, new File(locale_source, lang + ".lng"), "html,template,inc", "locale")){ //translate it
                            //write the new Versionnumber
                            final BufferedWriter bw = new BufferedWriter(new PrintWriter(new FileWriter(new File(destDir, "version"))));
                            bw.write(sb.getConfig("svnRevision", "Error getting Version"));
                            bw.close();
                        }
                    } catch (final IOException e) {}
                }
                // initialize number formatter with this locale
                Formatter.setLocale(lang);

                // registering shutdown hook
                Log.logConfig("STARTUP", "Registering Shutdown Hook");
                final Runtime run = Runtime.getRuntime();
                run.addShutdownHook(new shutdownHookThread(Thread.currentThread(), sb));

                // save information about available memory after all initializations
                //try {
                    sb.setConfig("memoryFreeAfterInitBGC", MemoryControl.free());
                    sb.setConfig("memoryTotalAfterInitBGC", MemoryControl.total());
                    System.gc();
                    sb.setConfig("memoryFreeAfterInitAGC", MemoryControl.free());
                    sb.setConfig("memoryTotalAfterInitAGC", MemoryControl.total());
                //} catch (ConcurrentModificationException e) {}

                // wait for server shutdown
                try {
                    sb.waitForShutdown();
                } catch (final Exception e) {
                    Log.logSevere("MAIN CONTROL LOOP", "PANIC: " + e.getMessage(),e);
                }
                // shut down
                Array.terminate();
                Log.logConfig("SHUTDOWN", "caught termination signal");
                server.terminate(false);
                server.interrupt();
                server.close();

                // idle until the processes are down
                if (server.isAlive()) {
                    server.interrupt();
                }
                Log.logConfig("SHUTDOWN", "server has terminated");
                sb.close();
            } catch (final Exception e) {
                Log.logSevere("STARTUP", "Unexpected Error: " + e.getClass().getName(),e);
                //System.exit(1);
            }
            if(lock != null) lock.release();
            if(channel != null) channel.close();
        } catch (final Exception ee) {
            Log.logSevere("STARTUP", "FATAL ERROR: " + ee.getMessage(),ee);
        } finally {
        }

        // save the triple store
        if (sb.getConfigBool("triplestore.persistent", false)) {
            JenaTripleStore.saveAll();
        }

        Log.logConfig("SHUTDOWN", "goodbye. (this is the last line)");
        Log.shutdown();
        shutdownSemaphore.release(1000);
        try {
            System.exit(0);
        } catch (final Exception e) {} // was once stopped by de.anomic.net.ftpc$sm.checkExit(ftpc.java:1790)
    }

	/**
	 * @param f
	 */
	private static void delete(final File f) {
		if(!f.delete())
		    Log.logSevere("STARTUP", "WARNING: the file " + f + " can not be deleted!");
	}

	/**
	 * @see File#mkdir()
	 * @param path
	 */
	private static void mkdirIfNeseccary(final File path) {
		if (!(path.exists()))
			if(!path.mkdir())
				Log.logWarning("STARTUP", "could not create directory "+ path.toString());
	}

	/**
	 * @see File#mkdirs()
	 * @param path
	 */
	public static void mkdirsIfNeseccary(final File path) {
		if (!(path.exists()))
			if(!path.mkdirs())
				Log.logWarning("STARTUP", "could not create directories "+ path.toString());
	}

	/**
    * Loads the configuration from the data-folder.
    * FIXME: Why is this called over and over again from every method, instead
    * of setting the configurationdata once for this class in main?
    *
    * @param mes Where are we called from, so that the errormessages can be
    * more descriptive.
    * @param homePath Root-path where all the information is to be found.
    * @return Properties read from the configurationfile.
    */
    private static Properties configuration(final String mes, final File homePath) {
        Log.logConfig(mes, "Application Root Path: " + homePath.toString());

        // read data folder
        final File dataFolder = new File(homePath, "DATA");
        if (!(dataFolder.exists())) {
            Log.logSevere(mes, "Application was never started or root path wrong.");
            System.exit(-1);
        }

        final Properties config = new Properties();
        FileInputStream fis = null;
		try {
        	fis  = new FileInputStream(new File(homePath, "DATA/SETTINGS/yacy.conf"));
            config.load(fis);
        } catch (final FileNotFoundException e) {
            Log.logSevere(mes, "could not find configuration file.");
            System.exit(-1);
        } catch (final IOException e) {
            Log.logSevere(mes, "could not read configuration file.");
            System.exit(-1);
        } finally {
        	if(fis != null) {
        		try {
					fis.close();
				} catch (final IOException e) {
				    Log.logException(e);
				}
        	}
        }

        return config;
    }

    /**
    * Call the shutdown-page of YaCy to tell it to shut down. This method is
    * called if you start yacy with the argument -shutdown.
    *
    * @param homePath Root-path where all the information is to be found.
    */
    public static void shutdown(final File homePath) {
        // start up
        System.out.println(copyright);
        System.out.println(hline);
        submitURL(homePath, "Steering.html?shutdown=", "Terminate YaCy");
    }

    public static void update(final File homePath) {
        // start up
        System.out.println(copyright);
        System.out.println(hline);
        submitURL(homePath, "ConfigUpdate_p.html?autoUpdate=", "Update YaCy to most recent version");
    }

    private static void submitURL(final File homePath, final String path, final String processdescription) {
        final Properties config = configuration("COMMAND-STEERING", homePath);

        // read port
        final int port = serverCore.getPortNr(config.getProperty("port", "8090"));

        // read password
        String encodedPassword = (String) config.get(SwitchboardConstants.ADMIN_ACCOUNT_B64MD5);
        if (encodedPassword == null) encodedPassword = ""; // not defined

        // send 'wget' to web interface
        final RequestHeader requestHeader = new RequestHeader();
        requestHeader.put(RequestHeader.AUTHORIZATION, "realm=" + encodedPassword); // for http-authentify
//        final Client con = new Client(10000, requestHeader);
        final HTTPClient con = new HTTPClient();
        con.setHeader(requestHeader.entrySet());
//        ResponseContainer res = null;
        try {
//            res = con.GET("http://localhost:"+ port +"/" + path);
            con.GETbytes("http://localhost:"+ port +"/" + path);

            // read response
//            if (res.getStatusLine().startsWith("2")) {
            if (con.getStatusCode() > 199 && con.getStatusCode() < 300) {
                Log.logConfig("COMMAND-STEERING", "YACY accepted steering command: " + processdescription);
//                final ByteArrayOutputStream bos = new ByteArrayOutputStream(); //This is stream is not used???
//                try {
//                    FileUtils.copyToStream(new BufferedInputStream(res.getDataAsStream()), new BufferedOutputStream(bos));
//                } finally {
//                    res.closeStream();
//                }
            } else {
//                Log.logSevere("COMMAND-STEERING", "error response from YACY socket: " + res.getStatusLine());
            	Log.logSevere("COMMAND-STEERING", "error response from YACY socket: " + con.getHttpResponse().getStatusLine());
                System.exit(-1);
            }
        } catch (final IOException e) {
            Log.logSevere("COMMAND-STEERING", "could not establish connection to YACY socket: " + e.getMessage());
            System.exit(-1);
//        } finally {
//            // release connection
//            if(res != null) {
//                res.closeStream();
//            }
        }

        try {
			HTTPClient.closeConnectionManager();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

        // finished
        Log.logConfig("COMMAND-STEERING", "SUCCESSFULLY FINISHED COMMAND: " + processdescription);
    }

    /**
     * Main-method which is started by java. Checks for special arguments or
     * starts up the application.
     *
     * @param args
     *            Given arguments from the command line.
     */
    public static void main(String args[]) {

    	try {

	        // check assertion status
	        //ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
	        boolean assertionenabled = false;
	        assert assertionenabled = true;
	        if (assertionenabled) System.out.println("Asserts are enabled");

	        // check memory amount
	        System.gc();
	        final long startupMemFree  = MemoryControl.free();
	        final long startupMemTotal = MemoryControl.total();

	        // maybe go into headless awt mode: we have three cases depending on OS and one exception:
	        // windows   : better do not go into headless mode
	        // mac       : go into headless mode because an application is shown in gui which may not be wanted
	        // linux     : go into headless mode because this does not need any head operation
	        // exception : if the -gui option is used then do not go into headless mode since that uses a gui
	        boolean headless = true;
	        if (OS.isWindows) headless = false;
	        if (args.length >= 1 && args[0].toLowerCase().equals("-gui")) headless = false;
	        System.setProperty("java.awt.headless", headless ? "true" : "false");

	        String s = ""; for (final String a: args) s += a + " ";
	        yacyRelease.startParameter = s.trim();

	        File applicationRoot = new File(System.getProperty("user.dir").replace('\\', '/'));
	        File dataRoot = applicationRoot;
	        //System.out.println("args.length=" + args.length);
	        //System.out.print("args=["); for (int i = 0; i < args.length; i++) System.out.print(args[i] + ", "); System.out.println("]");
	        if ((args.length >= 1) && (args[0].toLowerCase().equals("-startup") || args[0].equals("-start"))) {
	            // normal start-up of yacy
	            if (args.length > 1) dataRoot = new File(System.getProperty("user.home").replace('\\', '/'), args[1]);
	            startup(dataRoot, applicationRoot, startupMemFree, startupMemTotal, false);
	        } else if (args.length >= 1 && args[0].toLowerCase().equals("-gui")) {
	            // start-up of yacy with gui
	            if (args.length > 1) dataRoot = new File(System.getProperty("user.home").replace('\\', '/'), args[1]);
	            startup(dataRoot, applicationRoot, startupMemFree, startupMemTotal, true);
	        } else if ((args.length >= 1) && ((args[0].toLowerCase().equals("-shutdown")) || (args[0].equals("-stop")))) {
	            // normal shutdown of yacy
	            if (args.length == 2) applicationRoot= new File(args[1]);
	            shutdown(applicationRoot);
	        } else if ((args.length >= 1) && (args[0].toLowerCase().equals("-update"))) {
	            // aut-update yacy
	            if (args.length == 2) applicationRoot= new File(args[1]);
	            update(applicationRoot);
	        } else if ((args.length >= 1) && (args[0].toLowerCase().equals("-version"))) {
	            // show yacy version
	            System.out.println(copyright);
	        } else {
	            if (args.length == 1) applicationRoot= new File(args[0]);
	            startup(dataRoot, applicationRoot, startupMemFree, startupMemTotal, false);
	        }
    	} finally {
    		Log.shutdown();
    	}
    }
}

/**
* This class is a helper class whose instance is started, when the java virtual
* machine shuts down. Signals the plasmaSwitchboard to shut down.
*/
class shutdownHookThread extends Thread {
    private final Switchboard sb;
    private final Thread mainThread;

    public shutdownHookThread(final Thread mainThread, final Switchboard sb) {
        super();
        this.sb = sb;
        this.mainThread = mainThread;
    }

    @Override
    public void run() {
        try {
            if (!this.sb.isTerminated()) {
                Log.logConfig("SHUTDOWN","Shutdown via shutdown hook.");

                // sending the yacy main thread a shutdown signal
                Log.logFine("SHUTDOWN","Signaling shutdown to the switchboard.");
                this.sb.terminate("shutdown hook");

                // waiting for the yacy thread to finish execution
                Log.logFine("SHUTDOWN","Waiting for main thread to finish.");
                if (this.mainThread.isAlive() && !this.sb.isTerminated()) {
                    this.mainThread.join();
                }
            }
        } catch (final Exception e) {
            Log.logSevere("SHUTDOWN","Unexpected error. " + e.getClass().getName(),e);
        }
    }
}
