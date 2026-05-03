package com.ruoyi.yy;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class YyAiGatewayStreamTest {

    @Test
    void callStreamResult_defaultImpl_throwsUnsupportedOperation() {
        IYyAiGateway gateway = new IYyAiGateway() {
            @Override
            public YyAiResponse call(YyAiRequest request) {
                return YyAiResponse.ok("test", "qwen-turbo", 10, 5, 100);
            }
        };

        YyAiRequest request = new YyAiRequest();
        assertThrows(UnsupportedOperationException.class, () -> gateway.callStreamResult(request));
    }

    @Test
    void streamResult_closeReleasesResource() {
        boolean[] closed = {false};
        AutoCloseable resource = () -> closed[0] = true;
        IYyAiGateway.StreamResult result =
            new IYyAiGateway.StreamResult(Collections.emptyIterator(), resource);

        result.close();

        assertTrue(closed[0]);
    }

    @Test
    void streamResult_closeWithNullResource_noException() {
        IYyAiGateway.StreamResult result =
            new IYyAiGateway.StreamResult(Collections.emptyIterator(), null);

        assertDoesNotThrow(result::close);
    }
}
