/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.service.upnp;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.reflect.Field;

import org.apache.commons.lang3.exception.UncheckedException;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.AvoidUsingHardCodedIP" })
class UPnPSubnetTest {

    private UPnPSubnet subnet;

    @BeforeEach
    void setUp() {
        subnet = new UPnPSubnet();
        subnet.setDlnaBaseLANURL("http://192.168.1.10:4040");
    }

    @Documented
    private @interface SubnetDecisions {
        @interface Conditions {
            @interface DlnaBaseLANURL {
                @interface Null {
                }

                @interface NotNull {
                    @interface WithHostName {
                        @interface Valid {
                        }

                        @interface InValid {
                        }
                    }

                    @interface WithIp {
                    }
                }
            }

            @interface Address {
                @interface InRange {
                }

                @interface NotInRange {
                }
            }

        }

        @interface Results {
            @interface False {
            }

            @interface True {
            }
        }

    }

    @BeforeEach
    void setup() {
        subnet = new UPnPSubnet();
    }

    @Test
    @SubnetDecisions.Conditions.DlnaBaseLANURL.Null
    @SubnetDecisions.Results.False
    void c01() {
        assertFalse(subnet.isInUPnPRange("dummy"));
    }

    @Test
    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithHostName.InValid
    @SubnetDecisions.Results.False
    void c02() {
        subnet.setDlnaBaseLANURL("http://sjhbfdkljhf.com");
        assertFalse(subnet.isInUPnPRange("dummy"));
    }

    @Test
    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithHostName.InValid
    @SubnetDecisions.Conditions.Address.NotInRange
    @SubnetDecisions.Results.False
    void c021() {
        subnet.setDlnaBaseLANURL("http://sjhbfdkljhf.com");
        assertFalse(subnet.isInUPnPRange("20.27.178.1"));
    }

    @Test
    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithHostName.Valid
    @SubnetDecisions.Conditions.Address.NotInRange
    @SubnetDecisions.Results.False
    void c03() {
        subnet.setDlnaBaseLANURL("http://20.27.177.113");
        assertFalse(subnet.isInUPnPRange("20.27.178.1"));
    }

    @Test
    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithHostName.Valid
    @SubnetDecisions.Conditions.Address.InRange
    @SubnetDecisions.Results.True
    void c04() {
        subnet.setDlnaBaseLANURL("http://20.27.177.113");
        assertTrue(subnet.isInUPnPRange("20.27.177.1"));
    }

    @Test
    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithIp
    @SubnetDecisions.Conditions.Address.NotInRange
    @SubnetDecisions.Results.False
    void c05() {
        subnet.setDlnaBaseLANURL("http://192.168.1.5:8080/jpsonic");
        assertFalse(subnet.isInUPnPRange("192.168.2.4"));
    }

    @Test
    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithIp
    @SubnetDecisions.Conditions.Address.InRange
    @SubnetDecisions.Results.True
    void c06() {
        subnet.setDlnaBaseLANURL("http://192.168.1.5:8080/jpsonic");
        assertTrue(subnet.isInUPnPRange("192.168.1.2"));
    }

    @Test
    void testCashe() {
        subnet.setDlnaBaseLANURL("http://192.168.1.5:8080/jpsonic");
        assertTrue(subnet.isInUPnPRange("192.168.1.2"));
        assertTrue(subnet.isInUPnPRange("192.168.1.2"));
    }

    @Test
    void returnsFalseWhenAddressIsEmpty() {
        assertFalse(subnet.isInUPnPRange(""));
        assertFalse(subnet.isInUPnPRange(null));
    }

    @Test
    void returnsFalseWhenAddressIsNotIPv4() {
        assertFalse(subnet.isInUPnPRange("not-an-ip"));
    }

    @Test
    void throwsIllegalArgumentWhenAddressOctetOutOfRange() {
        subnet.setDlnaBaseLANURL("http://192.168.1.10:4040");

        assertThrows(IllegalArgumentException.class, () -> subnet.isInUPnPRange("999.999.999.999"));
    }

    @Test
    void returnsTrueWhenAddressIsInSame24Subnet() {
        subnet.setDlnaBaseLANURL("http://192.168.1.10:4040");
        assertTrue(subnet.isInUPnPRange("192.168.1.55"));
    }

    @Test
    void returnsFalseWhenAddressIsOutside24Subnet() {
        subnet.setDlnaBaseLANURL("http://192.168.1.10:4040");
        assertFalse(subnet.isInUPnPRange("192.168.2.1"));
    }

    @Test
    void returnsNPEWhenURLIsMalformed() {
        subnet.setDlnaBaseLANURL("::::://bad-url");

        assertThrows(NullPointerException.class, () -> subnet.isInUPnPRange("192.168.1.20"));
    }

    @Test
    void resolvesHostnameToIPAddress() {
        subnet.setDlnaBaseLANURL("http://localhost:4040");

        assertTrue(subnet.isInUPnPRange("127.0.0.5"));
        assertFalse(subnet.isInUPnPRange("192.168.1.1"));
    }

    @Test
    void subnetInfoIsAlwaysClearedWhenURLSet() {
        subnet.setDlnaBaseLANURL("http://192.168.1.10:4040");
        assertTrue(subnet.isInUPnPRange("192.168.1.20"));

        subnet.setDlnaBaseLANURL("http://192.168.1.10:4040");
        assertNull(getPrivateSubnetInfo(subnet), "URL set always clears cache");
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private SubnetInfo getPrivateSubnetInfo(UPnPSubnet s) {
        try {
            Field f = UPnPSubnet.class.getDeclaredField("subnetInfo");
            f.setAccessible(true);
            return (SubnetInfo) f.get(s);
        } catch (IllegalArgumentException | NoSuchFieldException | SecurityException
                | IllegalAccessException e) {
            throw new UncheckedException(e);
        }
    }
}
