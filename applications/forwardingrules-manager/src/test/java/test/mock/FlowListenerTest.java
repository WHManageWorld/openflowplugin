/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package test.mock;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.openflowplugin.applications.frm.impl.DeviceMastershipManager;
import org.opendaylight.openflowplugin.applications.frm.impl.ForwardingRulesManagerImpl;
import org.opendaylight.openflowplugin.applications.reconciliation.ReconciliationManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.StaleFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.StaleFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.StaleFlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.FRMTest;
import test.mock.util.RpcProviderRegistryMock;
import test.mock.util.SalFlowServiceMock;

@RunWith(MockitoJUnitRunner.class)
public class FlowListenerTest extends FRMTest {
    private ForwardingRulesManagerImpl forwardingRulesManager;
    private static final NodeId NODE_ID = new NodeId("testnode:1");
    private static final NodeKey NODE_KEY = new NodeKey(NODE_ID);
    RpcProviderRegistry rpcProviderRegistryMock = new RpcProviderRegistryMock();
    TableKey tableKey = new TableKey((short) 2);
    @Mock
    ClusterSingletonServiceProvider clusterSingletonService;
    @Mock
    DeviceMastershipManager deviceMastershipManager;
    @Mock
    private NotificationProviderService notificationService;
    @Mock
    private ReconciliationManager reconciliationManager;

    @Before
    public void setUp() {
        forwardingRulesManager = new ForwardingRulesManagerImpl(getDataBroker(), rpcProviderRegistryMock, getConfig(),
                clusterSingletonService, notificationService, getConfigurationService(), reconciliationManager);

        forwardingRulesManager.start();
        // TODO consider tests rewrite (added because of complicated access)
        forwardingRulesManager.setDeviceMastershipManager(deviceMastershipManager);
        Mockito.when(deviceMastershipManager.isDeviceMastered(NODE_ID)).thenReturn(true);
    }

