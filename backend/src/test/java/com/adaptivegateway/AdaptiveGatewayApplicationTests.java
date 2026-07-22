package com.adaptivegateway;

import org.junit.jupiter.api.Test;

class AdaptiveGatewayApplicationTests {

    @Test
    void applicationClassIsLoadable() {
        AdaptiveGatewayApplication application = new AdaptiveGatewayApplication();
        org.assertj.core.api.Assertions.assertThat(application).isNotNull();
    }
}
