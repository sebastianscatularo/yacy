// yacySearch.java
// -------------------------------------
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
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

package net.yacy.peers;

import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.regex.Pattern;

import net.yacy.cora.document.ASCII;
import net.yacy.kelondro.index.HandleSet;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.order.Bitfield;
import net.yacy.peers.dht.PeerSelection;
import net.yacy.repository.Blacklist;
import net.yacy.search.index.Segment;
import net.yacy.search.query.QueryParams;
import net.yacy.search.query.RWIProcess;
import net.yacy.search.query.SearchEvent;
import net.yacy.search.ranking.RankingProfile;


public class RemoteSearch extends Thread {

    private static final ThreadGroup ysThreadGroup = new ThreadGroup("yacySearchThreadGroup");

    final private String wordhashes, excludehashes, urlhashes, sitehash, authorhash, contentdom;
    final private boolean global;
    final private int partitions;
    final private Segment indexSegment;
    final private RWIProcess containerCache;
    final private SearchEvent.SecondarySearchSuperviser secondarySearchSuperviser;
    final private Blacklist blacklist;
    final private Seed targetPeer;
    private int urls;
    private final int count, maxDistance;
    private final long time;
    final private RankingProfile rankingProfile;
    final private Pattern prefer, filter, snippet;
    final private QueryParams.Modifier modifier;
    final private String language;
    final private Bitfield constraint;
    final private SeedDB peers;

    public RemoteSearch(
              final String wordhashes, final String excludehashes,
              final String urlhashes, // this is the field that is filled during a secondary search to restrict to specific urls that are to be retrieved
              final Pattern prefer,
              final Pattern filter,
              final Pattern snippet,
              final QueryParams.Modifier modifier,
              final String language,
              final String sitehash, final String authorhash, final String contentdom,
              final int count, final long time, final int maxDistance,
              final boolean global, final int partitions,
              final Seed targetPeer,
              final Segment indexSegment,
              final SeedDB peers,
              final RWIProcess containerCache,
              final SearchEvent.SecondarySearchSuperviser secondarySearchSuperviser,
              final Blacklist blacklist,
              final RankingProfile rankingProfile,
              final Bitfield constraint) {
        super(ysThreadGroup, "yacySearch_" + targetPeer.getName());
        //System.out.println("DEBUG - yacySearch thread " + this.getName() + " initialized " + ((urlhashes.length() == 0) ? "(primary)" : "(secondary)"));
        assert wordhashes.length() >= 12;
        this.wordhashes = wordhashes;
        this.excludehashes = excludehashes;
        this.urlhashes = urlhashes;
        this.prefer = prefer;
        this.filter = filter;
        this.snippet = snippet;
        this.modifier = modifier;
        this.language = language;
        this.sitehash = sitehash;
        this.authorhash = authorhash;
        this.contentdom = contentdom;
        this.global = global;
        this.partitions = partitions;
        this.indexSegment = indexSegment;
        this.peers = peers;
        this.containerCache = containerCache;
        this.secondarySearchSuperviser = secondarySearchSuperviser;
        this.blacklist = blacklist;
        this.targetPeer = targetPeer;
        this.urls = -1;
        this.count = count;
        this.time = time;
        this.maxDistance = maxDistance;
        this.rankingProfile = rankingProfile;
        this.constraint = constraint;
    }

    @Override
    public void run() {
        this.containerCache.oneFeederStarted();
        try {
            this.urls = Protocol.search(
                        this.peers.mySeed(),
                        this.wordhashes, this.excludehashes, this.urlhashes,
                        this.prefer, this.filter, this.snippet, this.modifier.getModifier(),
                        this.language, this.sitehash, this.authorhash, this.contentdom,
                        this.count, this.time, this.maxDistance, this.global, this.partitions,
                        this.targetPeer, this.indexSegment, this.containerCache, this.secondarySearchSuperviser,
                        this.blacklist, this.rankingProfile, this.constraint);
            if (this.urls >= 0) {
                // urls is an array of url hashes. this is only used for log output
                if (this.urlhashes != null && this.urlhashes.length() > 0) Network.log.logInfo("SECONDARY REMOTE SEARCH - remote peer " + this.targetPeer.hash + ":" + this.targetPeer.getName() + " contributed " + this.urls + " links for word hash " + this.wordhashes);
                this.peers.mySeed().incRI(this.urls);
                this.peers.mySeed().incRU(this.urls);
            } else {
                Network.log.logInfo("REMOTE SEARCH - no answer from remote peer " + this.targetPeer.hash + ":" + this.targetPeer.getName());
            }
        } catch (final Exception e) {
            Log.logException(e);
        } finally {
        	this.containerCache.oneFeederTerminated();
        }
    }

