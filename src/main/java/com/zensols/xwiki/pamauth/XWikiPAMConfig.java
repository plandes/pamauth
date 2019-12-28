package com.zensols.xwiki.pamauth;

import java.security.Provider;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

public class XWikiPAMConfig
{
    /**
     * Mapping fields separator.
     */
    public static final String DEFAULT_SEPARATOR = ",";

    /**
     * PAM properties names suffix in XWikiPreferences.
     */
    public static final String PREF_PAM_SUFFIX = "pam_";

    /**
     * PAM properties names suffix in xwiki.cfg.
     */
    public static final String CFG_PAM_SUFFIX = "xwiki.authentication.pam.";

    /**
     * Mapping fields separator.
     */
    public static final String USERMAPPING_SEP = DEFAULT_SEPARATOR;

    /**
     * Unique instance of {@link XWikiPAMConfig}.
     */
    private static XWikiPAMConfig instance;

    private final Map<String, String> memoryConfiguration;

    private ConfigurationSource configurationSource;

    private ConfigurationSource cfgConfigurationSource;

    private final Map<String, String> finalMemoryConfiguration;

    /**
     * Character user to link XWiki field name and PAM field name in user mappings property.
     */
    public static final String USERMAPPING_XWIKI_PAM_LINK = "=";

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiPAMConfig.class);

    /**
     * @param userId the complete user id given
     * @since 9.1.1
     */
    public XWikiPAMConfig(String userId)
    {
        this(userId, Utils.getComponent(ConfigurationSource.class, "wiki"));
    }

    /**
     * @param userId the complete user id given
     * @param configurationSource the Configuration source to use to find PAM parameters first (if not found in this
     *            source then the parameter will be searched for in xwiki.cfg).
     * @since 9.1.1
     */
    public XWikiPAMConfig(String userId, ConfigurationSource configurationSource)
    {
        this.memoryConfiguration = new HashMap<>();

        // Look for PAM parameters first in the XWikiPreferences document from the current wiki
        this.configurationSource = configurationSource;

        this.cfgConfigurationSource = Utils.getComponent(ConfigurationSource.class, "xwikicfg");

        this.finalMemoryConfiguration = new HashMap<>();

        if (userId != null) {
            parseRemoteUser(userId);
        }
    }

    /**
     * @return the custom configuration. Can be used to override any property.
     * @since 9.0
     */
    public Map<String, String> getMemoryConfiguration()
    {
        return this.memoryConfiguration;
    }

    private void parseRemoteUser(String ssoRemoteUser)
    {
        this.memoryConfiguration.put("auth.input", ssoRemoteUser);
        this.memoryConfiguration.put("uid", ssoRemoteUser.trim());

        Pattern remoteUserParser = getRemoteUserPattern();

        LOGGER.debug("remoteUserParser: {}", remoteUserParser);

        if (remoteUserParser != null) {
            Matcher marcher = remoteUserParser.matcher(ssoRemoteUser);

            if (marcher.find()) {
                int groupCount = marcher.groupCount();
                if (groupCount == 0) {
                    this.memoryConfiguration.put("uid", marcher.group().trim());
                } else {
                    for (int g = 1; g <= groupCount; ++g) {
                        String groupValue = marcher.group(g);

                        List<String> remoteUserMapping = getRemoteUserMapping(g);

                        for (String configName : remoteUserMapping) {
                            this.memoryConfiguration.put(configName, convertRemoteUserMapping(configName, groupValue));
                        }
                    }
                }
            }
        }
    }

    private String convertRemoteUserMapping(String propertyName, String propertyValue)
    {
        Map<String, String> hostConvertor = getRemoteUserMapping(propertyName, true);

        LOGGER.debug("hostConvertor: {}", hostConvertor);

        String converted = hostConvertor.get(propertyValue.toLowerCase());

        return converted != null ? converted : propertyValue;
    }

    /**
     * Try to find the configuration in the following order:
     * <ul>
     * <li>Local configuration stored in this {@link XWikiPAMConfig} instance (pam_*name*)</li>
     * <li>XWiki Preferences page (pam_*name*)</li>
     * <li>xwiki.cfg configuration file (pam.*name*)</li>
     * <li>A final configuration that could be overriden by extended authenticators</li>
     * </ul>
     *
     * @param name the name of the property in XWikiPreferences.
     * @param cfgName the name of the property in xwiki.cfg.
     * @param def default value.
     * @return the value of the property.
     * @since 9.1.1
     */
    public String getPAMParam(String name, String cfgName, String def)
    {
        if (this.memoryConfiguration.containsKey(name)) {
            return this.memoryConfiguration.get(name);
        }

        // First look for the parameter in the defined configuration source (by default in XWikiPreferences document
        // from the current wiki).
        String param = this.configurationSource.getProperty(name, String.class);

        // If not found, check in xwiki.cfg
        if (param == null || "".equals(param)) {
            param = this.cfgConfigurationSource.getProperty(cfgName);
        }

        if (param == null) {
            param = this.finalMemoryConfiguration.get(name);
        }

        if (param == null) {
            param = def;
        }

        return param;
    }

    /**
     * First try to retrieve value from XWiki Preferences and then from xwiki.cfg Syntax pam_*name* (for XWiki
     * Preferences) will be changed to pam.*name* for xwiki.cfg.
     *
     * @param name the name of the property in XWikiPreferences.
     * @param def default value.
     * @return the value of the property.
     * @since 9.1.1
     */
    public String getPAMParam(String name, String def)
    {
        return getPAMParam(name, name.replaceFirst(PREF_PAM_SUFFIX, CFG_PAM_SUFFIX), def);
    }

    /**
     * First try to retrieve value from XWiki Preferences and then from xwiki.cfg Syntax pam_*name* (for XWiki
     * Preferences) will be changed to pam.*name* for xwiki.cfg.
     *
     * @param name the name of the property in XWikiPreferences.
     * @param cfgName the name of the property in xwiki.cfg.
     * @param def default value.
     * @return the value of the property.
     * @since 9.1.1
     */
    public long getPAMParamAsLong(String name, String cfgName, long def)
    {
        String paramStr = getPAMParam(name, name.replace(PREF_PAM_SUFFIX, CFG_PAM_SUFFIX), String.valueOf(def));

        long value;

        try {
            value = Long.valueOf(paramStr);
        } catch (Exception e) {
            value = def;
        }

        return value;
    }

    /**
     * First try to retrieve value from XWiki Preferences and then from xwiki.cfg Syntax pam_*name* (for XWiki
     * Preferences) will be changed to pam.*name* for xwiki.cfg.
     *
     * @param name the name of the property in XWikiPreferences.
     * @param def default value.
     * @return the value of the property.
     * @since 9.1.1
     */
    public long getPAMParamAsLong(String name, long def)
    {
        return getPAMParamAsLong(name, name.replace(PREF_PAM_SUFFIX, CFG_PAM_SUFFIX), def);
    }

    /**
     * @return true if PAM is enabled.
     * @since 9.1.1
     */
    public boolean isPAMEnabled()
    {
        String param = getPAMParam("pam", "xwiki.authentication.pam", "0");

        return param != null && param.equals("1");
    }

    /**
     * Get mapping between XWiki groups names and PAM groups names.
     *
     * @return the mapping between XWiki users and PAM users. The key is the XWiki group, and the value is the list of
     *         mapped PAM groups.
     * @since 9.1.1
     */
    public Map<String, Set<String>> getGroupMappings()
    {
        String param = getPAMParam("pam_group_mapping", "");

        Map<String, Set<String>> groupMappings = new HashMap<String, Set<String>>();

        if (param.trim().length() > 0) {
            char[] buffer = param.trim().toCharArray();
            boolean escaped = false;
            StringBuilder mapping = new StringBuilder(param.length());
            for (int i = 0; i < buffer.length; ++i) {
                char c = buffer[i];

                if (escaped) {
                    mapping.append(c);
                    escaped = false;
                } else {
                    if (c == '\\') {
                        escaped = true;
                    } else if (c == '|') {
                        addGroupMapping(mapping.toString(), groupMappings);
                        mapping.setLength(0);
                    } else {
                        mapping.append(c);
                    }
                }
            }

            if (mapping.length() > 0) {
                addGroupMapping(mapping.toString(), groupMappings);
            }
        }

        return groupMappings;
    }

    /**
     * @param mapping the mapping to parse
     * @param groupMappings the map to add parsed group mapping to
     */
    private void addGroupMapping(String mapping, Map<String, Set<String>> groupMappings)
    {
        int splitIndex = mapping.indexOf('=');

        if (splitIndex < 1) {
            LOGGER.error("Error parsing pam_group_mapping attribute [{}]", mapping);
        } else {
            String xwikigroup = mapping.substring(0, splitIndex);
            String pamgroup = mapping.substring(splitIndex + 1);

            Set<String> pamGroups = groupMappings.get(xwikigroup);

            if (pamGroups == null) {
                pamGroups = new HashSet<String>();
                groupMappings.put(xwikigroup, pamGroups);
            }

            pamGroups.add(pamgroup);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Groupmapping found [{}] [{}]", xwikigroup, pamGroups);
            }
        }
    }

    /**
     * Get mapping between XWiki users attributes and PAM users attributes. The key in the Map is lower cased to easily
     * support any case.
     *
     * @param attrListToFill the list to fill with extracted PAM fields to use in PAM search.
     * @return the mapping between XWiki groups and PAM groups.
     * @since 9.1.1
     */
    public Map<String, String> getUserMappings(List<String> attrListToFill)
    {
        Map<String, String> userMappings = new HashMap<>();

        String pamFieldMapping = getPAMParam("pam_fields_mapping", null);

        if (pamFieldMapping != null && pamFieldMapping.length() > 0) {
            String[] fields = pamFieldMapping.split(USERMAPPING_SEP);

            for (int j = 0; j < fields.length; j++) {
                String[] field = fields[j].split(USERMAPPING_XWIKI_PAM_LINK);
                if (2 == field.length) {
                    String xwikiattr = field[0].replace(" ", "");
                    String pamattr = field[1].replace(" ", "");

                    userMappings.put(pamattr.toLowerCase(), xwikiattr);

                    if (attrListToFill != null) {
                        attrListToFill.add(pamattr);
                    }
                } else {
                    LOGGER.error("Error parsing PAM fields mapping attribute from configuration, got [{}]", fields[j]);
                }
            }
        }

        return userMappings;
    }

    /**
     * @return the maximum number of milliseconds the client waits for any operation under these constraints to
     *         complete.
     * @since 9.1.1
     */
    public int getPAMTimeout()
    {
        return (int) getPAMParamAsLong("pam_timeout", 1000);
    }

    /**
     * @param name the name of the property in XWikiPreferences.
     * @param def the default value
     * @return the configuration value as {@link List}
     * @since 9.1.1
     */
    public List<String> getPAMListParam(String name, List<String> def)
    {
        return getPAMListParam(name, ',', def);
    }

    /**
     * @param name the name of the property in XWikiPreferences.
     * @param separator the separator used to cut each element of the list
     * @param def the default value
     * @return the configuration value as {@link List}
     * @since 9.1.1
     */
    public List<String> getPAMListParam(String name, char separator, List<String> def)
    {
        List<String> list = def;

        String str = getPAMParam(name, null);

        if (str != null) {
            if (!StringUtils.isEmpty(str)) {
                list = splitParam(str, separator);
            } else {
                list = Collections.emptyList();
            }
        }

        return list;
    }

    /**
     * @param name the name of the property in XWikiPreferences.
     * @param def the default value
     * @param forceLowerCaseKey
     * @return the configuration value as {@link Map}
     * @since 9.1.1
     */
    public Map<String, String> getPAMMapParam(String name, Map<String, String> def, boolean forceLowerCaseKey)
    {
        return getPAMMapParam(name, '|', def, forceLowerCaseKey);
    }

    /**
     * @param name the name of the property in XWikiPreferences.
     * @param separator the separator used to cut each element of the list
     * @param def the default value
     * @param forceLowerCaseKey
     * @return the configuration value as {@link Map}
     * @since 9.1.1
     */
    public Map<String, String> getPAMMapParam(String name, char separator, Map<String, String> def,
        boolean forceLowerCaseKey)
    {
        Map<String, String> mappings = def;

        List<String> list = getPAMListParam(name, separator, null);

        if (list != null) {
            if (list.isEmpty()) {
                mappings = Collections.emptyMap();
            } else {
                mappings = new LinkedHashMap<>();

                for (String fieldStr : list) {
                    int index = fieldStr.indexOf('=');
                    if (index != -1) {
                        String key = fieldStr.substring(0, index);
                        String value = index + 1 == fieldStr.length() ? "" : fieldStr.substring(index + 1);

                        mappings.put(forceLowerCaseKey ? key.toLowerCase() : key, value);
                    } else {
                        LOGGER.warn("Error parsing PAM [{}] attribute from configuration, got [{}]", name, fieldStr);
                    }
                }
            }
        }

        return mappings;
    }

    private List<String> splitParam(String text, char delimiter)
    {
        List<String> tokens = new ArrayList<>();
        boolean escaped = false;
        StringBuilder sb = new StringBuilder();

        for (char ch : text.toCharArray()) {
            if (escaped) {
                sb.append(ch);
                escaped = false;
            } else if (ch == delimiter) {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.delete(0, sb.length());
                }
            } else if (ch == '\\') {
                escaped = true;
            } else {
                sb.append(ch);
            }
        }

        if (sb.length() > 0) {
            tokens.add(sb.toString());
        }

        return tokens;
    }

    /**
     * @return a Java regexp used to parse the remote user provided by JAAS.
     * @since 9.1.1
     */
    public Pattern getRemoteUserPattern()
    {
        String param = getPAMParam("pam_remoteUserParser", null);

        return param != null ? Pattern.compile(param) : null;
    }

    /**
     * @param groupId the identifier of the group matched by the REMOTE_USER regexp
     * @return the properties associated to the passed group
     * @since 9.1.1
     */
    public List<String> getRemoteUserMapping(int groupId)
    {
        return getPAMListParam("pam_remoteUserMapping." + groupId, ',', Collections.<String>emptyList());
    }

    /**
     * @param propertyName the name of the property
     * @param forceLowerCaseKey if true the keys will be stored lowered cased in the {@link Map}
     * @return the mapping (the value for each domain) associated to the passed property
     * @since 9.1.1
     */
    public Map<String, String> getRemoteUserMapping(String propertyName, boolean forceLowerCaseKey)
    {
        return getPAMMapParam("pam_remoteUserMapping." + propertyName, '|', Collections.<String, String>emptyMap(),
            forceLowerCaseKey);
    }

    /**
     * @return an HTTP header that could be used to retrieve the authenticated user (only in xwiki.cfg).
     * @since 9.1.1
     */
    public String getHttpHeader()
    {
        return this.cfgConfigurationSource.getProperty("xwiki.authentication.pam.httpHeader");
    }

    /**
     * @param key the key added to the map
     * @param value the value of the key added to the map
     * @since 9.2
     */
    @Unstable
    public void setFinalProperty(String key, String value)
    {
        this.finalMemoryConfiguration.put(key, value);
    }
}
