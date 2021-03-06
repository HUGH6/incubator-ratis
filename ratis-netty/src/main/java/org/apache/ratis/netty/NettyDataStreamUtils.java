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
package org.apache.ratis.netty;

import org.apache.ratis.datastream.impl.DataStreamReplyByteBuffer;
import org.apache.ratis.datastream.impl.DataStreamRequestByteBuffer;
import org.apache.ratis.netty.server.DataStreamRequestByteBuf;
import org.apache.ratis.proto.RaftProtos.DataStreamReplyHeaderProto;
import org.apache.ratis.proto.RaftProtos.DataStreamRequestHeaderProto;
import org.apache.ratis.proto.RaftProtos.DataStreamPacketHeaderProto;
import org.apache.ratis.protocol.DataStreamPacketHeader;
import org.apache.ratis.protocol.DataStreamReplyHeader;
import org.apache.ratis.protocol.DataStreamRequestHeader;
import org.apache.ratis.thirdparty.io.netty.buffer.ByteBuf;
import org.apache.ratis.thirdparty.io.netty.buffer.ByteBufAllocator;
import org.apache.ratis.thirdparty.io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface NettyDataStreamUtils {

  static ByteBuffer getDataStreamRequestHeaderProtoByteBuf(DataStreamRequestByteBuffer request) {
    DataStreamPacketHeaderProto.Builder b = DataStreamPacketHeaderProto
        .newBuilder()
        .setStreamId(request.getStreamId())
        .setStreamOffset(request.getStreamOffset())
        .setType(request.getType())
        .setDataLength(request.getDataLength());
    return DataStreamRequestHeaderProto
        .newBuilder()
        .setPacketHeader(b)
        .build()
        .toByteString()
        .asReadOnlyByteBuffer();
  }

  static ByteBuffer getDataStreamReplyHeaderProtoByteBuf(DataStreamReplyByteBuffer reply) {
    DataStreamPacketHeaderProto.Builder b = DataStreamPacketHeaderProto
        .newBuilder()
        .setStreamId(reply.getStreamId())
        .setStreamOffset(reply.getStreamOffset())
        .setType(reply.getType())
        .setDataLength(reply.getDataLength());
    return DataStreamReplyHeaderProto
        .newBuilder()
        .setPacketHeader(b)
        .setBytesWritten(reply.getBytesWritten())
        .setSuccess(reply.isSuccess())
        .build()
        .toByteString()
        .asReadOnlyByteBuffer();
  }

  static void encodeDataStreamRequestByteBuffer(DataStreamRequestByteBuffer request, Consumer<ByteBuf> out,
      ByteBufAllocator allocator) {
    ByteBuffer headerBuf = getDataStreamRequestHeaderProtoByteBuf(request);
    final ByteBuf headerLenBuf = allocator.directBuffer(DataStreamPacketHeader.getSizeOfHeaderLen());
    headerLenBuf.writeInt(headerBuf.remaining());
    out.accept(headerLenBuf);
    out.accept(Unpooled.wrappedBuffer(headerBuf));
    out.accept(Unpooled.wrappedBuffer(request.slice()));
  }

  static void encodeDataStreamReplyByteBuffer(DataStreamReplyByteBuffer reply, Consumer<ByteBuf> out,
      ByteBufAllocator allocator) {
    ByteBuffer headerBuf = getDataStreamReplyHeaderProtoByteBuf(reply);
    final ByteBuf headerLenBuf = allocator.directBuffer(DataStreamPacketHeader.getSizeOfHeaderLen());
    headerLenBuf.writeInt(headerBuf.remaining());
    out.accept(headerLenBuf);
    out.accept(Unpooled.wrappedBuffer(headerBuf));
    out.accept(Unpooled.wrappedBuffer(reply.slice()));
  }

  static DataStreamRequestByteBuf decodeDataStreamRequestByteBuf(ByteBuf buf) {
    return Optional.ofNullable(DataStreamRequestHeader.read(buf))
        .map(header -> checkHeader(header, buf))
        .map(header -> new DataStreamRequestByteBuf(header, decodeData(buf, header, ByteBuf::retain)))
        .orElse(null);
  }

  static DataStreamReplyByteBuffer decodeDataStreamReplyByteBuffer(ByteBuf buf) {
    return Optional.ofNullable(DataStreamReplyHeader.read(buf))
        .map(header -> checkHeader(header, buf))
        .map(header -> new DataStreamReplyByteBuffer(header, decodeData(buf, header, ByteBuf::nioBuffer)))
        .orElse(null);
  }

  static <HEADER extends DataStreamPacketHeader> HEADER checkHeader(HEADER header, ByteBuf buf) {
    if (header == null) {
      return null;
    }
    if (buf.readableBytes() < header.getDataLength()) {
      buf.resetReaderIndex();
      return null;
    }
    return header;
  }

  static <DATA> DATA decodeData(ByteBuf buf, DataStreamPacketHeader header, Function<ByteBuf, DATA> toData) {
    final int dataLength = Math.toIntExact(header.getDataLength());
    final DATA data;
    if (dataLength > 0) {
      data = toData.apply(buf.slice(buf.readerIndex(), dataLength));
      buf.readerIndex(buf.readerIndex() + dataLength);
    } else {
      data = null;
    }
    buf.markReaderIndex();
    return data;
  }
}
