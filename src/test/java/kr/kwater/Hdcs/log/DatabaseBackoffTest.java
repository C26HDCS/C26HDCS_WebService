package kr.kwater.Hdcs.log;
import kr.kwater.hdcs.common.DatabaseBackoff;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;

class DatabaseBackoffTest {
    @Test void singleProbeExponentialCapAndRecovery() {
        AtomicLong time = new AtomicLong(-100);
        DatabaseBackoff backoff = new DatabaseBackoff(time::get);
        for (long expected : new long[]{1000,2000,4000,8000,16000,32000,60000,60000}) {
            assertThat(backoff.acquire()).isTrue();
            assertThat(backoff.acquire()).isFalse();
            assertThat(backoff.failure()).isEqualTo(expected);
            assertThat(backoff.acquire()).isFalse();
            time.addAndGet(expected*1_000_000);
        }
        assertThat(backoff.acquire()).isTrue();
        backoff.success();
        assertThat(backoff.acquire()).isTrue();
        assertThat(backoff.failure()).isEqualTo(1000);
    }
}
