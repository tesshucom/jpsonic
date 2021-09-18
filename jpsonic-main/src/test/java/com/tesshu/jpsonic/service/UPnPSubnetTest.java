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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP") // It's test code!
class UPnPSubnetTest {

    private UPnPSubnet subnet;

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
    public void setup() {
        subnet = new UPnPSubnet();
    }

    @SubnetDecisions.Conditions.DlnaBaseLANURL.Null
    @SubnetDecisions.Results.False
    @Test
    void c01() {
        assertFalse(subnet.isInUPnPRange("dummy"));
    }

    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithHostName.InValid
    @SubnetDecisions.Results.False
    @Test
    void c02() {
        subnet.setDlnaBaseLANURL("http://sjhbfdkljhf.com");
        assertFalse(subnet.isInUPnPRange("dummy"));
    }

    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithHostName.Valid
    @SubnetDecisions.Conditions.Address.NotInRange
    @SubnetDecisions.Results.False
    @Test
    void c03() {
        subnet.setDlnaBaseLANURL("http://tesshu.com"); // 157.7.140.239
        assertFalse(subnet.isInUPnPRange("157.7.141.1"));
    }

    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithHostName.Valid
    @SubnetDecisions.Conditions.Address.InRange
    @SubnetDecisions.Results.True
    @Test
    void c04() {
        subnet.setDlnaBaseLANURL("http://tesshu.com");
        assertTrue(subnet.isInUPnPRange("157.7.140.1"));
    }

    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithIp
    @SubnetDecisions.Conditions.Address.NotInRange
    @SubnetDecisions.Results.False
    @Test
    void c05() {
        subnet.setDlnaBaseLANURL("http://192.168.1.5:8080/jpsonic");
        assertFalse(subnet.isInUPnPRange("192.168.2.4"));
    }

    @SubnetDecisions.Conditions.DlnaBaseLANURL.NotNull.WithIp
    @SubnetDecisions.Conditions.Address.InRange
    @SubnetDecisions.Results.True
    @Test
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
}
