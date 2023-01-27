package com.healthmetrix.connector.inbox.api.config

import com.healthmetrix.connector.commons.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class ProxyConfig(
    @Value("\${proxy.host:}")
    host: String,
    @Value("\${proxy.port:-1}")
    port: Int,
    @Value("\${proxy.user:}")
    username: String,
    @Value("\${proxy.password:}")
    password: String,
) {
    init {
        if (host.trim().isBlank() || portIsInvalid(port)) {
            logger.info("Egress Proxy: host is blank or port is invalid")
        } else {
            setProxyProp("Host", host)
            setProxyProp("Port", port.toString())

            if (username.trim().isNotBlank() && password.trim().isNotBlank()) {
                setProxyProp("User", username)
                setProxyProp("Password", password)
            }
        }
    }

    private fun setProxyProp(prop: String, value: String) {
        System.setProperty("https.proxy$prop", value)
        System.setProperty("http.proxy$prop", value)
        logger.info("Set systemProperty proxy$prop to $value")
    }

    private fun portIsInvalid(port: Int): Boolean {
        return port < 0 || port > 65535
    }
}
