/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.openflowplugin.impl.mastership;

import com.google.common.util.concurrent.FutureCallback;
import javax.annotation.Nonnull;
import org.opendaylight.openflowplugin.api.openflow.device.DeviceInfo;
import org.opendaylight.openflowplugin.api.openflow.mastership.MastershipChangeRegistration;
import org.opendaylight.openflowplugin.api.openflow.mastership.MastershipChangeService;
import org.opendaylight.openflowplugin.api.openflow.mastership.MastershipChangeServiceManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.rf.state.rev170713.ResultState;

public class MastershipServiceDelegate implements MastershipChangeService, MastershipChangeRegistration {

    private final MastershipChangeService service;
    private final MastershipChangeServiceManager manager;

    MastershipServiceDelegate(final MastershipChangeService service,
                              final MastershipChangeServiceManager manager) {
        this.service = service;
        this.manager = manager;
    }

    @Override
    public void onBecomeOwner(@Nonnull final DeviceInfo deviceInfo) {
        this.service.onBecomeOwner(deviceInfo);
    }

    @Override
    public void onLoseOwnership(@Nonnull final DeviceInfo deviceInfo) {
        this.service.onLoseOwnership(deviceInfo);
    }

    @Override
    public void onDevicePrepared(@Nonnull final DeviceInfo deviceInfo,
                                 @Nonnull final FutureCallback<ResultState> callback) {
        this.service.onDevicePrepared(deviceInfo, callback);
    }

    @Override
    public void close() throws Exception {
        this.manager.unregister(this.service);
        this.service.close();
    }
}
