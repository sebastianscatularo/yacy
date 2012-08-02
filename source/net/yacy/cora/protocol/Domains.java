/**
 *  Domains
 *  Copyright 2007 by Michael Peter Christen, mc@yacy.net, Frankfurt a. M., Germany
 *  First released 23.7.2007 at http://yacy.net
 *
 *  $LastChangedDate$
 *  $LastChangedRevision$
 *  $LastChangedBy$
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

package net.yacy.cora.protocol;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.yacy.cora.plugin.ClassProvider;
import net.yacy.cora.storage.ARC;
import net.yacy.cora.storage.ConcurrentARC;
import net.yacy.cora.storage.KeyList;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.util.MemoryControl;

import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

public class Domains {

    public  static final String LOCALHOST = "127.0.0.1"; // replace with IPv6 0:0:0:0:0:0:0:1 ?
    private static       String LOCALHOST_NAME = LOCALHOST; // this will be replaced with the actual name of the local host

    private static Class<?> InetAddressLocatorClass;
    private static Method InetAddressLocatorGetLocaleInetAddressMethod;

    static {
        // using http://javainetlocator.sourceforge.net/ if library is present
        // we use this class using reflection to be able to remove it because that class is old and without maintenancy
        InetAddressLocatorClass = ClassProvider.load("net.sf.javainetlocator.InetAddressLocator", new File("lib/InetAddressLocator.jar"));
        InetAddressLocatorGetLocaleInetAddressMethod = ClassProvider.getStaticMethod(InetAddressLocatorClass, "getLocale", new Class[]{InetAddress.class});
    }

    private static final String PRESENT = "";
    private static final String LOCAL_PATTERNS = "10\\..*,127\\..*,172\\.(1[6-9]|2[0-9]|3[0-1])\\..*,169\\.254\\..*,192\\.168\\..*,localhost";
    private static final int MAX_NAME_CACHE_HIT_SIZE = 100000;
    private static final int MAX_NAME_CACHE_MISS_SIZE = 100000;
    private static final int CONCURRENCY_LEVEL = Runtime.getRuntime().availableProcessors() + 1;

    // a dns cache
    private static final ARC<String, InetAddress> NAME_CACHE_HIT = new ConcurrentARC<String, InetAddress>(MAX_NAME_CACHE_HIT_SIZE, CONCURRENCY_LEVEL);
    private static final ARC<String, String> NAME_CACHE_MISS = new ConcurrentARC<String, String>(MAX_NAME_CACHE_MISS_SIZE, CONCURRENCY_LEVEL);
    private static final ConcurrentHashMap<String, Object> LOOKUP_SYNC = new ConcurrentHashMap<String, Object>(100, 0.75f, Runtime.getRuntime().availableProcessors() * 2);
    private static       List<Pattern> nameCacheNoCachingPatterns = Collections.synchronizedList(new LinkedList<Pattern>());
    private static final List<Pattern> INTRANET_PATTERNS = makePatterns(LOCAL_PATTERNS);
    public static long cacheHit_Hit = 0, cacheHit_Miss = 0, cacheHit_Insert = 0; // for statistics only; do not write
    public static long cacheMiss_Hit = 0, cacheMiss_Miss = 0, cacheMiss_Insert = 0; // for statistics only; do not write

    /**
     * ! ! !   A T T E N T I O N   A T T E N T I O N   A T T E N T I O N   ! ! !
     *
     * Do not move a TLD to another group (if you do not exactly know what you
     * are doing)! Because it will change the hash of the url!
     */
    private static final String[] TLD_NorthAmericaOceania = {
        // primary english-speaking countries
        // english-speaking countries from central america are also included
        // includes also dutch and french colonies in the caribbean sea
        // and US/English/Australian military bases in asia
         "EDU=US Educational",
         "GOV=US Government",
         "MIL=US Military",
         "NET=Network",
         "ORG=Non-Profit Organization",
         "AN=Netherlands Antilles",
         "AS=American Samoa",
         "AG=Antigua and Barbuda",
         "AI=Anguilla",
         "AU=Australia",
         "BB=Barbados",
         "BZ=Belize",
         "BM=Bermuda",
         "BS=Bahamas",
         "CA=Canada",
         "CC=Cocos (Keeling) Islands",
         "CK=Cook Islands",
         "CX=Christmas Island", // located in the Indian Ocean, but belongs to Australia
         "DM=Dominica",
         "FM=Micronesia",
         "FJ=Fiji",
         "GD=Grenada",
         "GP=Guadeloupe",
         "GS=South Georgia and the South Sandwich Islands", // south of south america, but administrated by british, has only a scientific base
         "GU=Guam", // strategic US basis close to Japan
         "HM=Heard and McDonald Islands", // uninhabited, sub-Antarctic island, owned by Australia
         "HT=Haiti",
         "IO=British Indian Ocean Territory", // UK-US naval support facility in the Indian Ocean
         "KI=Kiribati", // 33 coral atolls in the pacific, formerly owned by UK
         "KN=Saint Kitts and Nevis", // islands in the carribean see
         "KY=Cayman Islands",
         "LC=Saint Lucia",
         "MF=Saint Martin (French part)",
         "MH=Marshall Islands", // formerly US atomic bomb test site, now a key installation in the US missile defense network
         "MP=Northern Mariana Islands", // US strategic location in the western Pacific Ocean
         "NC=New Caledonia",
         "NF=Norfolk Island",
         "NR=Nauru", // independent UN island
         "NU=Niue", // one of world's largest coral islands
         "NZ=New Zealand (Aotearoa)",
         "PG=Papua New Guinea",
         "PN=Pitcairn", // overseas territory of the UK
         "PR=Puerto Rico", // territory of the US with commonwealth status
         "PW=Palau", // was once governed by Micronesia
         "SB=Solomon Islands",
         "TC=Turks and Caicos Islands", // overseas territory of the UK
         "TK=Tokelau", // group of three atolls in the South Pacific Ocean, british protectorat
         "TO=Tonga",
         "TT=Trinidad and Tobago",
         "TV=Tuvalu", //  nine coral atolls in the South Pacific Ocean; in 2000, Tuvalu leased its TLD ".tv" for $50 million over a 12-year period
         "UM=US Minor Outlying Islands", // nine insular United States possessions in the Pacific Ocean and the Caribbean Sea
         "US=United States",
         "VC=Saint Vincent and the Grenadines",
         "VG=Virgin Islands (British)",
         "VI=Virgin Islands (U.S.)",
         "VU=Vanuatu",
         "WF=Wallis and Futuna Islands",
         "WS=Samoa"
     };
     private static final String[] TLD_MiddleSouthAmerica = {
         // primary spanish and portugese-speaking
         "AR=Argentina",
         "AW=Aruba",
         "BR=Brazil",
         "BO=Bolivia",
         "CL=Chile",
         "CO=Colombia",
         "CR=Costa Rica",
         "CU=Cuba",
         "DO=Dominican Republic",
         "EC=Ecuador",
         "FK=Falkland Islands (Malvinas)",
         "GF=French Guiana",
         "GT=Guatemala",
         "GY=Guyana",
         "HN=Honduras",
         "JM=Jamaica",
         "MX=Mexico",
         "NI=Nicaragua",
         "PA=Panama",
         "PE=Peru",
         "PY=Paraguay",
         "SR=Suriname",
         "SV=El Salvador",
         "UY=Uruguay",
         "VE=Venezuela"
     };
     private static final String[] TLD_EuropeRussia = {
        // includes also countries that are mainly french- dutch- speaking
        // and culturally close to europe
         "AD=Andorra",
         "AL=Albania",
         "AQ=Antarctica",
         "AT=Austria",
         "AX=Aaland Islands",
         "BA=Bosnia and Herzegovina",
         "BE=Belgium",
         "BG=Bulgaria",
         "BV=Bouvet Island", // this island is uninhabited and covered by ice, south of africa but governed by Norway
         "BY=Belarus",
         "CAT=Catalan",
         "CH=Switzerland",
         "CS=Czechoslovakia (former)",
         "CZ=Czech Republic",
         "CY=Cyprus",
         "DE=Germany",
         "DK=Denmark",
         "ES=Spain",
         "EE=Estonia",
         "EU=Europe",
         "FI=Finland",
         "FO=Faroe Islands", // Viking Settlers
         "FR=France",
         "FX=France, Metropolitan",
         "GB=Great Britain (UK)",
         "GG=Guernsey",
         "GI=Gibraltar",
         "GL=Greenland",
         "GR=Greece",
         "HR=Croatia (Hrvatska)",
         "HU=Hungary",
         "IE=Ireland",
         "IM=Isle of Man",
         "IS=Iceland",
         "IT=Italy",
         "JE=Jersey",
         "LI=Liechtenstein",
         "LT=Lithuania",
         "LU=Luxembourg",
         "LV=Latvia",
         "MC=Monaco",
         "MD=Moldova",
         "ME=Montenegro",
         "MK=Macedonia",
         "MN=Mongolia",
         "MS=Montserrat", // British island in the Caribbean Sea, almost not populated because of strong vulcanic activity
         "MT=Malta",
         "MQ=Martinique", // island in the eastern Caribbean Sea, overseas department of France
         "NATO=Nato field",
         "NL=Netherlands",
         "NO=Norway",
         "PF=French Polynesia", // French annexed Polynesian island in the South Pacific, French atomic bomb test site
         "PL=Poland",
         "PM=St. Pierre and Miquelon", // french-administrated colony close to canada, belongs to France
         "PT=Portugal",
         "RO=Romania",
         "RS=Serbia",
         "RU=Russia",
         "SE=Sweden",
         "SI=Slovenia",
         "SJ=Svalbard and Jan Mayen Islands", // part of Norway
         "SM=San Marino",
         "SK=Slovak Republic",
         "SU=USSR (former)",
         "TF=French Southern Territories", // islands in the arctic see, no inhabitants
         "UK=United Kingdom",
         "UA=Ukraine",
         "VA=Vatican City State (Holy See)",
         "YU=Yugoslavia"
     };
     private static final String[] TLD_MiddleEastWestAsia = {
         // states that are influenced by islamic culture and arabic language
         // includes also eurasia states and those that had been part of the former USSR and close to southwest asia
         "AE=United Arab Emirates",
         "AF=Afghanistan",
         "AM=Armenia",
         "AZ=Azerbaijan",
         "BH=Bahrain",
         "GE=Georgia",
         "IL=Israel",
         "IQ=Iraq",
         "IR=Iran",
         "JO=Jordan",
         "KG=Kyrgyzstan",
         "KZ=Kazakhstan",
         "KW=Kuwait",
         "LB=Lebanon",
         "PS=Palestinian Territory",
         "OM=Oman",
         "QA=Qatar",
         "SA=Saudi Arabia",
         "SY=Syria",
         "TJ=Tajikistan",
         "TM=Turkmenistan",
         "PK=Pakistan",
         "TR=Turkey",
         "UZ=Uzbekistan",
         "YE=Yemen"
     };
     private static final String[] TLD_SouthEastAsia = {
         "ASIA=The Pan-Asia and Asia Pacific community",
         "BD=Bangladesh",
         "BN=Brunei Darussalam",
         "BT=Bhutan",
         "CN=China",
         "HK=Hong Kong",
         "ID=Indonesia",
         "IN=India",
         "LA=Laos",
         "NP=Nepal",
         "JP=Japan",
         "KH=Cambodia",
         "KP=Korea (North)",
         "KR=Korea (South)",
         "LK=Sri Lanka",
         "MY=Malaysia",
         "MM=Myanmar", // formerly known as Burma
         "MO=Macau", // Portuguese settlement, part of China, but has some autonomy
         "MV=Maldives", // group of atolls in the Indian Ocean
         "PH=Philippines",
         "SG=Singapore",
         "TP=East Timor",
         "TH=Thailand",
         "TL=Timor-Leste",
         "TW=Taiwan",
         "VN=Viet Nam"
     };
     private static final String[] TLD_Africa = {
         "AC=Ascension Island",
         "AO=Angola",
         "BF=Burkina Faso",
         "BI=Burundi",
         "BJ=Benin",
         "BW=Botswana",
         "CD=Democratic Republic of the Congo",
         "CF=Central African Republic",
         "CG=Congo",
         "CI=Cote D'Ivoire (Ivory Coast)",
         "CM=Cameroon",
         "CV=Cape Verde",
         "DJ=Djibouti",
         "DZ=Algeria",
         "EG=Egypt",
         "EH=Western Sahara",
         "ER=Eritrea",
         "ET=Ethiopia",
         "GA=Gabon",
         "GH=Ghana",
         "GM=Gambia",
         "GN=Guinea",
         "GQ=Equatorial Guinea",
         "GW=Guinea-Bissau",
         "KE=Kenya",
         "KM=Comoros",
         "LR=Liberia",
         "LS=Lesotho",
         "LY=Libya",
         "MA=Morocco",
         "MG=Madagascar",
         "ML=Mali",
         "MR=Mauritania",
         "MU=Mauritius",
         "MW=Malawi",
         "MZ=Mozambique",
         "NA=Namibia",
         "NE=Niger",
         "NG=Nigeria",
         "RE=Reunion",
         "RW=Rwanda",
         "SC=Seychelles",
         "SD=Sudan",
         "SH=St. Helena",
         "SL=Sierra Leone",
         "SN=Senegal",
         "SO=Somalia",
         "ST=Sao Tome and Principe",
         "SZ=Swaziland",
         "TD=Chad",
         "TG=Togo",
         "TN=Tunisia",
         "TZ=Tanzania",
         "UG=Uganda",
         "ZA=South Africa",
         "ZM=Zambia",
         "ZR=Zaire",
         "ZW=Zimbabwe",
         "YT=Mayotte"
     };
     private static final String[] TLD_Generic = {
         "COM=US Commercial",
         "AERO=The air-transport industry",
         "ARPA=operationally-critical infrastructural identifier spaces",
         "BIZ=Business",
         "COOP=cooperative associations",
         "INFO=",
         "JOBS=human resource managers",
         "MOBI=mobile products and services",
         "MUSEUM=Museums",
         "NAME=Individuals",
         "PRO=Credentialed professionals",
         "TEL=Published contact data",
         "TRAVEL=The travel industry",
         "INT=International",
         // domains from the OpenNIC project, http://www.opennicproject.org, see also http://wiki.opennic.glue/OpenNICNamespaces
         "GLUE=OpenNIC Internal Architectural use",
         "BBS=OpenNIC Bulletin Board System servers",
         "FREE=OpenNIC NAMESPACE, CERT AUTH",
         "FUR=OpenNIC Furries, Furry Fandom and other Anthropormorphic interest",
         "GEEK=OpenNIC Geek-oriented sites",
         "INDY=OpenNIC independent media and arts",
         "NULL=OpenNIC the DNS version of Usenet's alt. hierarchy",
         "OSS=OpenNIC reserved exclusively for Open Source Software projects",
         "PARODY=OpenNIC non-commercial parody work",
         "DNY=OpenNIC",
         "ING=OpenNIC",
         "GOPHER=OpenNIC",
         "MICRO=OpenNIC"
     };

    private static Map<String, Integer> TLDID = new ConcurrentHashMap<String, Integer>(32);
    //private static HashMap<String, String> TLDName = new HashMap<String, String>();

    private static void insertTLDProps(final String[] TLDList, final int id) {
        int p;
        String tld;
        //String name;
        final Integer ID = Integer.valueOf(id);
        for (final String TLDelement : TLDList) {
            p = TLDelement.indexOf('=');
            if (p > 0) {
                tld = TLDelement.substring(0, p).toLowerCase();
                //name = TLDList[i].substring(p + 1);
                TLDID.put(tld, ID);
                //TLDName.put(tld, name);
            }
        }
    }

    // TLD separation, partly separated into language groups
    // https://www.cia.gov/cia/publications/factbook/index.html
    // http://en.wikipedia.org/wiki/List_of_countries_by_continent
    public static final int TLD_EuropeRussia_ID        = 0; // European languages but no english
    public static final int TLD_MiddleSouthAmerica_ID  = 1; // mainly spanish-speaking countries
    public static final int TLD_SouthEastAsia_ID       = 2; // asia
    public static final int TLD_MiddleEastWestAsia_ID  = 3; // middle east
    public static final int TLD_NorthAmericaOceania_ID = 4; // english-speaking countries
    public static final int TLD_Africa_ID              = 5; // africa
    public static final int TLD_Generic_ID             = 6; // anything else, also raw ip numbers
    public static final int TLD_Local_ID               = 7; // a local address

    static {
        // assign TLD-ids and names
        insertTLDProps(TLD_EuropeRussia,        TLD_EuropeRussia_ID);
        insertTLDProps(TLD_MiddleSouthAmerica,  TLD_MiddleSouthAmerica_ID);
        insertTLDProps(TLD_SouthEastAsia,       TLD_SouthEastAsia_ID);
        insertTLDProps(TLD_MiddleEastWestAsia,  TLD_MiddleEastWestAsia_ID);
        insertTLDProps(TLD_NorthAmericaOceania, TLD_NorthAmericaOceania_ID);
        insertTLDProps(TLD_Africa,              TLD_Africa_ID);
        insertTLDProps(TLD_Generic,             TLD_Generic_ID);
        // the id=7 is used to flag local addresses
    }

    private static KeyList globalHosts = null;
    private static boolean noLocalCheck = false;

    public static void init(final File globalHostsnameCache) {
        if (globalHostsnameCache == null) {
            globalHosts = null;
        } else try {
            globalHosts = new KeyList(globalHostsnameCache);
            Log.logInfo("Domains", "loaded globalHosts cache of hostnames, size = " + globalHosts.size());
        } catch (final IOException e) {
            globalHosts = null;
        }
    }

    /**
     * the isLocal check can be switched off to gain a better crawling speed.
     * however, if the check is switched off, then ALL urls are considered as local
     * this will create url-hashes for global domains which do not fit in environments
     * where the isLocal switch is not de-activated. Please handle this method with great care
     * Bad usage will make peers inoperable.
     * @param v
     */
    public static void setNoLocalCheck(final boolean v) {
        noLocalCheck = v;
    }

    public static synchronized void close() {
        if (globalHosts != null) try {globalHosts.close();} catch (final IOException e) {Log.logException(e);}
    }

    /**
    * Does an DNS-Check to resolve a hostname to an IP.
    *
    * @param host Hostname of the host in demand.
    * @return String with the ip. null, if the host could not be resolved.
    */
    public static InetAddress dnsResolveFromCache(String host) throws UnknownHostException {
        if ((host == null) || host.isEmpty()) return null;
        host = host.toLowerCase().trim();

        // try to simply parse the address
        InetAddress ip = parseInetAddress(host);
        if (ip != null) return ip;

        // trying to resolve host by doing a name cache lookup
        ip = NAME_CACHE_HIT.get(host);
        if (ip != null) {
            cacheHit_Hit++;
            return ip;
        }
        cacheHit_Miss++;

        if (NAME_CACHE_MISS.containsKey(host)) {
            cacheMiss_Hit++;
            return null;
        }
        cacheMiss_Miss++;
        throw new UnknownHostException("host not in cache");
    }

    public static void setNoCachingPatterns(final String patternList) throws PatternSyntaxException {
        nameCacheNoCachingPatterns = makePatterns(patternList);
    }

    public static List<Pattern> makePatterns(final String patternList) throws PatternSyntaxException {
    	final String[] entries = (patternList != null) ? patternList.split(",") : new String[0];
    	final List<Pattern> patterns = new ArrayList<Pattern>(entries.length);
    	for (final String entry : entries) {
            patterns.add(Pattern.compile(entry.trim()));
        }
    	return patterns;
    }

    public static boolean matchesList(final String obj, final List<Pattern> patterns) {
        for (final Pattern nextPattern: patterns) {
            if (nextPattern.matcher(obj).matches()) return true;
        }
        return false;
    }

    public static String getHostName(final InetAddress i) {
        final Collection<String> hosts = NAME_CACHE_HIT.getKeys(i);
        if (!hosts.isEmpty()) return hosts.iterator().next();
        final String host = i.getHostName();
        NAME_CACHE_HIT.insertIfAbsent(host, i);
        cacheHit_Insert++;
        return host;
        /*
        // call i.getHostName() using concurrency to interrupt execution in case of a time-out
        try {
            //TimeoutRequest.getHostName(i, 1000);
        } catch (ExecutionException e) {
            return i.getHostAddress();
        }
        */
    }

    /**
     * in case that the host name was resolved using a time-out request
     * it can be nice to push that information to the name cache
     * @param i the inet address
     * @param host the known host name
     */
    public static void setHostName(final InetAddress i, final String host) {
        NAME_CACHE_HIT.insertIfAbsent(host, i);
        cacheHit_Insert++;
    }

    final private static TimeLimiter timeLimiter = new SimpleTimeLimiter(Executors.newFixedThreadPool(20));

    /**
     * resolve a host address using a local DNS cache and a DNS lookup if necessary
     * @param host
     * @return the hosts InetAddress or null if the address cannot be resolved
     */
    public static InetAddress dnsResolve(final String host0) {
        if (host0 == null || host0.isEmpty()) return null;
        final String host = host0.toLowerCase().trim();
        // try to simply parse the address
        InetAddress ip = parseInetAddress(host);
        if (ip != null) return ip;

        /*
        if (MemoryControl.shortStatus()) {
            NAME_CACHE_HIT.clear();
            NAME_CACHE_MISS.clear();
        }
        */

        // try to resolve host by doing a name cache lookup
        ip = NAME_CACHE_HIT.get(host);
        if (ip != null) {
            //System.out.println("DNSLOOKUP-CACHE-HIT(CONC) " + host);
            cacheHit_Hit++;
            return ip;
        }
        cacheHit_Miss++;
        if (NAME_CACHE_MISS.containsKey(host)) {
            //System.out.println("DNSLOOKUP-CACHE-MISS(CONC) " + host);
            cacheMiss_Hit++;
            return null;
        }
        cacheMiss_Miss++;

        // call dnsResolveNetBased(host) using concurrency to interrupt execution in case of a time-out
        final Object sync_obj_new = new Object();
        Object sync_obj = LOOKUP_SYNC.putIfAbsent(host, sync_obj_new);
        if (sync_obj == null) sync_obj = sync_obj_new;
        synchronized (sync_obj) {
            // now look again if the host is in the cache where it may be meanwhile because of the synchronization

            ip = NAME_CACHE_HIT.get(host);
            if (ip != null) {
                //System.out.println("DNSLOOKUP-CACHE-HIT(SYNC) " + host);
                LOOKUP_SYNC.remove(host);
                cacheHit_Hit++;
                return ip;
            }
            cacheHit_Miss++;
            if (NAME_CACHE_MISS.containsKey(host)) {
                //System.out.println("DNSLOOKUP-CACHE-MISS(SYNC) " + host);
                LOOKUP_SYNC.remove(host);
                cacheMiss_Hit++;
                return null;
            }
            cacheMiss_Miss++;

            // do the dns lookup on the dns server
            //if (!matchesList(host, nameCacheNoCachingPatterns)) System.out.println("DNSLOOKUP " + host);
            try {
                //final long t = System.currentTimeMillis();
                Thread.currentThread().setName("Domains: DNS resolve of '" + host + "'"); // thread dump show which host is resolved
                if (InetAddresses.isInetAddress(host)) {
                    try {
                        ip = InetAddresses.forString(host);
                        Log.logInfo("Domains", "using guava for host resolution:"  + host);
                    } catch (IllegalArgumentException e) {
                        ip = null;
                    }
                }
                if (ip == null) {
                    ip = timeLimiter.callWithTimeout(new Callable<InetAddress>() {
                        @Override
                        public InetAddress call() throws Exception {
                            return InetAddress.getByName(host);
                        }
                    }, 1000L, TimeUnit.MILLISECONDS, false);
                    //ip = TimeoutRequest.getByName(host, 1000); // this makes the DNS request to backbone
                }
                //.out.println("DNSLOOKUP-*LOOKUP* " + host + ", time = " + (System.currentTimeMillis() - t) + "ms");
            } catch (final Throwable e) {
                // add new entries
                NAME_CACHE_MISS.insertIfAbsent(host, PRESENT);
                cacheMiss_Insert++;
                LOOKUP_SYNC.remove(host);
                return null;
            }

            if (ip == null) {
                // add new entries
                NAME_CACHE_MISS.insertIfAbsent(host, PRESENT);
                cacheMiss_Insert++;
                LOOKUP_SYNC.remove(host);
                return null;
            }

            if (!ip.isLoopbackAddress() && !matchesList(host, nameCacheNoCachingPatterns)) {
                // add new ip cache entries
                NAME_CACHE_HIT.insertIfAbsent(host, ip);
                cacheHit_Insert++;

                // add also the isLocal host name caches
                final boolean localp = ip.isAnyLocalAddress() || ip.isLinkLocalAddress() || ip.isSiteLocalAddress();
                if (localp) {
                    localHostNames.add(host);
                } else {
                    if (globalHosts != null) try {
                        if (MemoryControl.shortStatus()) {
                            globalHosts.close();
                            globalHosts = null;
                        } else {
                            globalHosts.add(host);
                        }
                    } catch (final IOException e) {}
                }
            }
            LOOKUP_SYNC.remove(host);
            return ip;
        }
    }

    private final static Pattern dotPattern = Pattern.compile("\\.");

    public static final InetAddress parseInetAddress(String ip) {
        if (ip == null || ip.length() < 8) return null;
        ip = ip.trim();
        if (ip.charAt(0) == '[' && ip.charAt(ip.length() - 1) == ']') ip = ip.substring(1, ip.length() - 1);
        if (isLocalhost(ip)) ip = "127.0.0.1"; // normalize to IPv4 here since that is the way to calculate the InetAddress
        final String[] ips = dotPattern.split(ip);
        if (ips.length != 4) return null;
        final byte[] ipb = new byte[4];
        try {
            ipb[0] = (byte) Integer.parseInt(ips[0]);
            ipb[1] = (byte) Integer.parseInt(ips[1]);
            ipb[2] = (byte) Integer.parseInt(ips[2]);
            ipb[3] = (byte) Integer.parseInt(ips[3]);
        } catch (final NumberFormatException e) {
            return null;
        }
        try {
            return InetAddress.getByAddress(ipb);
        } catch (final UnknownHostException e) {
            return null;
        }
    }

    /**
    * Returns the number of entries in the nameCacheHit map
    *
    * @return int The number of entries in the nameCacheHit map
    */
    public static int nameCacheHitSize() {
        return NAME_CACHE_HIT.size();
    }

    public static int nameCacheMissSize() {
        return NAME_CACHE_MISS.size();
    }

    public static int nameCacheNoCachingPatternsSize() {
        return nameCacheNoCachingPatterns.size();
    }

    private static Set<InetAddress> localHostAddresses = new HashSet<InetAddress>();
    private static Set<String> localHostNames = new HashSet<String>();
    static {
        try {
            final InetAddress localHostAddress = InetAddress.getLocalHost();
            if (localHostAddress != null) localHostAddresses.add(localHostAddress);
        } catch (final UnknownHostException e) {}
        try {
            final InetAddress[] moreAddresses = InetAddress.getAllByName(LOCALHOST_NAME);
            if (moreAddresses != null) localHostAddresses.addAll(Arrays.asList(moreAddresses));
        } catch (final UnknownHostException e) {}

        // to get the local host name, a dns lookup is necessary.
        // if such a lookup blocks, it can cause that the static initiatializer does not finish fast
        // therefore we start the host name lookup as concurrent thread
        // meanwhile the host name is "127.0.0.1" which is not completely wrong
        new Thread() {
            @Override
            public void run() {
                Thread.currentThread().setName("Domains: init");
                // try to get local addresses from interfaces
                try {
                    final Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                    while (nis.hasMoreElements()) {
                        final NetworkInterface ni = nis.nextElement();
                        final Enumeration<InetAddress> addrs = ni.getInetAddresses();
                        while (addrs.hasMoreElements()) {
                            final InetAddress addr = addrs.nextElement();
                            if (addr != null) localHostAddresses.add(addr);
                        }
                    }
                } catch (final SocketException e) {
                }

                // now look up the host name
                try {
                    LOCALHOST_NAME = getHostName(InetAddress.getLocalHost());
                } catch (final UnknownHostException e) {}

                // after the host name was resolved, we try to look up more local addresses
                // using the host name:
                try {
                    final InetAddress[] moreAddresses = InetAddress.getAllByName(LOCALHOST_NAME);
                    if (moreAddresses != null) localHostAddresses.addAll(Arrays.asList(moreAddresses));
                } catch (final UnknownHostException e) {
                }

                // fill a cache of local host names
                for (final InetAddress a: localHostAddresses) {
                    final String hostname = getHostName(a);
                    if (hostname != null) {
                        localHostNames.add(hostname);
                        localHostNames.add(a.getHostAddress());
                    }
                }
            }
        }.start();
    }

    public static InetAddress myPublicLocalIP() {
        // list all addresses
        // for (int i = 0; i < localHostAddresses.length; i++) System.out.println("IP: " + localHostAddresses[i].getHostAddress()); // DEBUG
        if (localHostAddresses.isEmpty()) {
            return null;
        }
        if (localHostAddresses.size() == 1) {
            // only one network connection available
            return localHostAddresses.iterator().next();
        }
        // we have more addresses, find an address that is not local
        int b0, b1;
        for (final InetAddress a: localHostAddresses) {
            b0 = 0Xff & a.getAddress()[0];
            b1 = 0Xff & a.getAddress()[1];
            if (b0 != 10 && // class A reserved
                b0 != 127 && // loopback
                (b0 != 172 || b1 < 16 || b1 > 31) && // class B reserved
                (b0 != 192 || b1 != 168) && // class C reserved
                (a.getHostAddress().indexOf(':',0) < 0))
                return a;
        }
        // there is only a local address
        // return that one that is returned with InetAddress.getLocalHost()
        // if appropriate
        try {
            final InetAddress localHostAddress = InetAddress.getLocalHost();
            if (localHostAddress != null &&
                (0Xff & localHostAddress.getAddress()[0]) != 127 &&
                localHostAddress.getHostAddress().indexOf(':',0) < 0) return localHostAddress;
        } catch (final UnknownHostException e) {
        }
        // we filter out the loopback address 127.0.0.1 and all addresses without a name
        for (final InetAddress a: localHostAddresses) {
            if ((0Xff & a.getAddress()[0]) != 127 &&
                a.getHostAddress().indexOf(':',0) < 0 &&
                a.getHostName() != null &&
                !a.getHostName().isEmpty()) return a;
        }
        // if no address has a name, then take any other than the loopback
        for (final InetAddress a: localHostAddresses) {
            if ((0Xff & a.getAddress()[0]) != 127 &&
                a.getHostAddress().indexOf(':',0) < 0) return a;
        }
        // if all fails, give back whatever we have
        for (final InetAddress a: localHostAddresses) {
            if (a.getHostAddress().indexOf(':',0) < 0) return a;
        }
        // finally, just get any
        return localHostAddresses.iterator().next();
    }

    /**
     * generate a list of intranet InetAddresses without the loopback address 127.0.0.1
     * @return list of all intranet addresses
     */
    public static Set<InetAddress> myIntranetIPs() {
        // list all local addresses
        if (localHostAddresses.size() < 1) try {Thread.sleep(1000);} catch (final InterruptedException e) {}
        final Set<InetAddress> list = new HashSet<InetAddress>();
        if (localHostAddresses.isEmpty()) return list; // give up
        for (final InetAddress a: localHostAddresses) {
            if (((0Xff & a.getAddress()[0]) == 127) ||
                    (!matchesList(a.getHostAddress(), INTRANET_PATTERNS))) continue;
            list.add(a);
        }
        return list;
    }

    public static boolean isThisHostIP(final String hostName) {
        if ((hostName == null) || (hostName.isEmpty())) return false;

        boolean isThisHostIP = false;
        try {
            final InetAddress clientAddress = Domains.dnsResolve(hostName);
            if (clientAddress == null) return false;
            if (clientAddress.isAnyLocalAddress() || clientAddress.isLoopbackAddress()) return true;
            for (final InetAddress a: localHostAddresses) {
                if (a.equals(clientAddress)) {
                    isThisHostIP = true;
                    break;
                }
            }
        } catch (final Exception e) {}
        return isThisHostIP;
    }

    public static boolean isThisHostIP(final InetAddress clientAddress) {
        if (clientAddress == null) return false;

        boolean isThisHostIP = false;
        try {
            if (clientAddress.isAnyLocalAddress() || clientAddress.isLoopbackAddress()) return true;

            for (final InetAddress a: localHostAddresses) {
                if (a.equals(clientAddress)) {
                    isThisHostIP = true;
                    break;
                }
            }
        } catch (final Exception e) {}
        return isThisHostIP;
    }

    public static int getDomainID(final String host, final InetAddress hostaddress) {
        if (host == null || host.isEmpty()) return TLD_Local_ID;
        final int p = host.lastIndexOf('.');
        final String tld = (p > 0) ? host.substring(p + 1) : "";
        final Integer i = TLDID.get(tld);
        if (i != null) return i.intValue();
        return (isLocal(host, hostaddress)) ? TLD_Local_ID : TLD_Generic_ID;
    }

    /**
     * check if a given host is the name for a local host address
     * this method will return true if noLocalCheck is switched on. This means that
     * not only local and global addresses are then not distinguished but also that
     * global address hashes do not fit any more to previously stored address hashes since
     * local/global is marked in the hash.
     * @param host
     * @return
     */
    public static boolean isLocalhost(final String host) {
        return (noLocalCheck || // DO NOT REMOVE THIS! it is correct to return true if the check is off
                "127.0.0.1".equals(host) ||
                "localhost".equals(host) ||
                host.startsWith("0:0:0:0:0:0:0:1") || host.startsWith("[0:0:0:0:0:0:0:1]") ||
                host.startsWith("fe80:0:0:0:0:0:0:1") || host.startsWith("[fe80:0:0:0:0:0:0:1]") || // used by my mac as localhost
                host.startsWith("::1/") || host.startsWith("[::1/") ||
                "::1".equals(host) || "[::1]".equals(host)
                );
    }

    /**
     * check if the given host is a local address.
     * the hostaddress is optional and shall be given if the address is already known
     * @param host
     * @param hostaddress may be null if not known yet
     * @return true if the given host is local
     */
    public static boolean isLocal(final String host, final InetAddress hostaddress) {
        return isLocal(host, hostaddress, true);
    }

    private static boolean isLocal(final String host, InetAddress hostaddress, final boolean recursive) {

        if (noLocalCheck || // DO NOT REMOVE THIS! it is correct to return true if the check is off
            host == null ||
            host.isEmpty()) return true;

        // FIXME IPv4 only
        // check local ip addresses
        if (matchesList(host, INTRANET_PATTERNS)) return true;
        if (isLocalhost(host)) return true;

        // check if there are other local IP addresses that are not in
        // the standard IP range
        if (localHostNames.contains(host)) return true;
        if (globalHosts != null && globalHosts.contains(host)) {
            //System.out.println("ISLOCAL-GLOBALHOSTS-HIT " + host);
            return false;
        }

        // check simply if the tld in the host is a known tld
        final int p = host.lastIndexOf('.');
        final String tld = (p > 0) ? host.substring(p + 1) : "";
        final Integer i = TLDID.get(tld);
        if (i != null) return false;

        // check dns lookup: may be a local address even if the domain name looks global
        if (!recursive) return false;
        if (hostaddress == null) hostaddress = dnsResolve(host);
        return isLocal(hostaddress);
    }

    private static boolean isLocal(final InetAddress a) {
        final boolean
            localp = noLocalCheck || // DO NOT REMOVE THIS! it is correct to return true if the check is off
            a == null ||
            a.isAnyLocalAddress() ||
            a.isLinkLocalAddress() |
            a.isLoopbackAddress() ||
            a.isSiteLocalAddress() ||
            isLocal(a.getHostAddress(), a, false);
        return localp;
    }

    /**
     * find the locale for a given host. This feature is only available in full quality,
     * if the file InetAddressLocator.jar is placed in the /lib directory (as a plug-in)
     * from http://javainetlocator.sourceforge.net/
     * In case that that you know the InetAddress of the host, DO NOT call this method but the
     * other method with the InetAddress first to get better results.
     * @param host
     * @return the locale for the host
     */
    public static Locale getLocale(final String host) {
        if (host == null) return null;
        final Locale locale = getLocale(dnsResolve(host));
        if (locale != null && locale.getCountry() != null && locale.getCountry().length() > 0) return locale;

        final int p = host.lastIndexOf('.');
        if (p < 0) return null;
        String tld = host.substring(p + 1).toUpperCase();
        if (tld.length() < 2) return null;
        if (tld.length() > 2) tld = "US";
        return new Locale("en", tld);
    }

    /**
     * find the locale for a given Address
     * This uses the InetAddressLocator.jar library
     * TODO: integrate http://www.maxmind.com/app/geolitecountry
     * @param address
     * @return
     */
    public static Locale getLocale(final InetAddress address) {
        if (InetAddressLocatorGetLocaleInetAddressMethod == null) return null;
        if (address == null) return null;
        if (isLocal(address)) return null;
        try {
            return (Locale) InetAddressLocatorGetLocaleInetAddressMethod.invoke(null, new Object[]{address});
        } catch (final IllegalArgumentException e) {
            return null;
        } catch (final IllegalAccessException e) {
            return null;
        } catch (final InvocationTargetException e) {
            return null;
        }
    }

    public static void main(final String[] args) {
        /*
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    System.out.println(addr);
                }
            }
        } catch(SocketException e) {
            System.err.println(e);
        }
        */
        InetAddress a;
        a = dnsResolve("yacy.net"); System.out.println(a);
        a = dnsResolve("kaskelix.de"); System.out.println(a);
        a = dnsResolve("yacy.net"); System.out.println(a);

        try { Thread.sleep(1000);} catch (final InterruptedException e) {} // get time for class init
        System.out.println("myPublicLocalIP: " + myPublicLocalIP());
        for (final InetAddress b : myIntranetIPs()) {
            System.out.println("Intranet IP: " + b);
        }
    }
}
