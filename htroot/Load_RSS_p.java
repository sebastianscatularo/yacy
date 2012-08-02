/**
 *  Load_RSS_p
 *  Copyright 2010 by Michael Peter Christen, mc@yacy.net, Frankfurt am Main, Germany
 *  First released 20.08.2010 at http://yacy.net
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.yacy.cora.document.Hit;
import net.yacy.cora.document.RSSFeed;
import net.yacy.cora.document.RSSMessage;
import net.yacy.cora.document.RSSReader;
import net.yacy.cora.document.UTF8;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.services.federated.yacy.CacheStrategy;
import net.yacy.cora.util.SpaceExceededException;
import net.yacy.document.Parser.Failure;
import net.yacy.kelondro.blob.Tables;
import net.yacy.kelondro.blob.Tables.Row;
import net.yacy.kelondro.data.meta.DigestURI;
import net.yacy.kelondro.logging.Log;
import net.yacy.repository.Blacklist.BlacklistType;
import net.yacy.search.Switchboard;
import de.anomic.crawler.CrawlQueues;
import de.anomic.crawler.RSSLoader;
import de.anomic.crawler.retrieval.Response;
import de.anomic.data.WorkTables;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public class Load_RSS_p {

    public static serverObjects respond(@SuppressWarnings("unused") final RequestHeader header, final serverObjects post, final serverSwitch env) {

        final serverObjects prop = new serverObjects();
        final Switchboard sb = (Switchboard)env;

        prop.put("showload", 0);
        prop.put("showitems", 0);
        prop.put("shownewfeeds", 0);
        prop.put("showscheduledfeeds", 0);
        prop.put("url", "");

        if (post != null && post.containsKey("removeSelectedFeedsNewList")) {
            for (final Map.Entry<String, String> entry: post.entrySet()) {
                if (entry.getValue().startsWith("mark_")) try {
                    sb.tables.delete("rss", entry.getValue().substring(5).getBytes());
                } catch (final IOException e) {
                    Log.logException(e);
                }
            }
        }

        if (post != null && post.containsKey("removeAllFeedsNewList")) try {
            final Iterator<Row> plainIterator = sb.tables.iterator("rss");
            Row row;
            String messageurl;
            final List<byte[]> d = new ArrayList<byte[]>();
            while (plainIterator.hasNext()) {
                row = plainIterator.next();
                if (row == null) continue;
                messageurl = row.get("url", "");
                if (messageurl.isEmpty()) continue;
                final byte[] api_pk = row.get("api_pk");
                final Row r = api_pk == null ? null : sb.tables.select("api", api_pk);
                if (r == null || !r.get("comment", "").matches(".*" + Pattern.quote(messageurl) + ".*")) {
                    d.add(row.getPK());
                }
            }
            for (final byte[] pk: d) {
                sb.tables.delete("rss", pk);
            }
        } catch (final IOException e) {
            Log.logException(e);
        } catch (final SpaceExceededException e) {
            Log.logException(e);
        }

        if (post != null && post.containsKey("removeSelectedFeedsScheduler")) {
            for (final Map.Entry<String, String> entry: post.entrySet()) {
                if (entry.getValue().startsWith("mark_")) try {
                    final byte[] pk = entry.getValue().substring(5).getBytes();
                    final Row rssRow = sb.tables.select("rss", pk);
                    final byte[] schedulerPK = rssRow.get("api_pk", (byte[]) null);
                    if (schedulerPK != null) sb.tables.delete("api", schedulerPK);
                    rssRow.remove("api_pk");
                    sb.tables.insert("rss", pk, rssRow);
                } catch (final IOException e) {
                    Log.logException(e);
                } catch (final SpaceExceededException e) {
                    Log.logException(e);
                }
            }
        }

        if (post != null && post.containsKey("removeAllFeedsScheduler")) try {
            final Iterator<Row> plainIterator = sb.tables.iterator("rss");
            Row row;
            String messageurl;
            final List<byte[]> d = new ArrayList<byte[]>();
            while (plainIterator.hasNext()) {
                row = plainIterator.next();
                if (row == null) continue;
                messageurl = row.get("url", "");
                if (messageurl.isEmpty()) continue;
                final byte[] api_pk = row.get("api_pk");
                final Row r = api_pk == null ? null : sb.tables.select("api", api_pk);
                if (r != null && r.get("comment", "").matches(".*" + Pattern.quote(messageurl) + ".*")) {
                    d.add(row.getPK());
                }
            }
            for (final byte[] pk: d) {
                final Row rssRow = sb.tables.select("rss", pk);
                final byte[] schedulerPK = rssRow.get("api_pk", (byte[]) null);
                if (schedulerPK != null) sb.tables.delete("api", schedulerPK);
                rssRow.remove("api_pk");
                sb.tables.insert("rss", pk, rssRow);
            }
        } catch (final IOException e) {
            Log.logException(e);
        } catch (final SpaceExceededException e) {
            Log.logException(e);
        }

        if (post != null && post.containsKey("addSelectedFeedScheduler")) {
            for (final Map.Entry<String, String> entry: post.entrySet()) {
                if (entry.getValue().startsWith("mark_")) {
                    Row row;
                    try {
                        final byte [] pk = entry.getValue().substring(5).getBytes();
                        row = sb.tables.select("rss", pk);
                    } catch (final IOException e) {
                        Log.logException(e);
                        continue;
                    } catch (final SpaceExceededException e) {
                        Log.logException(e);
                        continue;
                    }
                    DigestURI url = null;
                    try {
                        url = new DigestURI(row.get("url", ""));
                    } catch (final MalformedURLException e) {
                        Log.logWarning("Load_RSS", "malformed url '" + row.get("url", "") + "': " + e.getMessage());
                        continue;
                    }
                    // load feeds concurrently to get better responsibility in web interface
                    new RSSLoader(sb, url).start();
                }
            }
        }

        if (post == null || (post != null && (
                post.containsKey("addSelectedFeedScheduler") ||
                post.containsKey("removeSelectedFeedsNewList") ||
                post.containsKey("removeAllFeedsNewList") ||
                post.containsKey("removeSelectedFeedsScheduler") ||
                post.containsKey("removeAllFeedsScheduler")
            ))) {
            try {
                // get list of primary keys from the api table with scheduled feed loading requests
                Tables.Row row;
                String messageurl;

                // check feeds
                int newc = 0, apic = 0;
                final Iterator<Row> plainIterator = sb.tables.iterator("rss");
                while (plainIterator.hasNext()) {
                    row = plainIterator.next();
                    if (row == null) continue;
                    messageurl = row.get("url", "");
                    if (messageurl.isEmpty()) continue;
                    // get referrer
                    final DigestURI referrer = sb.getURL(row.get("referrer", "").getBytes());
                    // check if feed is registered in scheduler
                    final byte[] api_pk = row.get("api_pk");
                    final Row r = api_pk == null ? null : sb.tables.select("api", api_pk);
                    if (r != null && r.get("comment", "").matches(".*" + Pattern.quote(messageurl) + ".*")) {
                        // this is a recorded entry
                        final Date date_next_exec = r.get(WorkTables.TABLE_API_COL_DATE_NEXT_EXEC, (Date) null);
                        prop.put("showscheduledfeeds_list_" + apic + "_pk", UTF8.String(row.getPK()));
                        prop.put("showscheduledfeeds_list_" + apic + "_count", apic);
                        prop.putXML("showscheduledfeeds_list_" + apic + "_rss", messageurl);
                        prop.putXML("showscheduledfeeds_list_" + apic + "_title", row.get("title", ""));
                        prop.putXML("showscheduledfeeds_list_" + apic + "_referrer", referrer == null ? "#" : referrer.toNormalform(true, false));
                        prop.put("showscheduledfeeds_list_" + apic + "_recording", DateFormat.getDateTimeInstance().format(row.get("recording_date", new Date())));
                        prop.put("showscheduledfeeds_list_" + apic + "_lastload", DateFormat.getDateTimeInstance().format(row.get("last_load_date", new Date())));
                        prop.put("showscheduledfeeds_list_" + apic + "_nextload", date_next_exec == null ? "" : DateFormat.getDateTimeInstance().format(date_next_exec));
                        prop.put("showscheduledfeeds_list_" + apic + "_lastcount", row.get("last_load_count", 0));
                        prop.put("showscheduledfeeds_list_" + apic + "_allcount", row.get("all_load_count", 0));
                        prop.put("showscheduledfeeds_list_" + apic + "_updperday", row.get("avg_upd_per_day", 0));
                        apic++;
                    } else {
                        // this is a new entry
                        prop.put("shownewfeeds_list_" + newc + "_pk", UTF8.String(row.getPK()));
                        prop.put("shownewfeeds_list_" + newc + "_count", newc);
                        prop.putXML("shownewfeeds_list_" + newc + "_rss", messageurl);
                        prop.putXML("shownewfeeds_list_" + newc + "_title", row.get("title", ""));
                        prop.putXML("shownewfeeds_list_" + newc + "_referrer", referrer == null ? "" : referrer.toNormalform(true, false));
                        prop.put("shownewfeeds_list_" + newc + "_recording", DateFormat.getDateTimeInstance().format(row.get("recording_date", new Date())));
                        newc++;
                    }
                    if (apic > 1000 || newc > 1000) break;
                }
                prop.put("showscheduledfeeds_list" , apic);
                prop.put("showscheduledfeeds_num", apic);
                prop.put("showscheduledfeeds", apic > 0 ? apic : 0);
                prop.put("shownewfeeds_list" , newc);
                prop.put("shownewfeeds_num", newc);
                prop.put("shownewfeeds", newc > 0 ? 1 : 0);
            } catch (final IOException e) {
                Log.logException(e);
            } catch (final SpaceExceededException e) {
                Log.logException(e);
            }

            return prop;
        }

        prop.put("url", post.get("url", ""));

        int repeat_time = post.getInt("repeat_time", -1);
        final String repeat_unit = post.get("repeat_unit", "seldays"); // selminutes, selhours, seldays
        if (!"on".equals(post.get("repeat", "off")) && repeat_time > 0) repeat_time = -1;

        boolean record_api = false;

        DigestURI url = null;
        try {
            url = post.containsKey("url") ? new DigestURI(post.get("url", "")) : null;
        } catch (final MalformedURLException e) {
            Log.logWarning("Load_RSS_p", "url not well-formed: '" + post.get("url", "") + "'");
        }

        // if we have an url then try to load the rss
        RSSReader rss = null;
        if (url != null) try {
            prop.put("url", url.toNormalform(true, false));
            final Response response = sb.loader.load(sb.loader.request(url, true, false), CacheStrategy.NOCACHE, Integer.MAX_VALUE, BlacklistType.CRAWLER, CrawlQueues.queuedMinLoadDelay);
            final byte[] resource = response == null ? null : response.getContent();
            rss = resource == null ? null : RSSReader.parse(RSSFeed.DEFAULT_MAXSIZE, resource);
        } catch (final IOException e) {
            Log.logException(e);
        }

        // index all selected items: description only
        if (rss != null && post.containsKey("indexSelectedItemContent")) {
            final RSSFeed feed = rss.getFeed();
            loop: for (final Map.Entry<String, String> entry: post.entrySet()) {
                if (entry.getValue().startsWith("mark_")) try {
                    final RSSMessage message = feed.getMessage(entry.getValue().substring(5));
                    final DigestURI messageurl = new DigestURI(message.getLink());
                    if (RSSLoader.indexTriggered.containsKey(messageurl.hash())) continue loop;
                    if (sb.urlExists(messageurl.hash()) != null) continue loop;
                    sb.addToIndex(messageurl, null, null);
                    RSSLoader.indexTriggered.insertIfAbsent(messageurl.hash(), new Date());
                } catch (final IOException e) {
                    Log.logException(e);
                } catch (final Failure e) {
                    Log.logException(e);
                }
            }
        }

        if (rss != null && post.containsKey("indexAllItemContent")) {
            record_api = true;
            final RSSFeed feed = rss.getFeed();
            RSSLoader.indexAllRssFeed(sb, url, feed);
        }

        if (record_api && rss != null && rss.getFeed() != null && rss.getFeed().getChannel() != null) {
            // record API action
            RSSLoader.recordAPI(sb, post.get(WorkTables.TABLE_API_COL_APICALL_PK, null), url, rss.getFeed(), repeat_time, repeat_unit);
        }

        // show items from rss
        if (rss != null) {
            prop.put("showitems", 1);
            final RSSFeed feed = rss.getFeed();
            final RSSMessage channel = feed.getChannel();
            prop.putHTML("showitems_title", channel == null ? "" : channel.getTitle());
            String author = channel == null ? "" : channel.getAuthor();
            if (author == null || author.isEmpty()) author = channel == null ? "" : channel.getCopyright();
            Date pubDate = channel == null ? null : channel.getPubDate();
            prop.putHTML("showitems_author", author == null ? "" : author);
            prop.putHTML("showitems_description", channel == null ? "" : channel.getDescription());
            prop.putHTML("showitems_language", channel == null ? "" : channel.getLanguage());
            prop.putHTML("showitems_date", (pubDate == null) ? "" : DateFormat.getDateTimeInstance().format(pubDate));
            prop.putHTML("showitems_ttl", channel == null ? "" : channel.getTTL());
            prop.putHTML("showitems_docs", channel == null ? "" : channel.getDocs());

            int i = 0;
            for (final Hit item: feed) {
                try {
                    final DigestURI messageurl = new DigestURI(item.getLink());
                    author = item.getAuthor();
                    if (author == null) author = item.getCopyright();
                    pubDate = item.getPubDate();
                    prop.put("showitems_item_" + i + "_state", sb.urlExists(messageurl.hash()) != null ? 2 : RSSLoader.indexTriggered.containsKey(messageurl.hash()) ? 1 : 0);
                    prop.put("showitems_item_" + i + "_state_count", i);
                    prop.putHTML("showitems_item_" + i + "_state_guid", item.getGuid());
                    prop.putHTML("showitems_item_" + i + "_author", author == null ? "" : author);
                    prop.putHTML("showitems_item_" + i + "_title", item.getTitle());
                    prop.putHTML("showitems_item_" + i + "_link", messageurl.toNormalform(false, false));
                    prop.putHTML("showitems_item_" + i + "_description", item.getDescription());
                    prop.putHTML("showitems_item_" + i + "_language", item.getLanguage());
                    prop.putHTML("showitems_item_" + i + "_date", (pubDate == null) ? "" : DateFormat.getDateTimeInstance().format(pubDate));
                    i++;
                } catch (final MalformedURLException e) {
                    Log.logException(e);
                    continue;
                }
            }
            prop.put("showitems_item", i);
            prop.put("showitems_num", i);
            prop.putHTML("showitems_rss", url.toNormalform(true, false));
            if (i > 0) {
                prop.put("showload", 1);
                prop.put("showload_rss", url.toNormalform(true, false));
            }
        }

        return prop;
    }

}