    public static String set2string(final HandleSet hashes) {
        final StringBuilder wh = new StringBuilder(hashes.size() * 12);
        final Iterator<byte[]> iter = hashes.iterator();
        while (iter.hasNext()) { wh.append(ASCII.String(iter.next())); }
        return wh.toString();
    }

    public int links() {
        return this.urls;
    }

    public int count() {
        return this.count;
    }

    public Seed target() {
        return this.targetPeer;
    }

    public static void primaryRemoteSearches(
            final List<RemoteSearch> searchThreads,
            final String wordhashes, final String excludehashes,
            final Pattern prefer, final Pattern filter, final Pattern snippet,
            final QueryParams.Modifier modifier,
            final String language,
            final String sitehash,
            final String authorhash,
            final String contentdom,
            final int count, final long time, final int maxDist,
            final Segment indexSegment,
            final SeedDB peers,
            final RWIProcess containerCache,
            final SearchEvent.SecondarySearchSuperviser secondarySearchSuperviser,
            final Blacklist blacklist,
            final RankingProfile rankingProfile,
            final Bitfield constraint,
            final SortedMap<byte[], String> clusterselection,
            final int burstRobinsonPercent,
            final int burstMultiwordPercent) {
        // check own peer status
        //if (wordIndex.seedDB.mySeed() == null || wordIndex.seedDB.mySeed().getPublicAddress() == null) { return null; }

        // prepare seed targets and threads
        assert language != null;
        assert wordhashes.length() >= 12 : "wordhashes = " + wordhashes;
        final Seed[] targetPeers =
            (clusterselection == null) ?
                    PeerSelection.selectSearchTargets(
                            peers,
                            QueryParams.hashes2Set(wordhashes),
                            peers.redundancy(),
                            burstRobinsonPercent,
                            burstMultiwordPercent)
                  : PeerSelection.selectClusterPeers(peers, clusterselection);
        if (targetPeers == null) return;
        final int targets = targetPeers.length;
        if (targets == 0) return;
        for (int i = 0; i < targets; i++) {
            if (targetPeers[i] == null || targetPeers[i].hash == null) continue;
            try {
                RemoteSearch rs = new RemoteSearch(
                    wordhashes, excludehashes, "", prefer, filter, snippet, modifier,
                    language, sitehash, authorhash, contentdom,
                    count, time, maxDist, true, targets, targetPeers[i],
                    indexSegment, peers, containerCache, secondarySearchSuperviser, blacklist, rankingProfile, constraint);
                rs.start();
                searchThreads.add(rs);
            } catch (final OutOfMemoryError e) {
                Log.logException(e);
                break;
            }
        }
    }

    public static RemoteSearch secondaryRemoteSearch(
            final String wordhashes, final String urlhashes,
            final long time,
            final Segment indexSegment,
            final SeedDB peers,
            final RWIProcess containerCache,
            final String targethash, final Blacklist blacklist,
            final RankingProfile rankingProfile,
            final Bitfield constraint, final SortedMap<byte[], String> clusterselection) {
    	assert wordhashes.length() >= 12 : "wordhashes = " + wordhashes;

        // check own peer status
        if (peers.mySeed() == null || peers.mySeed().getPublicAddress() == null) { return null; }
        assert urlhashes != null;
        assert urlhashes.length() > 0;

        // prepare seed targets and threads
        final Seed targetPeer = peers.getConnected(targethash);
        if (targetPeer == null || targetPeer.hash == null) return null;
        if (clusterselection != null) targetPeer.setAlternativeAddress(clusterselection.get(ASCII.getBytes(targetPeer.hash)));
        final RemoteSearch searchThread = new RemoteSearch(
                wordhashes, "", urlhashes, QueryParams.matchnothing_pattern, QueryParams.catchall_pattern, QueryParams.catchall_pattern, new QueryParams.Modifier(""), "", "", "", "all", 20, time, 9999, true, 0, targetPeer,
                indexSegment, peers, containerCache, null, blacklist, rankingProfile, constraint);
        searchThread.start();
        return searchThread;
    }

    public static int remainingWaiting(final RemoteSearch[] searchThreads) {
        if (searchThreads == null) return 0;
        int alive = 0;
        for (final RemoteSearch searchThread : searchThreads) {
            if (searchThread.isAlive()) alive++;
        }
        return alive;
    }

    public static int collectedLinks(final RemoteSearch[] searchThreads) {
        int links = 0;
        for (final RemoteSearch searchThread : searchThreads) {
            if (!(searchThread.isAlive()) && searchThread.urls > 0) {
                links += searchThread.urls;
            }
        }
        return links;
    }

    public static void interruptAlive(final RemoteSearch[] searchThreads) {
        for (final RemoteSearch searchThread : searchThreads) {
            if (searchThread.isAlive()) searchThread.interrupt();
        }
    }

}
