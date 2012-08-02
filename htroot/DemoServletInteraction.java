import net.yacy.cora.protocol.RequestHeader;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public final class DemoServletInteraction {

	public static serverObjects respond(@SuppressWarnings("unused") final RequestHeader header,
			@SuppressWarnings("unused") final serverObjects post, @SuppressWarnings("unused") final serverSwitch env) {
		// return variable that accumulates replacements
		final serverObjects prop = new serverObjects();

		prop.put("temperature", "-10°C");

		// return rewrite properties
		return prop;
	}

}
