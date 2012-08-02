import net.yacy.cora.protocol.RequestHeader;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public final class AugmentedBrowsing_p {

	public static serverObjects respond(@SuppressWarnings("unused") final RequestHeader header,
			final serverObjects post, final serverSwitch env) {
		// return variable that accumulates replacements
		final serverObjects prop = new serverObjects();

		if (post != null) {

			if (post.containsKey("urlproxySettings")) {

				env.setConfig("proxyURL.access", post.get("urlproxyfilter"));

				env.setConfig("proxyURL.rewriteURLs",
						post.get("urlproxydomains"));

				env.setConfig("proxyURL",
						"on".equals(post.get("urlproxyenabled")) ? true : false);

				env.setConfig("proxyURL.useforresults",
						"on".equals(post.get("urlproxyuseforresults")) ? true : false);

			}

			if (post.containsKey("augmentationSettings")) {

				env.setConfig("proxyAugmentation", "on".equals(post
						.get("augmentationenabled")) ? true : false);

			}

		}

		prop.putHTML("urlproxyfilter",
				env.getConfig("proxyURL.access", "127.0.0.1,0:0:0:0:0:0:0:1"));

		prop.putHTML("urlproxydomains",
				env.getConfig("proxyURL.rewriteURLs", "domainlist"));

		prop.put("urlproxyenabled_checked",
				env.getConfigBool("proxyURL", false) ? "1" : "0");

		prop.put("urlproxyuseforresults_checked",
				env.getConfigBool("proxyURL.useforresults", false) ? "1" : "0");

		prop.put("augmentationenabled_checked",
				env.getConfigBool("proxyAugmentation", false) ? "1" : "0");

		// return rewrite properties
		return prop;
	}

}
