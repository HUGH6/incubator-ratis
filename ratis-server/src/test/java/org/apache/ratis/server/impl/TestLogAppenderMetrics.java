/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ratis.server.impl;

import static org.apache.ratis.server.metrics.RaftLogMetrics.FOLLOWER_MATCH_INDEX;
import static org.apache.ratis.server.metrics.RaftLogMetrics.FOLLOWER_NEXT_INDEX;
import static org.apache.ratis.server.metrics.RaftLogMetrics.FOLLOWER_RPC_RESP_TIME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.ratis.metrics.RatisMetricRegistry;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftGroupMemberId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.server.metrics.LogAppenderMetrics;
import org.apache.ratis.util.Timestamp;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.Gauge;

public class TestLogAppenderMetrics {

  private RatisMetricRegistry ratisMetricRegistry;
  private RaftPeerId raftPeerId;
  private FollowerInfo followerInfo;

  @Before
  public void setup() {
    RaftGroupId raftGroupId = RaftGroupId.randomId();
    raftPeerId = RaftPeerId.valueOf("TestId");
    final RaftPeer raftPeer = RaftPeer.newBuilder().setId(raftPeerId).build();
    RaftGroupMemberId raftGroupMemberId = RaftGroupMemberId.valueOf(raftPeerId, raftGroupId);
    LogAppender logAppender = mock(LogAppender.class);
    followerInfo = new TestFollowerInfo(raftGroupMemberId, raftPeer, Timestamp.currentTime(), 100L, true, 1000);
    when(logAppender.getFollower()).thenReturn(followerInfo);
    LogAppenderMetrics logAppenderMetrics = new LogAppenderMetrics(raftGroupMemberId);
    ratisMetricRegistry = logAppenderMetrics.getRegistry();
    logAppenderMetrics.addFollowerGauges(followerInfo);
  }

  @Test
  public void testLogAppenderGauges() {
    Gauge nextIndex = ratisMetricRegistry.getGauges((s, metric) ->
        s.contains(String.format(FOLLOWER_NEXT_INDEX, raftPeerId.toString()))).values().iterator().next();
    Assert.assertEquals(100L, nextIndex.getValue());
    Gauge matchIndex = ratisMetricRegistry.getGauges((s, metric) ->
        s.contains(String.format(FOLLOWER_MATCH_INDEX, raftPeerId.toString()))).values().iterator().next();
    Assert.assertEquals(0L, matchIndex.getValue());
    Gauge rpcTime = ratisMetricRegistry.getGauges((s, metric) ->
        s.contains(String.format(FOLLOWER_RPC_RESP_TIME, raftPeerId.toString()))).values().iterator().next();
    Assert.assertTrue(((Long) rpcTime.getValue()) > 0);
    followerInfo.updateNextIndex(200L);
    followerInfo.updateMatchIndex(100L);
    followerInfo.updateLastRpcResponseTime();
    Assert.assertEquals(200L, nextIndex.getValue());
    Assert.assertEquals(100L, matchIndex.getValue());
    Assert.assertNotNull(rpcTime.getValue());
  }

  private static class TestFollowerInfo extends FollowerInfo {
    TestFollowerInfo(RaftGroupMemberId id, RaftPeer peer, Timestamp
        lastRpcTime, long nextIndex, boolean attendVote, int
        rpcSlownessTimeoutMs) {
      super(id, peer, lastRpcTime, nextIndex, attendVote, rpcSlownessTimeoutMs);
    }
  }
}