    @Test
    public void addTwoFlowsTest() throws Exception {
        addFlowCapableNode(NODE_KEY);

        FlowKey flowKey = new FlowKey(new FlowId("test_Flow"));
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        Table table = new TableBuilder().setKey(tableKey).setFlow(Collections.<Flow>emptyList()).build();
        Flow flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<AddFlowInput> addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(1, addFlowCalls.size());
        assertEquals("DOM-0", addFlowCalls.get(0).getTransactionUri().getValue());

        flowKey = new FlowKey(new FlowId("test_Flow2"));
        flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY).augmentation(FlowCapableNode.class)
                .child(Table.class, tableKey).child(Flow.class, flowKey);
        flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).build();
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(2, addFlowCalls.size());
        assertEquals("DOM-1", addFlowCalls.get(1).getTransactionUri().getValue());
        assertEquals(2, addFlowCalls.get(1).getTableId().intValue());
        assertEquals(flowII, addFlowCalls.get(1).getFlowRef().getValue());
    }

    @Test
    public void updateFlowTest() throws Exception {
        addFlowCapableNode(NODE_KEY);

        FlowKey flowKey = new FlowKey(new FlowId("test_Flow"));
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        Table table = new TableBuilder().setKey(tableKey).setFlow(Collections.<Flow>emptyList()).build();
        Flow flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<AddFlowInput> addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(1, addFlowCalls.size());
        assertEquals("DOM-0", addFlowCalls.get(0).getTransactionUri().getValue());

        flowKey = new FlowKey(new FlowId("test_Flow"));
        flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY).augmentation(FlowCapableNode.class)
                .child(Table.class, tableKey).child(Flow.class, flowKey);
        flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).setOutGroup((long) 5).build();
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<UpdateFlowInput> updateFlowCalls = salFlowService.getUpdateFlowCalls();
        assertEquals(1, updateFlowCalls.size());
        assertEquals("DOM-1", updateFlowCalls.get(0).getTransactionUri().getValue());
        assertEquals(flowII, updateFlowCalls.get(0).getFlowRef().getValue());
        assertEquals(Boolean.TRUE, updateFlowCalls.get(0).getOriginalFlow().isStrict());
        assertEquals(Boolean.TRUE, updateFlowCalls.get(0).getUpdatedFlow().isStrict());
    }

    @Test
    public void updateFlowScopeTest() throws Exception {
        addFlowCapableNode(NODE_KEY);

        FlowKey flowKey = new FlowKey(new FlowId("test_Flow"));
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        Table table = new TableBuilder().setKey(tableKey).setFlow(Collections.<Flow>emptyList()).build();
        IpMatch ipMatch = new IpMatchBuilder().setIpDscp(new Dscp((short) 4)).build();
        Match match = new MatchBuilder().setIpMatch(ipMatch).build();
        Flow flow = new FlowBuilder().setMatch(match).setKey(flowKey).setTableId((short) 2).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<AddFlowInput> addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(1, addFlowCalls.size());
        assertEquals("DOM-0", addFlowCalls.get(0).getTransactionUri().getValue());

        flowKey = new FlowKey(new FlowId("test_Flow"));
        flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY).augmentation(FlowCapableNode.class)
                .child(Table.class, tableKey).child(Flow.class, flowKey);
        ipMatch = new IpMatchBuilder().setIpDscp(new Dscp((short) 5)).build();
        match = new MatchBuilder().setIpMatch(ipMatch).build();
        flow = new FlowBuilder().setMatch(match).setKey(flowKey).setTableId((short) 2).build();
        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<UpdateFlowInput> updateFlowCalls = salFlowService.getUpdateFlowCalls();
        assertEquals(1, updateFlowCalls.size());
        assertEquals("DOM-1", updateFlowCalls.get(0).getTransactionUri().getValue());
        assertEquals(flowII, updateFlowCalls.get(0).getFlowRef().getValue());
        assertEquals(ipMatch, updateFlowCalls.get(0).getUpdatedFlow().getMatch().getIpMatch());
    }

    @Test
    public void deleteFlowTest() throws Exception {
        addFlowCapableNode(NODE_KEY);

        FlowKey flowKey = new FlowKey(new FlowId("test_Flow"));
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(Flow.class, flowKey);
        Table table = new TableBuilder().setKey(tableKey).setFlow(Collections.<Flow>emptyList()).build();
        Flow flow = new FlowBuilder().setKey(flowKey).setTableId((short) 2).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
        SalFlowServiceMock salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<AddFlowInput> addFlowCalls = salFlowService.getAddFlowCalls();
        assertEquals(1, addFlowCalls.size());
        assertEquals("DOM-0", addFlowCalls.get(0).getTransactionUri().getValue());

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, flowII);
        assertCommit(writeTx.submit());
        salFlowService = (SalFlowServiceMock) forwardingRulesManager.getSalFlowService();
        List<RemoveFlowInput> removeFlowCalls = salFlowService.getRemoveFlowCalls();
        assertEquals(1, removeFlowCalls.size());
        assertEquals("DOM-1", removeFlowCalls.get(0).getTransactionUri().getValue());
        assertEquals(flowII, removeFlowCalls.get(0).getFlowRef().getValue());
        assertEquals(Boolean.TRUE, removeFlowCalls.get(0).isStrict());
    }

    @Test
    public void staleMarkedFlowCreationTest() throws Exception {

        addFlowCapableNode(NODE_KEY);

        StaleFlowKey flowKey = new StaleFlowKey(new FlowId("stale_Flow"));
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
        InstanceIdentifier<StaleFlow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, NODE_KEY)
                .augmentation(FlowCapableNode.class).child(Table.class, tableKey).child(StaleFlow.class, flowKey);
        Table table = new TableBuilder().setKey(tableKey).setStaleFlow(Collections.<StaleFlow>emptyList()).build();
        StaleFlow flow = new StaleFlowBuilder().setKey(flowKey).setTableId((short) 2).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        assertCommit(writeTx.submit());
    }

    @After
    public void tearDown() throws Exception {
        forwardingRulesManager.close();
    }
}
