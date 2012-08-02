import java.io.IOException;

import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.util.SpaceExceededException;
import net.yacy.kelondro.logging.Log;
import net.yacy.search.Switchboard;
import de.anomic.data.UserDB;
import de.anomic.data.ymark.YMarkEntry;
import de.anomic.data.ymark.YMarkTables;
import de.anomic.data.ymark.YMarkUtil;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;


public class delete_ymark {

	private static Switchboard sb = null;

	public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();

        final UserDB.Entry user = sb.userDB.getUser(header);
        final boolean isAdmin = (sb.verifyAuthentication(header));
        final boolean isAuthUser = user!= null && user.hasRight(UserDB.AccessRight.BOOKMARK_RIGHT);

        if(isAdmin || isAuthUser) {
        	final String bmk_user = (isAuthUser ? user.getUserName() : YMarkTables.USER_ADMIN);
            byte[] urlHash = null;
            try {
	        	if(post.containsKey(YMarkEntry.BOOKMARKS_ID)) {
	        		urlHash = post.get(YMarkEntry.BOOKMARKS_ID).getBytes();
	        	} else if(post.containsKey(YMarkEntry.BOOKMARK.URL.key())) {
					urlHash = YMarkUtil.getBookmarkId(post.get(YMarkEntry.BOOKMARK.URL.key()));
	        	} else {
	        		prop.put("result", "0");
	        		return prop;
	        	}
	        	sb.tables.bookmarks.deleteBookmark(bmk_user, urlHash);
	        	prop.put("result", "1");
			} catch (final IOException e) {
				Log.logException(e);
			} catch (final SpaceExceededException e) {
				Log.logException(e);
			}
        } else {
        	prop.put(serverObjects.ACTION_AUTHENTICATE, YMarkTables.USER_AUTHENTICATE_MSG);
        }
        // return rewrite properties
        return prop;
	}
}
