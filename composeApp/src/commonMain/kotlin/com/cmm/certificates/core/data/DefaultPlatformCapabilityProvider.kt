package com.cmm.certificates.core.data

import com.cmm.certificates.core.domain.PlatformCapabilityProvider
import com.cmm.certificates.core.domain.getAppCapabilities

class DefaultPlatformCapabilityProvider : PlatformCapabilityProvider {
    override val capabilities = getAppCapabilities()
}
