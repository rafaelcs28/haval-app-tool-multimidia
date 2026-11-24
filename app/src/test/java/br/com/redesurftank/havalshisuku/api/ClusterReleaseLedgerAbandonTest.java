package br.com.redesurftank.havalshisuku.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ClusterReleaseLedgerAbandonTest {
    private static final class Value implements AutoCloseable {
        int closes;
        @Override public void close() { closes++; }
    }

    @Test
    public void quarantinedEntryIsAbandonedWithoutClosingAndAdmitsANewClaim() {
        ClusterReleaseLedger<Value> ledger = new ClusterReleaseLedger<>();
        Object oldService = new Object(), newService = new Object();
        Value old = new Value();
        ClusterReleaseLedger.Entry<Value> entry = ledger.claim(oldService, 1, 1, old);
        assertNull(ledger.claim(newService, 2, 2, new Value()));
        ledger.quarantine(entry);
        assertSame(old, ledger.abandonAfterOwnerDeath(entry));
        assertEquals(0, old.closes);
        assertNull(ledger.active());
        assertNotNull(ledger.claim(newService, 2, 2, new Value()));
    }

    @Test
    public void onlyAQuarantinedCurrentEntryCanBeAbandoned() throws Exception {
        ClusterReleaseLedger<Value> ledger = new ClusterReleaseLedger<>();
        Object service = new Object();
        ClusterReleaseLedger.Entry<Value> entry = ledger.claim(service, 1, 1, new Value());
        assertNull(ledger.abandonAfterOwnerDeath(entry));
        ledger.quarantine(entry);
        ledger.abandonAfterOwnerDeath(entry);
        assertNull(ledger.abandonAfterOwnerDeath(entry));
        ClusterReleaseLedger.Entry<Value> next = ledger.claim(service, 2, 2, new Value());
        assertFalse(ledger.isUncertain(next));
        assertNull(ledger.abandonAfterOwnerDeath(next));
    }
}
