/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.openflowplugin.api.openflow.lifecycle;

import javax.annotation.Nonnull;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.openflowplugin.api.openflow.OFPContext;
import org.opendaylight.openflowplugin.api.openflow.connection.ConnectionContext;
import org.opendaylight.openflowplugin.api.openflow.device.handlers.DeviceRemovedHandler;

/**
 * Chain of contexts, hold references to the contexts.
 * @since 0.4.0 Carbon
 */
public interface ContextChain extends ClusterSingletonService, AutoCloseable {

    /**
     * Add context to the chain, if reference already exist ignore it.
     * @param context child of OFPContext
     */
    <T extends OFPContext> void addContext(@Nonnull T context);

    @Override
    void close();

    /**
     * Slave was successfully set.
     */
    void makeContextChainStateSlave();

    /**
     * Registers context chain into cluster singleton service.
     * @param clusterSingletonServiceProvider provider
     */
    void registerServices(ClusterSingletonServiceProvider clusterSingletonServiceProvider);

    /**
     * After connect of device make this device SLAVE.
     */
    void makeDeviceSlave();

    /**
     * Check all needed to be master.
     * @param mastershipState - state master on device, initial gather, initial submit, initial registry fill
     * @return true if everything done fine
     */
    boolean isMastered(@Nonnull ContextChainMastershipState mastershipState);

    /**
     * Checks if context chain is currently closing.
     * @return true if context chain is closing
     */
    boolean isClosing();

    /**
     * Check all needed to be master except the device is written into data store.
     * Using by reconciliation framework.
     * Used only if {@link OwnershipChangeListener#isReconciliationFrameworkRegistered()}
     * is set to {@link Boolean#TRUE}.
     * @return true if all is set but not submitted txChain
     * @since 0.5.0 Nitrogen
     * @see org.opendaylight.openflowplugin.api.openflow.mastership.MastershipChangeService
     */
    boolean isPrepared();

    /**
     * Allow to continue after reconciliation framework callback success.
     * @return true if initial submitting was ok and device is fully mastered by controller
     * @since 0.5.0 Nitrogen
     * @see org.opendaylight.openflowplugin.api.openflow.mastership.MastershipChangeService
     * @see OwnershipChangeListener#isReconciliationFrameworkRegistered()
     */
    boolean continueInitializationAfterReconciliation();

    /**
     * Add new auxiliary connection if primary is ok.
     * @param connectionContext new connection to the device.
     * @return false if primary connection is broken
     */
    boolean addAuxiliaryConnection(@Nonnull ConnectionContext connectionContext);

    /**
     * Check if connection is auxiliary and if yes then continue working.
     * @param connectionContext connection to the device
     * @return false if this is primary connection
     */
    boolean auxiliaryConnectionDropped(@Nonnull ConnectionContext connectionContext);

    /**
     * This method registers device removed handler what will be executed when device should be removed.
     * @param deviceRemovedHandler device removed handler
     */
    void registerDeviceRemovedHandler(@Nonnull DeviceRemovedHandler deviceRemovedHandler);
}
