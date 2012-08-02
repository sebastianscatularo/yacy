// status_p
// (C) 2006 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 18.12.2006 on http://www.anomic.de
// this file was created using the an implementation from IndexCreate_p.java, published 02.12.2004
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

import net.yacy.cora.protocol.RequestHeader;
import net.yacy.kelondro.io.ByteCount;
import net.yacy.kelondro.util.MemoryControl;
import net.yacy.kelondro.workflow.WorkflowProcessor;
import net.yacy.search.Switchboard;
import net.yacy.search.SwitchboardConstants;
import net.yacy.search.index.Segment;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public class status_p {

    public static final String STATE_RUNNING = "running";
    public static final String STATE_PAUSED = "paused";

    public static serverObjects respond(@SuppressWarnings("unused") final RequestHeader header, final serverObjects post, final serverSwitch env) {
        // return variable that accumulates replacements
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        final boolean html = post != null && post.containsKey("html");
        prop.setLocalized(html);
        Segment segment = sb.index;

        prop.put("rejected", "0");
        sb.updateMySeed();
        final int cacheMaxSize = (int) sb.getConfigLong(SwitchboardConstants.WORDCACHE_MAX_COUNT, 10000);
        prop.putNum("ppm", Switchboard.currentPPM());
        prop.putNum("qpm", sb.peers.mySeed().getQPM());
        prop.putNum("wordCacheSize", segment.termIndex().getBufferSize());
        prop.putNum("wordCacheMaxSize", cacheMaxSize);

        // crawl queues
        prop.putNum("localCrawlSize", sb.getThread(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL).getJobCount());
        prop.putNum("limitCrawlSize", sb.crawlQueues.limitCrawlJobSize());
        prop.putNum("remoteCrawlSize", sb.getThread(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL).getJobCount());
        prop.putNum("noloadCrawlSize", sb.crawlQueues.noloadCrawlJobSize());
        prop.putNum("loaderSize", sb.crawlQueues.workerSize());
        prop.putNum("loaderMax", sb.getConfigLong(SwitchboardConstants.CRAWLER_THREADS_ACTIVE_MAX, 10));

		// memory usage and system attributes
        prop.putNum("freeMemory", MemoryControl.free());
        prop.putNum("totalMemory", MemoryControl.total());
        prop.putNum("maxMemory", MemoryControl.maxMemory());
        prop.putNum("processors", WorkflowProcessor.availableCPU);

		// proxy traffic
		prop.put("trafficIn", ByteCount.getGlobalCount());
		prop.put("trafficProxy", ByteCount.getAccountCount(ByteCount.PROXY));
		prop.put("trafficCrawler", ByteCount.getAccountCount(ByteCount.CRAWLER));

        // index size
        prop.putNum("urlpublictextSize", segment.urlMetadata().size());
        prop.putNum("rwipublictextSize", segment.termIndex().sizesMax());

        // loader queue
        prop.putNum("loaderSize", sb.crawlQueues.workerSize());
        prop.putNum("loaderMax", sb.getConfigLong(SwitchboardConstants.CRAWLER_THREADS_ACTIVE_MAX, 10));

        //local crawl queue
        prop.putNum("localCrawlSize", sb.getThread(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL).getJobCount());
        prop.put("localCrawlState", sb.crawlJobIsPaused(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL) ? STATE_PAUSED : STATE_RUNNING);

        //global crawl queue
        prop.putNum("limitCrawlSize", sb.crawlQueues.limitCrawlJobSize());
        prop.put("limitCrawlState", STATE_RUNNING);

        //remote crawl queue
        prop.putNum("remoteCrawlSize", sb.getThread(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL).getJobCount());
        prop.put("remoteCrawlState", sb.crawlJobIsPaused(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL) ? STATE_PAUSED : STATE_RUNNING);

        //noload crawl queue
        prop.putNum("noloadCrawlSize", sb.crawlQueues.noloadCrawlJobSize());
        prop.put("noloadCrawlState", STATE_RUNNING);

        // return rewrite properties
        return prop;
    }

}
