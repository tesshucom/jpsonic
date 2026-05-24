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

package com.tesshu.jpsonic.feature.auth.rememberme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.infrastructure.settings.SettingsStagingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RememberMeStagingApplierTest {

    private ArgumentCaptor<Boolean> enableCaptor;
    private ArgumentCaptor<Integer> typeCaptor;
    private ArgumentCaptor<Integer> rotationPeriodCaptor;
    private ArgumentCaptor<Integer> tokenPeriodCaptor;
    private ArgumentCaptor<Boolean> slidingEnabled;

    private SettingsStagingPort port;

    @BeforeEach
    void setup() {
        enableCaptor = ArgumentCaptor.forClass(Boolean.class);
        typeCaptor = ArgumentCaptor.forClass(Integer.class);
        rotationPeriodCaptor = ArgumentCaptor.forClass(Integer.class);
        tokenPeriodCaptor = ArgumentCaptor.forClass(Integer.class);
        slidingEnabled = ArgumentCaptor.forClass(Boolean.class);

        port = SettingsFacadeBuilder
            .create()
            .captureBoolean(RMSKeys.enable, enableCaptor)
            .captureInt(RMSKeys.rotationType, typeCaptor)
            .captureInt(RMSKeys.rotationPeriod, rotationPeriodCaptor)
            .captureInt(RMSKeys.tokenValidityPeriod, tokenPeriodCaptor)
            .captureBoolean(RMSKeys.slidingExpirationEnable, slidingEnabled)
            .build();
    }

    @Test
    void enableFalseUsesDefaults() {
        RememberMeFormImpl form = new RememberMeFormImpl();
        form.setRememberMeEnable(false);

        new RememberMeStagingApplier().apply(form, port);

        assertEquals(false, enableCaptor.getValue());
        assertEquals(RMSKeys.rotationType.defaultValue(), typeCaptor.getValue());
        assertEquals(RMSKeys.rotationPeriod.defaultValue(), rotationPeriodCaptor.getValue());
        assertEquals(RMSKeys.tokenValidityPeriod.defaultValue(), tokenPeriodCaptor.getValue());
        assertEquals(RMSKeys.slidingExpirationEnable.defaultValue(), slidingEnabled.getValue());
    }

    @Test
    void enableTrueWithPeriodUsesFormValues() {
        RememberMeFormImpl form = new RememberMeFormImpl();
        form.setRememberMeEnable(true);
        form.setRememberMeKeyRotationType(KeyRotationType.PERIOD);
        form.setRememberMeKeyRotationPeriod(KeyRotationPeriod.DAILY);
        form.setRememberMeTokenValidityPeriod(TokenValidityPeriod.MONTHLY);
        form.setSlidingExpirationEnabled(true);

        new RememberMeStagingApplier().apply(form, port);

        assertEquals(true, enableCaptor.getValue());
        assertEquals(KeyRotationType.PERIOD.value(), typeCaptor.getValue());
        assertEquals(KeyRotationPeriod.DAILY.value(), rotationPeriodCaptor.getValue());
        assertEquals(true, slidingEnabled.getValue());
    }

    @Test
    void enableTrueNonPeriodResetsPeriodToDefault() {
        RememberMeFormImpl form = new RememberMeFormImpl();
        form.setRememberMeEnable(true);
        form.setRememberMeKeyRotationType(KeyRotationType.RESTART);
        form.setRememberMeTokenValidityPeriod(TokenValidityPeriod.HALF_YEAR);
        form.setRememberMeKeyRotationPeriod(KeyRotationPeriod.DAILY);
        form.setRememberMeTokenValidityPeriod(TokenValidityPeriod.HALF_YEAR);
        form.setSlidingExpirationEnabled(true);

        new RememberMeStagingApplier().apply(form, port);

        assertEquals(true, enableCaptor.getValue());
        assertEquals(KeyRotationType.RESTART.value(), typeCaptor.getValue());
        assertEquals(RMSKeys.rotationPeriod.defaultValue(), rotationPeriodCaptor.getValue());
        assertEquals(TokenValidityPeriod.HALF_YEAR.value(), tokenPeriodCaptor.getValue());
    }
    
    @SuppressWarnings("PMD.PublicMemberInNonPublicType")
    class RememberMeFormImpl implements RememberMeForm {

        private boolean rememberMeEnable;
        private KeyRotationType rememberMeKeyRotationType;
        private KeyRotationPeriod rememberMeKeyRotationPeriod;
        private TokenValidityPeriod rememberMeTokenValidityPeriod;
        private boolean slidingExpirationEnabled;
        private String rememberMeLastUpdate;

        public boolean isRememberMeEnable() {
            return rememberMeEnable;
        }

        void setRememberMeEnable(boolean rememberMeEnable) {
            this.rememberMeEnable = rememberMeEnable;
        }

        public KeyRotationType getRememberMeKeyRotationType() {
            return rememberMeKeyRotationType;
        }

        void setRememberMeKeyRotationType(KeyRotationType rememberMeKeyRotationType) {
            this.rememberMeKeyRotationType = rememberMeKeyRotationType;
        }

        public KeyRotationPeriod getRememberMeKeyRotationPeriod() {
            return rememberMeKeyRotationPeriod;
        }

        void setRememberMeKeyRotationPeriod(KeyRotationPeriod rememberMeKeyRotationPeriod) {
            this.rememberMeKeyRotationPeriod = rememberMeKeyRotationPeriod;
        }

        public TokenValidityPeriod getRememberMeTokenValidityPeriod() {
            return rememberMeTokenValidityPeriod;
        }

        void setRememberMeTokenValidityPeriod(
                TokenValidityPeriod rememberMeTokenValidityPeriod) {
            this.rememberMeTokenValidityPeriod = rememberMeTokenValidityPeriod;
        }

        public boolean isSlidingExpirationEnabled() {
            return slidingExpirationEnabled;
        }

        void setSlidingExpirationEnabled(boolean slidingExpirationEnabled) {
            this.slidingExpirationEnabled = slidingExpirationEnabled;
        }

        public String getRememberMeLastUpdate() {
            return rememberMeLastUpdate;
        }

        void setRememberMeLastUpdate(String rememberMeLastUpdate) {
            this.rememberMeLastUpdate = rememberMeLastUpdate;
        }
    }
            
            
}
