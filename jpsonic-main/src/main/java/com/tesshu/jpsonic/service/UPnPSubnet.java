
package com.tesshu.jpsonic.service;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class UPnPSubnet {

    private static final Logger LOG = LoggerFactory.getLogger(UPnPSubnet.class);
    private static final Pattern IP_ADDRESS_PATTERN = Pattern
            .compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
    private String dlnaBaseLANURL;
    private SubnetInfo subnetInfo;

    @SuppressWarnings("PMD.NullAssignment") // To clear the cache if the url changes
    void setDlnaBaseLANURL(String dlnaBaseLANURL) {
        this.dlnaBaseLANURL = dlnaBaseLANURL;
        subnetInfo = null;
    }

    public boolean isInUPnPRange(final String address) {
        if (isEmpty(address) || !IP_ADDRESS_PATTERN.matcher(address).matches() || isEmpty(dlnaBaseLANURL)) {
            return false;
        } else if (isEmpty(subnetInfo)) {
            try {
                URL url = new URL(dlnaBaseLANURL);
                String cidrNotation = (IP_ADDRESS_PATTERN.matcher(url.getHost()).matches() ? url.getHost()
                        : InetAddress.getByName(url.getHost()).getHostAddress()).concat("/24");
                subnetInfo = new SubnetUtils(cidrNotation).getInfo();
            } catch (MalformedURLException | UnknownHostException e) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Unable to get subnet from the dlna base lan URL.");
                }
            }
        }
        return subnetInfo.isInRange(address);
    }
}
