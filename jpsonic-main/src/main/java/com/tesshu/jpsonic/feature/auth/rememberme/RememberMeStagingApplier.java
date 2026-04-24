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

import com.tesshu.jpsonic.controller.form.AdvancedSettingsCommand;
import com.tesshu.jpsonic.infrastructure.settings.SettingsStagingPort;
import com.tesshu.jpsonic.infrastructure.settings.StagingApplier;

public class RememberMeStagingApplier implements StagingApplier<AdvancedSettingsCommand> {

    @Override
    public void apply(AdvancedSettingsCommand form, SettingsStagingPort stagingPort) {
        stagingPort.staging(RMSKeys.enable, form.isRememberMeEnable());

        if (form.isRememberMeEnable()) {

            stagingPort.staging(RMSKeys.rotationType, form.getRememberMeKeyRotationType().value());
            stagingPort
                .staging(RMSKeys.tokenValidityPeriod,
                        form.getRememberMeTokenValidityPeriod().value());
            stagingPort.staging(RMSKeys.slidingExpirationEnable, form.isSlidingExpirationEnabled());

            if (form.getRememberMeKeyRotationType() == KeyRotationType.PERIOD) {
                stagingPort
                    .staging(RMSKeys.rotationPeriod, form.getRememberMeKeyRotationPeriod().value());
            } else {
                stagingPort.stagingDefault(RMSKeys.rotationPeriod);
            }

        } else {
            stagingPort
                .stagingDefault(RMSKeys.rotationType, RMSKeys.rotationPeriod,
                        RMSKeys.tokenValidityPeriod);
            stagingPort.stagingDefault(RMSKeys.slidingExpirationEnable);
        }
    }
}
