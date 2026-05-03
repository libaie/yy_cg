package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyAiRequest;
import com.ruoyi.yy.domain.YyAiResponse;
import com.ruoyi.yy.service.IYyAiGateway;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

    @Test
    void parseSseTokens_extractsTokensFromDataLines() {
        Iterator<String> lines = List.of(
            "data: {\"output\":{\"text\":\"Hello\"}}",
            "data: {\"output\":{\"text\":\" World\"}}",
            "data: [DONE]"
        ).iterator();

        Iterator<String> tokens = YyAiGatewayImpl.parseSseTokens(lines);

        assertTrue(tokens.hasNext());
        assertEquals("Hello", tokens.next());
        assertTrue(tokens.hasNext());
        assertEquals(" World", tokens.next());
        assertFalse(tokens.hasNext());
    }

    @Test
    void parseSseTokens_skipsMalformedLines() {
        Iterator<String> lines = List.of(
            "event: ping",
            "data: not-json",
            "data: {\"output\":{\"text\":\"Token\"}}",
            "data: [DONE]"
        ).iterator();

        Iterator<String> tokens = YyAiGatewayImpl.parseSseTokens(lines);

        assertTrue(tokens.hasNext());
        assertEquals("Token", tokens.next());
        assertFalse(tokens.hasNext());
    }

    @Test
    void parseSseTokens_emptyStream_returnsFalse() {
        Iterator<String> lines = Collections.emptyIterator();
        Iterator<String> tokens = YyAiGatewayImpl.parseSseTokens(lines);
        assertFalse(tokens.hasNext());
    }

    @Test
    void parseSseTokens_doneWithoutTokens_returnsFalse() {
        Iterator<String> lines = List.of("data: [DONE]").iterator();
        Iterator<String> tokens = YyAiGatewayImpl.parseSseTokens(lines);
        assertFalse(tokens.hasNext());
    }
}
