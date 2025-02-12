package com.transferwise.common.gaffer;

import com.transferwise.common.gaffer.util.Uid;
import java.nio.charset.StandardCharsets;
import javax.transaction.xa.Xid;

public class GafferXid implements Xid {

  private static final int FORMAT_ID = 0x47666672;

  private final Uid globalTransactionId;
  private final Uid branchQualifier;

  public GafferXid(Uid globalTransactionId, Uid branchQualifier) {
    this.globalTransactionId = globalTransactionId;
    this.branchQualifier = branchQualifier;
  }

  @Override
  public int getFormatId() {
    return FORMAT_ID;
  }

  @Override
  public byte[] getGlobalTransactionId() {
    return globalTransactionId.asBytes();
  }

  @Override
  public byte[] getBranchQualifier() {
    return branchQualifier.asBytes();
  }

  public static void main(String... args) {
    byte[] bytes = "Gffr".getBytes(StandardCharsets.UTF_8);
    StringBuilder sb = new StringBuilder("FORMAT_ID = 0x");
    for (int i = 0; i < 4; i++) {
      sb.append(bytes[i] / 16).append(bytes[i] % 16);
    }
    System.out.println(sb);
  }
}
