/**
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
package org.smartdata.hdfs.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.UnresolvedLinkException;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.DFSInputStream;
import org.apache.hadoop.hdfs.SmartInputStreamFactory;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartdata.SmartConstants;
import org.smartdata.client.SmartClient;
import org.smartdata.metrics.FileAccessEvent;
import org.smartdata.model.FileState;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

public class SmartDFSClient extends DFSClient {
  private static final Logger LOG = LoggerFactory.getLogger(SmartDFSClient.class);
  private SmartClient smartClient = null;
  private boolean healthy = false;

  public SmartDFSClient(InetSocketAddress nameNodeAddress, Configuration conf,
      InetSocketAddress smartServerAddress) throws IOException {
    super(nameNodeAddress, conf);
    if (isSmartClientDisabled()) {
      return;
    }
    try {
      smartClient = new SmartClient(conf, smartServerAddress);
      healthy = true;
    } catch (IOException e) {
      super.close();
      throw e;
    }
  }

  public SmartDFSClient(final URI nameNodeUri, final Configuration conf,
      final InetSocketAddress smartServerAddress) throws IOException {
    super(nameNodeUri, conf);
    if (isSmartClientDisabled()) {
      return;
    }
    try {
      smartClient = new SmartClient(conf, smartServerAddress);
      healthy = true;
    } catch (IOException e) {
      super.close();
      throw e;
    }
  }

  public SmartDFSClient(URI nameNodeUri, Configuration conf,
      FileSystem.Statistics stats, InetSocketAddress smartServerAddress)
      throws IOException {
    super(nameNodeUri, conf, stats);
    if (isSmartClientDisabled()) {
      return;
    }
    try {
      smartClient = new SmartClient(conf, smartServerAddress);
      healthy = true;
    } catch (IOException e) {
      super.close();
      throw e;
    }
  }

  public SmartDFSClient(Configuration conf,
      InetSocketAddress smartServerAddress) throws IOException {
    super(conf);
    if (isSmartClientDisabled()) {
      return;
    }
    try {
      smartClient = new SmartClient(conf, smartServerAddress);
      healthy = true;
    } catch (IOException e) {
      super.close();
      throw e;
    }
  }

  public SmartDFSClient(Configuration conf) throws IOException {
    super(conf);
    if (isSmartClientDisabled()) {
      return;
    }
    try {
      smartClient = new SmartClient(conf);
      healthy = true;
    } catch (IOException e) {
      super.close();
      throw e;
    }
  }

  @Override
  public DFSInputStream open(String src)
      throws IOException, UnresolvedLinkException {
    return super.open(src);
  }

  @Override
  public DFSInputStream open(String src, int buffersize,
      boolean verifyChecksum)
      throws IOException, UnresolvedLinkException {
    FileState fileState = smartClient.getFileState(src);
    if (fileState.getFileStage().equals(FileState.FileStage.PROCESSING)) {
      throw new IOException("Cannot open " + src + " when it is under PROCESSING to "
          + fileState.getFileType());
    }
    DFSInputStream is = SmartInputStreamFactory.get().create(this, src,
        verifyChecksum, fileState);
    reportFileAccessEvent(src);
    return is;
  }

  /*@Override
  public LocatedBlocks getLocatedBlocks(String src, long start, long length)
      throws IOException {
    if (!isFileCompressed(src)) {
      return super.getLocatedBlocks(src, start, length);
    }

    CompressionFileState compressionInfo = smartClient.getFileCompressionInfo(src);
    Long[] originalPos = compressionInfo.getOriginalPos().toArray(new Long[0]);
    Long[] compressedPos = compressionInfo.getCompressedPos().toArray(new Long[0]);
    int startIndex = compressionInfo.getPosIndexByOriginalOffset(start);
    int endIndex = compressionInfo.getPosIndexByOriginalOffset(start + length - 1);
    long compressedStart = compressedPos[startIndex];
    long compressedLength = 0;
    if (endIndex < compressedPos.length - 1) {
      compressedLength = compressedPos[endIndex + 1] - compressedStart;
    } else {
      compressedLength = compressionInfo.getCompressedLength() - compressedStart;
    }

    LocatedBlocks originalLocatedBlocks = super.getLocatedBlocks(src, compressedStart, compressedLength);

    List<LocatedBlock> blocks = new ArrayList<>();
    for (LocatedBlock block : originalLocatedBlocks.getLocatedBlocks()) {

      blocks.add(new LocatedBlock(
          block.getBlock(),
          block.getLocations(),
          block.getStorageIDs(),
          block.getStorageTypes(),
          compressionInfo.getPosIndexByCompressedOffset(block.getStartOffset()),
          block.isCorrupt(),
          block.getCachedLocations()
      ));
    }
    LocatedBlock lastLocatedBlock = originalLocatedBlocks.getLastLocatedBlock();
    long fileLength = compressionInfo.getOriginalLength();

    return new LocatedBlocks(fileLength,
        originalLocatedBlocks.isUnderConstruction(),
        blocks,
        lastLocatedBlock,
        originalLocatedBlocks.isLastBlockComplete(),
        originalLocatedBlocks.getFileEncryptionInfo());
  }*/

  /*
  // Not complete
  @Override
  public BlockLocation[] getBlockLocations(String src, long start,
      long length) throws IOException, UnresolvedLinkException {
    if (!isFileCompressed(src)) {
      return super.getBlockLocations(src, start, length);
    }
    BlockLocation[] blockLocations = super.getBlockLocations(src, start, length);
    return null;
  }

  @Override
  public DirectoryListing listPaths(String src, byte[] startAfter,
      boolean needLocation) throws IOException {
    if (!isFileCompressed(src)) {
      return super.listPaths(src, startAfter, needLocation);
    }
    return null;
  }

  @Override
  public HdfsFileStatus getFileInfo(String src) throws IOException {
    if (!isFileCompressed(src)) {
      return super.getFileInfo(src);
    }
    return null;
  }
  */

  @Deprecated
  @Override
  public DFSInputStream open(String src, int buffersize,
      boolean verifyChecksum, FileSystem.Statistics stats)
      throws IOException, UnresolvedLinkException {
    return super.open(src, buffersize, verifyChecksum, stats);
  }

  private void reportFileAccessEvent(String src) {
    try {
      if (!healthy) {
        return;
      }
      String userName;
      try {
        userName = UserGroupInformation.getCurrentUser().getUserName();
      } catch (IOException e) {
        userName = "Unknown";
      }
      smartClient.reportFileAccessEvent(new FileAccessEvent(src, userName));
    } catch (IOException e) {
      // Here just ignores that failed to report
      LOG.error("Cannot report file access event to SmartServer: " + src
          + " , for: " + e.getMessage()
          + " , report mechanism will be disabled now in this instance.");
      healthy = false;
    }
  }

  @Override
  public synchronized void close() throws IOException {
    try {
      super.close();
    } catch (IOException e) {
      throw e;
    } finally {
      try {
        if (smartClient != null) {
          smartClient.close();
        }
      } finally {
        healthy = false;
      }
    }
  }

  public SmartClient getSmartClient() {
    return smartClient;
  }

  private boolean isSmartClientDisabled() {
    File idFile = new File(SmartConstants.SMART_CLIENT_DISABLED_ID_FILE);
    return idFile.exists();
  }
}
