import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import net.yacy.cora.protocol.RequestHeader;
import net.yacy.kelondro.blob.Tables.Row;
import net.yacy.kelondro.logging.Log;
import net.yacy.search.Switchboard;
import de.anomic.data.UserDB;
import de.anomic.data.ymark.YMarkTables;
import de.anomic.data.ymark.YMarkTables.TABLES;
import de.anomic.data.ymark.YMarkUtil;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;


public class manage_tags {

	private static Switchboard sb = null;
	private static serverObjects prop = null;

	public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        sb = (Switchboard) env;
        prop = new serverObjects();

        String qtype;
        String query;
        String tags;
        String replace;

        final UserDB.Entry user = sb.userDB.getUser(header);
        final boolean isAdmin = (sb.verifyAuthentication(header));
        final boolean isAuthUser = user!= null && user.hasRight(UserDB.AccessRight.BOOKMARK_RIGHT);

        if(isAdmin || isAuthUser) {
        	final String bmk_user = (isAuthUser ? user.getUserName() : YMarkTables.USER_ADMIN);

            if(post != null) {
            	query = post.get("query", post.get("tags", YMarkUtil.EMPTY_STRING));
                qtype = post.get("qtype", "_tags");
                tags = YMarkUtil.cleanTagsString(post.get("tags", YMarkUtil.EMPTY_STRING));
                replace = post.get("replace", YMarkUtil.EMPTY_STRING);

            } else {
                query = ".*";
                qtype = YMarkUtil.EMPTY_STRING;
                tags = YMarkUtil.EMPTY_STRING;
                replace = YMarkUtil.EMPTY_STRING;
            }

            try {
                final String bmk_table = TABLES.BOOKMARKS.tablename(bmk_user);
                final Iterator<Row> row_iter;
                if(!query.isEmpty()) {
                    if(!qtype.isEmpty()) {
                        if(qtype.equals("_tags")) {
                        	if(query.isEmpty())
                        		query = tags;
                        	final String[] tagArray = YMarkUtil.cleanTagsString(query).split(YMarkUtil.TAGS_SEPARATOR);
                        	row_iter = sb.tables.bookmarks.getBookmarksByTag(bmk_user, tagArray);
                        } else if(qtype.equals("_folder")) {
                        	row_iter = sb.tables.bookmarks.getBookmarksByFolder(bmk_user, query);
                        } else {
                        	row_iter = sb.tables.iterator(bmk_table, qtype, Pattern.compile(query));
                        }
                    } else {
                    	row_iter = sb.tables.iterator(bmk_table, Pattern.compile(query));
                    }
                } else {
                	final String[] tagArray = YMarkUtil.cleanTagsString(tags).split(YMarkUtil.TAGS_SEPARATOR);
                	row_iter = sb.tables.bookmarks.getBookmarksByTag(bmk_user, tagArray);
                	// row_iter = sb.tables.iterator(bmk_table);
                }
                sb.tables.bookmarks.replaceTags(row_iter, bmk_user, tags, replace);
                prop.put("status", 1);
            } catch (final IOException e) {
                Log.logException(e);
            }
        } else {
        	prop.put(serverObjects.ACTION_AUTHENTICATE, YMarkTables.USER_AUTHENTICATE_MSG);
        }
        // return rewrite properties
        return prop;
	}
}
