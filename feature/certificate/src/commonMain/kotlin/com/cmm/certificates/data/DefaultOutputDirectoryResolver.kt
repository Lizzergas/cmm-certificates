package com.cmm.certificates.data

import com.cmm.certificates.OutputDirectory
import com.cmm.certificates.domain.port.OutputDirectoryResolver

class DefaultOutputDirectoryResolver : OutputDirectoryResolver {
    override fun resolve(path: String): String = OutputDirectory.resolve(path)

    override fun ensureExists(path: String): Boolean = OutputDirectory.ensureExists(path)

    override fun canWrite(path: String): Boolean = OutputDirectory.canWrite(path)
}
