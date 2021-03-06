/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ratis.datastream.impl;

import org.apache.ratis.protocol.DataStreamReply;
import org.apache.ratis.protocol.DataStreamReplyHeader;
import org.apache.ratis.proto.RaftProtos.DataStreamPacketHeaderProto.Type;

import java.nio.ByteBuffer;

/**
 * Implements {@link DataStreamReply} with {@link ByteBuffer}.
 *
 * This class is immutable.
 */
public class DataStreamReplyByteBuffer extends DataStreamPacketByteBuffer implements DataStreamReply {
  private final long bytesWritten;
  private final boolean success;

  public DataStreamReplyByteBuffer(long streamId, long streamOffset, ByteBuffer buffer,
      long bytesWritten, boolean success, Type type) {
    super(streamId, streamOffset, buffer, type);

    this.success = success;
    this.bytesWritten = bytesWritten;
  }

  public DataStreamReplyByteBuffer(DataStreamReplyHeader header, ByteBuffer buffer) {
    this(header.getStreamId(), header.getStreamOffset(), buffer, header.getBytesWritten(), header.isSuccess(),
        header.getType());
  }

  @Override
  public long getBytesWritten() {
    return bytesWritten;
  }

  @Override
  public boolean isSuccess() {
    return success;
  }
}
