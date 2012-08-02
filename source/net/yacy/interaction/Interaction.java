package net.yacy.interaction;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.yacy.cora.document.UTF8;
import net.yacy.cora.protocol.HeaderFramework;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.protocol.http.HTTPClient;
import net.yacy.cora.util.SpaceExceededException;
import net.yacy.kelondro.blob.Tables.Row;
import net.yacy.kelondro.data.meta.DigestURI;
import net.yacy.kelondro.logging.Log;
import net.yacy.peers.Seed;
import net.yacy.search.Switchboard;

import org.apache.http.entity.mime.content.ContentBody;

import de.anomic.data.UserDB;


public class Interaction {


	public static String GetLoggedOnUser (RequestHeader requestHeader) {

		UserDB.Entry entry = null;

        //String result = "anonymous";

        entry = Switchboard.getSwitchboard().userDB.proxyAuth((requestHeader.get(RequestHeader.AUTHORIZATION, "xxxxxx")));
        if(entry != null){

        }else{
            entry=Switchboard.getSwitchboard().userDB.cookieAuth(requestHeader.getHeaderCookies());

            if(entry == null){
                entry=Switchboard.getSwitchboard().userDB.ipAuth((requestHeader.get(HeaderFramework.CONNECTION_PROP_CLIENTIP, "xxxxxx")));
                if(entry != null){

                }
            }
        }

        //identified via userDB
        if(entry != null){

            return entry.getUserName();

        }else if(Switchboard.getSwitchboard().verifyAuthentication(requestHeader)){
        	return "staticadmin";
        }

        return "";
	}


	public static Set<String> GetUsers() {

		Set<String> res = new HashSet<String>();

		UserDB.Entry entry = null;

        final Iterator<UserDB.Entry> it = Switchboard.getSwitchboard().userDB.iterator(true);

        while (it.hasNext()) {
            entry = it.next();
            if (entry == null) {
                continue;
            }
            res.add (entry.getUserName());

        }

        return res;
	}

	public static String GetDomain (String url) {

		String domain = url;

		try {
			DigestURI uri = new DigestURI (url);

			domain = uri.getHost();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return domain;

	}





	public static String GetURLHash (String url) {

		String result = "";

		DigestURI uri;
		try {
			uri = new DigestURI (url);

			result = UTF8.String(uri.hash());



		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;

	}




public static String GetTableentry(String url, String type, String username) {

	final Switchboard sb = Switchboard.getSwitchboard();

	String retvalue = "";

	try {
		Iterator<Row> it = sb.tables.iterator(username+"_contribution", "url", url.getBytes());

		Log.logInfo ("TABLE", "GET "+username+" / "+url+" - "+type+" ...");

		it = sb.tables.orderBy(it, -1, "timestamp_creation").iterator();

		while (it.hasNext()) {
			Row r = it.next();

			if (r.get("type", "").equals (type)) {

				retvalue =  r.get("value", "");
			}

		}
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

	Log.logInfo ("TABLE", "GET "+username+" / "+url+" - "+type+" - "+retvalue);

	return retvalue;


}

public static String Tableentry(String url, String type, String comment, String from, String peer) {

	final Switchboard sb = Switchboard.getSwitchboard();

	if (peer == "") {
		peer = sb.peers.myName();
	}

	Boolean processlocal = false;

	Log.logInfo ("TABLE", "PUT "+from+" / "+url+" - "+type+" - "+comment);

	if (!sb.getConfig("interaction.contribution.accumulationpeer", "").equals("")) {

		if (sb.getConfig("interaction.contribution.accumulationpeer", "").equals(sb.peers.myName())) {

			// Our peer is meant to process the feedback.
			processlocal = true;

		} else {

			// Forward feedback to other peer
			Log.logInfo("INTERACTION", "Forwarding contribution to "+sb.getConfig("interaction.contribution.accumulationpeer", "")+": " + url + ": "
					+ comment);
			try {

				Seed host = sb.peers.lookupByName(sb.getConfig("interaction.contribution.accumulationpeer", ""));

				return (UTF8.String(new HTTPClient().POSTbytes(
						"http://"+host.getPublicAddress()+"/interaction/Contribution.json"
								+ "?url=" + url + "&comment=" + comment
								+ "&from=" + from + "&peer=" + peer,
						new HashMap<String, ContentBody>(), false)));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "";
			}
		}
	} else {
		// No forward defined
		processlocal = true;
	}

	if (processlocal) {

		final String date = String.valueOf(System.currentTimeMillis());

		final Map<String, byte[]> map = new HashMap<String, byte[]>();

        map.put("url", url.getBytes());
        map.put("username", from.getBytes());
        map.put("peer", peer.getBytes());
        map.put("status", "new".getBytes());
        map.put("type", type.getBytes());
        map.put("value", comment.getBytes());
        map.put("timestamp_creation", date.getBytes());

        try {
            sb.tables.insert(from+"_contribution", map);
        } catch (final IOException e) {
            Log.logException(e);
        } catch (SpaceExceededException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	return "";
}

}
