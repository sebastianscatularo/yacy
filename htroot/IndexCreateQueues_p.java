
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.yacy.cora.document.ASCII;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.kelondro.logging.Log;
import net.yacy.peers.Seed;
import net.yacy.search.Switchboard;
import de.anomic.crawler.CrawlProfile;
import de.anomic.crawler.CrawlSwitchboard;
import de.anomic.crawler.NoticedURL.StackType;
import de.anomic.crawler.retrieval.Request;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public class IndexCreateQueues_p {

    private static SimpleDateFormat dayFormatter = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
    private static String daydate(final Date date) {
        if (date == null) return "";
        return dayFormatter.format(date);
    }

    private static final int INVALID    = 0;
    private static final int URL        = 1;
    private static final int ANCHOR     = 2;
    private static final int PROFILE    = 3;
    private static final int DEPTH      = 4;
    private static final int INITIATOR  = 5;
    private static final int MODIFIED   = 6;

    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        // return variable that accumulates replacements
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        StackType stackType = StackType.LOCAL;
        int urlsPerHost = 5;
        boolean embed = false;
        String deletepattern = ".*";

        if (post != null) {
            stackType = StackType.valueOf(post.get("stack", stackType.name()).toUpperCase());
            urlsPerHost = post.getInt("urlsPerHost", urlsPerHost);
            if (post.containsKey("embed")) embed = true;

            if (post.containsKey("delete")) {
                deletepattern = post.get("pattern", deletepattern).trim();
                final int option  = post.getInt("option", INVALID);
                if (".*".equals(deletepattern)) {
                    sb.crawlQueues.noticeURL.clear(stackType);
                    try { sb.cleanProfiles(); } catch (final InterruptedException e) {/* ignore this */}
                } else if (option > INVALID) {
                    try {
                        // compiling the regular expression
                        final Pattern compiledPattern = Pattern.compile(deletepattern);

                        if (option == PROFILE) {
                            // search and delete the crawl profile (_much_ faster, independant of queue size)
                            // XXX: what to do about the annoying LOST PROFILE messages in the log?
                            CrawlProfile entry;
                            for (final byte[] handle: sb.crawler.getActive()) {
                                entry = sb.crawler.getActive(handle);
                                final String name = entry.name();
                                if (name.equals(CrawlSwitchboard.CRAWL_PROFILE_PROXY) ||
                                        name.equals(CrawlSwitchboard.CRAWL_PROFILE_REMOTE) ||
                                        name.equals(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_LOCAL_TEXT)  ||
                                        name.equals(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_GLOBAL_TEXT)  ||
                                        name.equals(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_LOCAL_MEDIA) ||
                                        name.equals(CrawlSwitchboard.CRAWL_PROFILE_SNIPPET_GLOBAL_MEDIA) ||
                                        name.equals(CrawlSwitchboard.CRAWL_PROFILE_SURROGATE))
                                    continue;
                                if (compiledPattern.matcher(name).find()) sb.crawler.removeActive(entry.handle().getBytes());
                            }
                        } else {
                            // iterating through the list of URLs
                            final Iterator<Request> iter = sb.crawlQueues.noticeURL.iterator(stackType);
                            Request entry;
                            final List<byte[]> removehashes = new ArrayList<byte[]>();
                            while (iter.hasNext()) {
                                if ((entry = iter.next()) == null) continue;
                                String value = null;

                                location: switch (option) {
                                    case URL:       value = (entry.url() == null) ? null : entry.url().toString(); break location;
                                    case ANCHOR:    value = entry.name(); break location;
                                    case DEPTH:     value = Integer.toString(entry.depth()); break location;
                                    case INITIATOR:
                                        value = (entry.initiator() == null || entry.initiator().length == 0) ? "proxy" : ASCII.String(entry.initiator());
                                        break location;
                                    case MODIFIED:  value = daydate(entry.appdate()); break location;
                                    default: value = null; break location;
                                }

                                if (value != null && compiledPattern.matcher(value).matches()) removehashes.add(entry.url().hash());
                            }
                            Log.logInfo("IndexCreateQueues_p", "created a remove list with " + removehashes.size() + " entries for pattern '" + deletepattern + "'");
                            for (final byte[] b: removehashes) {
                                sb.crawlQueues.noticeURL.removeByURLHash(b);
                            }
                        }
                    } catch (final PatternSyntaxException e) {
                        Log.logException(e);
                    }
                }
            }
        }

        int stackSize = sb.crawlQueues.noticeURL.stackSize(stackType);
        if (stackSize == 0) {
            prop.put("crawler", "0");
        } else {
            prop.put("crawler", "1");
            prop.put("crawler_embed", embed ? 1 : 0);
            prop.put("crawler_embed_deletepattern", deletepattern);
            prop.put("crawler_embed_queuename", stackType.name());

            final Map<String, Integer[]> hosts = sb.crawlQueues.noticeURL.getDomainStackHosts(stackType);

            int hc = 0;
            for (Map.Entry<String, Integer[]> host: hosts.entrySet()) {
                prop.putHTML("crawler_host_" + hc + "_hostname", host.getKey());
                prop.put("crawler_host_" + hc + "_embed", embed ? 1 : 0);
                prop.put("crawler_host_" + hc + "_urlsPerHost", urlsPerHost);
                prop.putHTML("crawler_host_" + hc + "_queuename", stackType.name());
                prop.put("crawler_host_" + hc + "_hostcount", host.getValue()[0]);
                prop.put("crawler_host_" + hc + "_hostdelta", host.getValue()[1]);
                List<Request> domainStackReferences = sb.crawlQueues.noticeURL.getDomainStackReferences(stackType, host.getKey(), urlsPerHost);

                Seed initiator;
                String profileHandle;
                CrawlProfile profileEntry;
                int count = 0;
                for (Request request: domainStackReferences) {
                    if (request == null) continue;
                    initiator = sb.peers.getConnected(request.initiator() == null ? "" : ASCII.String(request.initiator()));
                    profileHandle = request.profileHandle();
                    profileEntry = profileHandle == null ? null : sb.crawler.getActive(profileHandle.getBytes());
                    prop.putHTML("crawler_host_" + hc + "_list_" + count + "_initiator", ((initiator == null) ? "proxy" : initiator.getName()) );
                    prop.put("crawler_host_" + hc + "_list_" + count + "_profile", ((profileEntry == null) ? "unknown" : profileEntry.name()));
                    prop.put("crawler_host_" + hc + "_list_" + count + "_depth", request.depth());
                    prop.put("crawler_host_" + hc + "_list_" + count + "_modified", daydate(request.appdate()) );
                    prop.putHTML("crawler_host_" + hc + "_list_" + count + "_anchor", request.name());
                    prop.put("crawler_host_" + hc + "_list_" + count + "_delta", sb.crawlQueues.noticeURL.getDomainSleepTime(stackType, sb.robots, sb.crawler, request));
                    prop.putHTML("crawler_host_" + hc + "_list_" + count + "_url", request.url().toNormalform(false, true));
                    prop.put("crawler_host_" + hc + "_list_" + count + "_hash", request.url().hash());
                    count++;
                }
                prop.putNum("crawler_host_" + hc + "_list", count);
                hc++;
            }
            prop.put("crawler_host", hc);
        }

        prop.put("embed", embed ? 1 : 0);
        prop.put("queuename", stackType.name().charAt(0) + stackType.name().substring(1).toLowerCase());
        prop.put("embed_queuename", stackType.name().charAt(0) + stackType.name().substring(1).toLowerCase());

        // return rewrite properties
        return prop;
    }
}
