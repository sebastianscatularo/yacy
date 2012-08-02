// transferURL.java
// -----------------------
// part of the AnomicHTTPD caching proxy
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
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

// You must compile this file with
// javac -classpath .:../classes transferRWI.java

import java.io.IOException;
import java.text.ParseException;

import net.yacy.cora.date.GenericFormatter;
import net.yacy.cora.document.RSSMessage;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.kelondro.data.meta.URIMetadataRow;
import net.yacy.kelondro.logging.Log;
import net.yacy.peers.EventChannel;
import net.yacy.peers.Network;
import net.yacy.peers.Protocol;
import net.yacy.peers.Seed;
import net.yacy.repository.Blacklist.BlacklistType;
import net.yacy.search.Switchboard;
import de.anomic.crawler.ResultURLs;
import de.anomic.crawler.ResultURLs.EventOrigin;
import de.anomic.server.serverCore;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public final class transferURL {

    public static serverObjects respond(@SuppressWarnings("unused") final RequestHeader header, final serverObjects post, final serverSwitch env) throws InterruptedException {
        final long start = System.currentTimeMillis();
        long freshdate = 0;
        try {freshdate = GenericFormatter.SHORT_DAY_FORMATTER.parse("20061101").getTime();} catch (final ParseException e1) {}

        // return variable that accumulates replacements
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        if ((post == null) || (env == null)) return prop;
        if (!Protocol.authentifyRequest(post, env)) return prop;

        // request values
        final String iam      = post.get("iam", "");      // seed hash of requester
        final String youare   = post.get("youare", "");   // seed hash of the target peer, needed for network stability
//      final String key      = post.get("key", "");      // transmission key
        final int urlc        = post.getInt("urlc", 0);    // number of transported urls
        final boolean granted = sb.getConfigBool("allowReceiveIndex", false);
        final boolean blockBlacklist = sb.getConfigBool("indexReceiveBlockBlacklist", false);

        // response values
        String result = "";
        String doublevalues = "0";

        final Seed otherPeer = sb.peers.get(iam);
        final String otherPeerName = iam + ":" + ((otherPeer == null) ? "NULL" : (otherPeer.getName() + "/" + otherPeer.getVersion()));

        if ((youare == null) || (!youare.equals(sb.peers.mySeed().hash))) {
            Network.log.logInfo("Rejecting URLs from peer " + otherPeerName + ". Wrong target. Wanted peer=" + youare + ", iam=" + sb.peers.mySeed().hash);
            result = "wrong_target";
        } else if ((!granted) || (sb.isRobinsonMode())) {
            Network.log.logInfo("Rejecting URLs from peer " + otherPeerName + ". Not granted.");
            result = "error_not_granted";
        } else {
            int received = 0;
            int blocked = 0;
            final int sizeBefore = sb.index.urlMetadata().size();
            // read the urls from the other properties and store
            String urls;
            URIMetadataRow lEntry;
            for (int i = 0; i < urlc; i++) {
                serverCore.checkInterruption();

                // read new lurl-entry
                urls = post.get("url" + i);
                if (urls == null) {
                    if (Network.log.isFine()) Network.log.logFine("transferURL: got null URL-string from peer " + otherPeerName);
                    blocked++;
                    continue;
                }

                // parse new lurl-entry
                lEntry = URIMetadataRow.importEntry(urls);
                if (lEntry == null) {
                    Network.log.logWarning("transferURL: received invalid URL (entry null) from peer " + otherPeerName + "\n\tURL Property: " + urls);
                    blocked++;
                    continue;
                }

                // check if entry is well-formed
                if (lEntry.url() == null) {
                    Network.log.logWarning("transferURL: received invalid URL from peer " + otherPeerName + "\n\tURL Property: " + urls);
                    blocked++;
                    continue;
                }

                // check whether entry is too old
                if (lEntry.freshdate().getTime() <= freshdate) {
                    if (Network.log.isFine()) Network.log.logFine("transerURL: received too old URL from peer " + otherPeerName + ": " + lEntry.freshdate());
                    blocked++;
                    continue;
                }

                // check if the entry is blacklisted
                if ((blockBlacklist) && (Switchboard.urlBlacklist.isListed(BlacklistType.DHT, lEntry))) {
                    Network.log.logFine("transferURL: blocked blacklisted URL '" + lEntry.url().toNormalform(false, true) + "' from peer " + otherPeerName);
                    lEntry = null;
                    blocked++;
                    continue;
                }

                // check if the entry is in our network domain
                final String urlRejectReason = sb.crawlStacker.urlInAcceptedDomain(lEntry.url());
                if (urlRejectReason != null) {
                    if (Network.log.isFine()) Network.log.logFine("transferURL: blocked URL '" + lEntry.url() + "' (" + urlRejectReason + ") from peer " + otherPeerName);
                    lEntry = null;
                    blocked++;
                    continue;
                }

                // write entry to database
                if (Network.log.isFine()) Network.log.logFine("Accepting URL " + i + "/" + urlc + " from peer " + otherPeerName + ": " + lEntry.url().toNormalform(true, false));
                try {
                    sb.index.urlMetadata().store(lEntry);
                    ResultURLs.stack(lEntry, iam.getBytes(), iam.getBytes(), EventOrigin.DHT_TRANSFER);
                    if (Network.log.isFine()) Network.log.logFine("transferURL: received URL '" + lEntry.url().toNormalform(false, true) + "' from peer " + otherPeerName);
                    received++;
                } catch (final IOException e) {
                    Log.logException(e);
                }
            }

            sb.peers.mySeed().incRU(received);

            // return rewrite properties
            final int more = sb.index.urlMetadata().size() - sizeBefore;
            doublevalues = Integer.toString(received - more);
            Network.log.logInfo("Received " + received + " URLs from peer " + otherPeerName + " in " + (System.currentTimeMillis() - start) + " ms, blocked " + blocked + " URLs");
            EventChannel.channels(EventChannel.DHTRECEIVE).addMessage(new RSSMessage("Received " + received + ", blocked " + blocked + " URLs from peer " + otherPeerName, "", otherPeer.hash));
            if ((received - more) > 0) Network.log.logSevere("Received " + doublevalues + " double URLs from peer " + otherPeerName);
            result = "ok";
        }

        prop.put("double", doublevalues);
        prop.put("result", result);
        return prop;
    }
}
