package com.transferwise.common.gaffer.util;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class DummyXAResource implements XAResource {
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
    }

    @Override
    public void end(Xid xid, int flags) {
    }

    @Override
    public void forget(Xid xid) {
    }

    @Override
    public int getTransactionTimeout() {
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xares) {
        return false;
    }

    @Override
    public int prepare(Xid xid) {
        return 0;
    }

    @Override
    public Xid[] recover(int flag) {
        return null;
    }

    @Override
    public void rollback(Xid xid) throws XAException {
    }

    @Override
    public boolean setTransactionTimeout(int seconds) {
        return false;
    }

    @Override
    public void start(Xid xid, int flags) {
    }

}
